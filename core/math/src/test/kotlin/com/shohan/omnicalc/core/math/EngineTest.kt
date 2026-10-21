package com.shohan.omnicalc.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Engine verification — includes the spec §22.2 required examples plus
 * precedence, percent semantics, error paths and guards.
 *
 * Assertions compare plain decimal strings (scale-independent) so the same
 * sources run against real JUnit in Gradle and the local harness.
 */
class EngineTest {

    private fun ok(input: String, context: EvalContext = EvalContext.DEFAULT): String {
        val r = Engine.evaluate(input, context)
        assertTrue("expected success for '$input' but got $r", r is CalcResult.Success)
        return (r as CalcResult.Success).value.toPlainString()
    }

    private fun fail(input: String, context: EvalContext = EvalContext.DEFAULT): ErrorCode {
        val r = Engine.evaluate(input, context)
        assertTrue("expected failure for '$input' but got $r", r is CalcResult.Failure)
        return (r as CalcResult.Failure).error.code
    }

    // ---- Spec §22.2 required examples ----

    @Test
    fun `precedence - 2 plus 3 times 4 is 14`() {
        assertEquals("14", ok("2 + 3 × 4"))
    }

    @Test
    fun `parentheses - (2+3) x 4 is 20`() {
        assertEquals("20", ok("(2 + 3) × 4"))
    }

    @Test
    fun `sqrt 81 is 9`() {
        assertEquals("9", ok("sqrt(81)"))
        assertEquals("9", ok("√(81)"))
        assertEquals("9", ok("√81"))
    }

    @Test
    fun `log10 1000 is 3`() {
        assertEquals("3", ok("log10(1000)"))
        assertEquals("3", ok("log(1000)"))
    }

    @Test
    fun `sin 30 degrees is 0_5`() {
        assertEquals("0.5", ok("sin(30)"))
        // The degree suffix works in every angle mode (spec §4.1):
        assertEquals("0.5", ok("sin(30°)", EvalContext(angleMode = AngleMode.DEG)))
        assertEquals("0.5", ok("sin(30°)", EvalContext(angleMode = AngleMode.RAD)))
    }

    @Test
    fun `sin of pi over 6 in RAD mode`() {
        assertEquals("0.5", ok("sin(180°÷6)", EvalContext(angleMode = AngleMode.RAD)))
    }

    @Test
    fun `standard percent - 200 plus 10 percent is 220`() {
        assertEquals("220", ok("200 + 10%"))
    }

    @Test
    fun `standard percent - 200 minus 10 percent is 180`() {
        assertEquals("180", ok("200 - 10%"))
    }

    @Test
    fun `standard percent - 200 times 10 percent is 20`() {
        assertEquals("20", ok("200 × 10%"))
    }

    @Test
    fun `standard percent - division is literal`() {
        assertEquals("2000", ok("200 ÷ 10%"))
    }

    @Test
    fun `explicit percent mode is always literal`() {
        assertEquals("200.1", ok("200 + 10%", EvalContext(percentMode = PercentMode.EXPLICIT)))
    }

    @Test
    fun `chained percent is sequential`() {
        assertEquals("121", ok("100 + 10% + 10%"))
    }

    @Test
    fun `percent with unary sign`() {
        assertEquals("180", ok("200 + -10%"))
        assertEquals("220", ok("200 - -10%"))
    }

    // ---- Precedence & associativity ----

    @Test
    fun `unary minus binds looser than power`() {
        assertEquals("-4", ok("-2^2"))
        assertEquals("4", ok("(-2)^2"))
        assertEquals("-8", ok("-2^3"))
    }

    @Test
    fun `power is right associative`() {
        assertEquals("512", ok("2^3^2"))
    }

    @Test
    fun `power accepts negative exponent`() {
        assertEquals("0.5", ok("2^-1"))
    }

    @Test
    fun `left associativity of same precedence`() {
        assertEquals("10", ok("20 ÷ 10 × 5"))
        assertEquals("4", ok("2 + 3 - 1"))
    }

    @Test
    fun `implicit multiplication`() {
        assertEquals("6.283185307179586", ok("2π"))
        assertEquals("42", ok("6(7)"))
        assertEquals("20", ok("(1+2)(4+2)+2"))
        assertEquals("14", ok("2·7"))
    }

    @Test
    fun `factorial and postfix percent`() {
        assertEquals("6", ok("3!"))
        assertEquals("120", ok("5!"))
        assertEquals("1", ok("100%"))
        assertEquals("0.5", ok("50%"))
        assertEquals("36", ok("3!×(2+1)!")) // 6 × 6
    }

    @Test
    fun `nested parentheses`() {
        assertEquals("20", ok("((2 + 3) × 4)"))
        assertEquals("14", ok("2 + 3 × (4 - 4) + 12"))
    }

    // ---- Constants, functions, numeric behavior ----

    @Test
    fun `constants pi and e`() {
        assertEquals("3.141592653589793", ok("π"))
        assertEquals("3.141592653589793", ok("pi"))
        assertEquals("2.718281828459045", ok("e"))
    }

    @Test
    fun `decimal arithmetic is exact - no float noise`() {
        assertEquals("0.3", ok("0.1 + 0.2"))
        assertEquals("5.1", ok("1.1 + 4"))
    }

    @Test
    fun `division exactness flags`() {
        val repeating = Engine.evaluate("1 ÷ 3")
        assertTrue(repeating is CalcResult.Success)
        repeating as CalcResult.Success
        assertTrue(!repeating.exact)
        assertTrue(repeating.value.toPlainString().startsWith("0.3333"))

        val terminating = Engine.evaluate("1 ÷ 4") as CalcResult.Success
        assertTrue(terminating.exact)
        assertEquals("0.25", terminating.value.toPlainString())
    }

    @Test
    fun `scientific notation input`() {
        assertEquals("1000000", ok("1.5e6÷1.5"))
        assertEquals("0.002", ok("2e-3"))
        assertEquals("300", ok("3E2÷1"))
    }

    @Test
    fun `bengali and mixed digits accepted`() {
        assertEquals("6", ok("২ + ৪"))
        assertEquals("17", ok("5+১২"))
        assertEquals("3", ok("৯÷৩"))
    }

    @Test
    fun `trigonometry degree mode`() {
        assertEquals("1", ok("sin(90)"))
        assertEquals("0.5", ok("cos(60)"))
        // cos(90°) is ~6.1e-17 — not exactly zero in floating point; the
        // engine reports the honest tiny value rather than a fake 0.
        val cos90 = Engine.evaluate("cos(90)") as CalcResult.Success
        assertTrue("cos(90°) should be near zero, was ${cos90.value}",
            kotlin.math.abs(cos90.value.toDouble()) < 1e-12)
        // tan singularities are errors, not 1.6E16:
        assertEquals(ErrorCode.DOMAIN, fail("tan(90)"))
        assertEquals(ErrorCode.DOMAIN, fail("tan(-90)"))
    }

    @Test
    fun `inverse trigonometry`() {
        assertEquals("30", ok("asin(0.5)"))
        assertEquals(ErrorCode.DOMAIN, fail("asin(2)"))
    }

    @Test
    fun `logarithm domains`() {
        assertEquals(ErrorCode.DOMAIN, fail("ln(0)"))
        assertEquals(ErrorCode.DOMAIN, fail("ln(-5)"))
        assertEquals(ErrorCode.DOMAIN, fail("log(0)"))
        assertEquals(ErrorCode.DOMAIN, fail("log(8, 1)"))
        assertEquals("3", ok("log(8, 2)"))
    }

    @Test
    fun `roots including odd and negative`() {
        assertEquals("3", ok("cbrt(27)"))
        assertEquals("-3", ok("cbrt(-27)"))
        assertEquals("3", ok("root(4, 81)"))
        assertEquals(ErrorCode.DOMAIN, fail("sqrt(-4)"))
        assertEquals(ErrorCode.DOMAIN, fail("root(2, -9)"))
    }

    @Test
    fun `factorial domain errors`() {
        assertEquals(ErrorCode.FACTORIAL_DOMAIN, fail("(-3)!"))
        assertEquals(ErrorCode.FACTORIAL_DOMAIN, fail("2.5!"))
        assertEquals("3628800", ok("10!"))
    }

    @Test
    fun `modulo and division behavior`() {
        assertEquals("1", ok("7 mod 3"))
        assertEquals("-1", ok("-7 mod 3"))
        assertEquals("1", ok("mod(10, 3)"))
        assertEquals(ErrorCode.DIVISION_BY_ZERO, fail("5 ÷ 0"))
        assertEquals(ErrorCode.DIVISION_BY_ZERO, fail("5 mod 0"))
    }

    @Test
    fun `number theory functions`() {
        assertEquals("6", ok("gcd(18, 24)"))
        assertEquals("12", ok("lcm(3, 4)"))
        assertEquals("10", ok("ncr(5, 2)"))
        assertEquals("20", ok("npr(5, 2)"))
        assertEquals(ErrorCode.DOMAIN, fail("ncr(2, 5)"))
    }

    @Test
    fun `aggregate functions`() {
        assertEquals("6", ok("sum(1, 2, 3)"))
        assertEquals("2", ok("avg(1, 2, 3)"))
        assertEquals("1", ok("min(4, 1, 3)"))
        assertEquals("4", ok("max(4, 1, 3)"))
        assertEquals(ErrorCode.MISSING_OPERAND, fail("sum()"))
    }

    // ---- Error model ----

    @Test
    fun `syntax and structural errors`() {
        assertEquals(ErrorCode.EMPTY_INPUT, fail("   "))
        assertEquals(ErrorCode.UNEXPECTED_END, fail("2+"))
        assertEquals(ErrorCode.UNEXPECTED_END, fail("2 + ("))
        assertEquals(ErrorCode.MISMATCHED_PAREN, fail("(2 + 3"))   // missing ')'
        assertEquals(ErrorCode.MISMATCHED_PAREN, fail("2 + 3)"))   // extra ')'
        assertEquals(ErrorCode.SYNTAX, fail("2 3"))
        assertEquals(ErrorCode.SYNTAX, fail("* 5"))
        assertEquals(ErrorCode.SYNTAX, fail("2 + \$"))
    }

    @Test
    fun `unknown names are errors not zeros`() {
        assertEquals(ErrorCode.UNKNOWN_VARIABLE, fail("banana + 1"))
        assertEquals(ErrorCode.UNKNOWN_FUNCTION, fail("banana(1)"))
        assertEquals(ErrorCode.SYNTAX, fail("sin 30"))
    }

    @Test
    fun `overflow and size guards`() {
        assertEquals(ErrorCode.OVERFLOW, fail("9^999"))
        assertEquals(ErrorCode.TOO_LARGE, fail("2^2000000"))
        assertEquals(ErrorCode.INPUT_TOO_COMPLEX, fail("1".repeat(1001)))
        assertEquals("1000000", ok("10^6"))
    }

    @Test
    fun `invalid input never silently becomes zero`() {
        for (bad in listOf("0 ÷ 0", "sqrt(-1)", "ln(-1)", "asdf()", "1++")) {
            val r = Engine.evaluate(bad)
            assertTrue("'$bad' must fail, got $r", r is CalcResult.Failure)
            r as CalcResult.Failure
            assertTrue("failure must carry a message for '$bad'", r.error.message.isNotEmpty())
        }
    }

    @Test
    fun `money precision is exact`() {
        val r = Engine.evaluate("19.99 × 3") as CalcResult.Success
        assertEquals("59.97", r.value.toPlainString())
        assertTrue(r.exact)
    }

    @Test
    fun `deep nesting guarded`() {
        val deep = "(".repeat(70) + "1" + ")".repeat(70)
        assertEquals(ErrorCode.INPUT_TOO_COMPLEX, fail(deep))
        val okDepth = "(".repeat(30) + "1+1" + ")".repeat(30)
        assertEquals("2", ok(okDepth))
    }
}
