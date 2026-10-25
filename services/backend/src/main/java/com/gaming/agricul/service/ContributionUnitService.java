package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectCapital;
import com.gaming.agricul.repository.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Computes a contribution's Unités de Contribution (UC), per the document's formula:
 * {@code UC = valeur × facteurTemps × facteurRisque × facteurImpact} (§5-8, pseudo-code §17).
 */
@Service
@RequiredArgsConstructor
public class ContributionUnitService {

    private static final MathContext UC_PRECISION = new MathContext(12, RoundingMode.HALF_UP);

    private final FinancialParameterService financialParameterService;
    private final WithdrawalRepository withdrawalRepository;

    public BigDecimal computeUnits(Contribution contribution, Project project, LocalDate asOfDate) {
        BigDecimal effectiveAmount = contribution.getAmount()
                .subtract(withdrawalRepository.sumValidatedAmountByContributionId(contribution.getId()));
        BigDecimal timeFactor = computeTimeFactor(contribution, project.getCapital(), asOfDate);
        BigDecimal riskFactor = financialParameterService.getRiskFactor(project.getId(), contribution.getPhaseAtEntry());
        BigDecimal impactFactor = financialParameterService.getImpactFactor(project.getId());

        return effectiveAmount
                .multiply(timeFactor, UC_PRECISION)
                .multiply(riskFactor, UC_PRECISION)
                .multiply(impactFactor, UC_PRECISION);
    }

    /**
     * FT = MIN(1, joursCapitalEngagé / dureeDeReférenceEnJours). Without a configured
     * reference duration, the time factor is neutral (1) rather than blocking the
     * calculation on an admin setup step.
     */
    private BigDecimal computeTimeFactor(Contribution contribution, ProjectCapital capital, LocalDate asOfDate) {
        if (capital == null || capital.getReferenceDurationDays() == null || capital.getReferenceDurationDays() <= 0) {
            return BigDecimal.ONE;
        }
        if (asOfDate.isBefore(contribution.getEntryDate())) {
            return BigDecimal.ZERO;
        }
        long daysEngaged = ChronoUnit.DAYS.between(contribution.getEntryDate(), asOfDate);
        BigDecimal ratio = BigDecimal.valueOf(daysEngaged)
                .divide(BigDecimal.valueOf(capital.getReferenceDurationDays()), UC_PRECISION);
        return ratio.min(BigDecimal.ONE);
    }
}
