package com.gaming.agricul.dto;

import com.gaming.agricul.AssetDisposition;
import com.gaming.agricul.AssetOwnershipStatus;
import com.gaming.agricul.AssetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A physical good (land, farm, machine...) brought to or used by a project")
public record AssetDto(

        @Schema(description = "Asset identifier", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "Project using this asset", accessMode = Schema.AccessMode.READ_ONLY)
        Long projectId,

        @Schema(description = "Investor who owns this asset", requiredMode = Schema.RequiredMode.REQUIRED)
        Long ownerInvestorId,

        @Schema(description = "Asset type", example = "BUILDING", requiredMode = Schema.RequiredMode.REQUIRED)
        AssetType type,

        @Schema(description = "Free-text description", example = "Ferme + bâtiments + équipements")
        String description,

        @Schema(description = "Documented initial value", example = "8000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal initialValue,

        @Schema(description = "Current value, if reassessed")
        BigDecimal currentValue,

        @Schema(description = "Whether the asset is owned by its contributor (leasable) or transferred to the project's capital", accessMode = Schema.AccessMode.READ_ONLY)
        AssetOwnershipStatus ownershipStatus,

        @Schema(description = "Settlement at project closure, only meaningful for TRANSFERRED_TO_PROJECT assets", accessMode = Schema.AccessMode.READ_ONLY)
        AssetDisposition disposition,

        @Schema(description = "Settlement amount, once disposed", accessMode = Schema.AccessMode.READ_ONLY)
        BigDecimal disposalAmount,

        @Schema(description = "Buyer investor id, if RETAINED_BY_INVESTOR", accessMode = Schema.AccessMode.READ_ONLY)
        Long disposalInvestorId
) {}
