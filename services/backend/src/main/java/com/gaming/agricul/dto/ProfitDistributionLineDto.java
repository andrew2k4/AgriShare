package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "One investor's share of a profit distribution")
public record ProfitDistributionLineDto(

        @Schema(description = "Contribution this line is based on")
        Long contributionId,

        @Schema(description = "Investor identifier")
        Long investorId,

        @Schema(description = "Units of Contribution (UC) at the time of this distribution")
        BigDecimal unitsOfContribution,

        @Schema(description = "Share of the distributable profit (0-100)")
        BigDecimal sharePercentage,

        @Schema(description = "Amount paid to this investor")
        BigDecimal amount
) {}
