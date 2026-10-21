package com.shohan.omnicalc.core.math

/**
 * Public facade of the OmniCalc math engine.
 *
 * The engine is pure Kotlin/JVM: no Android, no network, no eval()/script
 * execution. Input is tokenized, parsed into a small AST with a Pratt parser,
 * and evaluated with documented precedence and percent semantics.
 *
 * Guarantees:
 *  - Invalid input yields a typed [CalcError] — never a silent 0, NaN or ∞.
 *  - Bengali and mixed digits are accepted (normalized via core:common).
 *  - Guard rails: input length ≤ 1000, nesting ≤ 64, factorial ≤ 10000,
 *    exponent ≤ 10^6, results bounded to 1E+308 (see docs/MATH_ENGINE.md).
 */
object Engine {

    const val MAX_INPUT_LENGTH: Int = 1000

    fun evaluate(
        input: String,
        context: EvalContext = EvalContext.DEFAULT,
    ): CalcResult {
        if (input.isBlank()) {
            return CalcResult.Failure(CalcError(ErrorCode.EMPTY_INPUT, "Expression is empty"))
        }
        if (input.length > MAX_INPUT_LENGTH) {
            return CalcResult.Failure(
                CalcError(
                    ErrorCode.INPUT_TOO_COMPLEX,
                    "Expression is too long (max $MAX_INPUT_LENGTH characters)",
                ),
            )
        }
        return try {
            val tokens = Tokenizer(input).tokenize()
            val ast = Parser(tokens, Functions.NAMES, context.variables.keys).parse()
            val value = Evaluator(context).eval(ast)
            CalcResult.Success(value.value.stripTrailingZeros(), value.exact)
        } catch (e: TokenizeException) {
            CalcResult.Failure(e.error)
        } catch (e: ParseException) {
            CalcResult.Failure(e.error)
        } catch (e: EvalException) {
            CalcResult.Failure(e.error)
        } catch (e: ArithmeticException) {
            CalcResult.Failure(
                CalcError(ErrorCode.SYNTAX, "Could not compute that expression (${e.message ?: "arithmetic error"})"),
            )
        } catch (e: IllegalArgumentException) {
            CalcResult.Failure(
                CalcError(ErrorCode.SYNTAX, "Invalid expression: ${e.message ?: "bad input"}"),
            )
        } catch (e: Exception) {
            // Defensive net: malformed input must never crash the app.
            CalcResult.Failure(
                CalcError(ErrorCode.SYNTAX, "Could not evaluate this expression (${e.javaClass.simpleName})"),
            )
        }
    }

    /**
     * Convenience for tests/tools: evaluated value or throws with the engine
     * error. Never use for user-facing paths.
     */
    fun evaluateOrThrow(
        input: String,
        context: EvalContext = EvalContext.DEFAULT,
    ): java.math.BigDecimal {
        return when (val result = evaluate(input, context)) {
            is CalcResult.Success -> result.value
            is CalcResult.Failure -> throw EvalException(result.error)
        }
    }
}
