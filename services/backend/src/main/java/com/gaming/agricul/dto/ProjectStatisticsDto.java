package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project production and profitability statistics")
public record ProjectStatisticsDto(

        @Schema(description = "Cumulative total production (units)", example = "3500.0")
        Double totalProduction,

        @Schema(description = "Total revenue generated", example = "85000.0")
        Double totalRevenue,

        @Schema(description = "Total net profit", example = "40000.0")
        Double totalProfit,

        @Schema(description = "Productivity rate as a percentage", example = "78.5")
        Double productivityRate
) {}
