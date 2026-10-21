package com.shohan.omnicalc.core.common

/**
 * Numeral systems and digit conversion.
 *
 * OmniCalc supports Bengali digits "০১২৩৪৫৬৭৮৯" and Western digits
 * "0123456789" independently of UI language. All conversion helpers here are
 * Unicode-safe and preserve every non-digit character (separators, symbols,
 * unit abbreviations) untouched.
 */
object Numerals {

    private const val BENGALI_ZERO = 0x09E6 // ০

    /** True if [c] is a Bengali digit ০–৯. */
    fun isBengaliDigit(c: Char): Boolean = c in '০'..'৯'

    /** True if [c] is a Western digit 0–9. */
    fun isWesternDigit(c: Char): Boolean = c in '0'..'9'

    /** True if [c] is a digit in either supported numeral system. */
    fun isAnyDigit(c: Char): Boolean = isBengaliDigit(c) || isWesternDigit(c)

    /**
     * Converts every Bengali digit in [text] to the matching Western digit.
     * All other characters pass through unchanged.
     */
    fun toWesternDigits(text: String): String = buildString(text.length) {
        for (c in text) {
            append(if (isBengaliDigit(c)) ('0' + (c.code - BENGALI_ZERO)) else c)
        }
    }

    /**
     * Converts every Western digit in [text] to the matching Bengali digit.
     * All other characters pass through unchanged.
     */
    fun toBengaliDigits(text: String): String = buildString(text.length) {
        for (c in text) {
            append(if (isWesternDigit(c)) ('০' + (c - '0')) else c)
        }
    }

    /**
     * Normalizes mixed input: converts Bengali digits to Western so the
     * expression parser always sees one canonical digit set.
     */
    fun normalizeToWesternDigits(text: String): String = toWesternDigits(text)

    /**
     * Applies [numeralStyle] to an already-formatted Western-digit string.
     * Decimal/grouping separators in [text] are preserved as-is.
     */
    fun applyNumeralStyle(text: String, numeralStyle: NumeralStyle): String =
        when (numeralStyle) {
            NumeralStyle.WESTERN -> text
            NumeralStyle.BENGALI -> toBengaliDigits(text)
        }
}

/** Which digits a display uses. Independent of interface language. */
enum class NumeralStyle { WESTERN, BENGALI }

/** Grouping style for thousands separators. */
enum class GroupingStyle {
    /** International: 1,234,567.89 */
    INTERNATIONAL,

    /** South Asian (India/Bangladesh): 12,34,567.89 */
    SOUTH_ASIAN,
}
