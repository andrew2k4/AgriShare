package com.gaming.agricul.mapper;

import com.gaming.agricul.ProductionDay;
import com.gaming.agricul.dto.ProductionDayDto;
import org.springframework.stereotype.Component;

@Component
public class ProductionDayMapper {

    public ProductionDayDto toDto(ProductionDay productionDay) {
        return new ProductionDayDto(productionDay.getId(), productionDay.getDate(), productionDay.getQuantity());
    }

    public ProductionDay toEntity(ProductionDayDto dto) {
        return new ProductionDay(dto.id(), dto.date(), dto.quantity());
    }
}
