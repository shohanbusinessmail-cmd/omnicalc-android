package com.shohan.omnicalc.core.common

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Locale-independent number grouping.
 *
 * OmniCalc never uses [String.format] with implicit locale rules for stored
 * values; grouping is applied explicitly so results are deterministic and
 * testable, then digit style is applied on top (see [Numerals]).
 *
 * Display grouping separators are configurable:
 *  - international: 1,234,567.89   (group 3, separator ",")
 *  - south Asian:   12,34,567.89    (first group 3, then groups of 2)
 * Decimal separator is "." for the canonical display form; Bengali display
 * maps "." to "।"? — NO: OmniCalc keeps "." as the decimal separator even in
 * Bengali (common practice in Bangladesh) — this is a settings decision and
 * documented in docs/LOCALIZATION.md.
 */
object NumberGrouping {

    /**
     * Groups the integer part of a plain decimal [number] string.
     *
     * @param plainDecimal canonical input: optional leading '-', digits,
     *   optional '.' fraction. Scientific notation is NOT accepted — callers
     *   must render those through the scientific formatter instead.
     */
    fun group(plainDecimal: String, style: GroupingStyle): String {
        require(plainDecimal.isNotEmpty()) { "empty number" }
        val negative = plainDecimal.startsWith("-")
        val unsigned = if (negative) plainDecimal.drop(1) else plainDecimal
        require(unsigned.isNotEmpty() && unsigned.all { it.isDigit() || it == '.' }) {
            "not a plain decimal: $plainDecimal"
        }

        val dotIndex = unsigned.indexOf('.')
        val intPart = if (dotIndex >= 0) unsigned.substring(0, dotIndex) else unsigned
        val fracPart = if (dotIndex >= 0) unsigned.substring(dotIndex) else ""
        require(intPart.isNotEmpty()) { "missing integer part: $plainDecimal" }

        val groupedInt = when (style) {
            GroupingStyle.INTERNATIONAL -> groupInThrees(intPart)
            GroupingStyle.SOUTH_ASIAN -> groupSouthAsian(intPart)
        }

        return (if (negative) "-" else "") + groupedInt + fracPart
    }

    private fun groupInThrees(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var count = 0
        for (i in digits.indices.reversed()) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append(',')
        }
        return sb.reverse().toString()
    }

    private fun groupSouthAsian(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var count = 0
        for (i in digits.indices.reversed()) {
            sb.append(digits[i])
            count++
            // First group is 3 digits; every subsequent group is 2 digits,
            // so a separator follows counts 3, 5, 7, … while digits remain.
            val groupDone = count >= 3 && (count - 3) % 2 == 0
            if (groupDone && i > 0) sb.append(',')
        }
        return sb.reverse().toString()
    }

    /**
     * Formats a [BigDecimal] with fixed [scale] decimals + grouping.
     * Rounding is explicit — callers choose [RoundingMode].
     */
    fun format(
        value: BigDecimal,
        scale: Int,
        style: GroupingStyle,
        rounding: RoundingMode = RoundingMode.HALF_UP,
        numeralStyle: NumeralStyle = NumeralStyle.WESTERN,
    ): String {
        val rounded = value.setScale(scale, rounding)
        val plain = rounded.toPlainString()
        return Numerals.applyNumeralStyle(group(plain, style), numeralStyle)
    }

    /**
     * Removes trailing zeros from the fraction (1.500 → 1.5, 2.00 → 2).
     * Never converts to scientific notation.
     */
    fun stripTrailingZeros(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plain = stripped.toPlainString()
        return plain
    }
}
