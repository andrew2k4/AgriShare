package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.dto.CreateAssetRequest;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.InvestorRepository;
import com.gaming.agricul.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProjectRepository projectRepository;
    private final InvestorRepository investorRepository;

    @Transactional(readOnly = true)
    public List<Asset> findByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return assetRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Asset findById(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + id));
    }

    /** New assets start OWNED_BY_CONTRIBUTOR: leasable immediately, or contributable to capital later (IN_KIND). */
    public Asset create(Long projectId, CreateAssetRequest request) {
        if (request.initialValue() == null || request.initialValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Initial value must be positive");
        }
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        if (!investorRepository.existsById(request.ownerInvestorId())) {
            throw new NotFoundException("Investor not found with id: " + request.ownerInvestorId());
        }

        Asset asset = new Asset(
                null, projectId, request.ownerInvestorId(), request.type(), request.description(),
                request.initialValue(), request.initialValue(), AssetOwnershipStatus.OWNED_BY_CONTRIBUTOR,
                AssetDisposition.PENDING, null, null, null
        );
        return assetRepository.save(asset);
    }
}
