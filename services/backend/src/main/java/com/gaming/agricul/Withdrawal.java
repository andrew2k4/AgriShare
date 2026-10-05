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
import java.time.Instant;

/**
 * A request to withdraw part (or all) of a contribution's capital. The requested amount
 * leaves the investor's ownership position (see {@code ContributionService#currentProjectValue}
 * and {@code ContributionUnitService}), but a configurable penalty may reduce what is
 * actually paid out (Q10 of the approved spec: a withdrawal can cause a financial loss).
 * Validation requires a different Admin/Manager than the one who requested it.
 */
@Entity
@Table(name = "withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contribution_id", nullable = false)
    private Long contributionId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "penalty_rate", precision = 10, scale = 4)
    private BigDecimal penaltyRate;

    @Column(name = "resulting_loss", precision = 19, scale = 4)
    private BigDecimal resultingLoss;

    @Column(name = "net_amount_paid", precision = 19, scale = 4)
    private BigDecimal netAmountPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;
}
