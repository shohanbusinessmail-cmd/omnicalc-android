package com.shohan.omnicalc.core.common

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The one display pipeline for results (spec §2.3):
 *
 *   precision (AUTO strip / FIXED half-up scale)
 *     → digit grouping (international or South Asian)
 *       → numeral style (Western 0-9 or Bengali ০-৯)
 *
 * Used by the calculator ViewModel, formula results and any future tool
 * output so settings behave identically everywhere. Pure JVM — unit-tested
 * in core:common.
 */
object DisplayFormatter {

    fun format(
        value: BigDecimal,
        precision: PrecisionMode,
        decimalPlaces: Int,
        grouping: GroupingStyle,
        numerals: NumeralStyle,
    ): String {
        val plain = when (precision) {
            PrecisionMode.AUTO -> value.stripTrailingZeros().toPlainString()
            PrecisionMode.FIXED -> value
                .setScale(decimalPlaces.coerceIn(0, 12), RoundingMode.HALF_UP)
                .toPlainString()
        }
        val grouped = NumberGrouping.group(plain, grouping)
        return Numerals.applyNumeralStyle(grouped, numerals)
    }

    /** Convenience overload driven by the persisted settings. */
    fun format(value: BigDecimal, settings: AppSettings): String = format(
        value = value,
        precision = settings.precisionMode,
        decimalPlaces = settings.decimalPlaces,
        grouping = settings.groupingStyle,
        numerals = settings.numeralStyle,
    )
}
