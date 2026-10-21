package com.shohan.omnicalc.core.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.common.AngleMode
import com.shohan.omnicalc.core.common.GroupingStyle
import com.shohan.omnicalc.core.common.LanguageMode
import com.shohan.omnicalc.core.common.NumeralStyle
import com.shohan.omnicalc.core.common.PercentMode
import com.shohan.omnicalc.core.common.PrecisionMode
import com.shohan.omnicalc.core.common.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "omnicalc_settings")

/**
 * DataStore-backed settings (spec §17). Unknown/corrupt values fall back to
 * [AppSettings] defaults — settings always load, never crash.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language_mode")
        val NUMERALS = stringPreferencesKey("numeral_style")
        val GROUPING = stringPreferencesKey("grouping_style")
        val PRECISION = stringPreferencesKey("precision_mode")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val ANGLE = stringPreferencesKey("angle_mode")
        val PERCENT = stringPreferencesKey("percent_mode")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val MEMORY = stringPreferencesKey("memory_value")
        val LAND_PROFILES = stringPreferencesKey("land_profiles")
        val VAT_SOURCE = stringPreferencesKey("vat_source")
        val VAT_EFFECTIVE = stringPreferencesKey("vat_effective")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME].toEnum(ThemeMode.values(), AppSettings().themeMode),
            languageMode = p[Keys.LANGUAGE].toEnum(LanguageMode.values(), AppSettings().languageMode),
            numeralStyle = p[Keys.NUMERALS].toEnum(NumeralStyle.values(), AppSettings().numeralStyle),
            groupingStyle = p[Keys.GROUPING].toEnum(GroupingStyle.values(), AppSettings().groupingStyle),
            precisionMode = p[Keys.PRECISION].toEnum(PrecisionMode.values(), AppSettings().precisionMode),
            decimalPlaces = (p[Keys.DECIMAL_PLACES] ?: AppSettings().decimalPlaces).coerceIn(0, 12),
            angleMode = p[Keys.ANGLE].toEnum(AngleMode.values(), AppSettings().angleMode),
            percentMode = p[Keys.PERCENT].toEnum(PercentMode.values(), AppSettings().percentMode),
            hapticsEnabled = p[Keys.HAPTICS] ?: AppSettings().hapticsEnabled,
            memoryValue = p[Keys.MEMORY],
            landProfilesRaw = p[Keys.LAND_PROFILES],
            vatSource = p[Keys.VAT_SOURCE],
            vatEffectiveDate = p[Keys.VAT_EFFECTIVE],
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME] = value.name }
    suspend fun setLanguageMode(value: LanguageMode) = edit { it[Keys.LANGUAGE] = value.name }
    suspend fun setNumeralStyle(value: NumeralStyle) = edit { it[Keys.NUMERALS] = value.name }
    suspend fun setGroupingStyle(value: GroupingStyle) = edit { it[Keys.GROUPING] = value.name }
    suspend fun setPrecisionMode(value: PrecisionMode) = edit { it[Keys.PRECISION] = value.name }
    suspend fun setDecimalPlaces(value: Int) = edit { it[Keys.DECIMAL_PLACES] = value.coerceIn(0, 12) }
    suspend fun setAngleMode(value: AngleMode) = edit { it[Keys.ANGLE] = value.name }
    suspend fun setPercentMode(value: PercentMode) = edit { it[Keys.PERCENT] = value.name }
    suspend fun setHapticsEnabled(value: Boolean) = edit { it[Keys.HAPTICS] = value }
    suspend fun setMemoryValue(value: String?) = edit { p ->
        if (value == null) p.remove(Keys.MEMORY) else p[Keys.MEMORY] = value
    }

    suspend fun setLandProfiles(value: String) = edit { it[Keys.LAND_PROFILES] = value }
    suspend fun setVatSource(value: String?) = edit { p ->
        if (value.isNullOrBlank()) p.remove(Keys.VAT_SOURCE) else p[Keys.VAT_SOURCE] = value
    }
    suspend fun setVatEffectiveDate(value: String?) = edit { p ->
        if (value.isNullOrBlank()) p.remove(Keys.VAT_EFFECTIVE) else p[Keys.VAT_EFFECTIVE] = value
    }

    /** Reset every preference to defaults (with confirmation in the UI). */
    suspend fun resetAll() = edit { it.clear() }

    private suspend fun edit(action: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(action)
    }

    private inline fun <reified T : Enum<T>> String?.toEnum(values: Array<T>, default: T): T =
        this?.let { name -> values.firstOrNull { it.name == name } } ?: default
}
