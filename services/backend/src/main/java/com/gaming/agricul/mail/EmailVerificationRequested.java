package com.gaming.agricul.mail;

/** Published when a verification link must be e-mailed; sent once the transaction commits. */
public record EmailVerificationRequested(String email, String rawToken) {
}
