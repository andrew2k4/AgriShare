package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.ProfitDistribution;
import com.gaming.agricul.ProfitDistributionLine;
import com.gaming.agricul.Project;
import com.gaming.agricul.dto.ProposeDistributionRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Proposes and validates profit distributions. Follows the document's model
 * (§2, §11, §12, §14): distributable profit = revenue - operating costs - taxes - reserve -
 * reinvestment, floored at 0 in case of a loss; each contribution's share is proportional to
 * its Units of Contribution (UC) as of the end of the period.
 *
 * Validation requires a <strong>second</strong>, different Admin/Manager than the one who
 * proposed the distribution (Q6/Q15 of the approved spec).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfitDistributionService {

    private final ProfitDistributionRepository distributionRepository;
    private final ProjectRepository projectRepository;
    private final ContributionRepository contributionRepository;
    private final OperationRepository operationRepository;
    private final ContributionUnitService contributionUnitService;

    @Transactional(readOnly = true)
    public List<ProfitDistribution> findByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return distributionRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public ProfitDistribution findById(Long id) {
        return distributionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Distribution not found with id: " + id));
    }

    public ProfitDistribution propose(Long projectId, ProposeDistributionRequest request, Long proposedByAppUserId) {
        if (request.periodStart() == null || request.periodEnd() == null) {
            throw new BusinessRuleException("Period start and end are required");
        }
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessRuleException("Period end must not be before period start");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        List<Operation> operations = operationRepository.findByProjectIdAndDateBetween(
                projectId, request.periodStart(), request.periodEnd());
        BigDecimal revenue = sumByType(operations, OperationType.INCOME);
        BigDecimal operatingCosts = sumByType(operations, OperationType.EXPENSE);

        BigDecimal taxes = nz(request.taxes());
        BigDecimal reserve = nz(request.reserve());
        BigDecimal reinvestment = nz(request.reinvestment());

        BigDecimal result = revenue.subtract(operatingCosts).subtract(taxes);
        BigDecimal distributableProfit = result.subtract(reserve).subtract(reinvestment).max(BigDecimal.ZERO);

        List<Contribution> contributions = contributionRepository.findByProjectId(projectId);
        List<ProfitDistributionLine> lines = buildLines(contributions, project, request.periodEnd(), distributableProfit);

        ProfitDistribution distribution = new ProfitDistribution(
                null, projectId, request.periodStart(), request.periodEnd(),
                revenue, operatingCosts, taxes, reserve, reinvestment, distributableProfit,
                DistributionStatus.PROPOSED, proposedByAppUserId, Instant.now(), null, null, lines, null
        );
        return distributionRepository.save(distribution);
    }

    public ProfitDistribution validate(Long distributionId, Long validatingAppUserId) {
        ProfitDistribution distribution = findById(distributionId);
        ensurePending(distribution);
        ensureDifferentFromProposer(distribution, validatingAppUserId);

        distribution.setStatus(DistributionStatus.VALIDATED);
        distribution.setValidatedBy(validatingAppUserId);
        distribution.setValidatedAt(Instant.now());
        return distributionRepository.save(distribution);
    }

    public ProfitDistribution reject(Long distributionId, Long rejectingAppUserId) {
        ProfitDistribution distribution = findById(distributionId);
        ensurePending(distribution);
        ensureDifferentFromProposer(distribution, rejectingAppUserId);

        distribution.setStatus(DistributionStatus.REJECTED);
        distribution.setValidatedBy(rejectingAppUserId);
        distribution.setValidatedAt(Instant.now());
        return distributionRepository.save(distribution);
    }

    private List<ProfitDistributionLine> buildLines(
            List<Contribution> allContributions, Project project, java.time.LocalDate asOfDate, BigDecimal distributableProfit) {
        List<ProfitDistributionLine> lines = new ArrayList<>();
        // LOAN/TEMPORARY_INVESTMENT/LABOR don't create ownership (doc §18) and are excluded
        // from profit-sharing; they're repaid/compensated separately (see RepaymentService).
        List<Contribution> contributions = allContributions.stream()
                .filter(c -> c.getType().isEquityLike())
                .toList();
        if (contributions.isEmpty()) {
            return lines;
        }

        List<BigDecimal> units = contributions.stream()
                .map(c -> contributionUnitService.computeUnits(c, project, asOfDate))
                .toList();
        BigDecimal totalUnits = units.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        for (int i = 0; i < contributions.size(); i++) {
            Contribution contribution = contributions.get(i);
            BigDecimal contributionUnits = units.get(i);
            BigDecimal sharePercentage = totalUnits.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : contributionUnits.multiply(BigDecimal.valueOf(100)).divide(totalUnits, 2, RoundingMode.HALF_UP);
            BigDecimal amount = totalUnits.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : distributableProfit.multiply(contributionUnits).divide(totalUnits, 4, RoundingMode.HALF_UP);

            lines.add(new ProfitDistributionLine(
                    contribution.getId(), contribution.getInvestorId(), contributionUnits, sharePercentage, amount));
        }
        return lines;
    }

    private void ensurePending(ProfitDistribution distribution) {
        if (distribution.getStatus() != DistributionStatus.PROPOSED) {
            throw new BusinessRuleException("This distribution has already been " + distribution.getStatus());
        }
    }

    private void ensureDifferentFromProposer(ProfitDistribution distribution, Long actingAppUserId) {
        if (distribution.getProposedBy().equals(actingAppUserId)) {
            throw new BusinessRuleException("A different responsible person must validate this distribution");
        }
    }

    private BigDecimal sumByType(List<Operation> operations, OperationType type) {
        return operations.stream()
                .filter(op -> op.getOperationType() == type)
                .map(op -> BigDecimal.valueOf(op.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
