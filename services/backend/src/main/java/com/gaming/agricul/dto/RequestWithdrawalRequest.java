package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to withdraw capital from a contribution")
public record RequestWithdrawalRequest(

        @Schema(description = "Amount to withdraw, must be positive and not exceed the remaining position", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount
) {}
