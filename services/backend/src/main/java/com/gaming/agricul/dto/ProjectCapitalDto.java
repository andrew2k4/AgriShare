package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project financial capital")
public record ProjectCapitalDto(

        @Schema(description = "Initial capital invested", example = "100000.0")
        Double initialCapital,

        @Schema(description = "Currently available capital", example = "75000.0")
        Double currentCapital,

        @Schema(description = "Total investments made", example = "120000.0")
        Double totalInvestment,

        @Schema(description = "Total expenses incurred", example = "45000.0")
        Double totalExpenses
) {}
