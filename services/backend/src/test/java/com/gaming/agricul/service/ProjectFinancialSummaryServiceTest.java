package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.ProfitDistribution;
import com.gaming.agricul.ProfitDistributionLine;
import com.gaming.agricul.Project;
import com.gaming.agricul.Repayment;
import com.gaming.agricul.RepaymentStatus;
import com.gaming.agricul.dto.ProjectFinancialSummaryDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.RepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectFinancialSummaryServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private RepaymentRepository repaymentRepository;
    @Mock
    private ProfitDistributionRepository profitDistributionRepository;
    @Mock
    private ContributionService contributionService;
    @Mock
    private ContributionUnitService contributionUnitService;

    private ProjectFinancialSummaryService service;

    @BeforeEach
    void setUp() {
        service = new ProjectFinancialSummaryService(
                projectRepository, contributionRepository, operationRepository, assetRepository, leaseRepository,
                repaymentRepository, profitDistributionRepository, contributionService, contributionUnitService);
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(new Project()));
        lenient().when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());
        lenient().when(operationRepository.findByProjectId(1L)).thenReturn(List.of());
        lenient().when(assetRepository.findByProjectId(1L)).thenReturn(List.of());
        lenient().when(profitDistributionRepository.findByProjectId(1L)).thenReturn(List.of());
        lenient().when(contributionService.currentProjectValue(1L)).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void compute_rejectsUnknownProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.compute(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void compute_sumsRevenueAndExpensesAcrossAllOperations() {
        when(operationRepository.findByProjectId(1L)).thenReturn(List.of(
                new Operation(1L, 1L, "Vente", LocalDate.now(), 1_000_000, OperationType.INCOME),
                new Operation(2L, 1L, "Charges", LocalDate.now(), 300_000, OperationType.EXPENSE)
        ));

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.totalRevenue()).isEqualByComparingTo("1000000");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("300000");
        assertThat(summary.netProfitAllTime()).isEqualByComparingTo("700000");
    }

    @Test
    void compute_subtractsPaidPrincipalFromRemainingTemporaryCapital() {
        Contribution loan = new Contribution(1L, 1L, 10L, ContributionType.LOAN,
                new BigDecimal("2000000"), LocalDate.now(), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(loan));
        when(repaymentRepository.findByContributionId(1L)).thenReturn(List.of(
                new Repayment(1L, 1L, new BigDecimal("500000"), BigDecimal.ZERO, LocalDate.now(), RepaymentStatus.PAID, java.time.Instant.now())
        ));

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.remainingTemporaryCapital()).isEqualByComparingTo("1500000");
    }

    @Test
    void compute_sumsAssetsOwnedByContributorsOnly() {
        Asset owned = new Asset(1L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null, null);
        Asset transferred = new Asset(2L, 1L, 20L, AssetType.MACHINE, "Tracteur", new BigDecimal("3000000"),
                new BigDecimal("3000000"), AssetOwnershipStatus.TRANSFERRED_TO_PROJECT, AssetDisposition.PENDING, null, null, null);
        when(assetRepository.findByProjectId(1L)).thenReturn(List.of(owned, transferred));
        when(leaseRepository.findFirstByAssetIdAndStatus(any(), any())).thenReturn(Optional.empty());

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.assetsValueOwnedByContributors()).isEqualByComparingTo("8000000");
    }

    @Test
    void compute_sumsActiveLeasesMonthlyRent() {
        Asset owned = new Asset(1L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null, null);
        when(assetRepository.findByProjectId(1L)).thenReturn(List.of(owned));
        when(leaseRepository.findFirstByAssetIdAndStatus(1L, LeaseStatus.ACTIVE))
                .thenReturn(Optional.of(new Lease(1L, 1L, new BigDecimal("80000"), LocalDate.now(), null, LeaseStatus.ACTIVE, null)));

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.currentMonthlyRent()).isEqualByComparingTo("80000");
    }

    @Test
    void compute_aggregatesValidatedDistributionsOnly() {
        ProfitDistributionLine line = new ProfitDistributionLine(1L, 10L, BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("50000"));
        ProfitDistribution validated = new ProfitDistribution(1L, 1L, LocalDate.now(), LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("50000"),
                DistributionStatus.VALIDATED, 1L, java.time.Instant.now(), 2L, java.time.Instant.now(), List.of(line), null);
        ProfitDistribution proposed = new ProfitDistribution(2L, 1L, LocalDate.now(), LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("99999"), BigDecimal.ZERO, new BigDecimal("99999"),
                DistributionStatus.PROPOSED, 1L, java.time.Instant.now(), null, null, List.of(), null);
        when(profitDistributionRepository.findByProjectId(1L)).thenReturn(List.of(validated, proposed));

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.totalReserveAccumulated()).isEqualByComparingTo("10000");
        assertThat(summary.totalDistributedToDate()).isEqualByComparingTo("50000");
    }

    @Test
    void compute_computesCurrentSharesFromEquityLikeContributionsOnly() {
        Contribution equity = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("2000000"), LocalDate.now(), BigDecimal.ZERO, null, null);
        Contribution loan = new Contribution(2L, 1L, 20L, ContributionType.LOAN,
                new BigDecimal("5000000"), LocalDate.now(), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(equity, loan));
        when(contributionUnitService.computeUnits(any(), any(), any())).thenReturn(new BigDecimal("2000000"));

        ProjectFinancialSummaryDto summary = service.compute(1L);

        assertThat(summary.currentShares()).hasSize(1);
        assertThat(summary.currentShares().get(0).investorId()).isEqualTo(10L);
        assertThat(summary.currentShares().get(0).sharePercentage()).isEqualByComparingTo("100.00");
    }
}
