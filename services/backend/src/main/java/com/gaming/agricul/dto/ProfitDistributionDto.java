package com.gaming.agricul.dto;

import com.gaming.agricul.DistributionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "A profit distribution proposed (and possibly validated) for a project and period")
public record ProfitDistributionDto(

        @Schema(description = "Distribution identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Project identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long projectId,

        @Schema(description = "Period start")
        LocalDate periodStart,

        @Schema(description = "Period end")
        LocalDate periodEnd,

        @Schema(description = "Revenue recorded for this period", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal revenue,

        @Schema(description = "Operating costs recorded for this period", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal operatingCosts,

        @Schema(description = "Taxes to deduct")
        BigDecimal taxes,

        @Schema(description = "Reserve to keep")
        BigDecimal reserve,

        @Schema(description = "Amount reinvested into the project")
        BigDecimal reinvestment,

        @Schema(description = "Distributable profit (floored at 0 in case of a loss)", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal distributableProfit,

        @Schema(description = "Status of the distribution", accessMode = Schema.AccessMode.READ_ONLY)
        DistributionStatus status,

        @Schema(description = "AppUser id who proposed this distribution", accessMode = Schema.AccessMode.READ_ONLY)
        Long proposedBy,

        @Schema(description = "Proposal timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant proposedAt,

        @Schema(description = "AppUser id who validated or rejected this distribution", accessMode = Schema.AccessMode.READ_ONLY)
        Long validatedBy,

        @Schema(description = "Validation/rejection timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant validatedAt,

        @Schema(description = "Per-investor breakdown")
        List<ProfitDistributionLineDto> lines
) {}
