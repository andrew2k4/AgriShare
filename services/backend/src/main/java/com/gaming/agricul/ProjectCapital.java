package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(name = "reference_duration_days")
    private Integer referenceDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase")
    private ProjectPhase currentPhase;
}
