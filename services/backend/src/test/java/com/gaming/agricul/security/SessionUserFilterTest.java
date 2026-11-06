package com.gaming.agricul.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SessionUserFilterTest {

    private static final Duration LIMIT = Duration.ofHours(12);
    private final Instant now = Instant.parse("2026-11-07T12:00:00Z");

    @Test
    void sessionWithinTheAbsoluteLimit_isKept() {
        assertThat(SessionUserFilter.isExpired(now.minus(Duration.ofHours(11)).toEpochMilli(), now, LIMIT)).isFalse();
    }

    @Test
    void sessionPastTheAbsoluteLimit_expires_evenIfStillActive() {
        assertThat(SessionUserFilter.isExpired(now.minus(Duration.ofHours(13)).toEpochMilli(), now, LIMIT)).isTrue();
    }

    @Test
    void sessionWithoutLoginTime_isTreatedAsExpired() {
        assertThat(SessionUserFilter.isExpired(null, now, LIMIT)).isTrue();
        assertThat(SessionUserFilter.isExpired("not-a-long", now, LIMIT)).isTrue();
    }
}
