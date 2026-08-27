package com.gaming.agricul.mapper;

import com.gaming.agricul.Investor;
import com.gaming.agricul.dto.InvestorDto;
import org.springframework.stereotype.Component;

@Component
public class InvestorMapper {

    public InvestorDto toDto(Investor investor) {
        return new InvestorDto(investor.getId(), investor.getFullName(), investor.getContribution());
    }

    public Investor toEntity(InvestorDto dto) {
        return new Investor(dto.id(), dto.fullName(), dto.contribution());
    }
}
