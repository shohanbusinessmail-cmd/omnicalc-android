package com.shohan.omnicalc.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Supported conversion families (spec §11). Temperature is affine. */
enum class UnitCategory { LENGTH, MASS, VOLUME, AREA, TEMPERATURE, SPEED, TIME, DATA }

/**
 * A conversion unit expressed as an exact affine map onto its category base:
 *
 *   base = (value + addend) × num / den        (to base)
 *   value = base × den / num − addend          (from base)
 *
 * Rational [num]/[den] keeps awkward ratios (5/9 for Fahrenheit, 10/36 for
 * km/h, 1/1000 for milli-) EXACT instead of lossy decimal factors. Only the
 * final divisions use [MATH_CONTEXT] (15 significant digits, HALF_UP — the
 * same policy as transcendental engine results).
 *
 * Definitions use international/US standard values (e.g. 1 in = 0.0254 m
 * exactly, 1 lb = 0.45359237 kg exactly). Regional land units are
 * deliberately NOT here — they vary by locality and live in Tools with
 * user-configurable profiles (spec §7, §11.3).
 */
data class UnitDef(
    val id: String,
    val category: UnitCategory,
    val symbol: String,
    val nameEn: String,
    val nameBn: String,
    val num: BigDecimal,
    val den: BigDecimal = BigDecimal.ONE,
    /** Added to the value BEFORE scaling (Kelvin offset for °C/°F). */
    val addend: BigDecimal = BigDecimal.ZERO,
    /** True when [nameEn]/[nameBn] need a parenthetical qualifier shown. */
    val note: String? = null,
)

object UnitConvert {

    private val MC = MathContext(15, RoundingMode.HALF_UP)

    /** null when units belong to different categories (caller shows a message). */
    fun convert(value: BigDecimal, from: UnitDef, to: UnitDef): BigDecimal? {
        if (from.category != to.category) return null
        val base = value.add(from.addend).multiply(from.num, MC).divide(from.den, MC)
        return base.multiply(to.den, MC).divide(to.num, MC).subtract(to.addend)
    }
}
