package com.gaming.agricul.security;

import com.gaming.agricul.exception.InvalidInputException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Format rules for e-mails, usernames and passwords of local accounts. */
public final class CredentialRules {

    public static final int PASSWORD_MIN_LENGTH = 8;
    /** Upper bound so a huge "password" cannot be used to make hashing expensive. */
    public static final int PASSWORD_MAX_LENGTH = 128;

    /**
     * 3 to 30 characters, starts with a letter, then letters, digits, '.', '_' or '-'.
     * No '@', so a login identifier containing '@' is always an e-mail.
     */
    private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9._-]{2,29}$");

    /** Deliberately loose: the verification e-mail is the real check. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EMAIL_MAX_LENGTH = 254;

    private CredentialRules() {
    }

    public static String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > EMAIL_MAX_LENGTH || !EMAIL.matcher(email).matches()) {
            throw new InvalidInputException("Invalid e-mail address");
        }
        return email;
    }

    /**
     * For e-mails coming from an identity provider (Firebase, Google): trimmed and lower-cased
     * but not validated, since the provider already vouches for them. Blank becomes null.
     */
    public static String lowerCaseProviderEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeUsername(String raw) {
        String username = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(username).matches()) {
            throw new InvalidInputException("Username must be 3 to 30 characters, start with a letter, "
                    + "and contain only letters, digits, '.', '_' or '-'");
        }
        return username;
    }

    /** Passwords are never trimmed or changed: only their length is checked. */
    public static void checkPassword(String password) {
        int length = password == null ? 0 : password.codePointCount(0, password.length());
        if (length < PASSWORD_MIN_LENGTH || length > PASSWORD_MAX_LENGTH) {
            throw new InvalidInputException("Password must be between " + PASSWORD_MIN_LENGTH
                    + " and " + PASSWORD_MAX_LENGTH + " characters");
        }
        if (password.isBlank()) {
            throw new InvalidInputException("Password cannot be only spaces");
        }
    }

    /** A login identifier is an e-mail if it contains '@', a username otherwise. */
    public static boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
