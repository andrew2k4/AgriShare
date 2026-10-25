package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.Project;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.InvestorRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private WithdrawalRepository withdrawalRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private LeaseRepository leaseRepository;

    private ContributionService contributionService;

    @BeforeEach
    void setUp() {
        contributionService = new ContributionService(
                contributionRepository, projectRepository, investorRepository, withdrawalRepository,
                assetRepository, leaseRepository);
        lenient().when(withdrawalRepository.sumValidatedAmountByProjectId(any())).thenReturn(BigDecimal.ZERO);
        lenient().when(projectRepository.findById(1L)).thenReturn(Optional.of(new Project()));
        lenient().when(investorRepository.existsById(10L)).thenReturn(true);
        lenient().when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addContribution_firstContributionToProject_hasZeroValuationBeforeEntry() {
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        Contribution result = contributionService.addContribution(
                1L, 10L, ContributionType.EQUITY, new BigDecimal("2000000"), LocalDate.of(2026, 1, 15), null);

        assertThat(result.getValuationBeforeEntry()).isEqualByComparingTo("0");
        assertThat(result.getAmount()).isEqualByComparingTo("2000000");
        assertThat(result.getType()).isEqualTo(ContributionType.EQUITY);
    }

    @Test
    void addContribution_defaultsToEquity_whenTypeOmitted() {
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        Contribution result = contributionService.addContribution(1L, 10L, null, BigDecimal.TEN, LocalDate.now(), null);

        assertThat(result.getType()).isEqualTo(ContributionType.EQUITY);
    }

    @Test
    void addContribution_secondContribution_snapshotsPriorTotalAsValuation() {
        Contribution existing = new Contribution(1L, 1L, 5L, ContributionType.EQUITY,
                new BigDecimal("20000000"), LocalDate.of(2025, 1, 1), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        ArgumentCaptor<Contribution> captor = ArgumentCaptor.forClass(Contribution.class);
        when(contributionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Matches the document's worked example: valeur=20 000 000, investissement=2 000 000 -> ~9.09%
        contributionService.addContribution(1L, 10L, ContributionType.EQUITY, new BigDecimal("2000000"), LocalDate.of(2026, 1, 15), null);

        assertThat(captor.getValue().getValuationBeforeEntry()).isEqualByComparingTo("20000000");
    }

    @Test
    void addContribution_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> contributionService.addContribution(1L, 10L, ContributionType.EQUITY, BigDecimal.ZERO, LocalDate.now(), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> contributionService.addContribution(1L, 10L, ContributionType.EQUITY, new BigDecimal("-5"), LocalDate.now(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addContribution_rejectsMissingEntryDate() {
        assertThatThrownBy(() -> contributionService.addContribution(1L, 10L, ContributionType.EQUITY, BigDecimal.TEN, null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addContribution_rejectsUnknownProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contributionService.addContribution(99L, 10L, ContributionType.EQUITY, BigDecimal.TEN, LocalDate.now(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addContribution_rejectsUnknownInvestor() {
        when(investorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> contributionService.addContribution(1L, 99L, ContributionType.EQUITY, BigDecimal.TEN, LocalDate.now(), null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addContribution_rejectsAssetIdOnNonInKindType() {
        assertThatThrownBy(() -> contributionService.addContribution(1L, 10L, ContributionType.EQUITY, BigDecimal.TEN, LocalDate.now(), 5L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addContribution_inKind_transfersAssetToCapitalAndRejectsIfLeased() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        // Not leased -> transfer succeeds
        when(leaseRepository.findFirstByAssetIdAndStatus(5L, LeaseStatus.ACTIVE)).thenReturn(Optional.empty());
        Contribution result = contributionService.addContribution(
                1L, 10L, ContributionType.IN_KIND, new BigDecimal("8000000"), LocalDate.now(), 5L);
        assertThat(result.getAssetId()).isEqualTo(5L);
        assertThat(asset.getOwnershipStatus()).isEqualTo(AssetOwnershipStatus.TRANSFERRED_TO_PROJECT);
        verify(assetRepository).save(asset);
    }

    @Test
    void addContribution_inKind_rejectsAssetUnderActiveLease() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(leaseRepository.findFirstByAssetIdAndStatus(5L, LeaseStatus.ACTIVE))
                .thenReturn(Optional.of(new Lease(1L, 5L, BigDecimal.TEN, LocalDate.now(), null, LeaseStatus.ACTIVE)));

        assertThatThrownBy(() -> contributionService.addContribution(
                1L, 10L, ContributionType.IN_KIND, new BigDecimal("8000000"), LocalDate.now(), 5L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addContribution_inKind_rejectsAssetNotOwnedByThisInvestor() {
        Asset asset = new Asset(5L, 1L, 99L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> contributionService.addContribution(
                1L, 10L, ContributionType.IN_KIND, new BigDecimal("8000000"), LocalDate.now(), 5L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void currentProjectValue_sumsOnlyEquityLikeContributionAmounts() {
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(
                new Contribution(1L, 1L, 5L, ContributionType.EQUITY, new BigDecimal("1000000"), LocalDate.now(), BigDecimal.ZERO, null, null),
                new Contribution(2L, 1L, 6L, ContributionType.IN_KIND, new BigDecimal("500000"), LocalDate.now(), new BigDecimal("1000000"), null, 7L),
                new Contribution(3L, 1L, 8L, ContributionType.LOAN, new BigDecimal("2000000"), LocalDate.now(), BigDecimal.ZERO, null, null)
        ));

        BigDecimal total = contributionService.currentProjectValue(1L);

        assertThat(total).isEqualByComparingTo("1500000");
    }

    @Test
    void findByProject_rejectsUnknownProject() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> contributionService.findByProject(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
