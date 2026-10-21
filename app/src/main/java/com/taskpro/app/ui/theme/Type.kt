package com.taskpro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.taskpro.app.R
import androidx.compose.ui.unit.sp

/**
 * App typography built on **Vazirmatn**, bundled under `res/font/`. Vazirmatn
 * has full Persian/Arabic-script glyphs, so both Latin and Persian (RTL) text
 * render with one consistent family throughout the app.
 */
val VazirFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = VazirFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val AppTypography = Typography(
    displayLarge = textStyle(28, 36, FontWeight.Bold),
    displayMedium = textStyle(28, 36, FontWeight.Bold),
    displaySmall = textStyle(28, 36, FontWeight.Bold),
    headlineLarge = textStyle(24, 32, FontWeight.Bold),
    headlineMedium = textStyle(24, 32, FontWeight.Bold),
    headlineSmall = textStyle(20, 28, FontWeight.Bold),
    titleLarge = textStyle(20, 28, FontWeight.Bold),
    titleMedium = textStyle(16, 24, FontWeight.Medium),
    titleSmall = textStyle(14, 20, FontWeight.Medium),
    bodyLarge = textStyle(16, 24, FontWeight.Normal),
    bodyMedium = textStyle(14, 22, FontWeight.Normal),
    bodySmall = textStyle(12, 18, FontWeight.Normal),
    labelLarge = textStyle(14, 20, FontWeight.Medium),
    labelMedium = textStyle(12, 18, FontWeight.Medium),
    labelSmall = textStyle(12, 18, FontWeight.Medium)
)
