package com.shohan.omnicalc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.common.LanguageMode
import com.shohan.omnicalc.core.database.AppServices
import com.shohan.omnicalc.core.designsystem.OmniCalcTheme
import com.shohan.omnicalc.ui.OmniCalcRoot

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val services = application as AppServices

        setContent {
            // Settings drive theme + language (spec §2.2, §17, §8.1).
            val settings by services.settingsRepository.settings
                .collectAsState(initial = AppSettings())

            LaunchedEffect(settings.languageMode) {
                applyLocales(settings.languageMode)
            }

            OmniCalcTheme(themeMode = settings.themeMode) {
                OmniCalcRoot(settings = settings)
            }
        }
    }

    /**
     * Single path for per-app language: our DataStore is the source of truth
     * and AppCompat applies it (works on API 24+ via autoStoreLocales;
     * native on API 33+).
     */
    private fun applyLocales(mode: LanguageMode) {
        val target = when (mode) {
            LanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            LanguageMode.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            LanguageMode.BENGALIC -> LocaleListCompat.forLanguageTags("bn")
        }
        if (AppCompatDelegate.getApplicationLocales() != target) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }
}
