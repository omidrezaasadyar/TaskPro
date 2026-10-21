package com.taskpro.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun vaultAccent(): Color = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xff8dd9c0) else Color(0xff16745b)

@Composable
fun WorkspaceBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val colors = if (dark) listOf(Color(0xff102b28), Color(0xff181d20), Color(0xff242524))
        else listOf(Color(0xffe3f3ed), Color(0xfff7f9fb), Color(0xffedf0f4))
    Box(modifier.background(Brush.linearGradient(colors))) {
        Canvas(Modifier.matchParentSize()) {
            val ink = if (dark) Color(0xff9acabc).copy(alpha = 0.055f) else Color(0xff357a68).copy(alpha = 0.045f)
            val step = 64.dp.toPx()
            var x = -size.height
            while (x < size.width) {
                drawLine(ink, Offset(x, 0f), Offset(x + size.height, size.height), 1.dp.toPx())
                x += step
            }
            drawLine(ink, Offset(0f, size.height * 0.24f), Offset(size.width, size.height * 0.24f), 1.dp.toPx())
            drawLine(ink, Offset(0f, size.height * 0.78f), Offset(size.width, size.height * 0.78f), 1.dp.toPx())
        }
        content()
    }
}
