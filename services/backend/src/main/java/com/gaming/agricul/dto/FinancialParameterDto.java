package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "A configurable financial coefficient (risk factor, impact factor...)")
public record FinancialParameterDto(

        @Schema(description = "Parameter identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Parameter key", example = "RISK_FACTOR:STABLE", requiredMode = Schema.RequiredMode.REQUIRED)
        String paramKey,

        @Schema(description = "Project this override applies to; null means a global default")
        Long projectId,

        @Schema(description = "Coefficient value", example = "1.10", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal value,

        @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant updatedAt
) {}
