package com.shohan.omnicalc.core.localization

import com.shohan.omnicalc.core.math.ErrorCode

/**
 * Maps engine error codes to localized string resources so the UI never
 * shows raw English engine messages when a translation exists (spec §8.1).
 * The engine's own message stays available as detail for logs/tests.
 */
object EngineErrorStrings {

    /** Resource id for a stable engine [ErrorCode], or a generic fallback. */
    fun messageRes(errorCode: ErrorCode): Int = when (errorCode) {
        ErrorCode.EMPTY_INPUT -> R.string.engine_error_empty_input
        ErrorCode.SYNTAX -> R.string.engine_error_syntax
        ErrorCode.UNEXPECTED_END -> R.string.engine_error_unexpected_end
        ErrorCode.MISSING_OPERAND -> R.string.engine_error_missing_operand
        ErrorCode.MISMATCHED_PAREN -> R.string.engine_error_mismatched_paren
        ErrorCode.DIVISION_BY_ZERO -> R.string.engine_error_division_by_zero
        ErrorCode.DOMAIN -> R.string.engine_error_domain
        ErrorCode.FACTORIAL_DOMAIN -> R.string.engine_error_factorial_domain
        ErrorCode.OVERFLOW -> R.string.engine_error_overflow
        ErrorCode.TOO_LARGE -> R.string.engine_error_too_large
        ErrorCode.UNKNOWN_FUNCTION -> R.string.engine_error_unknown_function
        ErrorCode.UNKNOWN_VARIABLE -> R.string.engine_error_unknown_variable
        ErrorCode.INPUT_TOO_COMPLEX -> R.string.engine_error_input_too_complex
    }
}
