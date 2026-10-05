package com.gaming.agricul;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One investor's share of a proposed/validated {@link ProfitDistribution}. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDistributionLine {

    private Long contributionId;
    private Long investorId;
    private BigDecimal unitsOfContribution;
    private BigDecimal sharePercentage;
    private BigDecimal amount;
}
