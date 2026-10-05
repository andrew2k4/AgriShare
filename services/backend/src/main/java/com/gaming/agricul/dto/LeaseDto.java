package com.gaming.agricul.dto;

import com.gaming.agricul.LeaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Terms under which a project pays rent for an asset it does not own")
public record LeaseDto(

        @Schema(description = "Lease identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Leased asset", accessMode = Schema.AccessMode.READ_ONLY)
        Long assetId,

        @Schema(description = "Monthly rent", example = "80000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal monthlyRent,

        @Schema(description = "Lease start date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate startDate,

        @Schema(description = "Lease end date, if ended")
        LocalDate endDate,

        @Schema(description = "Status", accessMode = Schema.AccessMode.READ_ONLY)
        LeaseStatus status
) {}
