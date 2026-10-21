package com.taskpro.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.data.*
import com.taskpro.app.ui.components.shared.TaskRow

@Composable
fun ManagedTaskRow(task: Task, metadata: String?, onEdit: (() -> Unit)?,
    onStatus: (TaskStatus) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier,
    overdue: Boolean = false, busy: Boolean = false, dragging: Boolean = false,
    onMoveUp: (() -> Unit)? = null, onMoveDown: (() -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null, dense: Boolean = false) {
    var menu by remember(task.id) { mutableStateOf(false) }
    var delete by remember(task.id) { mutableStateOf(false) }
    Box(modifier) {
        TaskRow(task, onEdit = { onEdit?.invoke() },
            onCompletionChange = { onStatus(if (it) TaskStatus.COMPLETED else TaskStatus.PENDING) },
            metadata = metadata, overdue = overdue, busy = busy, dragging = dragging,
            onMenu = { menu = true }, compact = true, bodyEnabled = onEdit != null, dragHandle = dragHandle, dense = dense)
        DropdownMenu(menu && !dragging, { menu = false }) {
            fun action(status: TaskStatus) { menu = false; onStatus(status) }
            if (task.status != TaskStatus.COMPLETED) DropdownMenuItem(
                text = { Text(stringResource(R.string.mark_complete)) }, onClick = { action(TaskStatus.COMPLETED) }, enabled = !busy)
            if (task.status != TaskStatus.PENDING) DropdownMenuItem(
                text = { Text(stringResource(R.string.reopen)) }, onClick = { action(TaskStatus.PENDING) }, enabled = !busy)
            if (task.status == TaskStatus.PENDING) DropdownMenuItem(
                text = { Text(stringResource(R.string.snooze)) }, onClick = { action(TaskStatus.SNOOZED) }, enabled = !busy)
            if (onMoveUp != null) DropdownMenuItem(text = { Text(stringResource(R.string.vault_move_up)) },
                onClick = { menu = false; onMoveUp() }, enabled = !busy)
            if (onMoveDown != null) DropdownMenuItem(text = { Text(stringResource(R.string.vault_move_down)) },
                onClick = { menu = false; onMoveDown() }, enabled = !busy)
            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) },
                onClick = { menu = false; delete = true }, enabled = !busy)
        }
    }
    if (delete) AlertDialog(onDismissRequest = { delete = false },
        title = { Text(stringResource(R.string.tasks_delete_title)) }, text = { Text(task.title) },
        confirmButton = { TextButton(onClick = { delete = false; onDelete() }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { delete = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) } })
}
