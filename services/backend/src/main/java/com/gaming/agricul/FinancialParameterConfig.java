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
 * Current value of a configurable financial coefficient (risk factor, impact factor...).
 * {@code projectId} null means a global default; a row with a non-null {@code projectId}
 * overrides the global default for that project only.
 */
@Entity
@Table(name = "financial_parameter_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialParameterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_key", nullable = false)
    private String paramKey;

    @Column(name = "project_id")
    private Long projectId;

    /** Named coefficientValue (not value): VALUE is a reserved SQL keyword in H2/Postgres. */
    @Column(name = "coefficient_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal coefficientValue;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
