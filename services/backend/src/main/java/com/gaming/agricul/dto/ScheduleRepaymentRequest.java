package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request to schedule a repayment for a LOAN or TEMPORARY_INVESTMENT contribution")
public record ScheduleRepaymentRequest(

        @Schema(description = "Principal to repay, must be positive", example = "2000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal principalAmount,

        @Schema(description = "Interest/compensation agreed", example = "100000.00")
        BigDecimal interestAmount,

        @Schema(description = "Due date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dueDate
) {}
