package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.Withdrawal;
import com.gaming.agricul.WithdrawalStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Conditional capital withdrawals (Q10 of the approved spec): a withdrawal can be blocked if
 * the project cannot absorb it (its value would fall below a configurable minimum), and can
 * cause the investor a financial loss via a configurable penalty rate. Validation requires a
 * different Admin/Manager than the one who requested it — same two-person pattern as profit
 * distributions (Q15).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final ContributionRepository contributionRepository;
    private final ProjectRepository projectRepository;
    private final OperationRepository operationRepository;
    private final ContributionService contributionService;
    private final FinancialParameterService financialParameterService;

    @Transactional(readOnly = true)
    public List<Withdrawal> findByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return withdrawalRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Withdrawal findById(Long id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Withdrawal not found with id: " + id));
    }

    public Withdrawal request(Long contributionId, BigDecimal amount, Long requestedByAppUserId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Withdrawal amount must be positive");
        }
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new NotFoundException("Contribution not found with id: " + contributionId));

        BigDecimal alreadyWithdrawn = withdrawalRepository.sumValidatedAmountByContributionId(contributionId);
        BigDecimal remainingPosition = contribution.getAmount().subtract(alreadyWithdrawn);
        if (amount.compareTo(remainingPosition) > 0) {
            throw new BusinessRuleException("Withdrawal amount exceeds the remaining position on this contribution");
        }

        Long projectId = contribution.getProjectId();
        BigDecimal projectedValue = contributionService.currentProjectValue(projectId).subtract(amount);
        BigDecimal minProjectValue = financialParameterService.resolveOrDefault(
                FinancialParameterService.MIN_PROJECT_VALUE_KEY, projectId, BigDecimal.ZERO);

        Withdrawal withdrawal = new Withdrawal(
                null, contributionId, projectId, contribution.getInvestorId(), amount,
                null, null, null, null, null,
                requestedByAppUserId, Instant.now(), null, null
        );

        if (projectedValue.compareTo(minProjectValue) < 0) {
            withdrawal.setStatus(WithdrawalStatus.BLOCKED);
            withdrawal.setBlockReason(
                    "Ce retrait ferait chuter la valeur du projet (" + projectedValue +
                            ") sous le seuil minimum configuré (" + minProjectValue +
                            "). Un apport compensatoire est nécessaire avant de pouvoir retenter ce retrait.");
        } else {
            withdrawal.setStatus(WithdrawalStatus.REQUESTED);
        }
        return withdrawalRepository.save(withdrawal);
    }

    public Withdrawal validate(Long withdrawalId, Long validatingAppUserId) {
        Withdrawal withdrawal = findById(withdrawalId);
        ensurePending(withdrawal);
        ensureDifferentFromRequester(withdrawal, validatingAppUserId);

        BigDecimal penaltyRate = financialParameterService.resolveOrDefault(
                FinancialParameterService.WITHDRAWAL_PENALTY_RATE_KEY, withdrawal.getProjectId(), BigDecimal.ZERO);
        BigDecimal resultingLoss = withdrawal.getRequestedAmount().multiply(penaltyRate).setScale(4, RoundingMode.HALF_UP);
        BigDecimal netAmountPaid = withdrawal.getRequestedAmount().subtract(resultingLoss);

        withdrawal.setPenaltyRate(penaltyRate);
        withdrawal.setResultingLoss(resultingLoss);
        withdrawal.setNetAmountPaid(netAmountPaid);
        withdrawal.setStatus(WithdrawalStatus.VALIDATED);
        withdrawal.setValidatedBy(validatingAppUserId);
        withdrawal.setValidatedAt(Instant.now());
        Withdrawal saved = withdrawalRepository.save(withdrawal);

        operationRepository.save(new Operation(
                null, withdrawal.getProjectId(),
                "Retrait de capital - investisseur #" + withdrawal.getInvestorId(),
                LocalDate.now(), netAmountPaid.doubleValue(), OperationType.EXPENSE
        ));

        return saved;
    }

    public Withdrawal reject(Long withdrawalId, Long rejectingAppUserId) {
        Withdrawal withdrawal = findById(withdrawalId);
        ensurePending(withdrawal);
        ensureDifferentFromRequester(withdrawal, rejectingAppUserId);

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setValidatedBy(rejectingAppUserId);
        withdrawal.setValidatedAt(Instant.now());
        return withdrawalRepository.save(withdrawal);
    }

    private void ensurePending(Withdrawal withdrawal) {
        if (withdrawal.getStatus() != WithdrawalStatus.REQUESTED) {
            throw new BusinessRuleException("This withdrawal has already been " + withdrawal.getStatus());
        }
    }

    private void ensureDifferentFromRequester(Withdrawal withdrawal, Long actingAppUserId) {
        if (withdrawal.getRequestedBy().equals(actingAppUserId)) {
            throw new BusinessRuleException("A different responsible person must decide on this withdrawal");
        }
    }
}
