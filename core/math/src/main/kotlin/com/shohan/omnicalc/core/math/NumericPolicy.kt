package com.shohan.omnicalc.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Central numeric policy for the OmniCalc math engine.
 *
 * Two precision domains:
 *  - [MONEY_MC] / [MONEY_SCALE]: decimal (BigDecimal) arithmetic with explicit
 *    rounding — used for financial, VAT, and other exact-decimal calculations
 *    so binary floating-point artifacts never appear in money results.
 *  - [SCIENCE_MC]: higher-precision decimal context for decimal-based exact
 *    math (power of ten results), while transcendental functions use
 *    [Double] with documented tolerances (see TrigFunctions / ScientificFunctions).
 *
 * Display precision is a separate, user-configurable concern (settings), and
 * never mutates stored results.
 */
object NumericPolicy {

    /** MathContext for money/tax calculations (decimal, HALF_UP — familiar
     *  invoice rounding; intermediate, not final display rounding). */
    val MONEY_MC: MathContext = MathContext(34, RoundingMode.HALF_UP)

    /** Default display scale for money (2 decimals) — display only. */
    const val MONEY_SCALE: Int = 2

    /** MathContext for general decimal arithmetic inside the engine. */
    val SCIENCE_MC: MathContext = MathContext(50, RoundingMode.HALF_UP)

    /** Tolerance for transcendental comparisons in tests and graphing. */
    const val EPSILON: Double = 1e-9

    /** Max magnitude before results are reported as overflow (1e308 ≈ Double.MAX). */
    val OVERFLOW_LIMIT: BigDecimal = BigDecimal("1E+308")

    /** Smallest non-zero magnitude treated as meaningful (below → underflow notice). */
    val UNDERFLOW_LIMIT: BigDecimal = BigDecimal("1E-308")

    /** Default rounding for monetary amounts (HALF_UP: 2.345 → 2.35).
     *  Final rounding is a per-tool, user-visible setting (docs/MATH_ENGINE.md). */
    fun money(value: BigDecimal): BigDecimal =
        value.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
}
