package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.dto.CreateAssetRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.InvestorRepository;
import com.gaming.agricul.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private InvestorRepository investorRepository;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(assetRepository, projectRepository, investorRepository);
    }

    @Test
    void create_startsOwnedByContributor() {
        when(projectRepository.existsById(1L)).thenReturn(true);
        when(investorRepository.existsById(10L)).thenReturn(true);
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Asset asset = assetService.create(1L, new CreateAssetRequest(10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000")));

        assertThat(asset.getOwnershipStatus().name()).isEqualTo("OWNED_BY_CONTRIBUTOR");
        assertThat(asset.getProjectId()).isEqualTo(1L);
    }

    @Test
    void create_rejectsNonPositiveValue() {
        assertThatThrownBy(() -> assetService.create(1L, new CreateAssetRequest(10L, AssetType.BUILDING, "Ferme", BigDecimal.ZERO)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_rejectsUnknownProject() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> assetService.create(99L, new CreateAssetRequest(10L, AssetType.BUILDING, "Ferme", BigDecimal.TEN)))
                .isInstanceOf(NotFoundException.class);
    }
}
