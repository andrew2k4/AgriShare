package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to set (create or override) a financial coefficient")
public record SetFinancialParameterRequest(

        @Schema(description = "Parameter key, e.g. RISK_FACTOR:STABLE or IMPACT_FACTOR", example = "RISK_FACTOR:STABLE", requiredMode = Schema.RequiredMode.REQUIRED)
        String paramKey,

        @Schema(description = "Project to override; omit for a global default")
        Long projectId,

        @Schema(description = "Coefficient value", example = "1.10", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value
) {}
