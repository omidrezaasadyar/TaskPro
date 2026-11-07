package com.taskpro.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.taskpro.app.BuildConfig
import com.taskpro.app.R
import com.taskpro.app.ui.components.shared.ModuleAccent
import com.taskpro.app.ui.components.shared.WorkspaceCard
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.theme.*
import com.taskpro.app.ui.AppViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun WorkspaceRoute(vaultLocked: Boolean, onSelect: (Screen) -> Unit,
    model: WorkspaceStatusViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val status by model.status.collectAsStateWithLifecycle()
    WorkspaceScreen(vaultLocked, status, onSelect = onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(vaultLocked: Boolean = true, status: WorkspaceStatus = WorkspaceStatus(),
    onSelect: (Screen) -> Unit) {
    val p = LocalVaurivoColors.current
    val animateDecorations = rememberWorkspaceMotionActive()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    // Compose duration scaling also makes this entrance instant when animations are disabled.
    val alpha by animateFloatAsState(if (entered) 1f else 0f,
        tween(VaurivoMotion.EntranceMillis, easing = VaurivoMotion.enterEasing), label = "chooserEntrance")
    BoxWithConstraints(Modifier.fillMaxSize().background(p.canvas).safeDrawingPadding()) {
        val short = maxHeight < 520.dp
        val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
        AmbientWaves(animateDecorations, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(start = VaurivoLayout.compactScreenMargin, end = VaurivoLayout.compactScreenMargin,
                top = VaurivoSpacing.large, bottom = VaurivoSpacing.display)
            .graphicsLayer { this.alpha = alpha }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                Icon(Icons.Default.VerifiedUser, null, Modifier.size(32.dp), tint = p.vaultAccent)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium,
                    color = p.textPrimary, modifier = Modifier.semantics { heading() })
            }
            Box(Modifier.fillMaxWidth().height(if (short) 144.dp else 208.dp), contentAlignment = Alignment.Center) {
                // The security symbol is non-directional; only navigation and text mirror.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ChooserSymbol(animateDecorations, Modifier.size(320.dp))
                }
            }
            ChooserHeadline()
            Spacer(Modifier.height(VaurivoSpacing.medium))
            Text(stringResource(R.string.chooser_description), Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            Spacer(Modifier.height(VaurivoSpacing.medium))
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
            Spacer(Modifier.height(VaurivoSpacing.small))
            WorkspaceStatusSection(status, stacked, onSelect)
        }
        Text(stringResource(R.string.chooser_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            Modifier.align(Alignment.BottomCenter).padding(bottom = VaurivoSpacing.small),
            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
            color = p.textTertiary)
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
private fun ChooserSymbol(animated: Boolean, modifier: Modifier) {
    val p = LocalVaurivoColors.current
    Box(modifier, contentAlignment = Alignment.Center) {
        HeroRings(animated, Modifier.matchParentSize())
        Box(Modifier.size(104.dp).background(p.vaultAccent, RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.VerifiedUser, null, Modifier.size(40.dp), tint = p.onAccent)
        }
    }
}

@Composable
private fun WorkspaceStatusSection(status: WorkspaceStatus, stacked: Boolean, onSelect: (Screen) -> Unit) {
    val p = LocalVaurivoColors.current
    val locale = LocalConfiguration.current.locales[0]
    val tasks = status.totalTasks?.let { NumberFormat.getIntegerInstance(locale).format(it) }
        ?: stringResource(R.string.chooser_loading)
    val vault = when {
        status.vaultAccounts != null -> NumberFormat.getIntegerInstance(locale).format(status.vaultAccounts)
        status.vaultExists == false -> NumberFormat.getIntegerInstance(locale).format(0)
        status.vaultExists == true -> stringResource(R.string.chooser_unlock_count)
        else -> stringResource(R.string.chooser_loading)
    }
    val vaultDetail = when (status.vaultExists) {
        true -> stringResource(R.string.chooser_encrypted)
        false -> stringResource(R.string.chooser_vault_setup)
        null -> null
    }
    val backup = status.lastBackupAt?.let { time ->
        remember(time, locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(time)) }
    } ?: if (status.totalTasks == null) stringResource(R.string.chooser_loading)
        else stringResource(R.string.chooser_backup_none)
    Text(stringResource(R.string.chooser_status), Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.labelMedium, color = p.textTertiary)
    Spacer(Modifier.height(VaurivoSpacing.small))
    @Composable fun taskCard(modifier: Modifier) {
        StatusCard(stringResource(R.string.chooser_total_tasks), tasks, null, ModuleAccent.Tasks,
            { onSelect(Screen.Home) }, modifier)
    }
    @Composable fun vaultCard(modifier: Modifier) {
        StatusCard(stringResource(R.string.chooser_vault_accounts), vault, vaultDetail, ModuleAccent.Vault,
            { onSelect(Screen.Vault) }, modifier)
    }
    if (stacked) Column(verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
        taskCard(Modifier.fillMaxWidth()); vaultCard(Modifier.fillMaxWidth())
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
        taskCard(Modifier.weight(1f)); vaultCard(Modifier.weight(1f))
    }
    Spacer(Modifier.height(VaurivoSpacing.small))
    StatusCard(stringResource(R.string.chooser_last_backup), backup, null, ModuleAccent.Tasks,
        null, Modifier.fillMaxWidth())
}

@Composable
private fun StatusCard(label: String, value: String, detail: String?, accent: ModuleAccent,
    onClick: (() -> Unit)?, modifier: Modifier) {
    val p = LocalVaurivoColors.current
    Surface(modifier.then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
        .heightIn(min = 76.dp), shape = VaurivoShapes.card, color = p.surface,
        border = BorderStroke(VaurivoStroke.border, p.borderSubtle)) {
        Column(Modifier.fillMaxWidth().padding(VaurivoLayout.densePadding),
            verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = p.textTertiary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = p.textPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall,
                color = if (accent == ModuleAccent.Vault) p.vaultAccent else p.tasksAccent)
        }
    }
}
