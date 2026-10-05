package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.Lease;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.dto.CreateLeaseRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Leases let a project use an asset that stays owned by its contributor, paying rent
 * instead of granting ownership (doc §9.2). An asset can be either leased or contributed to
 * capital (IN_KIND) — never both at once (doc §18 anti-double-comptage).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final AssetRepository assetRepository;
    private final OperationRepository operationRepository;

    @Transactional(readOnly = true)
    public List<Lease> findByAsset(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new NotFoundException("Asset not found with id: " + assetId);
        }
        return leaseRepository.findByAssetId(assetId);
    }

    @Transactional(readOnly = true)
    public Lease findById(Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found with id: " + id));
    }

    public Lease create(Long assetId, CreateLeaseRequest request) {
        if (request.monthlyRent() == null || request.monthlyRent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Monthly rent must be positive");
        }
        if (request.startDate() == null) {
            throw new BusinessRuleException("Start date is required");
        }
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));
        if (asset.getOwnershipStatus() != AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR) {
            throw new BusinessRuleException(
                    "This asset has already been contributed to capital; it cannot also be leased");
        }
        if (leaseRepository.findFirstByAssetIdAndStatus(assetId, LeaseStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException("This asset already has an active lease");
        }

        Lease lease = new Lease(null, assetId, request.monthlyRent(), request.startDate(), null, LeaseStatus.ACTIVE, null);
        return leaseRepository.save(lease);
    }

    public Lease end(Long leaseId) {
        Lease lease = findById(leaseId);
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new BusinessRuleException("This lease is already ended");
        }
        lease.setStatus(LeaseStatus.ENDED);
        lease.setEndDate(LocalDate.now());
        return leaseRepository.save(lease);
    }

    /** Manually posts one period's rent as an EXPENSE operation on the project (doc §9.2). */
    public Lease postRent(Long leaseId) {
        Lease lease = findById(leaseId);
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot post rent on an ended lease");
        }
        Asset asset = assetRepository.findById(lease.getAssetId())
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + lease.getAssetId()));

        operationRepository.save(new Operation(
                null, asset.getProjectId(),
                "Loyer - actif #" + asset.getId() + " (" + asset.getType() + ")",
                LocalDate.now(), lease.getMonthlyRent().doubleValue(), OperationType.EXPENSE
        ));
        return lease;
    }
}
