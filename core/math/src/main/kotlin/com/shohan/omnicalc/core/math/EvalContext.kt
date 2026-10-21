package com.shohan.omnicalc.core.math

/**
 * Evaluation context: the settings that change how an expression evaluates.
 * Pure data + pure config — no Android dependencies.
 */
data class EvalContext(
    val angleMode: AngleMode = AngleMode.DEG,
    val percentMode: PercentMode = PercentMode.STANDARD,
    /** Significant digits used when converting transcendental (Double) results. */
    val significantDigits: Int = 15,
    /** Named variables for lab/graph evaluation (e.g. "x" → 2.5). */
    val variables: Map<String, java.math.BigDecimal> = emptyMap(),
) {
    init {
        require(significantDigits in 1..34) { "significantDigits must be 1..34" }
    }

    companion object {
        /** Sensible default for the standard calculator. */
        val DEFAULT = EvalContext()
    }
}

/** Angle unit for trigonometric functions. */
enum class AngleMode { DEG, RAD, GRAD }

/**
 * Percent semantics (spec §4.5).
 *  - [STANDARD]: `200 + 10%` → 220, `200 − 10%` → 180, `200 × 10%` → 20.
 *  - [EXPLICIT]: `%` always means ÷100 (formula tools; unambiguous).
 */
enum class PercentMode { STANDARD, EXPLICIT }
