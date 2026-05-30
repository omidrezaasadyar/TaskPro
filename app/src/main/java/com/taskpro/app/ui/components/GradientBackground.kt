package com.taskpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme

/**
 * A soft, meaningful background used behind every screen. It blends two faint
 * brand-tinted radial glows over the theme surface so screens feel polished and
 * have depth without distracting from the content.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.background

    val topGlow = primary.copy(alpha = if (dark) 0.16f else 0.10f)
    val bottomGlow = secondary.copy(alpha = if (dark) 0.14f else 0.08f)

    val brush = Brush.linearGradient(
        colors = listOf(topGlow, surface, surface, bottomGlow),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .background(brush)
    ) {
        content()
    }
}
