package com.taskpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.taskpro.app.settings.BackgroundStyle

/** The chosen whole-app background style, provided from MainActivity. */
val LocalBackgroundStyle = staticCompositionLocalOf { BackgroundStyle.GRADIENT }

/** Provides [LocalBackgroundStyle] to the tree. */
@Composable
fun ProvideBackgroundStyle(style: BackgroundStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBackgroundStyle provides style, content = content)
}

/**
 * A meaningful background drawn behind every screen. The style is user-selectable
 * (Settings → Background): a plain surface, a soft brand gradient, or a more
 * colourful aurora gradient. Item/task content sits on top.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val style = LocalBackgroundStyle.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.background

    val box = Modifier
        .fillMaxSize()
        .background(surface)

    val withOverlay = when (style) {
        BackgroundStyle.PLAIN -> box
        BackgroundStyle.GRADIENT -> box.background(
            Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.12f),
                    surface,
                    surface,
                    secondary.copy(alpha = 0.10f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
        BackgroundStyle.AURORA -> box.background(
            Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.24f),
                    secondary.copy(alpha = 0.16f),
                    surface,
                    primary.copy(alpha = 0.14f),
                    secondary.copy(alpha = 0.22f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    }

    Box(modifier = modifier.then(withOverlay)) {
        content()
    }
}
