package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectClosureServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private OperationRepository operationRepository;

    private ProjectClosureService service;

    @BeforeEach
    void setUp() {
        service = new ProjectClosureService(projectRepository, assetRepository, operationRepository);
    }

    @Test
    void close_setsStatusToClosed() {
        Project project = new Project();
        project.setId(1L);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Project closed = service.close(1L);

        assertThat(closed.getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    void close_rejectsAlreadyClosedProject() {
        Project project = new Project();
        project.setId(1L);
        project.setStatus(ProjectStatus.CLOSED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.close(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disposeAsset_rejectsWhenProjectNotClosed() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.TRANSFERRED_TO_PROJECT, AssetDisposition.PENDING, null, null);
        Project project = new Project();
        project.setId(1L);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.disposeAsset(5L, AssetDisposition.SOLD, new BigDecimal("8500000"), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disposeAsset_rejectsAssetStillOwnedByContributor() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.disposeAsset(5L, AssetDisposition.SOLD, new BigDecimal("8500000"), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disposeAsset_sold_postsIncomeOperation() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.TRANSFERRED_TO_PROJECT, AssetDisposition.PENDING, null, null);
        Project project = new Project();
        project.setId(1L);
        project.setStatus(ProjectStatus.CLOSED);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<com.gaming.agricul.Operation> captor = ArgumentCaptor.forClass(com.gaming.agricul.Operation.class);

        Asset disposed = service.disposeAsset(5L, AssetDisposition.SOLD, new BigDecimal("8500000"), null);

        assertThat(disposed.getDisposition()).isEqualTo(AssetDisposition.SOLD);
        verify(operationRepository).save(captor.capture());
        assertThat(captor.getValue().getOperationType().name()).isEqualTo("INCOME");
        assertThat(captor.getValue().getAmount()).isEqualTo(8500000.0);
    }

    @Test
    void disposeAsset_rejectsAlreadySettledAsset() {
        Asset asset = new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.TRANSFERRED_TO_PROJECT, AssetDisposition.SOLD,
                new BigDecimal("8500000"), null);
        Project project = new Project();
        project.setId(1L);
        project.setStatus(ProjectStatus.CLOSED);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(asset));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.disposeAsset(5L, AssetDisposition.SOLD, new BigDecimal("1"), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disposeAsset_rejectsRetainedByInvestorWithoutBuyerId() {
        assertThatThrownBy(() -> service.disposeAsset(5L, AssetDisposition.RETAINED_BY_INVESTOR, new BigDecimal("1000"), null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
