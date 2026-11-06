package com.gaming.agricul;

public enum UserStatus {
    /** Registered with a password, e-mail not confirmed yet: cannot sign in. */
    PENDING,
    ACTIVE,
    /** Blocked by an administrator: cannot sign in, existing sessions are revoked. */
    SUSPENDED
}
