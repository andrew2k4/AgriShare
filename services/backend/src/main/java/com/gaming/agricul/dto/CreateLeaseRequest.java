package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request to lease an asset to a project")
public record CreateLeaseRequest(

        @Schema(description = "Monthly rent, must be positive", example = "80000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal monthlyRent,

        @Schema(description = "Lease start date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate
) {}
