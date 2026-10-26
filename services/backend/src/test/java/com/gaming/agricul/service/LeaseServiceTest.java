package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.dto.CreateLeaseRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private OperationRepository operationRepository;

    private LeaseService leaseService;

    @BeforeEach
    void setUp() {
        leaseService = new LeaseService(leaseRepository, assetRepository, operationRepository);
    }

    private Asset ownedAsset() {
        return new Asset(5L, 1L, 10L, AssetType.BUILDING, "Ferme", new BigDecimal("8000000"),
                new BigDecimal("8000000"), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR, AssetDisposition.PENDING, null, null, null);
    }

    @Test
    void create_succeedsForAssetOwnedByContributorWithNoActiveLease() {
        when(assetRepository.findById(5L)).thenReturn(Optional.of(ownedAsset()));
        when(leaseRepository.findFirstByAssetIdAndStatus(5L, LeaseStatus.ACTIVE)).thenReturn(Optional.empty());
        when(leaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lease lease = leaseService.create(5L, new CreateLeaseRequest(new BigDecimal("80000"), LocalDate.now()));

        assertThat(lease.getStatus()).isEqualTo(LeaseStatus.ACTIVE);
    }

    @Test
    void create_rejectsAssetAlreadyTransferredToCapital() {
        Asset transferred = ownedAsset();
        transferred.setOwnershipStatus(AssetOwnershipStatus.TRANSFERRED_TO_PROJECT);
        when(assetRepository.findById(5L)).thenReturn(Optional.of(transferred));

        assertThatThrownBy(() -> leaseService.create(5L, new CreateLeaseRequest(new BigDecimal("80000"), LocalDate.now())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_rejectsAssetWithAnExistingActiveLease() {
        when(assetRepository.findById(5L)).thenReturn(Optional.of(ownedAsset()));
        when(leaseRepository.findFirstByAssetIdAndStatus(5L, LeaseStatus.ACTIVE))
                .thenReturn(Optional.of(new Lease(1L, 5L, BigDecimal.TEN, LocalDate.now(), null, LeaseStatus.ACTIVE, null)));

        assertThatThrownBy(() -> leaseService.create(5L, new CreateLeaseRequest(new BigDecimal("80000"), LocalDate.now())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void postRent_postsAnExpenseOperationOnTheAssetsProject() {
        Lease lease = new Lease(1L, 5L, new BigDecimal("80000"), LocalDate.now(), null, LeaseStatus.ACTIVE, null);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));
        when(assetRepository.findById(5L)).thenReturn(Optional.of(ownedAsset()));
        ArgumentCaptor<com.gaming.agricul.Operation> captor = ArgumentCaptor.forClass(com.gaming.agricul.Operation.class);

        leaseService.postRent(1L);

        verify(operationRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(1L);
        assertThat(captor.getValue().getAmount()).isEqualTo(80000.0);
        assertThat(captor.getValue().getOperationType().name()).isEqualTo("EXPENSE");
    }

    @Test
    void postRent_rejectsEndedLease() {
        Lease ended = new Lease(1L, 5L, new BigDecimal("80000"), LocalDate.now(), LocalDate.now(), LeaseStatus.ENDED, null);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> leaseService.postRent(1L)).isInstanceOf(BusinessRuleException.class);
    }
}
