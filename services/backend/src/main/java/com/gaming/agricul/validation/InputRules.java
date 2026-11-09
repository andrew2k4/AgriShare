package com.gaming.agricul.validation;

import com.gaming.agricul.exception.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input rules for the API's write endpoints, mirroring the checks done in the frontend forms
 * (src/lib/validation.ts). Every failure is an {@link InvalidInputException}, i.e. a 400.
 */
public final class InputRules {

    /** Highest amount accepted: 1 000 milliards FCFA, far above any real project. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    /** Length of the VARCHAR(255) text columns. */
    public static final int MAX_TEXT = 255;
    /** Decimals kept by the NUMERIC(19,4) amount columns: more would be silently rounded. */
    private static final int MAX_SCALE = 4;

    private InputRules() {
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new InvalidInputException(field + " is required");
        }
        return value;
    }

    /** Trimmed text, required and at most {@code max} characters. */
    public static String text(String value, String field, int max) {
        String text = optionalText(value, field, max);
        if (text == null) {
            throw new InvalidInputException(field + " is required");
        }
        return text;
    }

    /** Trimmed text or null when blank, at most {@code max} characters. */
    public static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > max) {
            throw new InvalidInputException(field + " must be at most " + max + " characters");
        }
        return text;
    }

    /** A required amount strictly above zero. */
    public static BigDecimal positiveAmount(BigDecimal value, String field) {
        checkAmount(required(value, field), field);
        if (value.signum() == 0) {
            throw new InvalidInputException(field + " must be positive");
        }
        return value;
    }

    /** An optional amount (null stays null) that can be zero but not negative. */
    public static BigDecimal nonNegativeAmount(BigDecimal value, String field) {
        if (value != null) {
            checkAmount(value, field);
        }
        return value;
    }

    private static void checkAmount(BigDecimal value, String field) {
        if (value.signum() < 0) {
            throw new InvalidInputException(field + " must not be negative");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidInputException(field + " must not exceed " + MAX_AMOUNT.toPlainString());
        }
        if (value.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidInputException(field + " must have at most " + MAX_SCALE + " decimals");
        }
    }

    /** A double column (legacy tables): finite, within [0, max], and above zero unless allowZero. */
    public static double number(Double value, String field, double max, boolean allowZero) {
        required(value, field);
        if (value.isNaN() || value.isInfinite() || value < 0) {
            throw new InvalidInputException(field + " must not be negative");
        }
        if (value == 0 && !allowZero) {
            throw new InvalidInputException(field + " must be positive");
        }
        if (value > max) {
            throw new InvalidInputException(field + " must not exceed " + BigDecimal.valueOf(max).toPlainString());
        }
        return value;
    }

    /**
     * A date that has already happened. Tomorrow is still accepted: the user's day can be ahead
     * of the server's (e.g. Cameroon is UTC+1 and the server runs on UTC).
     */
    public static LocalDate pastOrToday(LocalDate value, String field) {
        date(value, field);
        if (value.isAfter(LocalDate.now().plusDays(1))) {
            throw new InvalidInputException(field + " must not be in the future");
        }
        return value;
    }

    /** A required date within a plausible range (catches typos such as year 0202 or 20266). */
    public static LocalDate date(LocalDate value, String field) {
        required(value, field);
        if (value.getYear() < 1900 || value.getYear() > 2100) {
            throw new InvalidInputException(field + " must be between 1900 and 2100");
        }
        return value;
    }
}
