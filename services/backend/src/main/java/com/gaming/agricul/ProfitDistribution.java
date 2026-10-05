package com.gaming.agricul;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A profit distribution for a project and period. Goes through a two-person workflow:
 * proposed (PROPOSED) by one Admin/Manager, then validated or rejected by a
 * <strong>different</strong> Admin/Manager (per the approved spec, Q6/Q15).
 */
@Entity
@Table(name = "profit_distributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal revenue;

    @Column(name = "operating_costs", nullable = false, precision = 19, scale = 4)
    private BigDecimal operatingCosts;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxes;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reserve;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reinvestment;

    @Column(name = "distributable_profit", nullable = false, precision = 19, scale = 4)
    private BigDecimal distributableProfit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DistributionStatus status;

    @Column(name = "proposed_by", nullable = false)
    private Long proposedBy;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @ElementCollection
    @CollectionTable(name = "profit_distribution_lines", joinColumns = @JoinColumn(name = "distribution_id"))
    private List<ProfitDistributionLine> lines = new ArrayList<>();
}
