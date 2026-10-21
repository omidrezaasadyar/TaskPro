package com.taskpro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.taskpro.app.settings.ThemeMode

/**
 * Whether the *app* is currently in dark mode. Use this instead of
 * `isSystemInDarkTheme()` everywhere, so colours follow the user's in-app
 * theme choice (Settings → Theme) rather than the device setting.
 */
val LocalAppDarkTheme = staticCompositionLocalOf { false }

private fun VaurivoColors.materialScheme(dark: Boolean): ColorScheme =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = tasksAccent,
        onPrimary = onAccent,
        primaryContainer = tasksContainer,
        onPrimaryContainer = textPrimary,
        inversePrimary = if (dark) VaurivoLightColors.tasksAccent else VaurivoDarkColors.tasksAccent,
        secondary = vaultAccent,
        onSecondary = onAccent,
        secondaryContainer = vaultContainer,
        onSecondaryContainer = textPrimary,
        tertiary = success,
        onTertiary = onAccent,
        tertiaryContainer = vaultContainer,
        onTertiaryContainer = textPrimary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = if (dark) textPrimary else VaurivoDarkColors.surface,
        inverseOnSurface = if (dark) canvas else VaurivoDarkColors.textPrimary,
        error = error,
        onError = onAccent,
        errorContainer = surfaceRaised,
        onErrorContainer = error,
        outline = outlineInteractive,
        outlineVariant = borderSubtle,
        // Material components apply their own scrim opacity.
        scrim = Color.Black,
        surfaceBright = surfaceRaised,
        surfaceDim = canvas,
        surfaceContainerLowest = canvas,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised
    )

private val LightColors = VaurivoLightColors.materialScheme(dark = false)
private val DarkColors = VaurivoDarkColors.materialScheme(dark = true)

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

    CompositionLocalProvider(
        LocalAppDarkTheme provides darkTheme,
        LocalVaurivoColors provides if (darkTheme) VaurivoDarkColors else VaurivoLightColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = VaurivoShapes.material,
            content = content
        )
    }
}
