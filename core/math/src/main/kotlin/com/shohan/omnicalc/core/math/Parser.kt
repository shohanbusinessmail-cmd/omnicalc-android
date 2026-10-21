package com.shohan.omnicalc.core.math

import com.shohan.omnicalc.core.math.ast.BinaryOp
import com.shohan.omnicalc.core.math.ast.Expr
import com.shohan.omnicalc.core.math.ast.UnaryOp
import java.math.BigDecimal

/**
 * Precedence-climbing (Pratt) parser for calculator expressions.
 *
 * Precedence (low → high), with tested edge cases:
 *   + −                 (left)   2 + 3 - 1 = (2+3) - 1
 *   × ÷ mod  implicit   (left)   2 × 3 ÷ 4 = (2×3)÷4 ; 2π = 2×π
 *   unary − +                    -2^2 = -(2^2) = -4 (documented convention)
 *   ^                           (right)  2^3^2 = 2^(3^2)
 *   postfix ! %                 3! = 6 ; 50% = 0.5 (context-sensitive)
 *   primary / calls / groups
 *
 * Implicit multiplication is inserted only where unambiguous:
 *   2(3+4), 2π, (1+2)(3+4). Number followed by number stays a syntax error.
 *
 * Function calls require parentheses: `sin 3` is rejected with
 * "function requires parentheses" instead of guessing sin×3.
 * `√`/`∛` are tight prefix operators: √9, √(2+2); √2^2 = (√2)^2 (same value
 * as √(2²) for positive bases — documented in docs/MATH_ENGINE.md).
 *
 * `mod` is an infix keyword after an operand (7 mod 3) and a two-argument
 * function at the start of an operand (mod(7,3)).
 */
class Parser(
    private val tokens: List<Token>,
    private val functionNames: Set<String>,
    /** Predefined variable names (e.g. x, y, t) — lets `x(2)` mean x×(2)
     *  while `banana(1)` reports an unknown *function*. */
    private val variableNames: Set<String> = emptySet(),
) {

    private var index = 0
    private var depth = 0

    private val current: Token get() = tokens[index]

    fun parse(): Expr {
        if (current.kind == TokenKind.END) {
            throw ParseException(CalcError(ErrorCode.EMPTY_INPUT, "Expression is empty", current.position))
        }
        val expr = parseExpression(MIN_PRECEDENCE)
        if (current.kind != TokenKind.END) {
            throw when (current.kind) {
                TokenKind.RPAREN -> ParseException(
                    CalcError(ErrorCode.MISMATCHED_PAREN, "Unmatched ')'", current.position),
                )
                TokenKind.COMMA -> ParseException(
                    CalcError(ErrorCode.SYNTAX, "Unexpected ','", current.position),
                )
                else -> ParseException(
                    CalcError(ErrorCode.SYNTAX, "Unexpected '${current.text}'", current.position),
                )
            }
        }
        return expr
    }

    private fun parseExpression(minBindingPower: Int): Expr {
        var left = parsePrefix()

        while (true) {
            val tok = current

            // ^ — right-associative power.
            if (tok.kind == TokenKind.CARET && POW_BP >= minBindingPower) {
                advance()
                val right = parseExpression(POW_BP)
                left = Expr.Binary(BinaryOp.POWER, left, right)
                continue
            }

            // Infix "mod" keyword: 7 mod 3
            if (tok.kind == TokenKind.IDENT && tok.text.equals("mod", ignoreCase = true) && MUL_BP >= minBindingPower) {
                advance()
                val right = parseExpression(MUL_BP + 1)
                left = Expr.Binary(BinaryOp.MODULO, left, right)
                continue
            }

            // Left-associative arithmetic binaries.
            val bp = infixBindingPower(tok.kind)
            if (bp != NO_BINDING && bp >= minBindingPower) {
                val op = when (tok.kind) {
                    TokenKind.PLUS -> BinaryOp.ADD
                    TokenKind.MINUS -> BinaryOp.SUBTRACT
                    TokenKind.STAR -> BinaryOp.MULTIPLY
                    TokenKind.SLASH -> BinaryOp.DIVIDE
                    else -> error("unreachable")
                }
                advance()
                val right = parseExpression(bp + 1)
                left = Expr.Binary(op, left, right)
                continue
            }

            // Postfix percent and factorial.
            if (tok.kind == TokenKind.PERCENT && POSTFIX_BP >= minBindingPower) {
                advance()
                left = Expr.Percent(left)
                continue
            }
            if (tok.kind == TokenKind.BANG && POSTFIX_BP >= minBindingPower) {
                advance()
                left = Expr.Factorial(left)
                continue
            }
            if (tok.kind == TokenKind.DEGREE && POSTFIX_BP >= minBindingPower) {
                advance()
                left = Expr.DegreeSuffix(left)
                continue
            }

            // Implicit multiplication: completed operand + operand start.
            // `2 3` (number straight after number) stays a syntax error —
            // there is no unambiguous meaning; `2 π`, `2(3)`, `(2)(3)` are ok.
            val numberAfterNumber =
                tok.kind == TokenKind.NUMBER && index > 0 &&
                    tokens[index - 1].kind == TokenKind.NUMBER
            if (canStartOperand(tok) && MUL_BP >= minBindingPower && !numberAfterNumber) {
                validateImplicitStart(tok)
                val right = parseExpression(MUL_BP + 1)
                left = Expr.Binary(BinaryOp.MULTIPLY, left, right)
                continue
            }

            break
        }
        return left
    }

    private fun infixBindingPower(kind: TokenKind): Int = when (kind) {
        TokenKind.PLUS, TokenKind.MINUS -> ADD_BP
        TokenKind.STAR, TokenKind.SLASH -> MUL_BP
        else -> NO_BINDING
    }

    /**
     * Guards implicit multiplication: a known function without parentheses
     * would be ambiguous (`2 sin`), so we reject it with a precise message
     * instead of guessing.
     */
    private fun validateImplicitStart(tok: Token) {
        if (tok.kind == TokenKind.IDENT && tok.text in functionNames && !isPrefixOperator(tok.text)) {
            val next = tokens.getOrNull(index + 1)
            if (next == null || next.kind != TokenKind.LPAREN) {
                throw ParseException(
                    CalcError(
                        ErrorCode.SYNTAX,
                        "Function '${tok.text}' requires parentheses, e.g. ${tok.text}(…)",
                        tok.position,
                        tok.text,
                    ),
                )
            }
        }
    }

    private fun canStartOperand(tok: Token): Boolean = when (tok.kind) {
        TokenKind.NUMBER, TokenKind.LPAREN -> true
        TokenKind.IDENT -> !tok.text.equals("mod", ignoreCase = true)
        else -> false
    }

    private fun parsePrefix(): Expr {
        val tok = current
        return when (tok.kind) {
            TokenKind.PLUS -> {
                advance()
                Expr.Unary(UnaryOp.PLUS, parseExpression(UNARY_RHS_BP))
            }
            TokenKind.MINUS -> {
                advance()
                Expr.Unary(UnaryOp.MINUS, parseExpression(UNARY_RHS_BP))
            }
            TokenKind.NUMBER -> {
                advance()
                val cleaned = tok.text.trimEnd('.')
                val value = runCatching { BigDecimal(cleaned) }.getOrElse {
                    throw ParseException(CalcError(ErrorCode.SYNTAX, "Invalid number '${tok.text}'", tok.position))
                }
                Expr.Literal(value)
            }
            TokenKind.LPAREN -> {
                enterDepth(tok)
                advance()
                if (current.kind == TokenKind.RPAREN) {
                    throw ParseException(
                        CalcError(ErrorCode.MISSING_OPERAND, "Empty parentheses", current.position),
                    )
                }
                val inner = parseExpression(MIN_PRECEDENCE)
                if (current.kind != TokenKind.RPAREN) {
                    throw ParseException(
                        CalcError(ErrorCode.MISMATCHED_PAREN, "Missing ')'", current.position),
                    )
                }
                advance()
                exitDepth()
                inner
            }
            TokenKind.IDENT -> parseIdentifier()
            TokenKind.END -> throw ParseException(
                CalcError(ErrorCode.UNEXPECTED_END, "Expression is incomplete", tok.position),
            )
            else -> throw ParseException(
                CalcError(ErrorCode.SYNTAX, "Unexpected '${tok.text}'", tok.position),
            )
        }
    }

    private fun parseIdentifier(): Expr {
        val tok = current
        val name = tok.text

        // Prefix operators √ / ∛
        if (isPrefixOperator(name)) {
            advance()
            enterDepth(tok)
            if (!canStartOperand(current)) {
                throw ParseException(
                    CalcError(ErrorCode.MISSING_OPERAND, "'$name' needs a value", current.position, name),
                )
            }
            val operand = parseExpression(POSTFIX_BP + 1)
            exitDepth()
            return Expr.FunctionCall(prefixOperatorFunction(name), listOf(operand))
        }

        advance()

        // Function call: name(args…)
        if (current.kind == TokenKind.LPAREN) {
            if (name in functionNames) {
                enterDepth(tok)
                advance()
                val args = mutableListOf<Expr>()
                if (current.kind == TokenKind.RPAREN) {
                    advance()
                    exitDepth()
                    return Expr.FunctionCall(name, args)
                }
                while (true) {
                    args += parseExpression(MIN_PRECEDENCE)
                    when (current.kind) {
                        TokenKind.COMMA -> advance()
                        TokenKind.RPAREN -> break
                        TokenKind.END -> throw ParseException(
                            CalcError(
                                ErrorCode.UNEXPECTED_END,
                                "Missing ')' in call to '$name'",
                                current.position,
                                name,
                            ),
                        )
                        else -> throw ParseException(
                            CalcError(ErrorCode.SYNTAX, "Expected ',' or ')' after argument", current.position, name),
                        )
                    }
                }
                advance() // consume ')'
                exitDepth()
                return Expr.FunctionCall(name, args)
            }
            val isKnownValue = name in variableNames ||
                variableNames.any { it.equals(name, ignoreCase = true) ||
                    it.equals(name, ignoreCase = true) } ||
                name.lowercase() in KNOWN_CONSTANTS ||
                name in KNOWN_CONSTANTS
            if (!isKnownValue && name !in functionNames) {
                throw ParseException(
                    CalcError(
                        ErrorCode.UNKNOWN_FUNCTION,
                        "Unknown function '$name'",
                        tok.position,
                        name,
                    ),
                )
            }
            // Known variable followed by a group: implicit multiplication (x(2)).
        } else if (name in functionNames) {
            throw ParseException(
                CalcError(
                    ErrorCode.SYNTAX,
                    "Function '$name' requires parentheses, e.g. $name(…)",
                    tok.position,
                    name,
                ),
            )
        }

        // Constant or variable — resolved at evaluation time.
        return Expr.Variable(name)
    }

    private fun isPrefixOperator(name: String): Boolean = name == "√" || name == "∛"

    private fun prefixOperatorFunction(name: String): String = if (name == "∛") "cbrt" else "sqrt"

    private fun enterDepth(tok: Token) {
        depth++
        if (depth > MAX_DEPTH) {
            throw ParseException(
                CalcError(ErrorCode.INPUT_TOO_COMPLEX, "Expression nests too deeply", tok.position),
            )
        }
    }

    private fun exitDepth() {
        depth--
    }

    private fun advance() {
        if (index < tokens.size - 1) index++
    }

    companion object {
        private const val NO_BINDING = -1
        private const val MIN_PRECEDENCE = 1
        private const val ADD_BP = 10
        private const val MUL_BP = 20
        private const val POW_BP = 30
        private const val UNARY_RHS_BP = 30
        private const val POSTFIX_BP = 40
        private const val MAX_DEPTH = 64

        /** Built-in constants that may be multiplied by a group: pi(2) = π×2. */
        private val KNOWN_CONSTANTS = setOf("pi", "e", "tau", "π", "τ", "PI", "E", "TAU")
    }
}

class ParseException(val error: CalcError) : Exception(error.message)
