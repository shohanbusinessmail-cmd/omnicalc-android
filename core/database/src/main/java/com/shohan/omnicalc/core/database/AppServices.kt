package com.shohan.omnicalc.core.database

/**
 * Application service surface (manual DI, spec §3.4 — "DI only when it adds
 * real value"). The Application class implements this interface; feature
 * ViewModels recover it from CreationExtras (APPLICATION_KEY).
 */
interface AppServices {
    val historyRepository: HistoryRepository
    val favoritesRepository: FavoritesRepository
    val settingsRepository: SettingsRepository
}
