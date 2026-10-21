package com.shohan.omnicalc.core.common

/**
 * User-facing preference enums shared by settings UI, persistence and the
 * math engine (domain models live in core:common per architecture docs).
 */

/** Which angle unit trigonometric functions use. */
enum class AngleMode { DEG, RAD, GRAD }

/**
 * Percent semantics (spec §4.5).
 *  - [STANDARD]: `200 + 10%` → 220, `200 − 10%` → 180, `200 × 10%` → 20.
 *  - [EXPLICIT]: `%` always means ÷100 (formula tools; unambiguous).
 */
enum class PercentMode { STANDARD, EXPLICIT }

/** Theme preference (spec §2.2). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Interface language (spec §8.1): default English, full Bengali support. */
enum class LanguageMode { SYSTEM, ENGLISH, BENGALIC }

/**
 * Result display precision (spec §4.3): [AUTO] strips trailing zeros;
 * [FIXED] uses [AppSettings.decimalPlaces] decimals.
 */
enum class PrecisionMode { AUTO, FIXED }
