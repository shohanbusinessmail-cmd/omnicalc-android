package com.shohan.omnicalc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeralsTest {

    @Test
    fun `bengali to western preserves non-digits`() {
        assertEquals("123", Numerals.toWesternDigits("১২৩"))
        assertEquals("a 4.5 ৳", Numerals.toWesternDigits("a 4.5 ৳"))
        assertEquals("1,234.56", Numerals.toWesternDigits("১,২৩৪.৫৬"))
    }

    @Test
    fun `western to bengali and back round trips`() {
        val western = "9876543210.25"
        val bengali = Numerals.toBengaliDigits(western)
        assertEquals("৯৮৭৬৫৪৩২১০.২৫", bengali)
        assertEquals(western, Numerals.toWesternDigits(bengali))
    }

    @Test
    fun `mixed expression normalizes to one digit set`() {
        assertEquals("5+12=17", Numerals.normalizeToWesternDigits("৫+১২=১৭"))
        assertEquals("5+12", Numerals.normalizeToWesternDigits("5+১২"))
    }

    @Test
    fun `digit predicates`() {
        assertTrue(Numerals.isBengaliDigit('০'))
        assertTrue(Numerals.isBengaliDigit('৯'))
        assertFalse(Numerals.isBengaliDigit('9'))
        assertTrue(Numerals.isAnyDigit('7'))
        assertTrue(Numerals.isAnyDigit('৭'))
        assertFalse(Numerals.isAnyDigit('.'))
    }

    @Test
    fun `apply numeral style to formatted number`() {
        assertEquals("1,234", Numerals.applyNumeralStyle("1,234", NumeralStyle.WESTERN))
        assertEquals("১,২৩৪", Numerals.applyNumeralStyle("1,234", NumeralStyle.BENGALI))
    }
}
