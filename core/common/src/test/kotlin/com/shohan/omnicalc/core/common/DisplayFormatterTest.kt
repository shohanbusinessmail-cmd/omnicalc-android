package com.shohan.omnicalc.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class DisplayFormatterTest {

    private fun fmt(
        raw: String,
        precision: PrecisionMode = PrecisionMode.AUTO,
        decimals: Int = 2,
        grouping: GroupingStyle = GroupingStyle.INTERNATIONAL,
        numerals: NumeralStyle = NumeralStyle.WESTERN,
    ): String = DisplayFormatter.format(
        BigDecimal(raw), precision, decimals, grouping, numerals,
    )

    @Test fun autoStripsTrailingZeros() {
        assertEquals("123.45", fmt("123.4500"))
    }

    @Test fun autoKeepsIntegerZero() {
        assertEquals("0", fmt("0.00"))
    }

    @Test fun fixedRoundsHalfUp() {
        assertEquals("1,234.57", fmt("1234.5678", precision = PrecisionMode.FIXED, decimals = 2))
    }

    @Test fun fixedZeroDecimals() {
        assertEquals("10", fmt("9.99", precision = PrecisionMode.FIXED, decimals = 0))
    }

    @Test fun negativeFixedKeepsSign() {
        assertEquals("-5.68", fmt("-5.678", precision = PrecisionMode.FIXED, decimals = 2))
    }

    @Test fun internationalGrouping() {
        assertEquals("1,234,567", fmt("1234567"))
    }

    @Test fun southAsianGrouping() {
        assertEquals("12,34,567", fmt("1234567", grouping = GroupingStyle.SOUTH_ASIAN))
    }

    @Test fun southAsianWithDecimals() {
        assertEquals("12,34,567.89", fmt("1234567.89", grouping = GroupingStyle.SOUTH_ASIAN))
    }

    @Test fun bengaliNumerals() {
        assertEquals("১২,৩৪,৫৬৭.৮৯", fmt("1234567.89", grouping = GroupingStyle.SOUTH_ASIAN, numerals = NumeralStyle.BENGALI))
    }

    @Test fun settingsOverloadMatches() {
        val s = AppSettings(
            precisionMode = PrecisionMode.FIXED,
            decimalPlaces = 1,
            groupingStyle = GroupingStyle.INTERNATIONAL,
            numeralStyle = NumeralStyle.WESTERN,
        )
        assertEquals(DisplayFormatter.format(BigDecimal("2.55"), s), fmt("2.55", precision = PrecisionMode.FIXED, decimals = 1))
    }
}
