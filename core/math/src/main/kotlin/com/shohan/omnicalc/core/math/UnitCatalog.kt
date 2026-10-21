package com.shohan.omnicalc.core.math

import java.math.BigDecimal

/**
 * Unit catalog (spec §11): international standard definitions only.
 * Bangla labels are used by the UI; [note] carries honesty qualifiers like
 * "US" or "365 days". Regional land units are intentionally absent — they
 * are configurable profiles in the Land tool (spec §7, §11.3).
 */
object UnitCatalog {

    private fun d(s: String) = BigDecimal(s)

    private val length = listOf(
        UnitDef("mm", UnitCategory.LENGTH, "mm", "Millimetre", "মিলিমিটার", d("1"), d("1000")),
        UnitDef("cm", UnitCategory.LENGTH, "cm", "Centimetre", "সেন্টিমিটার", d("1"), d("100")),
        UnitDef("m", UnitCategory.LENGTH, "m", "Metre", "মিটার", BigDecimal.ONE),
        UnitDef("km", UnitCategory.LENGTH, "km", "Kilometre", "কিলোমিটার", d("1000")),
        UnitDef("in", UnitCategory.LENGTH, "in", "Inch", "ইঞ্চি", d("0.0254")),
        UnitDef("ft", UnitCategory.LENGTH, "ft", "Foot", "ফুট", d("0.3048")),
        UnitDef("yd", UnitCategory.LENGTH, "yd", "Yard", "গজ", d("0.9144")),
        UnitDef("mi", UnitCategory.LENGTH, "mi", "Mile", "মাইল", d("1609.344")),
        UnitDef("nmi", UnitCategory.LENGTH, "nmi", "Nautical mile", "নটিক্যাল মাইল", d("1852")),
    )

    private val mass = listOf(
        UnitDef("mg", UnitCategory.MASS, "mg", "Milligram", "মিলিগ্রাম", d("1"), d("1000000")),
        UnitDef("g", UnitCategory.MASS, "g", "Gram", "গ্রাম", d("1"), d("1000")),
        UnitDef("kg", UnitCategory.MASS, "kg", "Kilogram", "কিলোগ্রাম", BigDecimal.ONE),
        UnitDef("t", UnitCategory.MASS, "t", "Tonne", "টন", d("1000"), note = "metric"),
        UnitDef("oz", UnitCategory.MASS, "oz", "Ounce", "আউন্স", d("0.028349523125")),
        UnitDef("lb", UnitCategory.MASS, "lb", "Pound", "পাউন্ড", d("0.45359237")),
    )

    private val volume = listOf(
        UnitDef("ml", UnitCategory.VOLUME, "mL", "Millilitre", "মিলিলিটার", d("1"), d("1000")),
        UnitDef("l", UnitCategory.VOLUME, "L", "Litre", "লিটার", BigDecimal.ONE),
        UnitDef("m3", UnitCategory.VOLUME, "m³", "Cubic metre", "ঘনমিটার", d("1000")),
        UnitDef("tsp", UnitCategory.VOLUME, "tsp", "Teaspoon (US)", "চা-চামচ (মার্কিন)", d("0.00492892159375"), note = "US"),
        UnitDef("tbsp", UnitCategory.VOLUME, "tbsp", "Tablespoon (US)", "টেবিল-চামচ (মার্কিন)", d("0.01478676478125"), note = "US"),
        UnitDef("cup", UnitCategory.VOLUME, "cup", "Cup (US)", "কাপ (মার্কিন)", d("0.2365882365"), note = "US"),
        UnitDef("floz", UnitCategory.VOLUME, "fl oz", "Fluid ounce (US)", "ফ্লুইড আউন্স (মার্কিন)", d("0.0295735295625"), note = "US"),
        UnitDef("gal", UnitCategory.VOLUME, "gal", "Gallon (US)", "গ্যালন (মার্কিন)", d("3.785411784"), note = "US"),
        UnitDef("impgal", UnitCategory.VOLUME, "imp gal", "Gallon (imperial)", "গ্যালন (ইম্পিরিয়াল)", d("4.54609"), note = "imperial"),
    )

    private val area = listOf(
        UnitDef("mm2", UnitCategory.AREA, "mm²", "Square millimetre", "বর্গমিলিমিটার", d("1"), d("1000000")),
        UnitDef("cm2", UnitCategory.AREA, "cm²", "Square centimetre", "বর্গসেন্টিমিটার", d("1"), d("10000")),
        UnitDef("m2", UnitCategory.AREA, "m²", "Square metre", "বর্গমিটার", BigDecimal.ONE),
        UnitDef("km2", UnitCategory.AREA, "km²", "Square kilometre", "বর্গকিলোমিটার", d("1000000")),
        UnitDef("ft2", UnitCategory.AREA, "ft²", "Square foot", "বর্গফুট", d("0.09290304")),
        UnitDef("ha", UnitCategory.AREA, "ha", "Hectare", "হেক্টর", d("10000")),
        UnitDef("acre", UnitCategory.AREA, "acre", "Acre", "একর", d("4046.8564224")),
    )

    private val temperature = listOf(
        UnitDef("c", UnitCategory.TEMPERATURE, "°C", "Celsius", "সেলসিয়াস", BigDecimal.ONE, BigDecimal.ONE, d("273.15")),
        UnitDef("f", UnitCategory.TEMPERATURE, "°F", "Fahrenheit", "ফারেনহাইট", d("5"), d("9"), d("459.67"), "→ K = (F + 459.67) × 5/9"),
        UnitDef("k", UnitCategory.TEMPERATURE, "K", "Kelvin", "কেলভিন", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO),
    )

    private val speed = listOf(
        UnitDef("ms", UnitCategory.SPEED, "m/s", "Metres per second", "মিটার/সেকেন্ড", BigDecimal.ONE),
        UnitDef("kmh", UnitCategory.SPEED, "km/h", "Kilometres per hour", "কিলোমিটার/ঘণ্টা", d("1000"), d("3600")),
        UnitDef("mph", UnitCategory.SPEED, "mph", "Miles per hour", "মাইল/ঘণ্টা", d("1609.344"), d("3600")),
        UnitDef("knot", UnitCategory.SPEED, "kn", "Knot", "নট", d("1852"), d("3600")),
    )

    private val time = listOf(
        UnitDef("ms_t", UnitCategory.TIME, "ms", "Millisecond", "মিলিসেকেন্ড", d("1"), d("1000")),
        UnitDef("s", UnitCategory.TIME, "s", "Second", "সেকেন্ড", BigDecimal.ONE),
        UnitDef("min", UnitCategory.TIME, "min", "Minute", "মিনিট", d("60")),
        UnitDef("h", UnitCategory.TIME, "h", "Hour", "ঘণ্টা", d("3600")),
        UnitDef("day", UnitCategory.TIME, "day", "Day", "দিন", d("86400")),
        UnitDef("week", UnitCategory.TIME, "week", "Week", "সপ্তাহ", d("604800")),
        UnitDef("year365", UnitCategory.TIME, "y", "Year", "বছর", d("31536000"), note = "365 days"),
        UnitDef("month30", UnitCategory.TIME, "mo", "Month", "মাস", d("2592000"), note = "30 days"),
    )

    private val data = listOf(
        UnitDef("bit", UnitCategory.DATA, "bit", "Bit", "বিট", d("1"), d("8")),
        UnitDef("b", UnitCategory.DATA, "B", "Byte", "বাইট", BigDecimal.ONE),
        UnitDef("kb", UnitCategory.DATA, "kB", "Kilobyte", "কিলোবাইট", d("1000"), note = "decimal"),
        UnitDef("mb", UnitCategory.DATA, "MB", "Megabyte", "মেগাবাইট", d("1000000"), note = "decimal"),
        UnitDef("gb", UnitCategory.DATA, "GB", "Gigabyte", "গিগাবাইট", d("1000000000"), note = "decimal"),
        UnitDef("tb", UnitCategory.DATA, "TB", "Terabyte", "টেরাবাইট", d("1000000000000"), note = "decimal"),
        UnitDef("kib", UnitCategory.DATA, "KiB", "Kibibyte", "কিবিবাইট", d("1024"), note = "binary"),
        UnitDef("mib", UnitCategory.DATA, "MiB", "Mebibyte", "মেবিবাইট", d("1048576"), note = "binary"),
        UnitDef("gib", UnitCategory.DATA, "GiB", "Gibibyte", "গিবিবাইট", d("1073741824"), note = "binary"),
    )

    val byCategory: Map<UnitCategory, List<UnitDef>> = mapOf(
        UnitCategory.LENGTH to length,
        UnitCategory.MASS to mass,
        UnitCategory.VOLUME to volume,
        UnitCategory.AREA to area,
        UnitCategory.TEMPERATURE to temperature,
        UnitCategory.SPEED to speed,
        UnitCategory.TIME to time,
        UnitCategory.DATA to data,
    )

    val categories: List<UnitCategory> = UnitCategory.entries

    fun units(category: UnitCategory): List<UnitDef> = byCategory.getValue(category)

    fun find(id: String): UnitDef? = byCategory.values.flatten().firstOrNull { it.id == id }
}
