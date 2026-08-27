package com.gaming.agricul.mapper;

import com.gaming.agricul.Operation;
import com.gaming.agricul.dto.OperationDto;
import org.springframework.stereotype.Component;

@Component
public class OperationMapper {

    public OperationDto toDto(Operation operation) {
        return new OperationDto(
                operation.getId(),
                operation.getLabel(),
                operation.getDate(),
                operation.getAmount(),
                operation.getOperationType()
        );
    }

    public Operation toEntity(OperationDto dto) {
        return new Operation(
                dto.id(),
                dto.label(),
                dto.date(),
                dto.amount(),
                dto.operationType()
        );
    }
}
