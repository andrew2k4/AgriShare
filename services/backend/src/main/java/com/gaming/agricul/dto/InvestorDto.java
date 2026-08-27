package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project investor")
public record InvestorDto(

        @Schema(description = "Investor identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Full name of the investor", example = "Marie Curie", requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @Schema(description = "Financial contribution amount", example = "25000.0", requiredMode = Schema.RequiredMode.REQUIRED)
        double contribution
) {}
