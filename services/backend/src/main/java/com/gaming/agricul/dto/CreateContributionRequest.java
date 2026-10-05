package com.gaming.agricul.dto;

import com.gaming.agricul.ContributionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request to record a new investor contribution to a project")
public record CreateContributionRequest(

        @Schema(description = "Investor identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long investorId,

        @Schema(description = "Contribution type; defaults to EQUITY if omitted", example = "EQUITY")
        ContributionType type,

        @Schema(description = "Contributed/valued amount, must be positive", example = "2000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Entry date", example = "2026-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate entryDate,

        @Schema(description = "Asset being contributed to capital; required for IN_KIND, ignored otherwise")
        Long assetId
) {}
