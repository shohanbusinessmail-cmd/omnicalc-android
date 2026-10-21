package com.shohan.omnicalc.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateMathTest {

    @Test fun epochIsZero() {
        assertEquals(0L, DateMath.toEpochDay(1970, 1, 1))
        assertEquals(intArrayOf(1970, 1, 1).toList(), DateMath.fromEpochDay(0).toList())
    }

    @Test fun roundTripLeapDay() {
        val d = DateMath.fromEpochDay(DateMath.toEpochDay(2024, 2, 29))
        assertEquals(listOf(2024, 2, 29), d.toList())
    }

    @Test fun februaryHas29In2000() {
        assertTrue(DateMath.isLeapYear(2000))
        assertTrue(DateMath.isValidDate(2000, 2, 29))
        assertFalse(DateMath.isValidDate(1900, 2, 29))
        assertTrue(DateMath.isValidDate(2024, 2, 29))
        assertFalse(DateMath.isValidDate(2023, 2, 29))
    }

    @Test fun diffAcrossLeapDay() {
        assertEquals(29L, DateMath.diffDays(2000, 2, 1, 2000, 3, 1))
    }

    @Test fun diffIsSigned() {
        assertEquals(-10L, DateMath.diffDays(2020, 1, 11, 2020, 1, 1))
    }

    @Test fun addDaysRollsMonthAndYear() {
        val d = DateMath.addDays(2023, 12, 30, 5)
        assertEquals(listOf(2024, 1, 4), d.toList())
    }

    @Test fun addNegativeDays() {
        val d = DateMath.addDays(2024, 3, 1, -1)
        assertEquals(listOf(2024, 2, 29), d.toList())
    }

    @Test fun ageBeforeBirthday() {
        assertEquals(35, DateMath.ageYears(1990, 6, 15, 2026, 3, 12))
    }

    @Test fun ageAfterBirthday() {
        assertEquals(36, DateMath.ageYears(1990, 6, 15, 2026, 10, 21))
    }

    @Test fun feb29BirthdayCountsMar1InCommonYear() {
        // 2000-02-29 → on 2024-02-28 the person has NOT had their birthday
        // yet (anniversary Feb 29 doesn't exist); turns 24 on Mar 1.
        assertEquals(23, DateMath.ageYears(2000, 2, 29, 2024, 2, 28))
        assertEquals(24, DateMath.ageYears(2000, 2, 29, 2024, 3, 1))
    }
}
