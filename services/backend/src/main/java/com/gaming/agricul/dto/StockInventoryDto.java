package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Project stock inventory")
public record StockInventoryDto(

        @Schema(description = "Inventory identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "List of items in stock")
        List<StockItemDto> items
) {}
