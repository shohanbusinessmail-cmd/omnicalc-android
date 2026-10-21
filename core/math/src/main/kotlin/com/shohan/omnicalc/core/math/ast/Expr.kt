package com.shohan.omnicalc.core.math.ast

/**
 * Expression AST produced by [com.shohan.omnicalc.core.math.Parser].
 * Kept intentionally small and side-effect free.
 */
sealed class Expr {

    /** Numeric literal already parsed to a decimal value. */
    data class Literal(val value: java.math.BigDecimal) : Expr()

    /** Function call: name plus parsed arguments. */
    data class FunctionCall(val name: String, val args: List<Expr>) : Expr()

    /** Variable reference (graphing/lab); resolved from EvalContext. */
    data class Variable(val name: String) : Expr()

    /**
     * A percent literal: `10%` — the inner expression is NOT divided by 100
     * here. Semantics depend on context (spec §4.5):
     *  - as an operand of × ÷ or standalone: value/100
     *  - as the right operand of + − in standard mode: relative to the left
     *    operand (200 + 10% → 220)
     * In explicit mode it always means value/100.
     */
    data class Percent(val inner: Expr) : Expr()

    /** Postfix factorial. */
    data class Factorial(val inner: Expr) : Expr()

    /**
     * Postfix degree suffix `30°`: measures the value in degrees and converts
     * it to the current angle mode (no-op in DEG). So `sin(30°)` = 0.5 in any
     * angle mode: in RAD the argument becomes π/6 first.
     */
    data class DegreeSuffix(val inner: Expr) : Expr()

    /** Binary operators by token kind (see [com.shohan.omnicalc.core.math.TokenKind]). */
    data class Binary(
        val op: BinaryOp,
        val left: Expr,
        val right: Expr,
    ) : Expr()

    /** Unary sign (plus kept for completeness of round-trips). */
    data class Unary(val op: UnaryOp, val expr: Expr) : Expr()
}

enum class BinaryOp { ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, MODULO }

enum class UnaryOp { PLUS, MINUS }
