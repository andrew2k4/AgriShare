package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.dto.ProjectUsageSimulationDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.LeaseRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private LeaseRepository leaseRepository;

    private SimulationService simulationService;

    @Test
    void simulateAssetUsage_rejectsUnknownProject() {
        simulationService = new SimulationService(projectRepository, assetRepository, leaseRepository);
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> simulationService.simulateAssetUsage(99L, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void simulateAssetUsage_matchesDocumentWorkedExample() {
        simulationService = new SimulationService(projectRepository, assetRepository, leaseRepository);
        when(projectRepository.existsById(1L)).thenReturn(true);
        Asset ferme = new Asset(1L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null, null);
        when(assetRepository.findByProjectId(1L)).thenReturn(List.of(ferme));
        when(leaseRepository.findFirstByAssetIdAndStatus(1L, LeaseStatus.ACTIVE)).thenReturn(Optional.empty());

        // Doc §9.3: ferme valorisée 8 000 000, taux 1%/mois -> loyer hypothétique 80 000
        ProjectUsageSimulationDto simulation = simulationService.simulateAssetUsage(1L, new BigDecimal("1.0"));

        assertThat(simulation.totalHypotheticalMonthlyCost()).isEqualByComparingTo("80000.0000");
        assertThat(simulation.actualCurrentMonthlyRent()).isEqualByComparingTo("0");
    }

    @Test
    void simulateAssetUsage_reportsActualRentFromActiveLease() {
        simulationService = new SimulationService(projectRepository, assetRepository, leaseRepository);
        when(projectRepository.existsById(1L)).thenReturn(true);
        Asset ferme = new Asset(1L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null, null);
        when(assetRepository.findByProjectId(1L)).thenReturn(List.of(ferme));
        when(leaseRepository.findFirstByAssetIdAndStatus(1L, LeaseStatus.ACTIVE))
                .thenReturn(Optional.of(new Lease(1L, 1L, new BigDecimal("80000"), LocalDate.now(), null, LeaseStatus.ACTIVE, null)));

        ProjectUsageSimulationDto simulation = simulationService.simulateAssetUsage(1L, new BigDecimal("1.0"));

        assertThat(simulation.actualCurrentMonthlyRent()).isEqualByComparingTo("80000");
    }
}
