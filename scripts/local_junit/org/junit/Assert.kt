package org.junit

import java.math.BigDecimal

/**
 * Local-only subset of org.junit.Assert (JUnit 4) used by the sandbox
 * verification harness. Signatures mirror the real JUnit 4 API so test
 * sources compile unchanged against the real artifact in Gradle/CI.
 */
object Assert {

    @JvmStatic
    fun fail(message: String? = null): Nothing = throw AssertionError(message ?: "failed")

    @JvmStatic
    fun assertTrue(message: String? = null, condition: Boolean) {
        if (!condition) throw AssertionError(message ?: "expected true")
    }

    @JvmStatic
    fun assertTrue(condition: Boolean) = assertTrue(null, condition)

    @JvmStatic
    fun assertFalse(message: String? = null, condition: Boolean) {
        if (condition) throw AssertionError(message ?: "expected false")
    }

    @JvmStatic
    fun assertFalse(condition: Boolean) = assertFalse(null, condition)

    @JvmStatic
    fun assertEquals(message: String? = null, expected: Any?, actual: Any?) {
        if (expected != actual) {
            throw AssertionError(format(message, expected, actual))
        }
    }

    @JvmStatic
    fun assertEquals(expected: Any?, actual: Any?) = assertEquals(null, expected, actual)

    @JvmStatic
    fun assertEquals(message: String?, expected: Double, actual: Double, delta: Double) {
        if (java.lang.Double.isNaN(expected) || java.lang.Double.isNaN(actual) ||
            Math.abs(expected - actual) > delta
        ) {
            throw AssertionError(format(message, expected, actual) + " (delta=$delta)")
        }
    }

    @JvmStatic
    fun assertEquals(expected: Double, actual: Double, delta: Double) =
        assertEquals(null, expected, actual, delta)

    @JvmStatic
    fun assertEquals(message: String?, expected: Float, actual: Float, delta: Float) {
        if (Math.abs(expected - actual) > delta) {
            throw AssertionError(format(message, expected, actual) + " (delta=$delta)")
        }
    }

    @JvmStatic
    fun assertEquals(expected: Float, actual: Float, delta: Float) =
        assertEquals(null, expected, actual, delta)

    @JvmStatic
    fun assertEquals(message: String?, expected: Long, actual: Long) {
        if (expected != actual) throw AssertionError(format(message, expected, actual))
    }

    @JvmStatic
    fun assertEquals(expected: Long, actual: Long) = assertEquals(null, expected, actual)

    @JvmStatic
    fun assertEquals(message: String?, expected: Int, actual: Int) {
        if (expected != actual) throw AssertionError(format(message, expected, actual))
    }

    @JvmStatic
    fun assertEquals(expected: Int, actual: Int) = assertEquals(null, expected, actual)

    @JvmStatic
    fun assertNotEquals(message: String? = null, unexpected: Any?, actual: Any?) {
        if (unexpected == actual) {
            throw AssertionError(format(message, unexpected, actual) + " — values must differ")
        }
    }

    @JvmStatic
    fun assertNotEquals(unexpected: Any?, actual: Any?) = assertNotEquals(null, unexpected, actual)

    @JvmStatic
    fun assertNull(message: String? = null, actual: Any?) {
        if (actual != null) throw AssertionError(format(message, null, actual))
    }

    @JvmStatic
    fun assertNull(actual: Any?) = assertNull(null, actual)

    @JvmStatic
    fun assertNotNull(message: String? = null, actual: Any?) {
        if (actual == null) throw AssertionError(message ?: "expected non-null")
    }

    @JvmStatic
    fun assertNotNull(actual: Any?) = assertNotNull(null, actual)

    @JvmStatic
    fun assertArrayEquals(message: String? = null, expected: LongArray, actual: LongArray) {
        if (!expected.contentEquals(actual)) {
            throw AssertionError(format(message, expected.toList(), actual.toList()))
        }
    }

    @JvmStatic
    fun assertArrayEquals(expected: LongArray, actual: LongArray) =
        assertArrayEquals(null, expected, actual)

    @JvmStatic
    fun assertArrayEquals(message: String? = null, expected: IntArray, actual: IntArray) {
        if (!expected.contentEquals(actual)) {
            throw AssertionError(format(message, expected.toList(), actual.toList()))
        }
    }

    @JvmStatic
    fun assertArrayEquals(expected: IntArray, actual: IntArray) =
        assertArrayEquals(null, expected, actual)

    @JvmStatic
    fun assertArrayEquals(message: String? = null, expected: Array<*>, actual: Array<*>) {
        if (!expected.contentEquals(actual)) {
            throw AssertionError(format(message, expected.toList(), actual.toList()))
        }
    }

    @JvmStatic
    fun assertArrayEquals(expected: Array<*>, actual: Array<*>) =
        assertArrayEquals(null, expected, actual)

    /** BigDecimal comparison by value (not by equals scale semantics). */
    @JvmStatic
    fun assertEquals(message: String?, expected: BigDecimal, actual: BigDecimal) {
        if (expected.compareTo(actual) != 0) {
            throw AssertionError(format(message, expected, actual))
        }
    }

    @JvmStatic
    fun assertEquals(expected: BigDecimal, actual: BigDecimal) =
        assertEquals(null, expected, actual)

    private fun format(message: String?, expected: Any?, actual: Any?): String {
        val prefix = if (message != null) "$message — " else ""
        return "${prefix}expected:<$expected> but was:<$actual>"
    }
}
