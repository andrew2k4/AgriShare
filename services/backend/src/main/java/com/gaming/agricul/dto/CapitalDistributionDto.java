package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Capital distribution among investors")
public record CapitalDistributionDto(

        @Schema(description = "Distribution identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @Schema(description = "List of shares per investor (total must equal 100%)")
        List<CapitalShareDto> shares
) {}
