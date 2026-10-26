package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.Repayment;
import com.gaming.agricul.RepaymentStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Repayments of LOAN/TEMPORARY_INVESTMENT contributions. A repayment never creates
 * ownership (doc §18: "Prêt ≠ participation") — it is tracked and paid independently of the
 * UC/profit-sharing engine, posting an EXPENSE operation on the project once paid.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final ContributionRepository contributionRepository;
    private final OperationRepository operationRepository;

    @Transactional(readOnly = true)
    public List<Repayment> findByContribution(Long contributionId) {
        if (!contributionRepository.existsById(contributionId)) {
            throw new NotFoundException("Contribution not found with id: " + contributionId);
        }
        return repaymentRepository.findByContributionId(contributionId);
    }

    @Transactional(readOnly = true)
    public Repayment findById(Long id) {
        return repaymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Repayment not found with id: " + id));
    }

    public Repayment schedule(Long contributionId, BigDecimal principalAmount, BigDecimal interestAmount, LocalDate dueDate) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Principal amount must be positive");
        }
        if (dueDate == null) {
            throw new BusinessRuleException("Due date is required");
        }
        Contribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new NotFoundException("Contribution not found with id: " + contributionId));
        if (contribution.getType().isEquityLike()) {
            throw new BusinessRuleException("Only LOAN or TEMPORARY_INVESTMENT contributions can be scheduled for repayment");
        }

        Repayment repayment = new Repayment(
                null, contributionId, principalAmount, interestAmount != null ? interestAmount : BigDecimal.ZERO,
                dueDate, RepaymentStatus.PENDING, null
        );
        return repaymentRepository.save(repayment);
    }

    public Repayment markPaid(Long repaymentId) {
        Repayment repayment = findById(repaymentId);
        if (repayment.getStatus() == RepaymentStatus.PAID) {
            throw new BusinessRuleException("This repayment has already been paid");
        }
        Contribution contribution = contributionRepository.findById(repayment.getContributionId())
                .orElseThrow(() -> new NotFoundException("Contribution not found with id: " + repayment.getContributionId()));

        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setPaidAt(Instant.now());
        Repayment saved = repaymentRepository.save(repayment);

        BigDecimal total = repayment.getPrincipalAmount().add(repayment.getInterestAmount());
        operationRepository.save(new Operation(
                null, contribution.getProjectId(),
                "Remboursement " + contribution.getType() + " - investisseur #" + contribution.getInvestorId(),
                LocalDate.now(), total.doubleValue(), OperationType.EXPENSE
        ));

        return saved;
    }
}
