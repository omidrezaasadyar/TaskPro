package com.taskpro.app.ui.screens.reminder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.ui.theme.LocalAppDarkTheme
import com.taskpro.app.util.DateFormat

@Composable
fun ReminderPopupScreen(
    title: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val time = remember { DateFormat.formatTime(System.currentTimeMillis()) }
    val dark = LocalAppDarkTheme.current
    val doneColor = if (dark) Color(0xFF9DDEB4) else Color(0xFF24623C)
    val onDoneColor = if (dark) Color(0xFF123820) else Color.White

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val viewportHeight = maxHeight
            val compact = maxHeight < 600.dp
            // Scroll the entire surface so large text and landscape never hide an action.
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth()
                        .heightIn(min = viewportHeight)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.reminder_default_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
                    ReminderBell(compact)
                    Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
                    Text(
                        stringResource(R.string.reminder_popup_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                time,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    ReminderAction(Icons.Default.Check, stringResource(R.string.reminder_done)) { source, modifier, content ->
                        Button(
                            onClick = onDone, interactionSource = source, modifier = modifier,
                            shape = RoundedCornerShape(8.dp), contentPadding = ActionPadding,
                            colors = ButtonDefaults.buttonColors(containerColor = doneColor, contentColor = onDoneColor),
                            content = content
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ReminderAction(Icons.Default.Snooze, stringResource(R.string.reminder_snooze_10)) { source, modifier, content ->
                        OutlinedButton(
                            onClick = onSnooze, interactionSource = source, modifier = modifier,
                            shape = RoundedCornerShape(8.dp), contentPadding = ActionPadding,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            content = content
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    ReminderAction(Icons.Default.Close, stringResource(R.string.reminder_dismiss)) { source, modifier, content ->
                        TextButton(
                            onClick = onDismiss, interactionSource = source, modifier = modifier,
                            shape = RoundedCornerShape(8.dp), contentPadding = ActionPadding,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            content = content
                        )
                    }
                }
            }
        }
    }
}

private val ActionPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)

@Composable
private fun ReminderAction(
    icon: ImageVector,
    label: String,
    button: @Composable (MutableInteractionSource, Modifier, @Composable RowScope.() -> Unit) -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "buttonPress")
    button(
        interactions,
        Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        // Multiline text gets its own width instead of competing with the icon.
        Text(
            label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun ReminderBell(compact: Boolean) {
    val transition = rememberInfiniteTransition(label = "reminder")
    val ripple = transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "ripple"
    )
    val swing = transition.animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes {
            durationMillis = 2600
            0f at 0 using FastOutSlowInEasing
            -8f at 180 using FastOutSlowInEasing
            6f at 380 using FastOutSlowInEasing
            -3f at 580 using FastOutSlowInEasing
            0f at 800
            0f at 2600
        }), label = "bellSwing"
    )
    val accent = if (LocalAppDarkTheme.current) Color(0xFFF0CA78) else Color(0xFF946000)
    val container = if (LocalAppDarkTheme.current) Color(0xFF393124) else Color(0xFFFFF0CF)
    Box(
        Modifier.size(if (compact) 112.dp else 144.dp).drawBehind {
            repeat(2) { index ->
                val phase = (ripple.value + index * 0.5f) % 1f
                val opacity = kotlin.math.sin(phase * Math.PI).toFloat() * 0.24f
                drawCircle(
                    accent.copy(alpha = opacity),
                    radius = size.minDimension * (0.29f + phase * 0.20f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }, contentAlignment = Alignment.Center
    ) {
        Surface(shape = CircleShape, color = container, modifier = Modifier.size(if (compact) 72.dp else 88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.NotificationsActive, contentDescription = null, tint = accent,
                    modifier = Modifier.size(36.dp).graphicsLayer {
                        rotationZ = swing.value
                        transformOrigin = TransformOrigin(0.5f, 0.2f)
                    }
                )
            }
        }
    }
}
