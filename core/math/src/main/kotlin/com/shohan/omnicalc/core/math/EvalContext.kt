package com.shohan.omnicalc.core.math

import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.common.PercentMode

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
