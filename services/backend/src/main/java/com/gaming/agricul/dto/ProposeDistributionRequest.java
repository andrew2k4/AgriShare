package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request to propose a new profit distribution for a project and period")
public record ProposeDistributionRequest(

        @Schema(description = "Period start", example = "2026-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate periodStart,

        @Schema(description = "Period end", example = "2026-03-31", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate periodEnd,

        @Schema(description = "Taxes to deduct from the period's result", example = "0")
        BigDecimal taxes,

        @Schema(description = "Reserve to keep aside before distributing", example = "0")
        BigDecimal reserve,

        @Schema(description = "Amount to reinvest into the project before distributing", example = "0")
        BigDecimal reinvestment
) {}
