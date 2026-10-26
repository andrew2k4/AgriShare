package com.gaming.agricul.dto;

import com.gaming.agricul.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to register a new asset for a project")
public record CreateAssetRequest(

        @Schema(description = "Investor who owns this asset", requiredMode = Schema.RequiredMode.REQUIRED)
        Long ownerInvestorId,

        @Schema(description = "Asset type", example = "BUILDING", requiredMode = Schema.RequiredMode.REQUIRED)
        AssetType type,

        @Schema(description = "Free-text description", example = "Ferme + bâtiments + équipements")
        String description,

        @Schema(description = "Documented initial value, must be positive", example = "8000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal initialValue
) {}
