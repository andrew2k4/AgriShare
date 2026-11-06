package com.gaming.agricul;

import java.time.Duration;

public enum UserTokenType {
    EMAIL_VERIFICATION(Duration.ofHours(24)),
    PASSWORD_RESET(Duration.ofMinutes(30));

    private final Duration validity;

    UserTokenType(Duration validity) {
        this.validity = validity;
    }

    public Duration validity() {
        return validity;
    }
}
