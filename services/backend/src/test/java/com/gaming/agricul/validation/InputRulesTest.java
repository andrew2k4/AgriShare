package com.gaming.agricul.validation;

import com.gaming.agricul.exception.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputRulesTest {

    @Test
    void text_trims() {
        assertThat(InputRules.text("  Ferme du Sud ", "name", 255)).isEqualTo("Ferme du Sud");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void text_rejectsBlank(String value) {
        assertThatThrownBy(() -> InputRules.text(value, "name", 255))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("name is required");
    }

    @Test
    void text_rejectsTooLong() {
        assertThatThrownBy(() -> InputRules.text("a".repeat(256), "name", 255))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void optionalText_turnsBlankIntoNull() {
        assertThat(InputRules.optionalText("  ", "notes", 255)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1000000000001", "1.00001"})
    void positiveAmount_rejects(String value) {
        assertThatThrownBy(() -> InputRules.positiveAmount(new BigDecimal(value), "amount"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void positiveAmount_rejectsNull() {
        assertThatThrownBy(() -> InputRules.positiveAmount(null, "amount"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("amount is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2000000.00", "1000000000000", "12.5000"})
    void positiveAmount_accepts(String value) {
        assertThat(InputRules.positiveAmount(new BigDecimal(value), "amount")).isEqualByComparingTo(value);
    }

    @Test
    void nonNegativeAmount_acceptsZeroAndNull() {
        assertThat(InputRules.nonNegativeAmount(BigDecimal.ZERO, "taxes")).isZero();
        assertThat(InputRules.nonNegativeAmount(null, "taxes")).isNull();
    }

    @Test
    void nonNegativeAmount_rejectsNegative() {
        assertThatThrownBy(() -> InputRules.nonNegativeAmount(new BigDecimal("-5"), "taxes"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void number_rejectsNaNNegativeZeroAndTooLarge() {
        assertThatThrownBy(() -> InputRules.number(Double.NaN, "budget", 100, false)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> InputRules.number(-1.0, "budget", 100, false)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> InputRules.number(0.0, "budget", 100, false)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> InputRules.number(101.0, "budget", 100, false)).isInstanceOf(InvalidInputException.class);
        assertThat(InputRules.number(0.0, "contribution", 100, true)).isZero();
    }

    @Test
    void pastOrToday_toleratesTomorrowForTimeZones() {
        assertThat(InputRules.pastOrToday(LocalDate.now().plusDays(1), "date")).isNotNull();
        assertThatThrownBy(() -> InputRules.pastOrToday(LocalDate.now().plusDays(2), "date"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("date must not be in the future");
    }

    @Test
    void date_rejectsImplausibleYears() {
        assertThatThrownBy(() -> InputRules.date(LocalDate.of(202, 1, 1), "startDate"))
                .isInstanceOf(InvalidInputException.class);
        assertThat(InputRules.date(LocalDate.of(2030, 1, 1), "startDate")).isNotNull();
    }
}
