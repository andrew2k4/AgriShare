package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An investor's participation in a project. {@code valuationBeforeEntry} snapshots the
 * project's value (see {@code ContributionService#currentProjectValue}) right before this
 * contribution was recorded, so the ownership percentage it bought can always be
 * reconstructed later even if the valuation method evolves.
 */
@Entity
@Table(name = "contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "valuation_before_entry", nullable = false, precision = 19, scale = 4)
    private BigDecimal valuationBeforeEntry;

    /** Snapshot of the project's lifecycle phase at entry, used to look up the risk factor. */
    @Enumerated(EnumType.STRING)
    @Column(name = "phase_at_entry")
    private ProjectPhase phaseAtEntry;

    /** Set only for IN_KIND contributions: the {@link Asset} being contributed to capital. */
    @Column(name = "asset_id")
    private Long assetId;
}
