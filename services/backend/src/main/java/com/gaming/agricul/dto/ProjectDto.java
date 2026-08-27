package com.gaming.agricul.dto;

import com.gaming.agricul.LivestockType;
import com.gaming.agricul.ProjectType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Agricultural or livestock project")
public record ProjectDto(

        @Schema(description = "Project identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Firebase UID of the project owner", accessMode = Schema.AccessMode.READ_ONLY)
        String ownerUid,

        @Schema(description = "Project name", example = "Corn Farm 2025")
        String name,

        @Schema(description = "Project type", example = "AGRICULTURE", requiredMode = Schema.RequiredMode.REQUIRED)
        ProjectType projectType,

        @Schema(description = "Livestock type (only for LIVESTOCK projects)", example = "CHICKEN")
        LivestockType livestockType,

        @Schema(description = "Project resources")
        ProjectResourcesDto resources,

        @Schema(description = "Project financial capital")
        ProjectCapitalDto capital,

        @Schema(description = "Production and profitability statistics")
        ProjectStatisticsDto statistics,

        @Schema(description = "Recorded production days")
        List<ProductionDayDto> productions,

        @Schema(description = "Financial operations of the project")
        List<OperationDto> operations
) {}
