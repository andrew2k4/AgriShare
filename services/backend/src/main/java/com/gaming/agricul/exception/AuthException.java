package com.gaming.agricul.exception;

import org.springframework.http.HttpStatus;

/**
 * Authentication failure with a stable machine-readable code the frontend can switch on
 * (e.g. INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED, TOO_MANY_ATTEMPTS).
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Long retryAfterSeconds;

    public AuthException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public AuthException(HttpStatus status, String code, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    public static AuthException tooManyAttempts(long retryAfterSeconds) {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                "Too many attempts, please try again later", retryAfterSeconds);
    }
}
