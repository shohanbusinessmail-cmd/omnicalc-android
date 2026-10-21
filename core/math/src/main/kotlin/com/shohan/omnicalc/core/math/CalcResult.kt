package com.shohan.omnicalc.core.math

import java.math.BigDecimal

/**
 * Engine error model (spec §4.4). Codes are stable and machine-checkable so
 * the UI can localize messages by code; [CalcError.message] is an English
 * fallback that always carries enough detail for logs and tests.
 */
data class CalcError(
    val code: ErrorCode,
    val message: String,
    /** Character position in the (normalized) input, or -1 when not applicable. */
    val position: Int = -1,
    /** Optional extra detail, e.g. the offending token or function name. */
    val detail: String? = null,
)

enum class ErrorCode {
    EMPTY_INPUT,
    /** Invalid token / malformed input. */
    SYNTAX,
    /** Expression ended while something was still required. */
    UNEXPECTED_END,
    /** An operand or operator is missing where required. */
    MISSING_OPERAND,
    /** Parentheses do not balance. */
    MISMATCHED_PAREN,
    DIVISION_BY_ZERO,
    /** Function applied outside its mathematical domain (log of ≤0, sqrt of <0, …). */
    DOMAIN,
    /** Factorial of a negative or non-integer number (or too large). */
    FACTORIAL_DOMAIN,
    OVERFLOW,
    /** Result or exponent too large to compute safely. */
    TOO_LARGE,
    UNKNOWN_FUNCTION,
    UNKNOWN_VARIABLE,
    /** Input longer / deeper than the engine guards allow. */
    INPUT_TOO_COMPLEX,
}

/**
 * Result of a successful evaluation. [exact] distinguishes exact decimal
 * results from rounded transcendental/approximate ones (spec §4.3, §7).
 */
sealed class CalcResult {

    data class Success(
        val value: BigDecimal,
        val exact: Boolean,
    ) : CalcResult()

    data class Failure(
        val error: CalcError,
    ) : CalcResult()
}

/** Convenience accessors — never invent a value on failure. */
val CalcResult.valueOrNull: BigDecimal?
    get() = (this as? CalcResult.Success)?.value

val CalcResult.errorOrNull: CalcError?
    get() = (this as? CalcResult.Failure)?.error
