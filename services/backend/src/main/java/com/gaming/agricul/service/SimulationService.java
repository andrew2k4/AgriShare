package com.gaming.agricul.service;

import com.gaming.agricul.Asset;
import com.gaming.agricul.LeaseStatus;
import com.gaming.agricul.dto.AssetUsageSimulationDto;
import com.gaming.agricul.dto.ProjectUsageSimulationDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.AssetRepository;
import com.gaming.agricul.repository.LeaseRepository;
import com.gaming.agricul.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * "Tout le monde paie un loyer" simulation (doc §9.4): compares the hypothetical cost of
 * using every asset as if rented against what is actually paid today, to measure a project's
 * real cost of production regardless of who happens to own which asset.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulationService {

    private static final BigDecimal DEFAULT_MONTHLY_RATE_PERCENT = new BigDecimal("1.0");

    private final ProjectRepository projectRepository;
    private final AssetRepository assetRepository;
    private final LeaseRepository leaseRepository;

    public ProjectUsageSimulationDto simulateAssetUsage(Long projectId, BigDecimal monthlyRatePercent) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        BigDecimal rate = monthlyRatePercent != null ? monthlyRatePercent : DEFAULT_MONTHLY_RATE_PERCENT;
        List<Asset> assets = assetRepository.findByProjectId(projectId);

        List<AssetUsageSimulationDto> perAsset = assets.stream()
                .map(a -> {
                    BigDecimal value = a.getCurrentValue() != null ? a.getCurrentValue() : a.getInitialValue();
                    BigDecimal cost = value.multiply(rate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    return new AssetUsageSimulationDto(a.getId(), a.getType(), value, cost);
                })
                .toList();

        BigDecimal totalHypothetical = perAsset.stream()
                .map(AssetUsageSimulationDto::hypotheticalMonthlyCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actualCurrentMonthlyRent = assets.stream()
                .map(a -> leaseRepository.findFirstByAssetIdAndStatus(a.getId(), LeaseStatus.ACTIVE))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getMonthlyRent())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProjectUsageSimulationDto(projectId, rate, perAsset, totalHypothetical, actualCurrentMonthlyRent);
    }
}
