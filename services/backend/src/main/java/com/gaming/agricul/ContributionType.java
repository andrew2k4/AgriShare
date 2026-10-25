package com.gaming.agricul;

/**
 * Per the document's "Types de participants" table (§3). Only EQUITY and IN_KIND represent
 * real ownership and participate in the UC/profit-sharing engine ({@link #isEquityLike()});
 * LOAN and TEMPORARY_INVESTMENT are repayable capital (see {@link Repayment}) and LABOR is
 * valued separately — none of these three create ownership (doc §18: "Prêt ≠ participation",
 * "Travail ≠ argent").
 */
public enum ContributionType {
    EQUITY,
    IN_KIND,
    LOAN,
    TEMPORARY_INVESTMENT,
    LABOR;

    public boolean isEquityLike() {
        return this == EQUITY || this == IN_KIND;
    }
}
