package com.shohan.omnicalc.core.math

import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.common.PercentMode

import com.shohan.omnicalc.core.math.ast.BinaryOp
import com.shohan.omnicalc.core.math.ast.Expr
import com.shohan.omnicalc.core.math.ast.UnaryOp
import java.math.BigDecimal

/**
 * Tree-walking evaluator.
 *
 * Percent semantics (spec §4.5, [PercentMode.STANDARD]):
 *  - `200 + 10%` → 220, `200 − 10%` → 180: a bare percent right of `+`/`−`
 *    is relative to the current left value.
 *  - `200 × 10%` → 20, standalone `10%` → 0.1: everywhere else percent is ÷100.
 *  - Unary wrappers are honoured: `200 + −10%` → 180.
 *  - In [PercentMode.EXPLICIT] percent is always ÷100 (formula tools).
 *
 * Every arithmetic result passes the overflow guard; division by zero and
 * domain errors raise [EvalException] — invalid input never becomes 0.
 */
internal class Evaluator(private val ctx: EvalContext) {

    fun eval(expr: Expr): EvalValue = when (expr) {
        is Expr.Literal -> EvalValue(expr.value, exact = true)

        is Expr.Variable -> resolveVariable(expr.name)

        is Expr.Unary -> {
            val v = eval(expr.expr)
            when (expr.op) {
                UnaryOp.PLUS -> v
                UnaryOp.MINUS -> EvalValue(v.value.negate(), v.exact)
            }
        }

        is Expr.Percent -> {
            val v = eval(expr.inner)
            // Division by 100 always terminates exactly — no MathContext
            // rounding may touch an exact value here.
            EvalValue(v.value.divide(BigDecimal(100)), v.exact)
        }

        is Expr.DegreeSuffix -> evalDegree(eval(expr.inner))

        is Expr.Factorial -> Functions.invoke("fact", listOf(eval(expr.inner)), ctx)

        is Expr.FunctionCall -> {
            val args = expr.args.map { eval(it) }
            Functions.invoke(expr.name, args, ctx)
        }

        is Expr.Binary -> evalBinary(expr)
    }

    private fun evalDegree(v: EvalValue): EvalValue = when (ctx.angleMode) {
        AngleMode.DEG -> v // already degrees
        AngleMode.RAD -> {
            // valueOf → shortest repr of Math.PI (15 digits), not the exact
            // binary expansion of the double.
            val radians = BigDecimal.valueOf(Math.PI).multiply(v.value)
                .divide(BigDecimal(180), NumericPolicy.SCIENCE_MC)
            Functions.ensureNoOverflow(radians)
            EvalValue(radians, exact = false)
        }
        AngleMode.GRAD -> {
            val grads = BigDecimal(200).multiply(v.value)
                .divide(BigDecimal(180), NumericPolicy.SCIENCE_MC)
            EvalValue(grads, exact = false)
        }
    }

    private fun evalBinary(expr: Expr.Binary): EvalValue {
        val op = expr.op

        // Context-aware percent for + and − (standard mode).
        if ((op == BinaryOp.ADD || op == BinaryOp.SUBTRACT) && ctx.percentMode == PercentMode.STANDARD) {
            val rawPercent = standardPercentRaw(expr.right)
            if (rawPercent != null) {
                val left = eval(expr.left)
                val delta = left.value.multiply(rawPercent.value)
                    .divide(BigDecimal(100), NumericPolicy.MONEY_MC)
                val result = if (op == BinaryOp.ADD) left.value.add(delta) else left.value.subtract(delta)
                Functions.ensureNoOverflow(result)
                return EvalValue(result, left.exact && rawPercent.exact)
            }
        }

        val left = eval(expr.left)
        val right = eval(expr.right)
        return evalWith(op, left, right)
    }

    /**
     * If [expr] is a bare percent operand (possibly unary-signed), returns its
     * raw (not yet ÷100) value for relative percent arithmetic.
     */
    private fun standardPercentRaw(expr: Expr): EvalValue? = when (expr) {
        is Expr.Percent -> eval(expr.inner)
        is Expr.Unary -> {
            val inner = standardPercentRaw(expr.expr)
            when {
                inner == null -> null
                expr.op == UnaryOp.MINUS -> EvalValue(inner.value.negate(), inner.exact)
                else -> inner
            }
        }
        else -> null
    }

    private fun evalWith(op: BinaryOp, left: EvalValue, right: EvalValue): EvalValue {
        val exact = left.exact && right.exact
        return when (op) {
            BinaryOp.ADD -> {
                val v = left.value.add(right.value)
                Functions.ensureNoOverflow(v)
                EvalValue(v, exact)
            }
            BinaryOp.SUBTRACT -> {
                val v = left.value.subtract(right.value)
                Functions.ensureNoOverflow(v)
                EvalValue(v, exact)
            }
            BinaryOp.MULTIPLY -> {
                val v = left.value.multiply(right.value)
                Functions.ensureNoOverflow(v)
                EvalValue(v, exact)
            }
            BinaryOp.DIVIDE -> {
                if (right.value.signum() == 0) {
                    throw EvalException(CalcError(ErrorCode.DIVISION_BY_ZERO, "Cannot divide by zero"))
                }
                // Exact first (terminating decimal division), fall back to the
                // high-precision context for repeating expansions.
                val quotient = try {
                    left.value.divide(right.value) to true
                } catch (_: ArithmeticException) {
                    left.value.divide(right.value, NumericPolicy.SCIENCE_MC) to false
                }
                Functions.ensureNoOverflow(quotient.first)
                EvalValue(quotient.first, exact && quotient.second)
            }
            BinaryOp.MODULO -> {
                if (right.value.signum() == 0) {
                    throw EvalException(CalcError(ErrorCode.DIVISION_BY_ZERO, "Modulo by zero"))
                }
                // Remainder sign follows the dividend (C/Java convention) —
                // documented in docs/MATH_ENGINE.md.
                EvalValue(left.value.remainder(right.value), exact)
            }
            BinaryOp.POWER ->
                Functions.Power.power(left.value, right.value, ctx, baseExact = exact)
        }
    }

    private fun resolveVariable(name: String): EvalValue = when (name) {
        "pi", "π", "PI" -> EvalValue(BigDecimal.valueOf(Math.PI), exact = false)
        "e", "E" -> EvalValue(BigDecimal.valueOf(Math.E), exact = false)
        "tau", "τ", "TAU" -> EvalValue(BigDecimal.valueOf(Math.PI).multiply(BigDecimal(2)), exact = false)
        else -> {
            val resolved = ctx.variables[name]
                ?: ctx.variables.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
                ?: throw EvalException(
                    CalcError(
                        ErrorCode.UNKNOWN_VARIABLE,
                        "Unknown name '$name'",
                        -1, name,
                    ),
                )
            EvalValue(resolved, exact = true)
        }
    }
}
