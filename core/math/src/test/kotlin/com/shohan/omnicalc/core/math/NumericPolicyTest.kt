package com.shohan.omnicalc.core.math

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class NumericPolicyTest {

    @Test
    fun `money rounding default is HALF_UP - 2_345 rounds to 2_35`() {
        assertEquals(
            BigDecimal("2.35"),
            NumericPolicy.money(BigDecimal("2.345")),
        )
        assertEquals(
            BigDecimal("2.36"),
            NumericPolicy.money(BigDecimal("2.355")),
        )
        assertEquals(
            BigDecimal("-2.35"),
            NumericPolicy.money(BigDecimal("-2.345")),
        )
    }

    @Test
    fun `money scale is 2`() {
        assertEquals(2, NumericPolicy.MONEY_SCALE)
        assertEquals(2, NumericPolicy.money(BigDecimal("7")).scale())
    }
}
