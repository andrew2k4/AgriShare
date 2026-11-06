package com.gaming.agricul.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final RateLimiter limiter = new RateLimiter(now::get);

    @Test
    void allowsUpToTheLimit_thenRefuses_untilTheWindowEnds() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
        }
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isFalse();
        assertThat(limiter.secondsUntilReset("k")).isEqualTo(60);

        now.addAndGet(Duration.ofMinutes(1).toMillis());
        assertThat(limiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void recordAndIsExhausted_countWithoutRefusing() {
        limiter.record("k", Duration.ofMinutes(1));
        limiter.record("k", Duration.ofMinutes(1));
        assertThat(limiter.isExhausted("k", 3)).isFalse();
        limiter.record("k", Duration.ofMinutes(1));
        assertThat(limiter.isExhausted("k", 3)).isTrue();

        limiter.reset("k");
        assertThat(limiter.isExhausted("k", 3)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        limiter.tryAcquire("a", 1, Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("a", 1, Duration.ofMinutes(1))).isFalse();
        assertThat(limiter.tryAcquire("b", 1, Duration.ofMinutes(1))).isTrue();
    }
}
