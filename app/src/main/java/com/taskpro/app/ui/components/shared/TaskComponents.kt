package com.taskpro.app.ui.components.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import com.taskpro.app.R
import com.taskpro.app.data.Task
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.theme.*

@Composable
fun TaskStatusPresentation(status: TaskStatus, modifier: Modifier = Modifier, overdue: Boolean = false) {
    val p = LocalVaurivoColors.current
    val label = stringResource(when (status) {
        TaskStatus.PENDING -> R.string.status_pending
        TaskStatus.COMPLETED -> R.string.status_completed
        TaskStatus.SNOOZED -> R.string.status_snoozed
    })
    val color = when {
        status == TaskStatus.COMPLETED -> p.success
        status == TaskStatus.SNOOZED -> p.warning
        overdue -> p.error
        else -> p.textSecondary
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
        Icon(when (status) { TaskStatus.COMPLETED -> Icons.Default.CheckCircle; TaskStatus.SNOOZED -> Icons.Default.Snooze; else -> Icons.Default.RadioButtonUnchecked }, null,
            Modifier.size(VaurivoLayout.compactIconSize), tint = color)
        Text(if (overdue && status == TaskStatus.PENDING) "$label · ${stringResource(R.string.component_overdue)}" else label,
            style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(task: Task, onEdit: () -> Unit, onCompletionChange: (Boolean) -> Unit,
            modifier: Modifier = Modifier, metadata: String? = null, busy: Boolean = false,
            overdue: Boolean = false, dragging: Boolean = false, onSnooze: (() -> Unit)? = null,
            onStatus: (() -> Unit)? = null, onMenu: (() -> Unit)? = null,
            onMoveUp: (() -> Unit)? = null, onMoveDown: (() -> Unit)? = null,
            dragHandle: (@Composable () -> Unit)? = null,
            compact: Boolean = false, bodyEnabled: Boolean = true, dense: Boolean = false) {
    val p = LocalVaurivoColors.current
    val busyText = stringResource(R.string.component_busy)
    val dragText = stringResource(R.string.component_reordering)
    val editSource = remember { MutableInteractionSource() }
    val verticalPadding = if (compact && dense) VaurivoSpacing.extraSmall else VaurivoSpacing.small
    Column(modifier.fillMaxWidth().semantics {
        if (busy) stateDescription = busyText else if (dragging) stateDescription = dragText
    }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(VaurivoLayout.minimumTouchTarget).toggleable(task.status == TaskStatus.COMPLETED,
                enabled = !busy && !dragging, role = Role.Checkbox, onValueChange = onCompletionChange)
                .semantics { contentDescription = task.title }, contentAlignment = Alignment.Center) {
                Checkbox(task.status == TaskStatus.COMPLETED, null, enabled = !busy && !dragging,
                    colors = CheckboxDefaults.colors(checkedColor = p.tasksAccent, checkmarkColor = p.onAccent, uncheckedColor = p.outlineInteractive))
            }
            Column(Modifier.weight(1f).heightIn(min = if (compact) VaurivoLayout.minimumTouchTarget + verticalPadding * 2 else VaurivoLayout.minimumTouchTarget)
                .componentFocus(editSource, ModuleAccent.Tasks, VaurivoShapes.control)
                .clickable(enabled = bodyEnabled && !busy && !dragging, role = Role.Button, interactionSource = editSource,
                    indication = ripple(), onClick = onEdit)
                .padding(horizontal = VaurivoSpacing.small, vertical = verticalPadding), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                Text(task.title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                    textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null,
                    color = if (task.status == TaskStatus.COMPLETED) p.textSecondary else p.textPrimary)
                if (task.notes.isNotEmpty()) Text(task.notes, Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content), color = p.textSecondary,
                    maxLines = if (compact) 2 else Int.MAX_VALUE, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (metadata != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                    if (compact && task.dueAt != null) Icon(Icons.Default.Schedule, null, Modifier.size(VaurivoLayout.compactIconSize), tint = if (overdue) p.error else p.textTertiary)
                    Text(metadata, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content), color = if (compact && overdue) p.error else p.textTertiary)
                }
                if (!compact || task.status != TaskStatus.PENDING) TaskStatusPresentation(task.status, overdue = overdue)
            }
            if (compact && onMenu != null) IconAction(stringResource(R.string.component_menu_for, task.title), Icons.Default.MoreVert, onMenu, enabled = !busy && !dragging)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small)) {
            if (onSnooze != null) IconAction(stringResource(R.string.snooze), Icons.Default.Snooze, onSnooze, enabled = !busy && !dragging)
            if (onStatus != null) IconAction(stringResource(R.string.component_status), Icons.Default.Checklist, onStatus, enabled = !busy && !dragging)
            if (onMoveUp != null) IconAction(stringResource(R.string.vault_move_up), Icons.Default.KeyboardArrowUp, onMoveUp, enabled = !busy && !dragging)
            if (onMoveDown != null) IconAction(stringResource(R.string.vault_move_down), Icons.Default.KeyboardArrowDown, onMoveDown, enabled = !busy && !dragging)
            if (!compact && onMenu != null) IconAction(stringResource(R.string.component_menu_for, task.title), Icons.Default.MoreVert, onMenu, enabled = !busy && !dragging)
            dragHandle?.invoke()
        }
        HorizontalDivider(color = p.borderSubtle)
    }
}
