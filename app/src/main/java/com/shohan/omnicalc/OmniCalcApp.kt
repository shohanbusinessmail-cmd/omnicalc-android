package com.shohan.omnicalc

import android.app.Application
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.database.FavoritesRepository
import com.shohan.omnicalc.core.database.HistoryRepository
import com.shohan.omnicalc.core.database.OmniCalcDatabase
import com.shohan.omnicalc.core.database.SettingsRepository

/**
 * Application entry point + manual dependency container (spec §3.4: DI only
 * where it earns its complexity). Implements [AppServices] so feature
 * ViewModels can recover their dependencies from CreationExtras.
 */
class OmniCalcApp : Application(), AppServices {

    override val historyRepository: HistoryRepository by lazy {
        HistoryRepository(OmniCalcDatabase.get(this).historyDao())
    }

    override val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepository(OmniCalcDatabase.get(this).favoritesDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }
}
