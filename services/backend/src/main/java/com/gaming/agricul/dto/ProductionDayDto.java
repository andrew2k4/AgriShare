package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Daily production record")
public record ProductionDayDto(

        @Schema(description = "Production day identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Production date", example = "2025-04-15", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(description = "Quantity produced on that day", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        int quantity
) {}
