package com.shohan.omnicalc.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberGroupingTest {

    @Test
    fun `international grouping`() {
        assertEquals("1,234,567.89", NumberGrouping.group("1234567.89", GroupingStyle.INTERNATIONAL))
        assertEquals("1,234", NumberGrouping.group("1234", GroupingStyle.INTERNATIONAL))
        assertEquals("999", NumberGrouping.group("999", GroupingStyle.INTERNATIONAL))
        assertEquals("0.5", NumberGrouping.group("0.5", GroupingStyle.INTERNATIONAL))
    }

    @Test
    fun `south asian grouping - spec examples`() {
        // Spec §8.3 examples
        assertEquals("12,34,567.89", NumberGrouping.group("1234567.89", GroupingStyle.SOUTH_ASIAN))
        assertEquals("1,234", NumberGrouping.group("1234", GroupingStyle.SOUTH_ASIAN))
        assertEquals("12,345", NumberGrouping.group("12345", GroupingStyle.SOUTH_ASIAN))
        assertEquals("999", NumberGrouping.group("999", GroupingStyle.SOUTH_ASIAN))
    }

    @Test
    fun `negative numbers keep sign`() {
        assertEquals("-1,234,567", NumberGrouping.group("-1234567", GroupingStyle.INTERNATIONAL))
        assertEquals("-12,34,567", NumberGrouping.group("-1234567", GroupingStyle.SOUTH_ASIAN))
    }

    @Test
    fun `bengali numeral display over grouped number - spec example`() {
        val grouped = NumberGrouping.group("1234567.89", GroupingStyle.SOUTH_ASIAN)
        val bengali = Numerals.applyNumeralStyle(grouped, NumeralStyle.BENGALI)
        assertEquals("১২,৩৪,৫৬৭.৮৯", bengali)
    }

    @Test
    fun `format with scale and rounding`() {
        val result = NumberGrouping.format(
            java.math.BigDecimal("1234.5678"),
            scale = 2,
            style = GroupingStyle.INTERNATIONAL,
        )
        assertEquals("1,234.57", result)
    }
}
