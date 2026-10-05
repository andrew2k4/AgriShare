package com.gaming.agricul.mapper;

import com.gaming.agricul.ProfitDistribution;
import com.gaming.agricul.ProfitDistributionLine;
import com.gaming.agricul.dto.ProfitDistributionDto;
import com.gaming.agricul.dto.ProfitDistributionLineDto;
import org.springframework.stereotype.Component;

@Component
public class ProfitDistributionMapper {

    public ProfitDistributionDto toDto(ProfitDistribution distribution) {
        return new ProfitDistributionDto(
                distribution.getId(),
                distribution.getProjectId(),
                distribution.getPeriodStart(),
                distribution.getPeriodEnd(),
                distribution.getRevenue(),
                distribution.getOperatingCosts(),
                distribution.getTaxes(),
                distribution.getReserve(),
                distribution.getReinvestment(),
                distribution.getDistributableProfit(),
                distribution.getStatus(),
                distribution.getProposedBy(),
                distribution.getProposedAt(),
                distribution.getValidatedBy(),
                distribution.getValidatedAt(),
                distribution.getLines().stream().map(this::toLineDto).toList()
        );
    }

    private ProfitDistributionLineDto toLineDto(ProfitDistributionLine line) {
        return new ProfitDistributionLineDto(
                line.getContributionId(),
                line.getInvestorId(),
                line.getUnitsOfContribution(),
                line.getSharePercentage(),
                line.getAmount()
        );
    }
}
