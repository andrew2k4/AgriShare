package com.gaming.agricul.dto;

import com.gaming.agricul.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Hypothetical monthly usage cost of one asset, as if it were rented")
public record AssetUsageSimulationDto(

        Long assetId,
        AssetType type,
        BigDecimal value,
        BigDecimal hypotheticalMonthlyCost
) {}
