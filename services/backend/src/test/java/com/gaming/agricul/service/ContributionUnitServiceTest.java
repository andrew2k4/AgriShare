package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.Project;
import com.gaming.agricul.ProjectCapital;
import com.gaming.agricul.ProjectPhase;
import com.gaming.agricul.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionUnitServiceTest {

    @Mock
    private FinancialParameterService financialParameterService;
    @Mock
    private WithdrawalRepository withdrawalRepository;

    private ContributionUnitService contributionUnitService;

    @BeforeEach
    void setUp() {
        contributionUnitService = new ContributionUnitService(financialParameterService, withdrawalRepository);
        lenient().when(withdrawalRepository.sumValidatedAmountByContributionId(any())).thenReturn(BigDecimal.ZERO);
    }

    private Project projectWith(ProjectCapital capital) {
        Project project = new Project();
        project.setId(1L);
        project.setCapital(capital);
        return project;
    }

    @Test
    void computeUnits_withoutReferenceDuration_usesNeutralTimeFactor() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.of(2026, 1, 1), BigDecimal.ZERO, ProjectPhase.STABLE, null);
        Project project = projectWith(new ProjectCapital(null, null, null, null, null, ProjectPhase.STABLE));
        when(financialParameterService.getRiskFactor(1L, ProjectPhase.STABLE)).thenReturn(BigDecimal.ONE);
        when(financialParameterService.getImpactFactor(1L)).thenReturn(BigDecimal.ONE);

        BigDecimal units = contributionUnitService.computeUnits(contribution, project, LocalDate.of(2026, 6, 1));

        assertThat(units).isEqualByComparingTo("1000000");
    }

    @Test
    void computeUnits_appliesTimeFactor_whenReferenceDurationConfigured() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.of(2026, 1, 1), BigDecimal.ZERO, ProjectPhase.STABLE, null);
        // 180 days engaged out of a 360-day reference -> time factor = 0.5
        Project project = projectWith(new ProjectCapital(null, null, null, null, 360, ProjectPhase.STABLE));
        when(financialParameterService.getRiskFactor(1L, ProjectPhase.STABLE)).thenReturn(BigDecimal.ONE);
        when(financialParameterService.getImpactFactor(1L)).thenReturn(BigDecimal.ONE);

        BigDecimal units = contributionUnitService.computeUnits(contribution, project, LocalDate.of(2026, 6, 30));

        assertThat(units).isEqualByComparingTo("500000");
    }

    @Test
    void computeUnits_capsTimeFactorAtOne_beyondReferenceDuration() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.of(2025, 1, 1), BigDecimal.ZERO, ProjectPhase.STABLE, null);
        Project project = projectWith(new ProjectCapital(null, null, null, null, 100, ProjectPhase.STABLE));
        when(financialParameterService.getRiskFactor(1L, ProjectPhase.STABLE)).thenReturn(BigDecimal.ONE);
        when(financialParameterService.getImpactFactor(1L)).thenReturn(BigDecimal.ONE);

        BigDecimal units = contributionUnitService.computeUnits(contribution, project, LocalDate.of(2026, 1, 1));

        assertThat(units).isEqualByComparingTo("1000000");
    }

    @Test
    void computeUnits_multipliesByRiskAndImpactFactors() {
        Contribution contribution = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.of(2026, 1, 1), BigDecimal.ZERO, ProjectPhase.STARTUP, null);
        Project project = projectWith(new ProjectCapital(null, null, null, null, null, ProjectPhase.STARTUP));
        when(financialParameterService.getRiskFactor(1L, ProjectPhase.STARTUP)).thenReturn(new BigDecimal("1.30"));
        when(financialParameterService.getImpactFactor(1L)).thenReturn(new BigDecimal("1.10"));

        BigDecimal units = contributionUnitService.computeUnits(contribution, project, LocalDate.of(2026, 1, 1));

        assertThat(units).isEqualByComparingTo("1430000.00");
    }
}
