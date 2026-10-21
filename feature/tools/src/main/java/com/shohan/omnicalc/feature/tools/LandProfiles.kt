package com.shohan.omnicalc.feature.tools

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

/**
 * Regional land-unit profiles (spec §7, §11.3).
 *
 * Land units (Katha/Bigha/Decimal/…) differ by region — NOTHING here is a
 * universal constant. Each profile carries provenance metadata (source,
 * effective date, region) and is fully editable at runtime; built-in values
 * are clearly labeled as reference starting points to verify locally before
 * reliance.
 *
 * Profiles persist as a simple delimited string in DataStore:
 *   id|nameEn|nameBn|region|source|effective|sqft-per-unit~unit=sqft:factor~...
 * (parsing is defensive: a corrupt record is skipped, never crashes).
 */
data class LandProfile(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val region: String,
    val source: String,
    val effectiveDate: String,
    /** unitName → how many SQUARE FEET one unit equals (base = sqft). */
    val units: Map<String, BigDecimal>,
) {
    init {
        require(id.isNotBlank() && units.isNotEmpty())
    }
}

object LandProfiles {

    /** Square feet per decimal — exact by definition (0.01 are = 43.56 sq ft). */
    val SQFT_PER_DECIMAL: BigDecimal = BigDecimal("43.56")

    val DEFAULT_SQFT_UNITS: Map<String, BigDecimal> = mapOf(
        "Square foot" to BigDecimal.ONE,
        "Square metre" to BigDecimal("10.7639104167097"),
        "Square yard" to BigDecimal("9"),
        "Decimal" to SQFT_PER_DECIMAL,
        "Acre" to BigDecimal("43560"),
        "Hectare" to BigDecimal("107639.104167097"),
    )

    /**
     * Built-in REFERENCE profiles — editable, dated, sourced. Values are
     * widely-cited reference figures; the UI tells users to verify for their
     * locality (spec §7.1: never hardcode as universal truth).
     */
    val BUILT_IN: List<LandProfile> = listOf(
        LandProfile(
            id = "bd-common",
            nameEn = "Bangladesh — common reference",
            nameBn = "বাংলাদেশ — প্রচলিত রেফারেন্স",
            region = "Bangladesh (general)",
            source = "Commonly cited BD usage (1 decimal = 43.56 sq ft; 1 katha = 720 sq ft; 1 bigha = 33 katha). Verify locally.",
            effectiveDate = "2026-01-01",
            units = mapOf(
                "Square foot" to BigDecimal.ONE,
                "Decimal" to SQFT_PER_DECIMAL,
                "Katha" to BigDecimal("720"),
                "Bigha" to BigDecimal("23760"), // 33 katha × 720 sq ft
                "Bigha (20-katha variant)" to BigDecimal("14400"),
            ),
        ),
        LandProfile(
            id = "dhaka-division",
            nameEn = "Dhaka Division — reference",
            nameBn = "ঢাকা বিভাগ — রেফারেন্স",
            region = "Dhaka Division",
            source = "Locally circulated Dhaka-area reference figures; katha/bigha conventions differ across districts. Verify with the local registration office.",
            effectiveDate = "2026-01-01",
            units = mapOf(
                "Square foot" to BigDecimal.ONE,
                "Decimal" to SQFT_PER_DECIMAL,
                "Katha (Dhaka reference)" to BigDecimal("1361.7"),
                "Bigha (Dhaka reference)" to BigDecimal("1600"), // sq-ft based bigha used in parts of Dhaka
            ),
        ),
    )

    private val MC = MathContext(15, RoundingMode.HALF_UP)

    /** Converts [amount] of [unit] into square feet within [profile]. */
    fun toSquareFeet(amount: BigDecimal, unit: String, profile: LandProfile): BigDecimal? {
        val factor = profile.units[unit] ?: return null
        return amount.multiply(factor, MC)
    }

    fun fromSquareFeet(squareFeet: BigDecimal, unit: String, profile: LandProfile): BigDecimal? {
        val factor = profile.units[unit] ?: return null
        if (factor.signum() == 0) return null
        return squareFeet.divide(factor, MC)
    }

    /** Convenience: convert between two units of the same profile. */
    fun convert(amount: BigDecimal, from: String, to: String, profile: LandProfile): BigDecimal? {
        val sqft = toSquareFeet(amount, from, profile) ?: return null
        return fromSquareFeet(sqft, to, profile)
    }

    // ------------------------------------------------------------ persistence --
    private const val REC_SEP = "\n"
    private const val FIELD_SEP = "|"
    private const val UNIT_SEP = "~"
    private const val UNIT_KV = "="

    fun serialize(profiles: List<LandProfile>): String = profiles.joinToString(REC_SEP) { p ->
        val units = p.units.entries.joinToString(UNIT_SEP) { (k, v) -> "$k$UNIT_KV${v.toPlainString()}" }
        listOf(p.id, p.nameEn, p.nameBn, p.region, p.source, p.effectiveDate, units)
            .joinToString(FIELD_SEP) { it.replace(FIELD_SEP, " ").replace(REC_SEP, " ") }
    }

    fun deserialize(raw: String?): List<LandProfile> {
        if (raw.isNullOrBlank()) return BUILT_IN
        return raw.lineSequence()
            .mapNotNull { line -> parseRecord(line) }
            .toList()
            .ifEmpty { BUILT_IN }
    }

    private fun parseRecord(line: String): LandProfile? {
        val parts = line.split(FIELD_SEP)
        if (parts.size < 7) return null
        val units = parts[6].split(UNIT_SEP).mapNotNull { kv ->
            val i = kv.indexOf(UNIT_KV)
            if (i <= 0) return@mapNotNull null
            val v = try {
                BigDecimal(kv.substring(i + 1))
            } catch (e: NumberFormatException) {
                return@mapNotNull null
            }
            kv.substring(0, i) to v
        }.toMap()
        if (units.isEmpty()) return null
        return LandProfile(
            id = parts[0],
            nameEn = parts[1],
            nameBn = parts[2],
            region = parts[3],
            source = parts[4],
            effectiveDate = parts[5],
            units = units,
        )
    }
}
