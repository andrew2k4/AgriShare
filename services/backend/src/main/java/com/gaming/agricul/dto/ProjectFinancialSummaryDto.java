package com.gaming.agricul.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Financial dashboard for a project (doc §19)")
public record ProjectFinancialSummaryDto(

        @Schema(description = "Project identifier")
        Long projectId,

        @Schema(description = "Current estimated project value (equity book value, net of withdrawals)")
        BigDecimal currentProjectValue,

        @Schema(description = "Total EQUITY/IN_KIND capital ever contributed (gross, before withdrawals)")
        BigDecimal totalCapitalInvested,

        @Schema(description = "Outstanding LOAN/TEMPORARY_INVESTMENT principal still to be repaid")
        BigDecimal remainingTemporaryCapital,

        @Schema(description = "Documented value of assets still owned by their contributors (leasable, not capital)")
        BigDecimal assetsValueOwnedByContributors,

        @Schema(description = "Sum of all currently active leases' monthly rent")
        BigDecimal currentMonthlyRent,

        @Schema(description = "All-time revenue (INCOME operations)")
        BigDecimal totalRevenue,

        @Schema(description = "All-time expenses (EXPENSE operations)")
        BigDecimal totalExpenses,

        @Schema(description = "totalRevenue - totalExpenses, all-time")
        BigDecimal netProfitAllTime,

        @Schema(description = "Cumulative reserve set aside across all validated distributions")
        BigDecimal totalReserveAccumulated,

        @Schema(description = "Total amount already paid out across all validated distributions")
        BigDecimal totalDistributedToDate,

        @Schema(description = "Current ownership breakdown across equity-like investors")
        List<InvestorShareDto> currentShares
) {}
