package com.gaming.agricul.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SecurityConfigTest {

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "TRUE", " false "})
    void acceptsExplicitBooleans(String value) {
        assertThatNoException().isThrownBy(() -> new SecurityConfig(value, new RateLimiter()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "1", "ture", ""})
    void refusesToStart_onAnyOtherValue(String value) {
        // These would match neither filter chain and leave Spring Boot's default one active
        assertThatIllegalStateException().isThrownBy(() -> new SecurityConfig(value, new RateLimiter()));
    }
}
