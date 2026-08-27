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
public class ProjectCapital {

    private Double initialCapital;
    private Double currentCapital;
    private Double totalInvestment;
    private Double totalExpenses;
}