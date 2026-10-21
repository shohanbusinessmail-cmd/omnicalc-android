package com.shohan.omnicalc.core.math

import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.common.PercentMode

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

/** Internal evaluated value: magnitude + whether it is an exact decimal result. */
internal class EvalValue(val value: BigDecimal, val exact: Boolean) {
    fun inexact(): EvalValue = EvalValue(value, exact = false)
}

/** Evaluation-time failure carrying a stable [CalcError]. */
class EvalException(val error: CalcError) : Exception(error.message)

/**
 * Built-in mathematical functions (spec §4, §6).
 *
 * Precision policy:
 *  - Decimal-exact operations (+ − × ÷ integer powers, factorials, gcd, …)
 *    stay on BigDecimal and keep exactness.
 *  - Transcendental functions compute in [Double] (IEEE-754, ~15–16 good
 *    digits) and convert back with the context's significant-digit setting,
 *    which absorbs representation noise: sin(30°) → 0.49999999999999994 →
 *    0.5 at 15 significant digits. Results are flagged inexact.
 *  - Domain violations raise [EvalException] with ErrorCode.DOMAIN — never
 *    silent NaN/∞.
 */
object Functions {

    /** All callable names (lowercase; lookup is case-insensitive). */
    val NAMES: Set<String> = setOf(
        // trigonometry
        "sin", "cos", "tan", "asin", "acos", "atan",
        // hyperbolic
        "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        // logarithms / exponentials
        "ln", "log", "log10", "log2", "exp",
        // roots / magnitude
        "sqrt", "cbrt", "abs", "hypot", "root",
        // rounding / sign
        "floor", "ceil", "round", "trunc", "sign",
        // powers
        "pow", "fact",
        // number theory
        "gcd", "lcm", "ncr", "npr",
        // aggregate
        "min", "max", "sum", "avg",
        // calculator helpers
        "mod", "percent", "deg", "rad",
    )

    /** min args, max args (-1 = unbounded) per function. */
    private val ARITY: Map<String, Pair<Int, Int>> = buildMap {
        for (n in listOf("sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
                "asinh", "acosh", "atanh", "ln", "log10", "log2", "exp", "sqrt", "cbrt", "abs",
                "floor", "ceil", "round", "trunc", "sign", "fact", "deg", "rad")
        ) put(n, 1 to 1)
        put("log", 1 to 2)
        put("pow", 2 to 2)
        put("root", 2 to 2)
        put("hypot", 2 to 2)
        put("gcd", 2 to -1)
        put("lcm", 2 to -1)
        put("ncr", 2 to 2)
        put("npr", 2 to 2)
        put("mod", 2 to 2)
        put("percent", 2 to 2)
        put("min", 1 to -1)
        put("max", 1 to -1)
        put("sum", 1 to -1)
        put("avg", 1 to -1)
    }

    fun isFunctionName(name: String): Boolean = name.lowercase() in NAMES

    internal fun invoke(name: String, args: List<EvalValue>, ctx: EvalContext): EvalValue {
        val n = name.lowercase()
        val (min, max) = ARITY[n] ?: throw EvalException(
            CalcError(ErrorCode.UNKNOWN_FUNCTION, "Unknown function '$name'", -1, name),
        )
        if (args.size < min || (max != -1 && args.size > max)) {
            val expected = if (max == -1 || max == min) "$min" else "$min–$max"
            throw EvalException(
                CalcError(
                    ErrorCode.MISSING_OPERAND,
                    "'$n' expects $expected argument(s), got ${args.size}",
                    -1, name,
                ),
            )
        }
        return when (n) {
            "sin" -> trigValue(args[0], ctx) { Math.sin(it) }
            "cos" -> trigValue(args[0], ctx) { Math.cos(it) }
            "tan" -> tanValue(args[0], ctx)
            "asin" -> inverseTrig(args, ctx, allowed = { it in -1.0..1.0 }) { Math.asin(it) }
            "acos" -> inverseTrig(args, ctx, allowed = { it in -1.0..1.0 }) { Math.acos(it) }
            "atan" -> inverseTrig(args, ctx, allowed = { true }) { Math.atan(it) }
            "sinh" -> fromDoubleStrict(Math.sinh(args[0].value.toDouble()), ctx, arg0 = args[0])
            "cosh" -> fromDoubleStrict(Math.cosh(args[0].value.toDouble()), ctx, arg0 = args[0])
            "tanh" -> fromDoubleStrict(Math.tanh(args[0].value.toDouble()), ctx, arg0 = args[0])
            // Java's Math lacks inverse hyperbolics — defined via logarithms:
            "asinh" -> {
                val x = args[0].value.toDouble()
                fromDoubleStrict(Math.log(x + Math.sqrt(x * x + 1.0)), ctx, arg0 = args[0])
            }
            "acosh" -> {
                val x = args[0].value.toDouble()
                if (x < 1.0) domain("acosh", "requires x ≥ 1", args[0])
                fromDoubleStrict(Math.log(x + Math.sqrt(x * x - 1.0)), ctx, arg0 = args[0])
            }
            "atanh" -> {
                val x = args[0].value.toDouble()
                if (x <= -1.0 || x >= 1.0) domain("atanh", "requires -1 < x < 1", args[0])
                fromDoubleStrict(0.5 * Math.log((1.0 + x) / (1.0 - x)), ctx, arg0 = args[0])
            }

            "ln" -> logValue(args[0], base = null, ctx, label = "ln")
            "log10" -> logValue(args[0], base = null, ctx, label = "log", useLog10 = true)
            "log" ->
                if (args.size == 2) logValue(args[0], base = args[1], ctx, label = "log")
                else logValue(args[0], base = null, ctx, label = "log", useLog10 = true)
            "log2" -> logValue(args[0], base = null, ctx, label = "log2", useLog2 = true)
            "exp" -> fromDoubleStrict(Math.exp(args[0].value.toDouble()), ctx, arg0 = args[0])

            "sqrt" -> {
                val x = args[0].value
                if (x.signum() < 0) domain("sqrt", "requires x ≥ 0 (complex results are not enabled in this mode)", args[0])
                fromDoubleStrict(Math.sqrt(x.toDouble()), ctx, arg0 = args[0])
            }
            "cbrt" -> fromDoubleStrict(Math.cbrt(args[0].value.toDouble()), ctx, arg0 = args[0])
            "abs" -> EvalValue(args[0].value.abs(), args[0].exact)
            "hypot" -> fromDoubleStrict(
                Math.hypot(args[0].value.toDouble(), args[1].value.toDouble()),
                ctx, arg0 = args[0],
            )
            "root" -> {
                // root(n, x): n-th root of x
                val n = args[0].value
                val x = args[1].value
                nthRoot(n, x, ctx, args[1])
            }

            "floor" -> EvalValue(args[0].value.setScale(0, RoundingMode.FLOOR), args[0].exact)
            "ceil" -> EvalValue(args[0].value.setScale(0, RoundingMode.CEILING), args[0].exact)
            "round" -> EvalValue(args[0].value.setScale(0, RoundingMode.HALF_UP), args[0].exact)
            "trunc" -> EvalValue(args[0].value.setScale(0, RoundingMode.DOWN), args[0].exact)
            "sign" -> EvalValue(BigDecimal(args[0].value.signum()), args[0].exact)

            "pow" -> Power.power(args[0].value, args[1].value, ctx, baseExact = args[0].exact && args[1].exact)
            "fact" -> factorialValue(args[0])

            "gcd" -> intFunction(args, "gcd") { acc, v -> acc.gcd(v) }
            "lcm" -> intFunction(args, "lcm") { acc, v ->
                if (acc.signum() == 0 || v.signum() == 0) BigInteger.ZERO
                else acc.divide(acc.gcd(v)).multiply(v).abs()
            }
            "ncr" -> binomial(args, permutations = false)
            "npr" -> binomial(args, permutations = true)

            "min" -> EvalValue(args.map { it.value }.minOrNull()!!, args.all { it.exact })
            "max" -> EvalValue(args.map { it.value }.maxOrNull()!!, args.all { it.exact })
            "sum" -> {
                var acc = BigDecimal.ZERO
                for (a in args) acc = acc.add(a.value)
                EvalValue(acc, args.all { it.exact })
            }
            "avg" -> {
                var acc = BigDecimal.ZERO
                for (a in args) acc = acc.add(a.value)
                val mean = acc.divide(BigDecimal(args.size), NumericPolicy.SCIENCE_MC)
                EvalValue(mean, false)
            }

            "mod" -> {
                val b = args[1].value
                if (b.signum() == 0) throw EvalException(
                    CalcError(ErrorCode.DIVISION_BY_ZERO, "Modulo by zero"),
                )
                EvalValue(args[0].value.remainder(b), args[0].exact && args[1].exact)
            }
            // percent(a, b) = a × b / 100  (explicit, unambiguous)
            "percent" -> {
                val product = args[0].value.multiply(args[1].value)
                EvalValue(product.divide(BigDecimal(100)), false)
            }
            "deg" -> EvalValue(
                args[0].value.multiply(BigDecimal.valueOf(Math.PI))
                    .divide(BigDecimal(180), NumericPolicy.SCIENCE_MC),
                false,
            )
            "rad" -> EvalValue(
                args[0].value.multiply(BigDecimal(180))
                    .divide(BigDecimal.valueOf(Math.PI), NumericPolicy.SCIENCE_MC),
                false,
            )

            else -> throw EvalException(
                CalcError(ErrorCode.UNKNOWN_FUNCTION, "Unknown function '$name'", -1, name),
            )
        }
    }

    // -- helpers -----------------------------------------------------------

    private fun domain(fn: String, why: String, arg: EvalValue): Nothing =
        throw EvalException(
            CalcError(ErrorCode.DOMAIN, "$fn(): $why", -1, "value=${arg.value}"),
        )

    private fun trigValue(arg: EvalValue, ctx: EvalContext, f: (Double) -> Double): EvalValue {
        // Note: sin/cos are defined for every angle — singularity checks live
        // only in tanValue (and inverse functions check their ranges).
        val radians = toRadians(arg.value, ctx.angleMode)
        return fromDoubleStrict(f(radians), ctx, arg0 = arg)
    }

    private fun tanValue(arg: EvalValue, ctx: EvalContext): EvalValue {
        if (ctx.angleMode == AngleMode.DEG &&
            arg.value.remainder(BigDecimal(180)).abs() == BigDecimal(90)
        ) {
            throw EvalException(
                CalcError(ErrorCode.DOMAIN, "tan is undefined at 90° + n·180°", -1),
            )
        }
        val radians = toRadians(arg.value, ctx.angleMode)
        if (Math.cos(radians) == 0.0 || Math.abs(Math.cos(radians)) < 1e-15) {
            throw EvalException(
                CalcError(ErrorCode.DOMAIN, "tan is undefined at this angle (vertical asymptote)", -1),
            )
        }
        return fromDoubleStrict(Math.tan(radians), ctx, arg0 = arg)
    }

    private fun inverseTrig(
        args: List<EvalValue>,
        ctx: EvalContext,
        allowed: (Double) -> Boolean,
        f: (Double) -> Double,
    ): EvalValue {
        val x = args[0].value.toDouble()
        if (!allowed(x)) {
            throw EvalException(
                CalcError(ErrorCode.DOMAIN, "Inverse trigonometric function argument out of range", -1, "x=${args[0].value}"),
            )
        }
        val radians = f(x)
        val converted = when (ctx.angleMode) {
            AngleMode.DEG -> radians * 180.0 / Math.PI
            AngleMode.GRAD -> radians * 200.0 / Math.PI
            AngleMode.RAD -> radians
        }
        return fromDoubleStrict(converted, ctx, arg0 = args[0])
    }

    private fun logValue(
        arg: EvalValue,
        base: EvalValue?,
        ctx: EvalContext,
        label: String,
        useLog10: Boolean = false,
        useLog2: Boolean = false,
    ): EvalValue {
        val x = arg.value
        if (x.signum() <= 0) {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "$label(): requires x > 0", -1, "x=$x"))
        }
        val d = x.toDouble()
        val result = when {
            useLog10 -> Math.log10(d)
            useLog2 -> Math.log(d) / Math.log(2.0)
            base == null -> Math.log(d)
            else -> {
                val b = base.value
                if (b.signum() <= 0) {
                    throw EvalException(CalcError(ErrorCode.DOMAIN, "log(): requires base > 0", -1, "base=$b"))
                }
                if (b.compareTo(BigDecimal.ONE) == 0) {
                    throw EvalException(CalcError(ErrorCode.DOMAIN, "log(): base cannot be 1", -1))
                }
                val bd = b.toDouble()
                Math.log(d) / Math.log(bd)
            }
        }
        return fromDoubleStrict(result, ctx, arg0 = arg)
    }

    private fun nthRoot(n: BigDecimal, x: BigDecimal, ctx: EvalContext, arg: EvalValue): EvalValue {
        if (n.signum() == 0) {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "root(): degree cannot be 0", -1))
        }
        if (x.signum() == 0) return EvalValue(BigDecimal.ZERO, true)
        val nInt = n.stripTrailingZeros().toBigIntegerExact()
        val isEvenDegree = !nInt.testBit(0)
        if (isEvenDegree && x.signum() < 0) {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "root(): even root of a negative number", -1))
        }
        val nd = n.toDouble()
        val xd = x.toDouble()
        val result = Math.signum(xd) * Math.pow(Math.abs(xd), 1.0 / nd)
        return fromDoubleStrict(result, ctx, arg0 = arg)
    }

    internal fun toRadians(value: BigDecimal, mode: AngleMode): Double {
        val d = value.toDouble()
        return when (mode) {
            AngleMode.RAD -> d
            AngleMode.DEG -> d * Math.PI / 180.0
            AngleMode.GRAD -> d * Math.PI / 200.0
        }
    }

    private fun fromDoubleStrict(d: Double, ctx: EvalContext, arg0: EvalValue): EvalValue {
        if (d.isNaN()) {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "Result is undefined for this input", -1))
        }
        if (d.isInfinite()) {
            throw EvalException(CalcError(ErrorCode.OVERFLOW, "Result is out of range", -1))
        }
        val value = BigDecimal(d, MathContext(ctx.significantDigits, RoundingMode.HALF_UP))
        ensureNoOverflow(value)
        return EvalValue(value, false)
    }

    private fun factorialValue(arg: EvalValue): EvalValue {
        val v = arg.value
        val asInt = runCatching { v.toBigIntegerExact() }.getOrElse {
            throw EvalException(
                CalcError(ErrorCode.FACTORIAL_DOMAIN, "Factorial requires a whole number ≥ 0", -1, "x=$v"),
            )
        }
        if (asInt.signum() < 0) {
            throw EvalException(
                CalcError(ErrorCode.FACTORIAL_DOMAIN, "Factorial requires a whole number ≥ 0", -1, "x=$v"),
            )
        }
        if (asInt > BigInteger.valueOf(10_000)) {
            throw EvalException(
                CalcError(ErrorCode.TOO_LARGE, "Factorial argument too large (max 10000)", -1, "x=$v"),
            )
        }
        var acc = BigInteger.ONE
        var i = BigInteger.valueOf(2)
        val end = asInt.add(BigInteger.ONE)
        while (i < end) {
            acc = acc.multiply(i)
            i = i.add(BigInteger.ONE)
        }
        return EvalValue(BigDecimal(acc), true)
    }

    private fun intFunction(
        args: List<EvalValue>,
        name: String,
        f: (BigInteger, BigInteger) -> BigInteger,
    ): EvalValue {
        val ints = args.map { a ->
            runCatching { a.value.toBigIntegerExact() }.getOrElse {
                throw EvalException(
                    CalcError(ErrorCode.DOMAIN, "$name() requires whole numbers", -1, "x=${a.value}"),
                )
            }
        }
        var acc = ints[0]
        for (i in 1 until ints.size) acc = f(acc, ints[i])
        return EvalValue(BigDecimal(acc), true)
    }

    private fun binomial(args: List<EvalValue>, permutations: Boolean): EvalValue {
        val name = if (permutations) "npr" else "ncr"
        val n = runCatching { args[0].value.toBigIntegerExact() }.getOrElse {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "$name() requires whole numbers", -1))
        }
        val k = runCatching { args[1].value.toBigIntegerExact() }.getOrElse {
            throw EvalException(CalcError(ErrorCode.DOMAIN, "$name() requires whole numbers", -1))
        }
        if (n.signum() < 0 || k.signum() < 0 || k > n) {
            throw EvalException(
                CalcError(ErrorCode.DOMAIN, "$name() requires 0 ≤ k ≤ n", -1, "n=$n, k=$k"),
            )
        }
        if (n > BigInteger.valueOf(10_000)) {
            throw EvalException(CalcError(ErrorCode.TOO_LARGE, "$name() argument too large (max 10000)", -1))
        }
        val result = if (permutations) {
            // nPr = n! / (n-k)!
            var acc = BigInteger.ONE
            var i = n
            var count = BigInteger.ZERO
            while (count < k) {
                acc = acc.multiply(i)
                i = i.subtract(BigInteger.ONE)
                count = count.add(BigInteger.ONE)
            }
            acc
        } else {
            // nCr = n! / (k! · (n-k)!) computed multiplicatively (no huge factorials)
            var acc = BigInteger.ONE
            var i = BigInteger.ZERO
            val kk = k.min(n.subtract(k))
            while (i < kk) {
                acc = acc.multiply(n.subtract(i)).divide(i.add(BigInteger.ONE))
                i = i.add(BigInteger.ONE)
            }
            acc
        }
        return EvalValue(BigDecimal(result), true)
    }

    /** Central overflow guard for every result (1E+308 policy, spec §4.3). */
    fun ensureNoOverflow(value: BigDecimal) {
        if (value.abs().compareTo(NumericPolicy.OVERFLOW_LIMIT) > 0) {
            throw EvalException(CalcError(ErrorCode.OVERFLOW, "Result exceeds the numeric range limit", -1))
        }
    }

    /** Integer power with pre-computed magnitude guards (never allocate gigabytes). */
    internal object Power {
        fun power(base: BigDecimal, exp: BigDecimal, ctx: EvalContext, baseExact: Boolean): EvalValue {
            if (base.signum() == 0 && exp.signum() < 0) {
                throw EvalException(CalcError(ErrorCode.DIVISION_BY_ZERO, "Zero cannot be raised to a negative power"))
            }
            if (base.signum() == 0 && exp.signum() == 0) return EvalValue(BigDecimal.ONE, true)

            val expIsInteger = runCatching { exp.toBigIntegerExact(); true }.getOrElse { false }

            if (expIsInteger) {
                val n = exp.toBigIntegerExact()
                if (n.abs() > BigInteger.valueOf(1_000_000)) {
                    throw EvalException(
                        CalcError(ErrorCode.TOO_LARGE, "Exponent too large to compute (max 1000000)", -1),
                    )
                }
                val ni = n.toInt()

                // Allocation guard: digit count of the magnitude cannot exceed 1e6.
                val estimatedDigits =
                    if (ni == 0) 1L
                    else java.lang.Math.abs(ni).toLong() * approxDigitCount(base).coerceAtLeast(1).toLong()
                if (estimatedDigits > 1_000_000) {
                    throw EvalException(CalcError(ErrorCode.TOO_LARGE, "Result too large to compute", -1))
                }

                // Fast overflow rejection for big positive results
                // (log10 in double is precise enough for a >308 test with margin).
                if (ni > 0 && base.abs().signum() > 0) {
                    val estimatedExponent = ni.toDouble() * Math.log10(base.abs().toDouble())
                    if (estimatedExponent > 308.0) {
                        throw EvalException(
                            CalcError(ErrorCode.OVERFLOW, "Result exceeds the numeric range limit", -1),
                        )
                    }
                }

                if (ni >= 0) {
                    val result = base.pow(ni)
                    ensureNoOverflow(result)
                    return EvalValue(result, baseExact)
                }

                // Negative integer exponent: BigDecimal.pow() rejects negative
                // exponents, so compute magnitude first, then take the exact
                // reciprocal (1/2 = 0.5 exact; 1/3 falls back to the precision
                // context and is marked inexact).
                val magnitude = base.pow(-ni)
                val quotient = try {
                    BigDecimal.ONE.divide(magnitude) to true
                } catch (_: ArithmeticException) {
                    BigDecimal.ONE.divide(magnitude, NumericPolicy.SCIENCE_MC) to false
                }
                ensureNoOverflow(quotient.first)
                return EvalValue(quotient.first, baseExact && quotient.second)
            }

            // Non-integer exponent → Double path with documented precision.
            val bd = base.toDouble()
            if (bd < 0.0) {
                throw EvalException(
                    CalcError(ErrorCode.DOMAIN, "A negative base requires a whole-number exponent", -1, "base=$base"),
                )
            }
            val d = Math.pow(bd, exp.toDouble())
            if (d.isNaN()) throw EvalException(CalcError(ErrorCode.DOMAIN, "Power is undefined for these inputs", -1))
            if (d.isInfinite()) throw EvalException(CalcError(ErrorCode.OVERFLOW, "Result is out of range", -1))
            val value = BigDecimal(d, MathContext(ctx.significantDigits, RoundingMode.HALF_UP))
            ensureNoOverflow(value)
            return EvalValue(value, false)
        }

        /** Approximate decimal digit count of |v| (≥1 for any non-zero v). */
        internal fun approxDigitCount(v: BigDecimal): Int {
            val abs = v.abs()
            if (abs.signum() == 0) return 1
            return if (abs >= BigDecimal.ONE) abs.precision() else abs.scale() + 1
        }
    }
}
