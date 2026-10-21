package com.taskpro.app.ui.screens.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.*
import com.taskpro.app.data.Item
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.data.Task
import com.taskpro.app.util.DateFormat
import java.util.Calendar

/**
 * Bottom sheet for creating or editing a task with an optional date + time reminder.
 * The phone will sound an alert at the chosen moment.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskSheet(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, dueAt: Long?) -> Unit = { _, _, _ -> },
    groups: List<Item>? = null,
    onSave: (suspend (groupId: Long?, title: String, notes: String, dueAt: Long?) -> Unit)? = null
) {
    val isEditMode = taskToEdit != null

    var title by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.title.orEmpty()) }
    var notes by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.notes.orEmpty()) }
    var dueAt by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.dueAt) }
    var groupId by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.itemId) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dirty = title != taskToEdit?.title.orEmpty() || notes != taskToEdit?.notes.orEmpty() ||
        dueAt != taskToEdit?.dueAt || (!isEditMode && groupId != null)
    fun requestClose() { if (!saving) { if (dirty) discard = true else onDismiss() } }
    androidx.activity.compose.BackHandler { requestClose() }
    val currentDirty by androidx.compose.runtime.rememberUpdatedState(dirty)
    val currentSaving by androidx.compose.runtime.rememberUpdatedState(saving)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && (currentSaving || currentDirty)) {
                if (!currentSaving) discard = true
                false
            } else true
        })

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = ::requestClose, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(if (isEditMode) R.string.edit_task else R.string.add_task),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            if (groups != null && !isEditMode) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded, { if (!saving) expanded = !expanded }) {
                    OutlinedTextField(value = groups.firstOrNull { it.id == groupId }?.name.orEmpty(),
                        onValueChange = {}, readOnly = true, enabled = !saving,
                        label = { Text(stringResource(R.string.tasks_choose_group)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        groups.forEach { group -> DropdownMenuItem(text = { Text(group.name) },
                            onClick = { groupId = group.id; expanded = false }) }
                    }
                }
                if (groups.isEmpty()) Text(stringResource(R.string.tasks_group_required), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = !saving
            )
            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(6.dp))
                    Text(dueAt?.let { DateFormat.formatDate(it) }
                        ?: stringResource(R.string.set_date))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = dueAt != null && !saving,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(6.dp))
                    Text(dueAt?.let { DateFormat.formatTime(it) }
                        ?: stringResource(R.string.set_time))
                }
                if (dueAt != null) {
                    IconButton(onClick = { dueAt = null }, enabled = !saving) {
                        Icon(Icons.Default.Clear, stringResource(R.string.clear_reminder))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (failed) Text(stringResource(R.string.tasks_save_failed), color = MaterialTheme.colorScheme.error)

            Button(
                onClick = {
                    if (!saving) {
                        if (onSave == null) onConfirm(title, notes, dueAt)
                        else {
                            saving = true; failed = false
                            scope.launch {
                                try { onSave(groupId, title, notes, dueAt); onDismiss() }
                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (_: Exception) { failed = true }
                                finally { saving = false }
                            }
                        }
                    }
                },
                enabled = !saving && title.isNotBlank() && (groups == null || isEditMode || groups.any { it.id == groupId }),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) { Text(stringResource(if (isEditMode) R.string.save else R.string.add)) }
            TextButton(onClick = ::requestClose, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false },
        title = { Text(stringResource(R.string.tasks_discard_title)) },
        text = { Text(stringResource(R.string.tasks_discard_message)) },
        confirmButton = { TextButton(onClick = { discard = false; onDismiss() }) { Text(stringResource(R.string.tasks_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.tasks_keep_editing)) } })

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = pickerDate(dueAt ?: System.currentTimeMillis())
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        dueAt = mergeDate(picked, dueAt)
                    }
                    showDatePicker = false
                    if (dueAt != null) showTimePicker = true
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val base = dueAt ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = base }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        DatePickerDialog( // reuse dialog scaffold for the time picker
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueAt = mergeTime(base, timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}

/** Combine a picked date with the existing time-of-day (or now). */
internal fun mergeDate(dateMillis: Long, existing: Long?): Long {
    val src = Calendar.getInstance().apply { timeInMillis = existing ?: System.currentTimeMillis() }
    val picked = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillis }
    val result = Calendar.getInstance().apply {
        clear()
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }
    result.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
    result.set(Calendar.MINUTE, src.get(Calendar.MINUTE))
    result.set(Calendar.SECOND, 0)
    result.set(Calendar.MILLISECOND, 0)
    return result.timeInMillis
}

internal fun pickerDate(localMillis: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun mergeTime(base: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = base }
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
