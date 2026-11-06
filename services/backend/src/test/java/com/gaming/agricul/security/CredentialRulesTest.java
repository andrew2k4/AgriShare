package com.gaming.agricul.security;

import com.gaming.agricul.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CredentialRulesTest {

    @Test
    void normalizeEmail_trimsAndLowerCases() {
        assertThat(CredentialRules.normalizeEmail("  Marie.Curie@AgriShare.Farm "))
                .isEqualTo("marie.curie@agrishare.farm");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-at-sign", "two@@signs.com", "a@b", "with space@x.com"})
    void normalizeEmail_rejectsMalformedAddresses(String email) {
        assertThatThrownBy(() -> CredentialRules.normalizeEmail(email))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void normalizeEmail_ignoresSurroundingSpaces() {
        assertThat(CredentialRules.normalizeEmail("a@b.c ")).isEqualTo("a@b.c");
    }

    @Test
    void lowerCaseProviderEmail_onlyNormalizes_andTurnsBlankIntoNull() {
        assertThat(CredentialRules.lowerCaseProviderEmail(" Admin@LocalHost ")).isEqualTo("admin@localhost");
        assertThat(CredentialRules.lowerCaseProviderEmail("  ")).isNull();
        assertThat(CredentialRules.lowerCaseProviderEmail(null)).isNull();
    }

    @Test
    void normalizeUsername_lowerCasesValidNames() {
        assertThat(CredentialRules.normalizeUsername(" Jean.Pierre_92 ")).isEqualTo("jean.pierre_92");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "1abc", "_abc", "has space", "mail@domain", "a23456789012345678901234567890x"})
    void normalizeUsername_rejectsInvalidNames(String username) {
        assertThatThrownBy(() -> CredentialRules.normalizeUsername(username))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void checkPassword_acceptsEightCharactersMinimum() {
        assertThatNoException().isThrownBy(() -> CredentialRules.checkPassword("12345678"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567", "        "})
    void checkPassword_rejectsShortOrBlankPasswords(String password) {
        assertThatThrownBy(() -> CredentialRules.checkPassword(password))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void checkPassword_rejectsOverlyLongPasswords() {
        assertThatThrownBy(() -> CredentialRules.checkPassword("x".repeat(129)))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void checkPassword_countsCharactersNotUtf16Units() {
        // 8 emoji = 16 UTF-16 units but 8 characters: accepted at exactly the minimum
        assertThatNoException().isThrownBy(() -> CredentialRules.checkPassword("🌱".repeat(8)));
        assertThatThrownBy(() -> CredentialRules.checkPassword("🌱".repeat(7)))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void isEmail_dependsOnTheAtSign() {
        assertThat(CredentialRules.isEmail("a@b.com")).isTrue();
        assertThat(CredentialRules.isEmail("jean")).isFalse();
        assertThat(CredentialRules.isEmail(null)).isFalse();
    }
}
