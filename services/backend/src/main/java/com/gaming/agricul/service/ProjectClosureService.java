package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.Operation;
import com.gaming.agricul.OperationType;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Closes a project and settles its capital assets (Q11 of the approved spec): once closed,
 * each asset TRANSFERRED_TO_PROJECT is either sold or bought out by an investor; proceeds are
 * posted as project income so the final profit distribution ({@link ProfitDistributionService})
 * naturally includes them — no separate "closure distribution" mechanism is needed.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectClosureService {

    private final ProjectRepository projectRepository;
    private final AssetRepository assetRepository;
    private final OperationRepository operationRepository;

    public Project close(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("This project is already closed");
        }
        project.setStatus(ProjectStatus.CLOSED);
        return projectRepository.save(project);
    }

    public Asset disposeAsset(Long assetId, AssetDisposition disposition, BigDecimal amount, Long buyerInvestorId) {
        if (disposition == null || disposition == AssetDisposition.PENDING) {
            throw new BusinessRuleException("Disposition must be SOLD or RETAINED_BY_INVESTOR");
        }
        if (disposition == AssetDisposition.RETAINED_BY_INVESTOR && buyerInvestorId == null) {
            throw new BusinessRuleException("buyerInvestorId is required when an investor retains the asset");
        }
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));
        if (asset.getOwnershipStatus() != AssetOwnershipStatus.TRANSFERRED_TO_PROJECT) {
            throw new BusinessRuleException("Only assets contributed to capital (IN_KIND) need to be settled at closure");
        }
        Project project = projectRepository.findById(asset.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + asset.getProjectId()));
        if (project.getStatus() != ProjectStatus.CLOSED) {
            throw new BusinessRuleException("The project must be closed before settling its assets");
        }
        if (asset.getDisposition() != AssetDisposition.PENDING) {
            throw new BusinessRuleException("This asset has already been settled");
        }

        asset.setDisposition(disposition);
        asset.setDisposalAmount(amount);
        asset.setDisposalInvestorId(buyerInvestorId);
        Asset saved = assetRepository.save(asset);

        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            String label = disposition == AssetDisposition.SOLD
                    ? "Vente de l'actif #" + assetId + " (" + asset.getType() + ")"
                    : "Rachat de l'actif #" + assetId + " par l'investisseur #" + buyerInvestorId;
            operationRepository.save(new Operation(
                    null, asset.getProjectId(), label, LocalDate.now(), amount.doubleValue(), OperationType.INCOME
            ));
        }

        return saved;
    }
}
