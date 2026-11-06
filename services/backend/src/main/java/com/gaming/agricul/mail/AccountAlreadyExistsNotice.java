package com.gaming.agricul.mail;

/**
 * Published when someone registers with an e-mail that already has an account. The API
 * answers exactly as for a new account (so it cannot be used to find out who is registered)
 * and the owner of the address is told instead.
 */
public record AccountAlreadyExistsNotice(String email) {
}
