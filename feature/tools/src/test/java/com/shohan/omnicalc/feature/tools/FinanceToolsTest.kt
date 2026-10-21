package com.shohan.omnicalc.feature.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinanceToolsTest {

    @Test fun vatExclusive() {
        val r = FinanceTools.vat(BigDecimal("100"), BigDecimal("15"), FinanceTools.VatMode.EXCLUSIVE)!!
        assertEquals(BigDecimal("15"), r.tax)
        assertEquals(BigDecimal("100"), r.net)
        assertEquals(BigDecimal("115"), r.gross)
    }

    @Test fun vatInclusive() {
        val r = FinanceTools.vat(BigDecimal("115"), BigDecimal("15"), FinanceTools.VatMode.INCLUSIVE)!!
        assertEquals(BigDecimal("100"), r.net)
        assertEquals(BigDecimal("15"), r.tax)
        assertEquals(BigDecimal("115"), r.gross)
    }

    @Test fun vatZeroRateKeepsAmount() {
        val r = FinanceTools.vat(BigDecimal("42"), BigDecimal("0"), FinanceTools.VatMode.INCLUSIVE)!!
        assertEquals(BigDecimal("42"), r.net)
        assertEquals(BigDecimal.ZERO, r.tax)
    }

    @Test fun vatRejectsNegativeRate() {
        assertNull(FinanceTools.vat(BigDecimal.ONE, BigDecimal("-1"), FinanceTools.VatMode.EXCLUSIVE))
    }

    @Test fun salaryTotalsAndPercent() {
        val e = listOf(
            FinanceTools.SalaryRow(1, "Basic", BigDecimal("50000")),
            FinanceTools.SalaryRow(2, "House", BigDecimal("20000")),
        )
        val d = listOf(FinanceTools.SalaryRow(3, "Tax", BigDecimal("7000")))
        val t = FinanceTools.salaryTotals(e, d)
        assertEquals(BigDecimal("70000"), t.earnings)
        assertEquals(BigDecimal("7000"), t.deductions)
        assertEquals(BigDecimal("63000"), t.net)
        assertEquals(BigDecimal("10"), t.deductionPercent)
    }
}

class LandProfilesTest {

    @Test fun builtInProfilesCarryProvenance() {
        for (p in LandProfiles.BUILT_IN) {
            assertTrue(p.source.isNotBlank())
            assertTrue(p.effectiveDate.isNotBlank())
            assertTrue(p.region.isNotBlank())
            assertTrue(p.units.isNotEmpty())
        }
    }

    @Test fun decimalToSquareFeet() {
        val p = LandProfiles.BUILT_IN.first()
        assertEquals(
            BigDecimal("435.6"),
            LandProfiles.toSquareFeet(BigDecimal.TEN, "Decimal", p)!!.stripTrailingZeros(),
        )
    }

    @Test fun roundTripBetweenUnits() {
        val p = LandProfiles.BUILT_IN.first()
        val sqft = LandProfiles.toSquareFeet(BigDecimal("2"), "Katha", p)!!
        val back = LandProfiles.fromSquareFeet(sqft, "Katha", p)!!
        assertEquals(0, back.compareTo(BigDecimal("2")))
    }

    @Test fun unknownUnitIsNull() {
        val p = LandProfiles.BUILT_IN.first()
        assertNull(LandProfiles.toSquareFeet(BigDecimal.ONE, "Ropani", p))
    }

    @Test fun serializationRoundTripKeepsAllProfiles() {
        val raw = LandProfiles.serialize(LandProfiles.BUILT_IN)
        val back = LandProfiles.deserialize(raw)
        assertEquals(LandProfiles.BUILT_IN.size, back.size)
        assertEquals(LandProfiles.BUILT_IN[0].id, back[0].id)
        assertEquals(LandProfiles.BUILT_IN[1].units, back[1].units)
        assertEquals(LandProfiles.BUILT_IN[1].source, back[1].source)
    }

    @Test fun corruptPayloadFallsBackToBuiltIns() {
        assertEquals(LandProfiles.BUILT_IN, LandProfiles.deserialize("garbage|||"))
        assertEquals(LandProfiles.BUILT_IN, LandProfiles.deserialize(null))
    }
}
