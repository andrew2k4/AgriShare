package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.ProfitDistribution;
import com.gaming.agricul.Project;
import com.gaming.agricul.dto.ProposeDistributionRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.ProjectRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitDistributionServiceTest {

    @Mock
    private ProfitDistributionRepository distributionRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private ContributionUnitService contributionUnitService;

    private ProfitDistributionService service;

    private final LocalDate periodStart = LocalDate.of(2026, 1, 1);
    private final LocalDate periodEnd = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        service = new ProfitDistributionService(
                distributionRepository, projectRepository, contributionRepository, operationRepository, contributionUnitService);
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(new Project()));
        lenient().when(distributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void propose_computesDistributableProfit_asRevenueMinusCostsMinusTaxesMinusReserveMinusReinvestment() {
        when(operationRepository.findByProjectIdAndDateBetween(1L, periodStart, periodEnd)).thenReturn(List.of(
                new Operation(1L, 1L, "Vente d'oeufs", periodStart, 1_000_000, OperationType.INCOME),
                new Operation(2L, 1L, "Aliments", periodStart, 300_000, OperationType.EXPENSE)
        ));
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        ProfitDistribution distribution = service.propose(1L,
                new ProposeDistributionRequest(periodStart, periodEnd, new BigDecimal("50000"), new BigDecimal("100000"), new BigDecimal("20000")),
                42L);

        assertThat(distribution.getRevenue()).isEqualByComparingTo("1000000");
        assertThat(distribution.getOperatingCosts()).isEqualByComparingTo("300000");
        // 1 000 000 - 300 000 - 50 000 - 100 000 - 20 000 = 530 000
        assertThat(distribution.getDistributableProfit()).isEqualByComparingTo("530000");
        assertThat(distribution.getStatus()).isEqualTo(DistributionStatus.PROPOSED);
        assertThat(distribution.getProposedBy()).isEqualTo(42L);
    }

    @Test
    void propose_flooresDistributableProfitAtZero_onALoss() {
        when(operationRepository.findByProjectIdAndDateBetween(1L, periodStart, periodEnd)).thenReturn(List.of(
                new Operation(1L, 1L, "Vente", periodStart, 100_000, OperationType.INCOME),
                new Operation(2L, 1L, "Charges", periodStart, 500_000, OperationType.EXPENSE)
        ));
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        ProfitDistribution distribution = service.propose(1L,
                new ProposeDistributionRequest(periodStart, periodEnd, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                42L);

        assertThat(distribution.getDistributableProfit()).isEqualByComparingTo("0");
    }

    @Test
    void propose_splitsProfitProportionallyToUnitsOfContribution() {
        Contribution c1 = new Contribution(1L, 1L, 10L, ContributionType.EQUITY, new BigDecimal("2000000"), periodStart, BigDecimal.ZERO, null, null);
        Contribution c2 = new Contribution(2L, 1L, 20L, ContributionType.EQUITY, new BigDecimal("1000000"), periodStart, new BigDecimal("2000000"), null, null);
        when(operationRepository.findByProjectIdAndDateBetween(1L, periodStart, periodEnd)).thenReturn(List.of(
                new Operation(1L, 1L, "Vente", periodStart, 900_000, OperationType.INCOME)
        ));
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(c1, c2));
        when(contributionUnitService.computeUnits(eq(c1), any(), any())).thenReturn(new BigDecimal("2000000"));
        when(contributionUnitService.computeUnits(eq(c2), any(), any())).thenReturn(new BigDecimal("1000000"));

        ProfitDistribution distribution = service.propose(1L,
                new ProposeDistributionRequest(periodStart, periodEnd, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                42L);

        assertThat(distribution.getDistributableProfit()).isEqualByComparingTo("900000");
        assertThat(distribution.getLines()).hasSize(2);
        var line1 = distribution.getLines().stream().filter(l -> l.getContributionId().equals(1L)).findFirst().orElseThrow();
        var line2 = distribution.getLines().stream().filter(l -> l.getContributionId().equals(2L)).findFirst().orElseThrow();
        assertThat(line1.getSharePercentage()).isEqualByComparingTo("66.67");
        assertThat(line1.getAmount()).isEqualByComparingTo("600000.0000");
        assertThat(line2.getSharePercentage()).isEqualByComparingTo("33.33");
        assertThat(line2.getAmount()).isEqualByComparingTo("300000.0000");
    }

    @Test
    void propose_rejectsUnknownProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.propose(99L,
                new ProposeDistributionRequest(periodStart, periodEnd, null, null, null), 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void propose_rejectsPeriodEndBeforePeriodStart() {
        assertThatThrownBy(() -> service.propose(1L,
                new ProposeDistributionRequest(periodEnd, periodStart, null, null, null), 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validate_requiresADifferentPersonThanTheProposer() {
        ProfitDistribution proposed = proposedDistribution(1L);
        when(distributionRepository.findById(100L)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> service.validate(100L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validate_succeedsWithADifferentValidator() {
        ProfitDistribution proposed = proposedDistribution(1L);
        when(distributionRepository.findById(100L)).thenReturn(Optional.of(proposed));

        ProfitDistribution validated = service.validate(100L, 2L);

        assertThat(validated.getStatus()).isEqualTo(DistributionStatus.VALIDATED);
        assertThat(validated.getValidatedBy()).isEqualTo(2L);
    }

    @Test
    void validate_rejectsAlreadyDecidedDistribution() {
        ProfitDistribution alreadyValidated = proposedDistribution(1L);
        alreadyValidated.setStatus(DistributionStatus.VALIDATED);
        when(distributionRepository.findById(100L)).thenReturn(Optional.of(alreadyValidated));

        assertThatThrownBy(() -> service.validate(100L, 2L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reject_requiresADifferentPersonThanTheProposer() {
        ProfitDistribution proposed = proposedDistribution(1L);
        when(distributionRepository.findById(100L)).thenReturn(Optional.of(proposed));

        assertThatThrownBy(() -> service.reject(100L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    private ProfitDistribution proposedDistribution(Long proposedBy) {
        return new ProfitDistribution(100L, 1L, periodStart, periodEnd,
                new BigDecimal("1000000"), new BigDecimal("300000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("700000"), DistributionStatus.PROPOSED, proposedBy, java.time.Instant.now(), null, null, List.of());
    }
}
