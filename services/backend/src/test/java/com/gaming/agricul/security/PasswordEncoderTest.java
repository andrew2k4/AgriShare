package com.gaming.agricul.security;

import com.gaming.agricul.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    @Test
    void newHashesUseArgon2id() {
        String hash = encoder.encode("correct horse");

        assertThat(hash).startsWith("{argon2}$argon2id$");
        assertThat(encoder.matches("correct horse", hash)).isTrue();
        assertThat(encoder.matches("wrong horse", hash)).isFalse();
    }

    @Test
    void sameInputGivesDifferentHashes_becauseOfTheSalt() {
        assertThat(encoder.encode("password1")).isNotEqualTo(encoder.encode("password1"));
    }

    @Test
    void bcryptHashesStillVerify_andAreFlaggedForUpgrade() {
        String bcrypt = "{bcrypt}" + new BCryptPasswordEncoder().encode("legacy-pass");

        assertThat(encoder.matches("legacy-pass", bcrypt)).isTrue();
        assertThat(encoder.upgradeEncoding(bcrypt)).isTrue();
    }
}
