package com.gaming.agricul.service;

import com.gaming.agricul.FinancialParameterConfig;
import com.gaming.agricul.ProjectPhase;
import com.gaming.agricul.repository.FinancialParameterConfigRepository;
import com.gaming.agricul.repository.FinancialParameterHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialParameterServiceTest {

    @Mock
    private FinancialParameterConfigRepository configRepository;
    @Mock
    private FinancialParameterHistoryRepository historyRepository;

    private FinancialParameterService service;

    @BeforeEach
    void setUp() {
        service = new FinancialParameterService(configRepository, historyRepository);
    }

    @Test
    void getRiskFactor_returnsNeutralFactor_whenNothingConfigured() {
        when(configRepository.findByParamKeyAndProjectId("RISK_FACTOR:STABLE", 1L)).thenReturn(Optional.empty());
        when(configRepository.findByParamKeyAndProjectIdIsNull("RISK_FACTOR:STABLE")).thenReturn(Optional.empty());

        BigDecimal factor = service.getRiskFactor(1L, ProjectPhase.STABLE);

        assertThat(factor).isEqualByComparingTo("1");
    }

    @Test
    void getRiskFactor_returnsNeutralFactor_whenPhaseIsNull() {
        BigDecimal factor = service.getRiskFactor(1L, null);

        assertThat(factor).isEqualByComparingTo("1");
    }

    @Test
    void getRiskFactor_prefersProjectOverride_overGlobalDefault() {
        FinancialParameterConfig override = new FinancialParameterConfig(2L, "RISK_FACTOR:STARTUP", 5L, new BigDecimal("1.50"), null, null);
        when(configRepository.findByParamKeyAndProjectId("RISK_FACTOR:STARTUP", 5L)).thenReturn(Optional.of(override));

        BigDecimal factor = service.getRiskFactor(5L, ProjectPhase.STARTUP);

        assertThat(factor).isEqualByComparingTo("1.50");
    }

    @Test
    void getRiskFactor_fallsBackToGlobalDefault_whenNoProjectOverride() {
        FinancialParameterConfig global = new FinancialParameterConfig(1L, "RISK_FACTOR:STARTUP", null, new BigDecimal("1.30"), null, null);
        when(configRepository.findByParamKeyAndProjectId("RISK_FACTOR:STARTUP", 5L)).thenReturn(Optional.empty());
        when(configRepository.findByParamKeyAndProjectIdIsNull("RISK_FACTOR:STARTUP")).thenReturn(Optional.of(global));

        BigDecimal factor = service.getRiskFactor(5L, ProjectPhase.STARTUP);

        assertThat(factor).isEqualByComparingTo("1.30");
    }

    @Test
    void setParameter_createsConfigAndAppendsHistory() {
        when(configRepository.findByParamKeyAndProjectId("IMPACT_FACTOR", null)).thenReturn(Optional.empty());
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<com.gaming.agricul.FinancialParameterHistory> historyCaptor =
                ArgumentCaptor.forClass(com.gaming.agricul.FinancialParameterHistory.class);

        service.setParameter("IMPACT_FACTOR", null, new BigDecimal("1.10"), 7L);

        verify(historyRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getOldValue()).isNull();
        assertThat(historyCaptor.getValue().getNewValue()).isEqualByComparingTo("1.10");
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo(7L);
    }
}
