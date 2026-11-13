package com.insightboard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NumberParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "42|42.0",
            "-7.5|-7.5",
            "1234.56|1234.56",
            "'1.234,56'|1234.56",
            "'1,234.56'|1234.56",
            "'12,5'|12.5",
            "'R$ 1.500,00'|1500.0",
            "'15%'|15.0",
            "'1.234.567'|1234567.0",
            "'1,234,567'|1234567.0"
    })
    void parsesRealWorldNumbers(String input, double expected) {
        assertThat(NumberParser.parse(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "abc", "2024-01-15", "15.01.2024", "1,2,3", "12abc", "--5"})
    void rejectsNonNumbers(String input) {
        assertThat(NumberParser.parse(input)).isNull();
    }

    @Test
    void zeroIsANumber() {
        assertThat(NumberParser.parse("0")).isEqualTo(0.0);
    }
}
