package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.WithdrawalStatus;
import com.gaming.agricul.dto.RoiDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Return on investment (doc §13): ROI = (totalReçu - totalInvesti) / totalInvesti * 100, plus
 * an annualized figure to compare investments of different durations. Scoped to EQUITY/IN_KIND
 * positions — LOAN/TEMPORARY_INVESTMENT returns are a fixed rate already known at contract
 * time, not a profit-sharing ROI.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoiService {

    private final ContributionRepository contributionRepository;
    private final ProfitDistributionRepository profitDistributionRepository;
    private final WithdrawalRepository withdrawalRepository;

    public RoiDto compute(Long projectId, Long investorId) {
        List<Contribution> equityContributions = contributionRepository.findByProjectId(projectId).stream()
                .filter(c -> c.getInvestorId().equals(investorId) && c.getType().isEquityLike())
                .toList();
        if (equityContributions.isEmpty()) {
            throw new NotFoundException(
                    "No equity-like contribution found for investor " + investorId + " on project " + projectId);
        }

        BigDecimal totalInvested = equityContributions.stream()
                .map(Contribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate earliestEntry = equityContributions.stream()
                .map(Contribution::getEntryDate)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        BigDecimal fromDistributions = profitDistributionRepository.findByProjectId(projectId).stream()
                .filter(d -> d.getStatus() == DistributionStatus.VALIDATED)
                .flatMap(d -> d.getLines().stream())
                .filter(l -> l.getInvestorId().equals(investorId))
                .map(com.gaming.agricul.ProfitDistributionLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fromWithdrawals = withdrawalRepository.findByProjectIdAndInvestorId(projectId, investorId).stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.VALIDATED)
                .map(com.gaming.agricul.Withdrawal::getNetAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceived = fromDistributions.add(fromWithdrawals);

        BigDecimal roiPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalReceived.subtract(totalInvested)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalInvested, 4, RoundingMode.HALF_UP);

        long daysInvested = ChronoUnit.DAYS.between(earliestEntry, LocalDate.now());
        BigDecimal annualizedRoiPercent = annualize(roiPercent, daysInvested);

        return new RoiDto(investorId, projectId, totalInvested, totalReceived, roiPercent, annualizedRoiPercent, daysInvested);
    }

    private BigDecimal annualize(BigDecimal roiPercent, long daysInvested) {
        if (daysInvested <= 0) {
            return BigDecimal.ZERO;
        }
        double base = 1 + roiPercent.doubleValue() / 100.0;
        if (base <= 0) {
            return BigDecimal.valueOf(-100);
        }
        double annualized = Math.pow(base, 365.0 / daysInvested) - 1;
        return BigDecimal.valueOf(annualized * 100).setScale(4, RoundingMode.HALF_UP);
    }
}
