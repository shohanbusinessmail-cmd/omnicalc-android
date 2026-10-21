package com.shohan.omnicalc.core.common

/**
 * Complete persisted user preferences (spec §17). Pure data — serialized by
 * the DataStore layer in core:database. Every field has a safe default so a
 * fresh install (or corrupted storage) always yields usable settings.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
    val numeralStyle: NumeralStyle = NumeralStyle.WESTERN,
    val groupingStyle: GroupingStyle = GroupingStyle.INTERNATIONAL,
    val precisionMode: PrecisionMode = PrecisionMode.AUTO,
    /** Used only when [precisionMode] == FIXED; 0..12. */
    val decimalPlaces: Int = 2,
    val angleMode: AngleMode = AngleMode.DEG,
    val percentMode: PercentMode = PercentMode.STANDARD,
    /** Haptic key-click feedback (spec §2.4). */
    val hapticsEnabled: Boolean = true,
    /** Remember last memory value across restarts (spec §4.6). */
    val memoryValue: String? = null,
    /** Serialized editable land profiles (see LandProfiles.serialize). */
    val landProfilesRaw: String? = null,
    /** Provenance metadata the user entered for the VAT tool (spec §11.5). */
    val vatSource: String? = null,
    val vatEffectiveDate: String? = null,
) {
    init {
        require(decimalPlaces in 0..12) { "decimalPlaces must be 0..12" }
    }
}
