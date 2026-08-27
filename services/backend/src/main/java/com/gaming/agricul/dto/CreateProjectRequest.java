package com.gaming.agricul.dto;

import com.gaming.agricul.LivestockType;
import com.gaming.agricul.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload to create a new project")
public record CreateProjectRequest(

        @Schema(description = "Project name", example = "Corn Farm 2025", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Project type", example = "LIVESTOCK", requiredMode = Schema.RequiredMode.REQUIRED)
        ProjectType projectType,

        @Schema(description = "Livestock type — required when projectType is LIVESTOCK", example = "CHICKEN")
        LivestockType livestockType,

        @Schema(description = "Budget in FCFA", example = "5000000")
        Double budget,

        @Schema(description = "Land size in m²", example = "1000")
        Double landSize,

        @Schema(description = "Whether existing structures are present", example = "false")
        Boolean existingStructures,

        @Schema(description = "Additional notes", example = "Terrain proche de Douala")
        String notes
) {}
