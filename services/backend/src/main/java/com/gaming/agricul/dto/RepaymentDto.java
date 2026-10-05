package com.gaming.agricul.dto;

import com.gaming.agricul.RepaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "A planned or paid repayment of principal + interest for a LOAN or TEMPORARY_INVESTMENT contribution")
public record RepaymentDto(

        @Schema(description = "Repayment identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Contribution being repaid", accessMode = Schema.AccessMode.READ_ONLY)
        Long contributionId,

        @Schema(description = "Principal to repay", example = "2000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal principalAmount,

        @Schema(description = "Interest/compensation agreed", example = "100000.00")
        BigDecimal interestAmount,

        @Schema(description = "Due date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate,

        @Schema(description = "Status", accessMode = Schema.AccessMode.READ_ONLY)
        RepaymentStatus status,

        @Schema(description = "Payment timestamp, once paid", accessMode = Schema.AccessMode.READ_ONLY)
        Instant paidAt
) {}
