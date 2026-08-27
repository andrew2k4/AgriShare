package com.gaming.agricul.dto;

import com.gaming.agricul.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Financial operation (income or expense)")
public record OperationDto(

        @Schema(description = "Operation identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Operation label", example = "Seed purchase", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,

        @Schema(description = "Operation date", example = "2025-04-10", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date,

        @Schema(description = "Operation amount", example = "1500.0", requiredMode = Schema.RequiredMode.REQUIRED)
        double amount,

        @Schema(description = "Operation type: INCOME or EXPENSE", example = "EXPENSE", requiredMode = Schema.RequiredMode.REQUIRED)
        OperationType operationType
) {}
