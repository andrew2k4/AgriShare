package com.gaming.agricul.dto;

import com.gaming.agricul.ContributionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "An investor's contribution to a project, with its entry valuation")
public record ContributionDto(

        @Schema(description = "Contribution identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Project this contribution belongs to", accessMode = Schema.AccessMode.READ_ONLY)
        Long projectId,

        @Schema(description = "Investor identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long investorId,

        @Schema(description = "Contribution type", example = "EQUITY")
        ContributionType type,

        @Schema(description = "Contributed amount", example = "2000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @Schema(description = "Entry date", example = "2026-01-15", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate entryDate,

        @Schema(description = "Project value immediately before this contribution", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal valuationBeforeEntry,

        @Schema(description = "Ownership percentage this contribution bought at entry (0-100); 0 for non-equity types (LOAN, TEMPORARY_INVESTMENT, LABOR)", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal sharePercentageAtEntry,

        @Schema(description = "Asset contributed to capital, only set for IN_KIND contributions")
        Long assetId
) {}
