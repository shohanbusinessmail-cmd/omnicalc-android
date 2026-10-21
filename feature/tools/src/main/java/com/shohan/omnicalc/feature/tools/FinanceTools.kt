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

    // ------------------------------------------------------------ currency --
    /**
     * One offline reference row. [rate] = BDT per 1 unit of [code].
     * A null rate means "not entered yet" — the UI prompts for the user's
     * own verified figure with source and effective date (spec §11.6:
     * never invent exchange rates).
     */
    data class CurrencyRow(
        val code: String,
        val nameEn: String,
        val nameBn: String,
        val rate: BigDecimal?,
        val source: String,
        val effectiveDate: String,
    )

    val CURRENCY_CATALOG: List<Pair<String, Pair<String, String>>> = listOf(
        "USD" to ("US Dollar" to "মার্কিন ডলার"),
        "EUR" to ("Euro" to "ইউরো"),
        "GBP" to ("Pound Sterling" to "ব্রিটিশ পাউন্ড"),
        "SAR" to ("Saudi Riyal" to "সৌদি রিয়াল"),
        "AED" to ("UAE Dirham" to "UAE দিরহাম"),
        "MYR" to ("Malaysian Ringgit" to "মালয়েশিয়ান রিঙ্গিত"),
        "INR" to ("Indian Rupee" to "ভারতীয় রুপি"),
        "SGD" to ("Singapore Dollar" to "সিঙ্গাপুর ডলার"),
        "JPY" to ("Japanese Yen" to "জাপানি ইয়েন"),
        "AUD" to ("Australian Dollar" to "অস্ট্রেলিয়ান ডলার"),
        "CAD" to ("Canadian Dollar" to "কানাডিয়ান ডলার"),
        "KWD" to ("Kuwaiti Dinar" to "কুয়েতি দিনার"),
        "QAR" to ("Qatari Riyal" to "কাতারি রিয়াল"),
        "CNY" to ("Chinese Yuan" to "চীনা ইয়ুয়ান"),
        "PKR" to ("Pakistani Rupee" to "পাকিস্তানি রুপি"),
    )

    fun defaultCurrencyRows(): List<CurrencyRow> = CURRENCY_CATALOG.map { (code, names) ->
        CurrencyRow(code, names.first, names.second, rate = null, source = "", effectiveDate = "")
    }

    /** amount of [code] → BDT (only when a rate exists). */
    fun toBdt(amount: BigDecimal, row: CurrencyRow): BigDecimal? {
        val rate = row.rate ?: return null
        return amount.multiply(rate, MC)
    }

    /** amount of BDT → [code]. */
    fun fromBdt(bdt: BigDecimal, row: CurrencyRow): BigDecimal? {
        val rate = row.rate ?: return null
        if (rate.signum() == 0) return null
        return bdt.divide(rate, MC)
    }

    // ---- serialization: code|rate|source|effective per line (rate may be "") --
    fun serializeCurrency(rows: List<CurrencyRow>): String = rows.joinToString("\n") { r ->
        listOf(
            r.code,
            r.rate?.toPlainString() ?: "",
            r.source.replace("|", "/").replace("\n", " "),
            r.effectiveDate.replace("|", "/").replace("\n", " "),
        ).joinToString("|")
    }

    fun deserializeCurrency(raw: String?): List<CurrencyRow> {
        val defaults = defaultCurrencyRows().associateBy { it.code }.toMutableMap()
        if (!raw.isNullOrBlank()) {
            for (line in raw.lineSequence()) {
                val parts = line.split("|")
                if (parts.size < 4) continue
                val code = parts[0]
                val existing = defaults[code] ?: continue
                val rate = parts[1].takeIf { it.isNotBlank() }?.let {
                    try {
                        BigDecimal(it)
                    } catch (e: NumberFormatException) {
                        null
                    }
                }
                defaults[code] = existing.copy(rate = rate, source = parts[2], effectiveDate = parts[3])
            }
        }
        return defaults.values.toList()
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
