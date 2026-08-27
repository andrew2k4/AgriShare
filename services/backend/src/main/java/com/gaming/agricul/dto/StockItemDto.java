package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stock item")
public record StockItemDto(

        @Schema(description = "Item name", example = "Fertilizer bag", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Quantity in stock", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
        int quantity,

        @Schema(description = "Unit of measure", example = "kg", requiredMode = Schema.RequiredMode.REQUIRED)
        String unit
) {}
