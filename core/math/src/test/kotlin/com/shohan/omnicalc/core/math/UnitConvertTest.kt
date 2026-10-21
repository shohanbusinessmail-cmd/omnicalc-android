package com.shohan.omnicalc.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UnitConvertTest {

    private fun u(id: String): UnitDef = UnitCatalog.find(id) ?: error("missing unit $id")

    private fun conv(v: String, from: String, to: String): String =
        UnitConvert.convert(BigDecimal(v), u(from), u(to))!!.stripTrailingZeros().toPlainString()

    @Test fun inchToMetreExact() {
        assertEquals("0.0254", conv("1", "in", "m"))
    }

    @Test fun footToInchRoundTrip() {
        assertEquals("12", conv("1", "ft", "in"))
    }

    @Test fun kilometreToMile() {
        // 1000 / 1609.344 @ 15 significant digits
        assertEquals("0.621371192237334", conv("1", "km", "mi"))
    }

    @Test fun poundToKilogramExact() {
        assertEquals("0.45359237", conv("1", "lb", "kg"))
    }

    @Test fun celsiusToBoilingFahrenheit() {
        assertEquals("212", conv("100", "c", "f"))
    }

    @Test fun fahrenheitToCelsiusFreezing() {
        assertEquals("0", conv("32", "f", "c"))
    }

    @Test fun celsiusToKelvin() {
        assertEquals("273.15", conv("0", "c", "k"))
    }

    @Test fun kelvinToCelsius() {
        assertEquals("26.85", conv("300", "k", "c"))
    }

    @Test fun kmhToMs() {
        assertEquals("10", conv("36", "kmh", "ms"))
    }

    @Test fun usGallonToLitre() {
        assertEquals("3.785411784", conv("1", "gal", "l"))
    }

    @Test fun gigibyteToByte() {
        assertEquals("1073741824", conv("1", "gib", "b"))
    }

    @Test fun hourToMinute() {
        assertEquals("60", conv("1", "h", "min"))
    }

    @Test fun crossCategoryIsNull() {
        assertNull(UnitConvert.convert(BigDecimal.ONE, u("m"), u("kg")))
    }

    @Test fun catalogCoversAllCategories() {
        for (c in UnitCategory.entries) {
            assertTrue("units for $c", UnitCatalog.units(c).size >= 3)
        }
    }
}
