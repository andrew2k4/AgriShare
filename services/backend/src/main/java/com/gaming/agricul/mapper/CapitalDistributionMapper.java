package com.gaming.agricul.mapper;

import com.gaming.agricul.CapitalDistribution;
import com.gaming.agricul.CapitalShare;
import com.gaming.agricul.dto.CapitalDistributionDto;
import com.gaming.agricul.dto.CapitalShareDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CapitalDistributionMapper {

    public CapitalDistributionDto toDto(CapitalDistribution entity) {
        List<CapitalShareDto> shares = entity.getShares().stream()
                .map(s -> new CapitalShareDto(s.getInvestorName(), s.getPercentage()))
                .toList();
        return new CapitalDistributionDto(entity.getId(), shares);
    }

    public CapitalDistribution toEntity(CapitalDistributionDto dto) {
        CapitalDistribution entity = new CapitalDistribution();
        entity.setId(dto.id());
        List<CapitalShare> shares = dto.shares().stream()
                .map(s -> new CapitalShare(s.investorName(), s.percentage()))
                .toList();
        entity.setShares(shares);
        return entity;
    }
}
