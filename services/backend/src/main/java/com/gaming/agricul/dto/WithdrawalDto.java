package com.gaming.agricul.dto;

import com.gaming.agricul.WithdrawalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "A capital withdrawal request against a contribution")
public record WithdrawalDto(

        @Schema(description = "Withdrawal identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Contribution this withdrawal is taken from")
        Long contributionId,

        @Schema(description = "Project identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long projectId,

        @Schema(description = "Investor identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long investorId,

        @Schema(description = "Requested amount", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal requestedAmount,

        @Schema(description = "Penalty rate applied at validation (0-1)", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal penaltyRate,

        @Schema(description = "Resulting loss for the investor", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal resultingLoss,

        @Schema(description = "Net amount actually paid out", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal netAmountPaid,

        @Schema(description = "Status", accessMode = Schema.AccessMode.READ_ONLY)
        WithdrawalStatus status,

        @Schema(description = "Reason the withdrawal is blocked, if applicable", accessMode = Schema.AccessMode.READ_ONLY)
        String blockReason,

        @Schema(description = "AppUser id who requested this withdrawal", accessMode = Schema.AccessMode.READ_ONLY)
        Long requestedBy,

        @Schema(description = "Request timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant requestedAt,

        @Schema(description = "AppUser id who validated or rejected this withdrawal", accessMode = Schema.AccessMode.READ_ONLY)
        Long validatedBy,

        @Schema(description = "Validation/rejection timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        Instant validatedAt
) {}
