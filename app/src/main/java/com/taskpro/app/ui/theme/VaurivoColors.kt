package com.taskpro.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VaurivoColors(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val borderSubtle: Color,
    val outlineInteractive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val tasksAccent: Color,
    val tasksContainer: Color,
    val vaultAccent: Color,
    val vaultContainer: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val focusTasks: Color,
    val focusVault: Color,
    val scrim: Color
)

val VaurivoDarkColors = VaurivoColors(
    canvas = Color(0xFF0C1110),
    surface = Color(0xFF131B19),
    surfaceRaised = Color(0xFF1B2522),
    borderSubtle = Color(0xFF26322E),
    outlineInteractive = Color(0xFF71827A),
    textPrimary = Color(0xFFE7EEEC),
    textSecondary = Color(0xFFA5B3AC),
    textTertiary = Color(0xFF8C9D94),
    disabledContainer = Color(0xFF1A211E),
    disabledContent = Color(0xFF708079),
    tasksAccent = Color(0xFFADB8FF),
    tasksContainer = Color(0xFF24283B),
    vaultAccent = Color(0xFF5EDFB2),
    vaultContainer = Color(0xFF19362C),
    onAccent = Color(0xFF0C1110),
    success = Color(0xFF5EDFB2),
    warning = Color(0xFFF0C06B),
    error = Color(0xFFFF8A7A),
    focusTasks = Color(0xFFC7CEFF),
    focusVault = Color(0xFF8CEBC9),
    scrim = Color.Black.copy(alpha = 0.56f)
)

val VaurivoLightColors = VaurivoColors(
    canvas = Color(0xFFF4F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEAF0EC),
    borderSubtle = Color(0xFFD2DCD6),
    outlineInteractive = Color(0xFF718279),
    textPrimary = Color(0xFF17231D),
    textSecondary = Color(0xFF4D6256),
    textTertiary = Color(0xFF596D61),
    disabledContainer = Color(0xFFE4EBE6),
    disabledContent = Color(0xFF77857C),
    tasksAccent = Color(0xFF4654A6),
    tasksContainer = Color(0xFFE8ECFF),
    vaultAccent = Color(0xFF087556),
    vaultContainer = Color(0xFFDDF3E9),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF147346),
    warning = Color(0xFF8A5700),
    error = Color(0xFFB33C32),
    focusTasks = Color(0xFF4654A6),
    focusVault = Color(0xFF087556),
    scrim = Color.Black.copy(alpha = 0.32f)
)

val LocalVaurivoColors = staticCompositionLocalOf { VaurivoLightColors }
