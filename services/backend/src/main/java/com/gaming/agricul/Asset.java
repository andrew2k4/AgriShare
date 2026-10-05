package com.gaming.agricul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A physical good (land, farm, machine...) brought to or used by a project. Either
 * transferred to the project's capital (see {@code Contribution} of type IN_KIND) or
 * leased to it for rent while staying owned by the contributor (see {@link Lease}) — never
 * both, to avoid double-counting its value (doc §18).
 */
@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "owner_investor_id", nullable = false)
    private Long ownerInvestorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    private String description;

    @Column(name = "initial_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialValue;

    @Column(name = "current_value", precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "ownership_status", nullable = false)
    private AssetOwnershipStatus ownershipStatus;

    /** Only meaningful once the project is CLOSED, for assets TRANSFERRED_TO_PROJECT (Q11). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetDisposition disposition = AssetDisposition.PENDING;

    @Column(name = "disposal_amount", precision = 19, scale = 4)
    private BigDecimal disposalAmount;

    @Column(name = "disposal_investor_id")
    private Long disposalInvestorId;

    /** Optimistic lock: protects concurrent ownership/disposition transitions on the same asset. */
    @Version
    private Long version;
}
