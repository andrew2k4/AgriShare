package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Return on investment for an investor's equity-like position in a project (doc §13)")
public record RoiDto(

        @Schema(description = "Investor identifier")
        Long investorId,

        @Schema(description = "Project identifier")
        Long projectId,

        @Schema(description = "Total EQUITY/IN_KIND capital invested")
        BigDecimal totalInvested,

        @Schema(description = "Total received so far (profit distributions + validated withdrawals)")
        BigDecimal totalReceived,

        @Schema(description = "ROI = (totalReceived - totalInvested) / totalInvested * 100")
        BigDecimal roiPercent,

        @Schema(description = "Annualized ROI: (1 + ROI)^(365/daysInvested) - 1, as a percentage")
        BigDecimal annualizedRoiPercent,

        @Schema(description = "Days since the earliest equity-like contribution")
        long daysInvested
) {}
