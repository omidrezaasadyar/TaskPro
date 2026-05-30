package com.taskpro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.taskpro.app.R

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

private fun base(weight: FontWeight) =
    TextStyle(fontFamily = VazirFontFamily, fontWeight = weight)

// Material 3 typography re-based on Vazirmatn.
val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.merge(base(FontWeight.Normal)),
        displayMedium = displayMedium.merge(base(FontWeight.Normal)),
        displaySmall = displaySmall.merge(base(FontWeight.Normal)),
        headlineLarge = headlineLarge.merge(base(FontWeight.Bold)),
        headlineMedium = headlineMedium.merge(base(FontWeight.Bold)),
        headlineSmall = headlineSmall.merge(base(FontWeight.Bold)),
        titleLarge = titleLarge.merge(base(FontWeight.Bold)),
        titleMedium = titleMedium.merge(base(FontWeight.Medium)),
        titleSmall = titleSmall.merge(base(FontWeight.Medium)),
        bodyLarge = bodyLarge.merge(base(FontWeight.Normal)),
        bodyMedium = bodyMedium.merge(base(FontWeight.Normal)),
        bodySmall = bodySmall.merge(base(FontWeight.Normal)),
        labelLarge = labelLarge.merge(base(FontWeight.Medium)),
        labelMedium = labelMedium.merge(base(FontWeight.Medium)),
        labelSmall = labelSmall.merge(base(FontWeight.Medium)),
    )
}
