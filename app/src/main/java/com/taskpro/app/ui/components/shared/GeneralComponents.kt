package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.taskpro.app.R
import com.taskpro.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, count: String? = null,
                  actionLabel: String? = null, onAction: (() -> Unit)? = null, enabled: Boolean = true,
                  module: ModuleAccent = ModuleAccent.Tasks) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
        Text(listOfNotNull(title, count).joinToString(" · "), Modifier.align(Alignment.CenterVertically).semantics { heading() },
            style = MaterialTheme.typography.titleSmall, color = LocalVaurivoColors.current.textSecondary)
        if (actionLabel != null && onAction != null) TextButton(onAction, enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = module.color()),
            modifier = Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget)) { Text(actionLabel) }
    }
}

@Composable
fun PrimaryActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                        module: ModuleAccent = ModuleAccent.Tasks, icon: ImageVector? = null,
                        enabled: Boolean = true, busy: Boolean = false, unavailableReason: String? = null,
                        compact: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val p = LocalVaurivoColors.current
    val busyLabel = stringResource(R.string.component_busy)
    Column(modifier) {
        Button(onClick, enabled = enabled && !busy, interactionSource = source,
            modifier = (if (compact) Modifier else Modifier.fillMaxWidth()).heightIn(min = VaurivoLayout.primaryActionHeight)
                .componentFocus(source, module, VaurivoShapes.control).semantics { if (busy) stateDescription = busyLabel },
            shape = VaurivoShapes.control, contentPadding = PaddingValues(horizontal = VaurivoSpacing.extraLarge, vertical = VaurivoSpacing.medium),
            colors = ButtonDefaults.buttonColors(containerColor = module.color(), contentColor = p.onAccent,
                disabledContainerColor = p.disabledContainer, disabledContentColor = p.disabledContent)) {
            Box(Modifier.size(VaurivoLayout.iconSize), contentAlignment = Alignment.Center) {
                if (busy) Icon(Icons.Default.HourglassEmpty, null) else if (icon != null) Icon(icon, null)
            }
            Spacer(Modifier.width(VaurivoSpacing.small))
            Text(label, if (compact) Modifier else Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (!compact) Spacer(Modifier.width(VaurivoLayout.iconSize + VaurivoSpacing.small))
        }
        if (!enabled && !unavailableReason.isNullOrBlank()) Text(unavailableReason, style = MaterialTheme.typography.bodySmall,
            color = p.textSecondary, modifier = Modifier.fillMaxWidth().padding(top = VaurivoSpacing.extraSmall))
    }
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
                module: ModuleAccent = ModuleAccent.Tasks, enabled: Boolean = true, busy: Boolean = false,
                error: String? = null, scope: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
        OutlinedTextField(query, onQueryChange, enabled = enabled, label = { Text(label) },
            modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight),
            shape = VaurivoShapes.control, isError = error != null,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Content),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (query.isNotEmpty()) ({ IconAction(stringResource(R.string.component_clear_search), Icons.Default.Close,
                { onQueryChange("") }, enabled = enabled, module = module) }) else null,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = module.color(), focusedLabelColor = module.color(),
                unfocusedBorderColor = LocalVaurivoColors.current.outlineInteractive, cursorColor = module.color()))
        scope?.invoke()
        if (busy) LoadingState(stringResource(R.string.component_busy), module = module)
        if (error != null) ErrorState(error)
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
               module: ModuleAccent = ModuleAccent.Tasks, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val p = LocalVaurivoColors.current
    Surface(modifier = modifier.widthIn(min = VaurivoLayout.minimumTouchTarget).componentFocus(source, module, VaurivoShapes.control)
        .toggleable(selected, enabled = enabled, role = Role.Checkbox, interactionSource = source,
            indication = androidx.compose.material3.ripple(), onValueChange = { onClick() }),
        shape = VaurivoShapes.control, color = if (selected) module.container() else p.surface,
        contentColor = if (!enabled) p.disabledContent else p.textPrimary,
        border = BorderStroke(VaurivoStroke.border, if (selected) module.color() else p.outlineInteractive)) {
        Row(Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget).padding(VaurivoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            Box(Modifier.size(VaurivoLayout.compactIconSize)) {
                if (selected) Icon(Icons.Default.Check, null, Modifier.size(VaurivoLayout.compactIconSize), tint = module.color())
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector, modifier: Modifier = Modifier,
               actionLabel: String? = null, onAction: (() -> Unit)? = null, module: ModuleAccent = ModuleAccent.Tasks) {
    Column(modifier.widthIn(max = 360.dp).padding(VaurivoSpacing.section), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
        Icon(icon, null, tint = module.color())
        Text(title, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.titleMedium,
            color = LocalVaurivoColors.current.textPrimary)
        Text(message, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = LocalVaurivoColors.current.textSecondary)
        if (actionLabel != null && onAction != null) PrimaryActionButton(actionLabel, onAction, module = module)
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, retryLabel: String? = null, onRetry: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            Icon(Icons.Default.ErrorOutline, null, tint = LocalVaurivoColors.current.error)
            Text(message, Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite; error(message) },
                color = LocalVaurivoColors.current.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (retryLabel != null && onRetry != null) TextButton(onRetry, modifier = Modifier.heightIn(min = VaurivoLayout.minimumTouchTarget),
            colors = ButtonDefaults.textButtonColors(contentColor = LocalVaurivoColors.current.textPrimary)) { Text(retryLabel) }
    }
}

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier, progress: Float? = null, module: ModuleAccent = ModuleAccent.Tasks) {
    require(progress == null || progress in 0f..1f)
    Column(modifier.fillMaxWidth().heightIn(min = VaurivoLayout.minimumTouchTarget), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            Icon(Icons.Default.HourglassEmpty, null, tint = module.color())
            Text(label, Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.bodyMedium,
                color = LocalVaurivoColors.current.textSecondary)
        }
        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = module.color())
    }
}

@Composable
fun ConfirmationDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit,
                       onDismiss: () -> Unit, busy: Boolean = false, destructive: Boolean = false,
                       error: String? = null) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(
        usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.padding(VaurivoSpacing.large).widthIn(max = 560.dp).semantics { paneTitle = title }, shape = VaurivoShapes.card,
            color = LocalVaurivoColors.current.surfaceRaised, contentColor = LocalVaurivoColors.current.textPrimary) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(VaurivoSpacing.section),
                verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.large)) {
                Text(title, Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                Text(message, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium)
                if (error != null) ErrorState(error)
                TextButton(onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.minimumTouchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = LocalVaurivoColors.current.textSecondary)) {
                    Text(stringResource(R.string.cancel))
                }
                if (destructive) OutlinedButton(onConfirm, enabled = !busy, shape = VaurivoShapes.control,
                    modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight),
                    border = BorderStroke(VaurivoStroke.border, LocalVaurivoColors.current.error)) {
                    Text(confirmLabel, color = LocalVaurivoColors.current.error)
                } else PrimaryActionButton(confirmLabel, onConfirm, busy = busy)
                if (busy) LoadingState(stringResource(R.string.component_busy))
            }
        }
    }
}
