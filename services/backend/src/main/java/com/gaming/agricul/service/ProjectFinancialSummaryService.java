package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProfitDistributionLine;
import com.gaming.agricul.Repayment;
import com.gaming.agricul.RepaymentStatus;
import com.gaming.agricul.dto.InvestorShareDto;
import com.gaming.agricul.dto.ProjectFinancialSummaryDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregates the dashboard indicators from doc §19 into a single read model. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectFinancialSummaryService {

    private final ProjectRepository projectRepository;
    private final ContributionRepository contributionRepository;
    private final OperationRepository operationRepository;
    private final AssetRepository assetRepository;
    private final LeaseRepository leaseRepository;
    private final RepaymentRepository repaymentRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final ContributionService contributionService;
    private final ContributionUnitService contributionUnitService;

    public ProjectFinancialSummaryDto compute(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));
        List<Contribution> contributions = contributionRepository.findByProjectId(projectId);
        List<Operation> operations = operationRepository.findByProjectId(projectId);
        List<Asset> assets = assetRepository.findByProjectId(projectId);

        BigDecimal currentProjectValue = contributionService.currentProjectValue(projectId);

        BigDecimal totalCapitalInvested = contributions.stream()
                .filter(c -> c.getType().isEquityLike())
                .map(Contribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingTemporaryCapital = contributions.stream()
                .filter(c -> c.getType() == ContributionType.LOAN || c.getType() == ContributionType.TEMPORARY_INVESTMENT)
                .map(c -> c.getAmount().subtract(paidPrincipal(c.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal assetsValueOwnedByContributors = assets.stream()
                .filter(a -> a.getOwnershipStatus() == AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR)
                .map(a -> a.getCurrentValue() != null ? a.getCurrentValue() : a.getInitialValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentMonthlyRent = assets.stream()
                .map(a -> leaseRepository.findFirstByAssetIdAndStatus(a.getId(), LeaseStatus.ACTIVE))
                .filter(java.util.Optional::isPresent)
                .map(opt -> opt.get().getMonthlyRent())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = sumByType(operations, OperationType.INCOME);
        BigDecimal totalExpenses = sumByType(operations, OperationType.EXPENSE);
        BigDecimal netProfitAllTime = totalRevenue.subtract(totalExpenses);

        var validatedDistributions = profitDistributionRepository.findByProjectId(projectId).stream()
                .filter(d -> d.getStatus() == DistributionStatus.VALIDATED)
                .toList();
        BigDecimal totalReserveAccumulated = validatedDistributions.stream()
                .map(com.gaming.agricul.ProfitDistribution::getReserve)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDistributedToDate = validatedDistributions.stream()
                .flatMap(d -> d.getLines().stream())
                .map(ProfitDistributionLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InvestorShareDto> currentShares = computeCurrentShares(contributions, project);

        return new ProjectFinancialSummaryDto(
                projectId, currentProjectValue, totalCapitalInvested, remainingTemporaryCapital,
                assetsValueOwnedByContributors, currentMonthlyRent, totalRevenue, totalExpenses, netProfitAllTime,
                totalReserveAccumulated, totalDistributedToDate, currentShares
        );
    }

    private List<InvestorShareDto> computeCurrentShares(List<Contribution> contributions, Project project) {
        LocalDate today = LocalDate.now();
        List<Contribution> equityContributions = contributions.stream()
                .filter(c -> c.getType().isEquityLike())
                .toList();

        Map<Long, BigDecimal> unitsByInvestor = new LinkedHashMap<>();
        BigDecimal totalUnits = BigDecimal.ZERO;
        for (Contribution c : equityContributions) {
            BigDecimal units = contributionUnitService.computeUnits(c, project, today);
            unitsByInvestor.merge(c.getInvestorId(), units, BigDecimal::add);
            totalUnits = totalUnits.add(units);
        }

        BigDecimal finalTotalUnits = totalUnits;
        return unitsByInvestor.entrySet().stream()
                .map(e -> new InvestorShareDto(
                        e.getKey(),
                        finalTotalUnits.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : e.getValue().multiply(BigDecimal.valueOf(100)).divide(finalTotalUnits, 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private BigDecimal paidPrincipal(Long contributionId) {
        return repaymentRepository.findByContributionId(contributionId).stream()
                .filter(r -> r.getStatus() == RepaymentStatus.PAID)
                .map(Repayment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByType(List<Operation> operations, OperationType type) {
        return operations.stream()
                .filter(op -> op.getOperationType() == type)
                .map(op -> BigDecimal.valueOf(op.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
