package com.gaming.agricul.mapper;

import com.gaming.agricul.FinancialParameterConfig;
import com.gaming.agricul.dto.FinancialParameterDto;
import org.springframework.stereotype.Component;

@Component
public class FinancialParameterMapper {

    public FinancialParameterDto toDto(FinancialParameterConfig config) {
        return new FinancialParameterDto(
                config.getId(),
                config.getParamKey(),
                config.getProjectId(),
                config.getCoefficientValue(),
                config.getUpdatedAt()
        );
    }
}
