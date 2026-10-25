package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "An investor's current ownership share of a project, based on Units of Contribution")
public record InvestorShareDto(

        @Schema(description = "Investor identifier")
        Long investorId,

        @Schema(description = "Current ownership percentage (0-100)")
        BigDecimal sharePercentage
) {}
