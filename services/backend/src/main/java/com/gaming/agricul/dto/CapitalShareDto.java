package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Capital share of an investor")
public record CapitalShareDto(

        @Schema(description = "Investor name", example = "John Smith", requiredMode = Schema.RequiredMode.REQUIRED)
        String investorName,

        @Schema(description = "Participation percentage (0-100)", example = "35.0", requiredMode = Schema.RequiredMode.REQUIRED)
        double percentage
) {}
