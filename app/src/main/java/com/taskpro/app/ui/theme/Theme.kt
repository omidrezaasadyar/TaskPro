package com.taskpro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.taskpro.app.settings.ThemeMode

/**
 * Whether the *app* is currently in dark mode. Use this instead of
 * `isSystemInDarkTheme()` everywhere, so colours follow the user's in-app
 * theme choice (Settings → Theme) rather than the device setting.
 */
val LocalAppDarkTheme = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Indigo,
    secondary = Teal,
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    secondary = Teal,
)

@Composable
fun TaskProTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Fixed brand colour scheme (no Material You dynamic colour) so the look is
    // consistent and we never inherit an off-brand surface tint from the wallpaper.
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
