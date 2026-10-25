package com.gaming.agricul.dto;

import com.gaming.agricul.AssetDisposition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Request to settle a capital asset once its project is closed (doc Q11)")
public record DisposeAssetRequest(

        @Schema(description = "SOLD (to a third party) or RETAINED_BY_INVESTOR (an investor buys it)", requiredMode = Schema.RequiredMode.REQUIRED)
        AssetDisposition disposition,

        @Schema(description = "Sale/settlement proceeds credited to the project as income", example = "8500000.00")
        BigDecimal amount,

        @Schema(description = "Buyer investor id, required when disposition is RETAINED_BY_INVESTOR")
        Long buyerInvestorId
) {}
