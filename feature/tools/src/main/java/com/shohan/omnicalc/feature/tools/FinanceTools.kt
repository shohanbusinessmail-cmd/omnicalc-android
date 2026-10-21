package com.shohan.omnicalc.feature.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Finance tool logic (spec §11.4–§11.6): VAT with explicit mode + provenance,
 * an offline currency table whose rates are ONLY user-entered (never
 * fabricated by the app), and user-defined salary rows.
 *
 * All monetary math is BigDecimal HALF_UP at 15 significant digits (the
 * shared policy documented in docs/MATH_ENGINE.md).
 */
object FinanceTools {

    private val MC = MathContext(15, RoundingMode.HALF_UP)

    // ---------------------------------------------------------------- VAT --
    enum class VatMode { EXCLUSIVE, INCLUSIVE }

    data class VatResult(
        /** Tax portion. */
        val tax: BigDecimal,
        /** Net (tax-excluded) amount. */
        val net: BigDecimal,
        /** Gross (tax-inclusive) amount. */
        val gross: BigDecimal,
    )

    /**
     * @param amount the entered price (net when exclusive, gross when inclusive)
     * @param ratePercent VAT % e.g. 15
     */
    fun vat(amount: BigDecimal, ratePercent: BigDecimal, mode: VatMode): VatResult? {
        if (ratePercent.signum() < 0 || ratePercent.compareTo(BigDecimal("1000")) > 0) return null
        val hundred = BigDecimal(100)
        return when (mode) {
            VatMode.EXCLUSIVE -> {
                val tax = amount.multiply(ratePercent, MC).divide(hundred, MC)
                VatResult(tax = tax, net = amount, gross = amount.add(tax))
            }
            VatMode.INCLUSIVE -> {
                val divisor = hundred.add(ratePercent)
                if (divisor.signum() == 0) return null
                val gross = amount
                val net = gross.multiply(hundred, MC).divide(divisor, MC)
                VatResult(tax = gross.subtract(net), net = net, gross = gross)
            }
        }
    }

    // ------------------------------------------------------------ salary --
    /** User-defined salary line (label + amount); direction separates lists. */
    data class SalaryRow(val id: Long, val label: String, val amount: BigDecimal)

    data class SalaryTotals(
        val earnings: BigDecimal,
        val deductions: BigDecimal,
        val net: BigDecimal,
        val deductionPercent: BigDecimal?,
    )

    fun salaryTotals(earnings: List<SalaryRow>, deductions: List<SalaryRow>): SalaryTotals {
        val e = earnings.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
        val d = deductions.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
        val net = e.subtract(d)
        val pct = if (e.signum() > 0) d.multiply(BigDecimal(100), MC).divide(e, MC) else null
        return SalaryTotals(earnings = e, deductions = d, net = net, deductionPercent = pct)
    }
}
