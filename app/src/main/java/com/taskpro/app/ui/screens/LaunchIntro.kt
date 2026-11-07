package com.taskpro.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.ui.theme.VaurivoSpacing
import kotlinx.coroutines.delay

@Composable
fun LaunchIntro(onFinished: () -> Unit, animate: Boolean = true) {
    val p = LocalVaurivoColors.current
    var stage by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    fun finish() { if (!finished) { finished = true; onFinished() } }
    LaunchedEffect(animate) {
        if (!animate) { finish(); return@LaunchedEffect }
        delay(350); stage = 1
        delay(400); stage = 2
        delay(450); stage = 3
        delay(400); finish()
    }
    val scale by animateFloatAsState(if (stage == 2) 1.07f else 1f, tween(180), label = "introSymbolScale")
    Box(Modifier.fillMaxSize().background(p.canvas)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClick = ::finish)
        .safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.section)) {
            Box(Modifier.size(236.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    listOf(0.95f, 0.72f, 0.49f).forEach { fraction ->
                        drawCircle(p.outlineInteractive.copy(alpha = 0.15f), radius * fraction,
                            style = Stroke(1.dp.toPx()))
                    }
                }
                Box(Modifier.size(104.dp).scale(scale)
                    .background(if (stage < 2) p.surfaceRaised else p.vaultAccent, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center) {
                    Crossfade(targetState = stage >= 2, animationSpec = tween(180), label = "introIcon") { shield ->
                        if (shield) Icon(Icons.Default.VerifiedUser, null, Modifier.size(48.dp), tint = p.onAccent)
                        else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(2) { index ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(if (index == 0 || stage >= 1) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null, Modifier.size(18.dp), tint = p.tasksAccent)
                                    Box(Modifier.width(34.dp).height(3.dp).background(p.tasksAccent.copy(alpha = if (index == 0 || stage >= 1) 1f else 0.4f)))
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(stage >= 3, enter = fadeIn(tween(180))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                        color = p.textPrimary)
                    Text(stringResource(R.string.intro_tagline), style = MaterialTheme.typography.titleMedium,
                        color = p.textSecondary, textAlign = TextAlign.Center)
                }
            }
        }
        if (stage < 3) Text(stringResource(R.string.intro_skip), Modifier.align(Alignment.BottomCenter)
            .padding(bottom = VaurivoSpacing.section), style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
    }
}
