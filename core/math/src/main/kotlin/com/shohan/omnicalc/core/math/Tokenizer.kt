package com.shohan.omnicalc.core.math

import com.shohan.omnicalc.core.common.Numerals

/**
 * Token kinds recognized by the calculator lexer.
 */
enum class TokenKind {
    NUMBER,
    IDENT,          // functions, constants, variables
    PLUS, MINUS, STAR, SLASH, CARET,
    PERCENT,        // postfix %
    BANG,           // postfix !
    DEGREE,         // postfix °
    LPAREN, RPAREN, COMMA,
    END,
}

data class Token(
    val kind: TokenKind,
    /** Original text (numbers: normalized Western digits). */
    val text: String,
    /** Start index in the normalized input. */
    val position: Int,
)

class TokenizeException(
    val error: CalcError,
) : Exception(error.message)

/**
 * Converts calculator input into tokens.
 *
 * Input handling:
 *  - Bengali digits are normalized to Western digits first (spec §8.1).
 *  - Display operators are folded: × → *, ÷ → /, − (U+2212) → -, ** → ^.
 *  - `2e3`, `1.5E-2` are single NUMBER tokens (scientific notation).
 *  - Implicit multiplication is NOT inserted here; the parser does it where
 *    rules are unambiguous (spec §4.1).
 */
class Tokenizer(private val rawInput: String) {

    fun tokenize(): List<Token> {
        val input = Numerals.normalizeToWesternDigits(rawInput)
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    i = scanNumber(input, i)
                    tokens += Token(TokenKind.NUMBER, input.substring(start, i), start)
                }

                c == '+' -> { tokens += Token(TokenKind.PLUS, "+", i); i++ }
                c == '-' || c == '\u2212' || c == '\u2013' -> {
                    tokens += Token(TokenKind.MINUS, "-", i); i++
                }
                c == '*' -> {
                    if (i + 1 < input.length && input[i + 1] == '*') {
                        tokens += Token(TokenKind.CARET, "^", i); i += 2
                    } else {
                        tokens += Token(TokenKind.STAR, "*", i); i++
                    }
                }
                // Display multiplication sign → *
                c == '\u00D7' || c == '\u00B7' -> { tokens += Token(TokenKind.STAR, "*", i); i++ }
                c == '/' || c == '\u00F7' -> { tokens += Token(TokenKind.SLASH, "/", i); i++ }
                c == '^' -> { tokens += Token(TokenKind.CARET, "^", i); i++ }
                c == '%' -> { tokens += Token(TokenKind.PERCENT, "%", i); i++ }
                c == '!' -> { tokens += Token(TokenKind.BANG, "!", i); i++ }
                c == '°' || c == 'º' -> { tokens += Token(TokenKind.DEGREE, "°", i); i++ }
                c == '(' -> { tokens += Token(TokenKind.LPAREN, "(", i); i++ }
                c == ')' -> { tokens += Token(TokenKind.RPAREN, ")", i); i++ }
                c == ',' || c == ';' -> { tokens += Token(TokenKind.COMMA, ",", i); i++ }

                // Letters/underscores start multi-character identifiers (sin, x1, log2);
                // symbol identifiers (π τ √ ∛) are always single characters so that
                // `√81` and `π2` tokenize as operator/constant + number.
                isLetterIdentStart(c) || isSymbolIdent(c) -> {
                    if (isSymbolIdent(c)) {
                        tokens += Token(TokenKind.IDENT, c.toString(), i)
                        i++
                    } else {
                        val start = i
                        i++
                        while (i < input.length && isLetterIdentPart(input[i])) i++
                        tokens += Token(TokenKind.IDENT, input.substring(start, i), start)
                    }
                }

                else -> throw TokenizeException(
                    CalcError(ErrorCode.SYNTAX, "Unexpected character '$c'", i),
                )
            }
        }
        tokens += Token(TokenKind.END, "", input.length)
        return tokens
    }

    private fun scanNumber(input: String, start: Int): Int {
        var i = start
        var digitsBefore = 0
        while (i < input.length && input[i].isDigit()) { i++; digitsBefore++ }
        if (i < input.length && input[i] == '.') {
            i++
            var digitsAfter = 0
            while (i < input.length && input[i].isDigit()) { i++; digitsAfter++ }
            if (digitsBefore == 0 && digitsAfter == 0) {
                throw TokenizeException(
                    CalcError(ErrorCode.SYNTAX, "A number needs digits around the decimal point", start),
                )
            }
            if (digitsBefore > 0 && digitsAfter == 0) {
                // "5." — allow a trailing dot (5. == 5), common on calculators.
            }
        }
        if (digitsBefore == 0 && (start >= input.length || input[start] != '.')) {
            throw TokenizeException(CalcError(ErrorCode.SYNTAX, "Expected a number", start))
        }
        // Scientific notation: only when 'e'/'E' is followed by digits or sign+digits.
        if (i < input.length && (input[i] == 'e' || input[i] == 'E')) {
            var j = i + 1
            if (j < input.length && (input[j] == '+' || input[j] == '-')) j++
            if (j < input.length && input[j].isDigit()) {
                while (j < input.length && input[j].isDigit()) j++
                i = j
            }
        }
        return i
    }

    private fun isLetterIdentStart(c: Char): Boolean =
        c == '_' || c in 'a'..'z' || c in 'A'..'Z'

    private fun isLetterIdentPart(c: Char): Boolean =
        isLetterIdentStart(c) || c in '0'..'9'

    private fun isSymbolIdent(c: Char): Boolean =
        c == '\u03C0' || // π
        c == '\u03C4' || // τ
        c == '\u221A' || // √
        c == '\u221B'    // ∛
}
