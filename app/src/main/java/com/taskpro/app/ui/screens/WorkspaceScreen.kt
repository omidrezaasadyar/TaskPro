package com.taskpro.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.taskpro.app.BuildConfig
import com.taskpro.app.R
import com.taskpro.app.ui.components.shared.ModuleAccent
import com.taskpro.app.ui.components.shared.WorkspaceCard
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(vaultLocked: Boolean = true, onSelect: (Screen) -> Unit) {
    val p = LocalVaurivoColors.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    // Compose duration scaling also makes this entrance instant when animations are disabled.
    val alpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(VaurivoMotion.EntranceMillis, easing = VaurivoMotion.enterEasing), label = "chooserEntrance")
    BoxWithConstraints(Modifier.fillMaxSize().background(p.canvas).safeDrawingPadding()) {
        val short = maxHeight < 520.dp
        val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
            .padding(horizontal = VaurivoLayout.compactScreenMargin, vertical = VaurivoSpacing.large)
            .graphicsLayer { this.alpha = alpha }) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    Icon(Icons.Default.VerifiedUser, null, Modifier.size(32.dp), tint = p.vaultAccent)
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium,
                        color = p.textPrimary, modifier = Modifier.semantics { heading() })
                }
                Text(stringResource(R.string.chooser_version, BuildConfig.VERSION_NAME),
                    Modifier.border(VaurivoStroke.border, p.borderSubtle, VaurivoShapes.utility)
                        .padding(horizontal = VaurivoSpacing.small, vertical = VaurivoSpacing.extraSmall),
                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr), color = p.textSecondary)
            }
            Box(Modifier.fillMaxWidth().height(if (short) 144.dp else 264.dp), contentAlignment = Alignment.Center) {
                // The security symbol is non-directional; only navigation and text mirror.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ChooserSymbol(Modifier.size(if (short) 136.dp else 248.dp))
                }
            }
            ChooserHeadline()
            Spacer(Modifier.height(VaurivoSpacing.medium))
            Text(stringResource(R.string.chooser_description), Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            Spacer(Modifier.height(VaurivoSpacing.extraLarge))
            @Composable fun tasks(modifier: Modifier) {
                WorkspaceCard(stringResource(R.string.workspace_tasks), stringResource(R.string.chooser_tasks_subtitle),
                    Icons.Default.Checklist, ModuleAccent.Tasks, { onSelect(Screen.Home) },
                    modifier.border(VaurivoStroke.border, p.tasksAccent.copy(alpha = 0.4f), VaurivoShapes.card))
            }
            @Composable fun vault(modifier: Modifier) {
                WorkspaceCard(stringResource(R.string.vault_title), stringResource(R.string.chooser_vault_subtitle),
                    Icons.Default.EnhancedEncryption, ModuleAccent.Vault, { onSelect(Screen.Vault) },
                    modifier.border(VaurivoStroke.border, p.vaultAccent.copy(alpha = 0.4f), VaurivoShapes.card), locked = vaultLocked)
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    tasks(Modifier.fillMaxWidth())
                    vault(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    tasks(Modifier.weight(1f).fillMaxHeight())
                    vault(Modifier.weight(1f).fillMaxHeight())
                }
            }
            Spacer(Modifier.height(VaurivoSpacing.medium))
            Text(stringResource(R.string.chooser_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
                color = p.textTertiary)
        }
    }
}

@Composable
private fun ChooserHeadline() {
    val p = LocalVaurivoColors.current
    Text(buildAnnotatedString {
        append(stringResource(R.string.chooser_day))
        withStyle(SpanStyle(color = p.tasksAccent)) {
            append(stringResource(R.string.chooser_organized))
        }
        append('\n')
        append(stringResource(R.string.chooser_logins))
        withStyle(SpanStyle(color = p.vaultAccent)) {
            append(stringResource(R.string.chooser_secured))
        }
    }, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.displaySmall, color = p.textPrimary)
}

@Composable
private fun ChooserSymbol(modifier: Modifier) {
    val p = LocalVaurivoColors.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2
            listOf(0.97f, 0.74f, 0.50f).forEach { fraction ->
                drawCircle(p.outlineInteractive.copy(alpha = 0.15f), radius * fraction, style = Stroke(1.dp.toPx()))
            }
            val step = 18.dp.toPx()
            var x = step / 2
            while (x < size.width) {
                var y = step / 2
                while (y < size.height) {
                    drawCircle(p.textTertiary.copy(alpha = 0.10f), 0.7.dp.toPx(), Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        Box(Modifier.size(104.dp).background(p.vaultAccent, RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.VerifiedUser, null, Modifier.size(40.dp), tint = p.onAccent)
        }
    }
}
