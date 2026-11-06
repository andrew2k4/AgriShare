package com.gaming.agricul.service;

import com.gaming.agricul.UserToken;
import com.gaming.agricul.UserTokenType;
import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and consumes the single-use tokens sent by e-mail (verification, password reset).
 * The raw token only exists in the e-mail; the database keeps its SHA-256 hash, so a leaked
 * table cannot be used to verify an address or reset a password.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserTokenService {

    static final String INVALID_LINK = "This link is invalid or has expired";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserTokenRepository userTokenRepository;

    /** Creates a new token for the user and voids the previous unused ones of the same type. */
    public String issue(Long userId, UserTokenType type) {
        Instant now = Instant.now();
        userTokenRepository.invalidateUnused(userId, type, now);

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        userTokenRepository.save(UserToken.issue(userId, type, hash(rawToken), now));
        return rawToken;
    }

    /**
     * Checks the token and marks it used. Returns the id of the user it was issued to.
     * Every failure gives the same message, so a caller cannot tell an unknown token from an
     * expired or already used one.
     */
    public Long consume(String rawToken, UserTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidInputException(INVALID_LINK);
        }
        Instant now = Instant.now();
        UserToken token = userTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> t.getType() == type)
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidInputException(INVALID_LINK));

        if (userTokenRepository.markUsed(token.getId(), now) == 0) {
            throw new InvalidInputException(INVALID_LINK);
        }
        return token.getUserId();
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
