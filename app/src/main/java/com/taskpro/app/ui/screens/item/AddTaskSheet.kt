package com.taskpro.app.ui.screens.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.taskpro.app.util.DateFormat
import java.util.Calendar

/**
 * Bottom sheet for creating a task with an optional date + time reminder.
 * The phone will sound an alert at the chosen moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String, dueAt: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                stringResource(R.string.add_task),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(6.dp))
                    Text(dueAt?.let { DateFormat.formatDate(it) }
                        ?: stringResource(R.string.set_date))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = dueAt != null
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(6.dp))
                    Text(dueAt?.let { DateFormat.formatTime(it) }
                        ?: stringResource(R.string.set_time))
                }
                if (dueAt != null) {
                    IconButton(onClick = { dueAt = null }) {
                        Icon(Icons.Default.Clear, stringResource(R.string.clear_reminder))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onConfirm(title, notes, dueAt) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.add)) }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueAt ?: System.currentTimeMillis()
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
private fun mergeDate(dateMillis: Long, existing: Long?): Long {
    val src = Calendar.getInstance().apply { timeInMillis = existing ?: System.currentTimeMillis() }
    val result = Calendar.getInstance().apply { timeInMillis = dateMillis }
    result.set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
    result.set(Calendar.MINUTE, src.get(Calendar.MINUTE))
    result.set(Calendar.SECOND, 0)
    result.set(Calendar.MILLISECOND, 0)
    return result.timeInMillis
}

private fun mergeTime(base: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = base }
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
