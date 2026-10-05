package com.gaming.agricul.service;

import com.gaming.agricul.FinancialParameterConfig;
import com.gaming.agricul.FinancialParameterHistory;
import com.gaming.agricul.ProjectPhase;
import com.gaming.agricul.repository.FinancialParameterConfigRepository;
import com.gaming.agricul.repository.FinancialParameterHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Configurable coefficients (risk factor, impact factor) used by the Unités de Contribution
 * (UC) formula. Every change is versioned in {@link FinancialParameterHistory} so past
 * distribution calculations stay auditable (doc §20 - transparency & governance).
 *
 * Global defaults (projectId = null) can be overridden per project. When nothing has ever
 * been configured, a neutral factor of 1 is used so the engine works out of the box.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FinancialParameterService {

    public static final BigDecimal NEUTRAL_FACTOR = BigDecimal.ONE;
    private static final String IMPACT_FACTOR_KEY = "IMPACT_FACTOR";

    /** No floor on the project's value by default: withdrawals are never blocked unless an admin sets one. */
    public static final String MIN_PROJECT_VALUE_KEY = "MIN_PROJECT_VALUE";
    /** No penalty on withdrawals by default. */
    public static final String WITHDRAWAL_PENALTY_RATE_KEY = "WITHDRAWAL_PENALTY_RATE";

    private final FinancialParameterConfigRepository configRepository;
    private final FinancialParameterHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public BigDecimal getRiskFactor(Long projectId, ProjectPhase phase) {
        if (phase == null) {
            return NEUTRAL_FACTOR;
        }
        return resolve(riskFactorKey(phase), projectId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getImpactFactor(Long projectId) {
        return resolve(IMPACT_FACTOR_KEY, projectId);
    }

    /** Generic lookup for coefficients without a domain-specific neutral default (e.g. thresholds, rates). */
    @Transactional(readOnly = true)
    public BigDecimal resolveOrDefault(String paramKey, Long projectId, BigDecimal defaultValue) {
        if (projectId != null) {
            Optional<FinancialParameterConfig> projectOverride = configRepository.findByParamKeyAndProjectId(paramKey, projectId);
            if (projectOverride.isPresent()) {
                return projectOverride.get().getCoefficientValue();
            }
        }
        return configRepository.findByParamKeyAndProjectIdIsNull(paramKey)
                .map(FinancialParameterConfig::getCoefficientValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public List<FinancialParameterConfig> listGlobalDefaults() {
        return configRepository.findByProjectIdIsNull();
    }

    public FinancialParameterConfig setParameter(String paramKey, Long projectId, BigDecimal value, Long changedByUserId) {
        FinancialParameterConfig config = configRepository.findByParamKeyAndProjectId(paramKey, projectId)
                .orElseGet(() -> {
                    FinancialParameterConfig created = new FinancialParameterConfig();
                    created.setParamKey(paramKey);
                    created.setProjectId(projectId);
                    return created;
                });

        BigDecimal oldValue = config.getCoefficientValue();
        config.setCoefficientValue(value);
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(changedByUserId);
        FinancialParameterConfig saved = configRepository.save(config);

        FinancialParameterHistory history = new FinancialParameterHistory(
                null, paramKey, projectId, oldValue, value, changedByUserId, Instant.now()
        );
        historyRepository.save(history);

        return saved;
    }

    public static String riskFactorKey(ProjectPhase phase) {
        return "RISK_FACTOR:" + phase.name();
    }

    private BigDecimal resolve(String paramKey, Long projectId) {
        return resolveOrDefault(paramKey, projectId, NEUTRAL_FACTOR);
    }
}
