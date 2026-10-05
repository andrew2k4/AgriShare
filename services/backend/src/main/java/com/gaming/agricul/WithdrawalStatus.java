package com.gaming.agricul;

public enum WithdrawalStatus {
    REQUESTED,
    VALIDATED,
    REJECTED,
    /** The project cannot currently absorb this withdrawal (would breach the configured minimum value). */
    BLOCKED
}
