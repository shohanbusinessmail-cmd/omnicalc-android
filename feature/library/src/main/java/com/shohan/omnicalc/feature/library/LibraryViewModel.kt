package com.shohan.omnicalc.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.database.FavoriteEntity
import com.shohan.omnicalc.core.database.HistoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Library destination: history with search/clear/delete/reuse,
 * favorites, and every persisted preference (spec §15, §17).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val services: AppServices,
) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = query
        .flatMapLatest { q ->
            if (q.isBlank()) services.historyRepository.observeAll()
            else services.historyRepository.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> =
        services.favoritesRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Payloads of calculations currently favorited (for star toggling). */
    val favoritePayloads: StateFlow<Set<String>> = favorites
        .map { list -> list.filter { it.type == TYPE_CALCULATION }.map { it.payload }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val settings: StateFlow<AppSettings> = services.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun deleteHistory(entry: HistoryEntity) = viewModelScope.launch {
        services.historyRepository.delete(entry)
    }

    fun clearHistory() = viewModelScope.launch {
        services.historyRepository.clearAll()
    }

    fun toggleCalculationFavorite(entry: HistoryEntity) = viewModelScope.launch {
        val isFav = services.favoritesRepository.isFavorite(TYPE_CALCULATION, entry.expression)
        if (isFav) {
            services.favoritesRepository.removeByPayload(TYPE_CALCULATION, entry.expression)
        } else {
            services.favoritesRepository.add(
                type = TYPE_CALCULATION,
                title = entry.expression,
                payload = entry.expression,
                subtitle = entry.resultPlain,
            )
        }
    }

    fun removeFavorite(id: Long) = viewModelScope.launch {
        services.favoritesRepository.remove(id)
    }

    // ---- settings (spec §17) ----
    fun setThemeMode(v: com.shohan.omnicalc.core.common.ThemeMode) = launch { services.settingsRepository.setThemeMode(v) }
    fun setLanguageMode(v: com.shohan.omnicalc.core.common.LanguageMode) = launch { services.settingsRepository.setLanguageMode(v) }
    fun setNumeralStyle(v: com.shohan.omnicalc.core.common.NumeralStyle) = launch { services.settingsRepository.setNumeralStyle(v) }
    fun setGroupingStyle(v: com.shohan.omnicalc.core.common.GroupingStyle) = launch { services.settingsRepository.setGroupingStyle(v) }
    fun setPrecisionMode(v: com.shohan.omnicalc.core.common.PrecisionMode) = launch { services.settingsRepository.setPrecisionMode(v) }
    fun setDecimalPlaces(v: Int) = launch { services.settingsRepository.setDecimalPlaces(v) }
    fun setAngleMode(v: com.shohan.omnicalc.core.common.AngleMode) = launch { services.settingsRepository.setAngleMode(v) }
    fun setPercentMode(v: com.shohan.omnicalc.core.common.PercentMode) = launch { services.settingsRepository.setPercentMode(v) }
    fun setHaptics(v: Boolean) = launch { services.settingsRepository.setHapticsEnabled(v) }
    fun resetPreferences() = launch { services.settingsRepository.resetAll() }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }

    companion object {
        const val TYPE_CALCULATION = "calculation"

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AppServices
                LibraryViewModel(app)
            }
        }
    }
}
