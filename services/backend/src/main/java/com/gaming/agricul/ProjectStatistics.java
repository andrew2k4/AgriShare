package com.gaming.agricul;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatistics {

    private Double totalProduction;
    private Double totalRevenue;
    private Double totalProfit;
    private Double productivityRate;
}