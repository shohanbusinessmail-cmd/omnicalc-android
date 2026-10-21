package com.shohan.omnicalc.core.math

/**
 * Pure civil-date arithmetic on the proleptic Gregorian calendar with
 * epoch-day 0 = 1970-01-01 (spec §14). No java.util/LocalDate — works on
 * every supported API level and is fully unit-tested in the JVM harness.
 */
object DateMath {

    /** @return epoch day of y-m-d (negative before 1970). */
    fun toEpochDay(year: Int, month: Int, day: Int): Long {
        require(month in 1..12 && day in 1..31) { "invalid date" }
        var y = year
        val m = month
        if (m <= 2) y -= 1
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + if (m > 2) -3 else 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468L
    }

    /** @return intArrayOf(year, month, day) for an epoch day. */
    fun fromEpochDay(epochDay: Long): IntArray {
        var z = epochDay + 719468L
        val era = Math.floorDiv(z, 146097L)
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        val year = (y + if (m <= 2) 1 else 0).toInt()
        return intArrayOf(year, m.toInt(), d.toInt())
    }

    /** Signed day difference: end − start (negative when end is earlier). */
    fun diffDays(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int,
    ): Long = toEpochDay(endYear, endMonth, endDay) - toEpochDay(startYear, startMonth, startDay)

    /** y-m-d shifted by [days] (may be negative). */
    fun addDays(year: Int, month: Int, day: Int, days: Int): IntArray =
        fromEpochDay(toEpochDay(year, month, day) + days)

    /**
     * Completed years between two civil dates. A Feb-29 birthday counts in
     * non-leap years only after Feb 28 (i.e. on Mar 1) — documented in
     * docs/REGIONAL_RULES.md as the chosen convention.
     */
    fun ageYears(birthYear: Int, birthMonth: Int, birthDay: Int, onYear: Int, onMonth: Int, onDay: Int): Int {
        require(toEpochDay(onYear, onMonth, onDay) >= toEpochDay(birthYear, birthMonth, birthDay)) {
            "reference date is before the birth date"
        }
        var years = onYear - birthYear
        val beforeBirthday = onMonth < birthMonth || (onMonth == birthMonth && onDay < birthDay)
        if (beforeBirthday) years -= 1
        return years
    }

    fun isValidDate(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day < 1) return false
        val maxDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (isLeapYear(year)) 29 else 28
        }
        return day <= maxDay
    }

    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
