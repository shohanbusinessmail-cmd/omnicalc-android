package com.shohan.omnicalc.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shohan.omnicalc.core.common.ThemeMode

// ---------------------------------------------------------------------------
// OmniCalc design tokens (spec §2.1). Values validated for WCAG contrast:
//  - onPrimary #FFFFFF on #326CF4  → ~4.9:1 (AA for normal text)  ✓
//  - textMain  #172033 on #FFFFFF  → ~15.6:1 (AAA)               ✓
//  - secondary #64748B on #FFFFFF  → ~4.8:1 (AA)                 ✓
// Dark tokens likewise checked against #101625-family surfaces.
// ---------------------------------------------------------------------------

// Light theme tokens
val OmniPrimary = Color(0xFF326CF4)
val OmniPrimaryContainer = Color(0xFFE8EEFF)
val OmniBackground = Color(0xFFF4F6FA)
val OmniSurface = Color(0xFFFFFFFF)
val OmniTextMain = Color(0xFF172033)
val OmniTextSecondary = Color(0xFF64748B)
val OmniBorder = Color(0xFFE4E8F0)
val OmniErrorBackground = Color(0xFFFCE9E9)
val OmniErrorText = Color(0xFFB42318)

// Derived light-theme roles
val OmniOnPrimary = Color(0xFFFFFFFF)
val OmniOnSurfaceVariant = Color(0xFF475569)
val OmniSuccess = Color(0xFF157F3D)
val OmniSuccessContainer = Color(0xFFE3F6EA)
val OmniWarning = Color(0xFF9A6700)

// Dark theme tokens (contrast-checked against #101625 surfaces)
val OmniDarkBackground = Color(0xFF101625)
val OmniDarkSurface = Color(0xFF171E30)
val OmniDarkSurfaceElevated = Color(0xFF1F2740)
val OmniDarkPrimary = Color(0xFF7BA0FF)
val OmniDarkPrimaryContainer = Color(0xFF25386A)
val OmniDarkTextMain = Color(0xFFE9EEF7)
val OmniDarkTextSecondary = Color(0xFF9AA7BD)
val OmniDarkBorder = Color(0xFF2A3350)
val OmniDarkError = Color(0xFFF98A80)
val OmniDarkErrorContainer = Color(0xFF5A1F1B)

private val LightColors = lightColorScheme(
    primary = OmniPrimary,
    onPrimary = OmniOnPrimary,
    primaryContainer = OmniPrimaryContainer,
    onPrimaryContainer = OmniTextMain,
    background = OmniBackground,
    onBackground = OmniTextMain,
    surface = OmniSurface,
    onSurface = OmniTextMain,
    onSurfaceVariant = OmniOnSurfaceVariant,
    surfaceVariant = OmniBackground,
    outline = OmniBorder,
    outlineVariant = OmniBorder,
    error = OmniErrorText,
    onError = Color(0xFFFFFFFF),
    errorContainer = OmniErrorBackground,
    onErrorContainer = OmniErrorText,
    secondary = OmniTextSecondary,
    onSecondary = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = OmniDarkPrimary,
    onPrimary = Color(0xFF0B1740),
    primaryContainer = OmniDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFDCE6FF),
    background = OmniDarkBackground,
    onBackground = OmniDarkTextMain,
    surface = OmniDarkSurface,
    onSurface = OmniDarkTextMain,
    onSurfaceVariant = OmniDarkTextSecondary,
    surfaceVariant = OmniDarkSurfaceElevated,
    outline = OmniDarkBorder,
    outlineVariant = OmniDarkBorder,
    error = OmniDarkError,
    onError = Color(0xFF3B0A08),
    errorContainer = OmniDarkErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    secondary = OmniDarkTextSecondary,
    onSecondary = Color(0xFF101625),
)

/**
 * OmniCalc theme. Follows the system by default; the persisted
 * [ThemeMode] is passed by the app once settings are wired.
 */
@Composable
fun OmniCalcTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
