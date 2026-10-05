package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Append-only audit trail of every change made to a {@link FinancialParameterConfig},
 * so past distribution calculations stay explainable (transparency principle, doc §20).
 */
@Entity
@Table(name = "financial_parameter_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialParameterHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", nullable = false)
    private String paramKey;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "old_value", precision = 10, scale = 4)
    private BigDecimal oldValue;

    @Column(name = "new_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal newValue;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
