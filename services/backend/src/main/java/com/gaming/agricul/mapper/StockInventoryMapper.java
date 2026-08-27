package com.gaming.agricul.mapper;

import com.gaming.agricul.StockInventory;
import com.gaming.agricul.StockItem;
import com.gaming.agricul.dto.StockInventoryDto;
import com.gaming.agricul.dto.StockItemDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockInventoryMapper {

    public StockInventoryDto toDto(StockInventory entity) {
        List<StockItemDto> items = entity.getItems().stream()
                .map(i -> new StockItemDto(i.getName(), i.getQuantity(), i.getUnit()))
                .toList();
        return new StockInventoryDto(entity.getId(), items);
    }

    public StockInventory toEntity(StockInventoryDto dto) {
        StockInventory entity = new StockInventory();
        entity.setId(dto.id());
        List<StockItem> items = dto.items().stream()
                .map(i -> new StockItem(i.name(), i.quantity(), i.unit()))
                .toList();
        entity.setItems(items);
        return entity;
    }
}
