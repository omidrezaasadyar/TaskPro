package com.taskpro.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.data.TaskStatus
import com.taskpro.app.ui.components.shared.IconAction
import com.taskpro.app.ui.theme.LocalVaurivoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimelineFilterPanel(filter: TimelineFilter, selectedDate: String, active: Boolean,
    onScope: (DateScope) -> Unit, onStatus: (TaskStatus?) -> Unit,
    onNotes: (Boolean) -> Unit, onDue: (Boolean) -> Unit, onReset: () -> Unit,
    onSearch: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalVaurivoColors.current
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = p.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tasks_filters), Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                IconAction(stringResource(R.string.tasks_reset_filters), Icons.Default.RestartAlt, onReset, enabled = active)
                IconAction(stringResource(R.string.tasks_close_filters), Icons.Default.Close, onDismiss)
            }
            FilterChoice(stringResource(R.string.tasks_date_scope), filter.scope,
                DateScope.entries, { scope -> when (scope) {
                    DateScope.ALL -> stringResource(R.string.tasks_all_dates)
                    DateScope.UNDATED -> stringResource(R.string.tasks_undated)
                    DateScope.DAY -> "${stringResource(R.string.tasks_selected_day)} · $selectedDate"
                } }, onScope)
            FilterChoice(stringResource(R.string.tasks_status), filter.status,
                listOf(null) + TaskStatus.entries, { status ->
                    stringResource(status?.let(::statusLabel) ?: R.string.tasks_all_statuses)
                }, onStatus)
            FilterToggle(stringResource(R.string.tasks_notes), filter.withNotes, onNotes)
            FilterToggle(stringResource(R.string.tasks_due), filter.withDueDate, onDue)
            TextButton(onClick = onSearch, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.Search, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.search))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterChoice(label: String, value: T, choices: List<T>,
    display: @Composable (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(value = display(value), onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
        ExposedDropdownMenu(expanded, { expanded = false }) {
            choices.forEach { choice ->
                DropdownMenuItem(text = { Text(display(choice)) },
                    leadingIcon = { RadioButton(value == choice, null) },
                    onClick = { onSelect(choice); expanded = false })
            }
        }
    }
}

@Composable
private fun FilterToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(checked, role = Role.Checkbox, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, null)
        Text(label, Modifier.weight(1f).padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
