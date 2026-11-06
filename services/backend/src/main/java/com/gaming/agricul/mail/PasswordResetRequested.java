package com.gaming.agricul.mail;

/** Published when a password-reset link must be e-mailed; sent once the transaction commits. */
public record PasswordResetRequested(String email, String rawToken) {
}
