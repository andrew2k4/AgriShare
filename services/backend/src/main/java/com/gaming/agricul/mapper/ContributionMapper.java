package com.gaming.agricul.mapper;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.dto.ContributionDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ContributionMapper {

    public ContributionDto toDto(Contribution contribution) {
        return new ContributionDto(
                contribution.getId(),
                contribution.getProjectId(),
                contribution.getInvestorId(),
                contribution.getType(),
                contribution.getAmount(),
                contribution.getEntryDate(),
                contribution.getValuationBeforeEntry(),
                sharePercentageAtEntry(contribution),
                contribution.getAssetId()
        );
    }

    private BigDecimal sharePercentageAtEntry(Contribution contribution) {
        if (!contribution.getType().isEquityLike()) {
            return BigDecimal.ZERO;
        }
        BigDecimal valuationAfterEntry = contribution.getValuationBeforeEntry().add(contribution.getAmount());
        if (valuationAfterEntry.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return contribution.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(valuationAfterEntry, 2, RoundingMode.HALF_UP);
    }
}
