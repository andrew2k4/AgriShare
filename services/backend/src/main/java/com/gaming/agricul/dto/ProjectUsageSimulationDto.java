package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "\"Everyone pays rent\" simulation (doc §9.4): the hypothetical cost of using every " +
        "asset as if it were rented, compared to the rent actually being paid today")
public record ProjectUsageSimulationDto(

        Long projectId,

        @Schema(description = "Monthly rate applied to each asset's value, as a percentage")
        BigDecimal monthlyRatePercent,

        List<AssetUsageSimulationDto> perAsset,

        @Schema(description = "Sum of every asset's hypothetical monthly cost")
        BigDecimal totalHypotheticalMonthlyCost,

        @Schema(description = "Sum of currently active leases' monthly rent (what is actually paid today)")
        BigDecimal actualCurrentMonthlyRent
) {}
