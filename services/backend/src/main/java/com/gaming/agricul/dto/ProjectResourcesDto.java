package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project resources")
public record ProjectResourcesDto(

        @Schema(description = "Initial budget allocated to the project", example = "50000.0")
        double budget,

        @Schema(description = "Land size in hectares", example = "2.5")
        double landSize,

        @Schema(description = "Whether existing structures are present on the land", example = "true")
        boolean existingStructures,

        @Schema(description = "Additional notes", example = "Fenced land with a well")
        String notes
) {}
