package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectPhase;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.InvestorRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final ProjectRepository projectRepository;
    private final InvestorRepository investorRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final AssetRepository assetRepository;
    private final LeaseRepository leaseRepository;

    @Transactional(readOnly = true)
    public List<Contribution> findByProject(Long projectId) {
        ensureProjectExists(projectId);
        return contributionRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<Contribution> findByInvestor(Long investorId) {
        return contributionRepository.findByInvestorId(investorId);
    }

    /**
     * Book value of the project's permanent equity: the sum of EQUITY/IN_KIND contributions
     * recorded so far, minus any validated withdrawal. LOAN/TEMPORARY_INVESTMENT capital is
     * excluded — it is repayable, not part of the equity base new investors buy into (doc
     * §18: "Prêt ≠ participation"). This is a v1 simplification (no mark-to-market of
     * profit/assets) — see the implementation plan for why this is documented as a
     * deliberate, revisitable choice.
     */
    @Transactional(readOnly = true)
    public BigDecimal currentProjectValue(Long projectId) {
        BigDecimal totalContributed = contributionRepository.findByProjectId(projectId).stream()
                .filter(c -> c.getType().isEquityLike())
                .map(Contribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWithdrawn = withdrawalRepository.sumValidatedAmountByProjectId(projectId);
        return totalContributed.subtract(totalWithdrawn);
    }

    /**
     * Records a new contribution. For EQUITY/IN_KIND, snapshots the project's value
     * immediately before it (valorisation à l'entrée) so the ownership percentage it bought
     * can be reconstructed later. For IN_KIND, the referenced {@link Asset} is transferred to
     * the project's capital as a side effect — rejected if it is currently under an active
     * lease, to avoid counting its value twice (doc §18). LOAN/TEMPORARY_INVESTMENT/LABOR
     * contributions don't participate in ownership; see {@link ContributionType#isEquityLike()}.
     */
    public Contribution addContribution(
            Long projectId, Long investorId, ContributionType type, BigDecimal amount, LocalDate entryDate, Long assetId) {
        ContributionType effectiveType = type != null ? type : ContributionType.EQUITY;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Contribution amount must be positive");
        }
        if (entryDate == null) {
            throw new BusinessRuleException("Entry date is required");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));
        if (!investorRepository.existsById(investorId)) {
            throw new NotFoundException("Investor not found with id: " + investorId);
        }

        if (effectiveType == ContributionType.IN_KIND) {
            assetId = transferAssetToCapital(assetId, projectId, investorId);
        } else if (assetId != null) {
            throw new BusinessRuleException("assetId is only applicable to IN_KIND contributions");
        }

        BigDecimal valuationBeforeEntry = currentProjectValue(projectId);
        ProjectPhase phaseAtEntry = project.getCapital() != null ? project.getCapital().getCurrentPhase() : null;
        Contribution contribution = new Contribution(
                null, projectId, investorId, effectiveType, amount, entryDate, valuationBeforeEntry, phaseAtEntry, assetId
        );
        return contributionRepository.save(contribution);
    }

    private Long transferAssetToCapital(Long assetId, Long projectId, Long investorId) {
        if (assetId == null) {
            throw new BusinessRuleException("assetId is required for IN_KIND contributions");
        }
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Asset not found with id: " + assetId));
        if (!asset.getProjectId().equals(projectId) || !asset.getOwnerInvestorId().equals(investorId)) {
            throw new BusinessRuleException("This asset does not belong to this investor on this project");
        }
        if (leaseRepository.findFirstByAssetIdAndStatus(assetId, LeaseStatus.ACTIVE).isPresent()) {
            throw new BusinessRuleException(
                    "This asset is currently under an active lease; end the lease before contributing it to capital");
        }
        asset.setOwnershipStatus(AssetOwnershipStatus.TRANSFERRED_TO_PROJECT);
        assetRepository.save(asset);
        return assetId;
    }

    private void ensureProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
    }
}
