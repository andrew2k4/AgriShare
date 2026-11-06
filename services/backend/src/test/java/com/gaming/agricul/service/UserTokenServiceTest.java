package com.gaming.agricul.service;

import com.gaming.agricul.UserToken;
import com.gaming.agricul.UserTokenType;
import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.repository.UserTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTokenServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    @InjectMocks
    private UserTokenService userTokenService;

    @Test
    void issue_storesOnlyTheHash_andVoidsPreviousTokens() {
        String raw = userTokenService.issue(7L, UserTokenType.PASSWORD_RESET);

        verify(userTokenRepository).invalidateUnused(eq(7L), eq(UserTokenType.PASSWORD_RESET), any());
        ArgumentCaptor<UserToken> saved = ArgumentCaptor.forClass(UserToken.class);
        verify(userTokenRepository).save(saved.capture());

        UserToken token = saved.getValue();
        assertThat(raw).hasSizeGreaterThanOrEqualTo(43).matches("[A-Za-z0-9_-]+");
        assertThat(token.getTokenHash()).isEqualTo(UserTokenService.hash(raw)).isNotEqualTo(raw).hasSize(64);
        assertThat(token.getUserId()).isEqualTo(7L);
        assertThat(token.getUsedAt()).isNull();
        assertThat(token.getExpiresAt())
                .isCloseTo(Instant.now().plus(Duration.ofMinutes(30)), within(Duration.ofSeconds(5)));
    }

    @Test
    void issue_neverReturnsTheSameTokenTwice() {
        assertThat(userTokenService.issue(1L, UserTokenType.EMAIL_VERIFICATION))
                .isNotEqualTo(userTokenService.issue(1L, UserTokenType.EMAIL_VERIFICATION));
    }

    @Test
    void consume_returnsTheUser_andMarksTheTokenUsed() {
        UserToken token = token(UserTokenType.EMAIL_VERIFICATION, Instant.now().plusSeconds(60), null);
        when(userTokenRepository.findByTokenHash(UserTokenService.hash("raw"))).thenReturn(Optional.of(token));
        when(userTokenRepository.markUsed(eq(3L), any())).thenReturn(1);

        assertThat(userTokenService.consume("raw", UserTokenType.EMAIL_VERIFICATION)).isEqualTo(42L);
    }

    @Test
    void consume_rejectsAnUnknownToken() {
        when(userTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertInvalid("nope", UserTokenType.PASSWORD_RESET);
    }

    @Test
    void consume_rejectsAnExpiredToken() {
        UserToken token = token(UserTokenType.PASSWORD_RESET, Instant.now().minusSeconds(1), null);
        when(userTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertInvalid("raw", UserTokenType.PASSWORD_RESET);
        verify(userTokenRepository, never()).markUsed(any(), any());
    }

    @Test
    void consume_rejectsAnAlreadyUsedToken() {
        UserToken token = token(UserTokenType.PASSWORD_RESET, Instant.now().plusSeconds(60), Instant.now());
        when(userTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertInvalid("raw", UserTokenType.PASSWORD_RESET);
    }

    @Test
    void consume_rejectsATokenOfAnotherType() {
        // A verification link must not be usable as a password-reset link
        UserToken token = token(UserTokenType.EMAIL_VERIFICATION, Instant.now().plusSeconds(60), null);
        when(userTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertInvalid("raw", UserTokenType.PASSWORD_RESET);
    }

    @Test
    void consume_rejectsTheLoserOfAConcurrentUse() {
        UserToken token = token(UserTokenType.PASSWORD_RESET, Instant.now().plusSeconds(60), null);
        when(userTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userTokenRepository.markUsed(eq(3L), any())).thenReturn(0);

        assertInvalid("raw", UserTokenType.PASSWORD_RESET);
    }

    @Test
    void consume_rejectsBlankInput_withoutQueryingTheDatabase() {
        assertInvalid(" ", UserTokenType.PASSWORD_RESET);
        assertInvalid(null, UserTokenType.PASSWORD_RESET);
        verify(userTokenRepository, never()).findByTokenHash(any());
    }

    private void assertInvalid(String raw, UserTokenType type) {
        assertThatThrownBy(() -> userTokenService.consume(raw, type))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(UserTokenService.INVALID_LINK);
    }

    private static UserToken token(UserTokenType type, Instant expiresAt, Instant usedAt) {
        UserToken token = UserToken.issue(42L, type, "hash", Instant.now().minusSeconds(10));
        token.setId(3L);
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
