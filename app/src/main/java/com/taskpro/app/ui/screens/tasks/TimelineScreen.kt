package com.taskpro.app.ui.screens.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.*
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.shared.IconAction
import com.taskpro.app.ui.screens.item.AddTaskSheet
import com.taskpro.app.ui.screens.item.mergeDate
import com.taskpro.app.ui.screens.item.pickerDate
import com.taskpro.app.ui.theme.LocalVaurivoColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TimelineRoute(onGroups: () -> Unit, onSearch: () -> Unit,
    onOpenStatus: (Long, String) -> Unit,
    viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var createDueAt by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun action(block: suspend () -> Unit) {
        if (!busy) { busy = true; scope.launch {
            try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { failed = true } finally { busy = false }
        } }
    }
    TimelineScreen(state, onGroups, onSearch, onNew = {
        createDueAt = selectedDayDueAt(selectedDay ?: state.now, state.now, TimeZone.getTimeZone(state.zoneId))
        creating = true
    },
        onEdit = { row -> if (row.task.status == TaskStatus.PENDING) editing = row.task
            else onOpenStatus(row.task.itemId, row.task.status.name) },
        onStatus = { task, status -> action { viewModel.status(task, status) } },
        onDelete = { task -> action { viewModel.delete(task) } }, onRetry = viewModel::retry,
        selectedDay = selectedDay, onSelectDay = viewModel::selectDay, busy = busy)
    if (creating || editing != null) AddTaskSheet(taskToEdit = editing,
        initialDueAt = if (creating) createDueAt else null,
        groups = state.groups.map { it.item }, onDismiss = { creating = false; editing = null },
        onSave = { group, title, notes, due -> viewModel.save(editing, group, title, notes, due) })
    if (failed) AlertDialog(onDismissRequest = { failed = false },
        text = { Text(stringResource(R.string.tasks_action_failed)) },
        confirmButton = { TextButton(onClick = { failed = false }) { Text(stringResource(R.string.ok)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksViewSelector(groups: Boolean, onTimeline: () -> Unit, onGroups: () -> Unit) {
    val p = LocalVaurivoColors.current
    val options = listOf(R.string.tasks_timeline, R.string.tasks_groups)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        options.forEachIndexed { index, label ->
            SegmentedButton(selected = groups == (index == 1), onClick = if (index == 1) onGroups else onTimeline,
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp), icon = {},
                colors = SegmentedButtonDefaults.colors(activeContainerColor = p.tasksContainer,
                    activeContentColor = p.tasksAccent, activeBorderColor = p.tasksAccent,
                    inactiveContainerColor = p.surfaceRaised, inactiveContentColor = p.textSecondary,
                    inactiveBorderColor = p.borderSubtle)) {
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimelineScreen(state: TasksState, onGroups: () -> Unit, onSearch: () -> Unit, onNew: () -> Unit,
    onEdit: (TaskWithItemName) -> Unit, onStatus: (Task, TaskStatus) -> Unit,
    onDelete: (Task) -> Unit, onRetry: () -> Unit,
    selectedDay: Long?, onSelectDay: (Long?) -> Unit, busy: Boolean = false) {
    val p = LocalVaurivoColors.current
    var scopeName by rememberSaveable { mutableStateOf(DateScope.ALL.name) }
    var statusName by rememberSaveable { mutableStateOf<String?>(null) }
    var withNotes by rememberSaveable { mutableStateOf(false) }
    var withDue by rememberSaveable { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    var calendarMenu by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    val zone = TimeZone.getTimeZone(state.zoneId)
    val anchor = selectedDay ?: state.now
    val filter = TimelineFilter(DateScope.valueOf(scopeName), selectedDay, statusName?.let(TaskStatus::valueOf), withNotes, withDue)
    val active = filter.scope != DateScope.ALL || statusName != null || withNotes || withDue
    val scopeLabel = when (filter.scope) {
        DateScope.ALL -> stringResource(R.string.tasks_all_dates)
        DateScope.UNDATED -> stringResource(R.string.tasks_undated)
        DateScope.DAY -> "${stringResource(R.string.tasks_selected_day)} · ${dateText(anchor, "d MMM yyyy", zone)}"
    }
    val summary = listOfNotNull(scopeLabel,
        stringResource(filter.status?.let(::statusLabel) ?: R.string.tasks_all_statuses),
        if (withNotes) stringResource(R.string.tasks_notes) else null,
        if (withDue) stringResource(R.string.tasks_due) else null).joinToString(" · ")
    val filterDescription = if (active) stringResource(R.string.tasks_filters_active, summary) else stringResource(R.string.tasks_all_dates)
    val sections = remember(state.rows, filter, state.now, state.zoneId) { timelineSections(state.rows, filter, state.now, zone) }
    val (dayStart, dayEnd) = dayBounds(anchor, zone)
    val dailyRows = remember(state.rows, dayStart, dayEnd) {
        state.rows.filter { row -> row.task.dueAt?.let { it >= dayStart && it < dayEnd } == true }
    }
    val doneToday = dailyRows.count { it.task.status == TaskStatus.COMPLETED }
    fun resetToday() { onSelectDay(null); scopeName = DateScope.DAY.name }
    val dayWidth = if (LocalDensity.current.fontScale > 1.3f) 72.dp else 48.dp
    val layoutDirection = LocalLayoutDirection.current
    val currentSelectDay by rememberUpdatedState(onSelectDay)
    val week = remember(anchor, state.zoneId) { weekDays(anchor, zone) }
    Scaffold(containerColor = p.canvas, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
        com.taskpro.app.ui.components.shared.PrimaryActionButton(stringResource(R.string.tasks_new), onNew,
            icon = Icons.Default.Add, compact = true)
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(p.surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.tasks_title), style = MaterialTheme.typography.labelMedium, color = p.textTertiary)
                        Text(dateText(anchor, "EEEE, d MMM", zone), style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                        Text(stringResource(R.string.tasks_daily_summary,
                            pluralStringResource(R.plurals.tasks_daily_count, dailyRows.size, dailyRows.size),
                            pluralStringResource(R.plurals.tasks_daily_done, doneToday, doneToday)),
                            style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                        LinearProgressIndicator(progress = { if (dailyRows.isEmpty()) 0f else doneToday.toFloat() / dailyRows.size },
                            modifier = Modifier.width(160.dp).height(4.dp), color = p.vaultAccent, trackColor = p.borderSubtle)
                    }
                    Box {
                        IconAction(stringResource(R.string.tasks_calendar), Icons.Default.CalendarMonth, { calendarMenu = true })
                        DropdownMenu(calendarMenu, { calendarMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.tasks_calendar)) }, onClick = { calendarMenu = false; calendar = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.tasks_previous)) }, onClick = {
                                onSelectDay(shiftDay(anchor, -7, zone)); scopeName = DateScope.DAY.name; calendarMenu = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.tasks_next)) }, onClick = {
                                onSelectDay(shiftDay(anchor, 7, zone)); scopeName = DateScope.DAY.name; calendarMenu = false
                            })
                        }
                    }
                    Box {
                        IconAction(stringResource(R.string.tasks_filters), Icons.Default.Tune, { filters = true },
                            modifier = Modifier.semantics { stateDescription = filterDescription })
                        if (active) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(6.dp)
                            .background(p.tasksAccent, RoundedCornerShape(3.dp)))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(weekRangeLabel(week, zone), Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium, color = p.textSecondary)
                    TextButton(onClick = ::resetToday, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.tasks_today))
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().testTag("tasksWeekStrip")
                    .pointerInput(anchor, state.zoneId, layoutDirection) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var previousX = down.position.x
                        var drag = 0f
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            drag += change.position.x - previousX
                            previousX = change.position.x
                        } while (change.pressed)
                        if (kotlin.math.abs(drag) >= 48.dp.toPx()) {
                            val rtl = layoutDirection == LayoutDirection.Rtl
                            val forward = if (rtl) drag > 0f else drag < 0f
                            currentSelectDay(shiftDay(anchor, if (forward) 7 else -7, zone))
                            scopeName = DateScope.DAY.name
                        }
                    }
                }) {
                val cellWidth = maxOf(dayWidth, maxWidth / 7)
                val narrowScroll = if (cellWidth * 7 > maxWidth) Modifier.horizontalScroll(rememberScrollState()) else Modifier
                Row(Modifier.fillMaxWidth().then(narrowScroll)) {
                    val weekdays = stringArrayResource(R.array.tasks_weekdays)
                    week.forEach { day ->
                        val chosen = dayBounds(anchor, zone).first == day
                        val today = dayBounds(state.now, zone).first == day
                        val (start, end) = dayBounds(day, zone)
                        val hasTasks = state.rows.any { row -> row.task.dueAt?.let { it >= start && it < end } == true }
                        Column(Modifier.width(cellWidth).heightIn(min = 64.dp)
                            .background(if (chosen) p.tasksAccent else p.surface, RoundedCornerShape(12.dp))
                            .then(if (today) Modifier.border(1.dp, p.vaultAccent, RoundedCornerShape(12.dp)) else Modifier)
                            .selectable(chosen, role = Role.Tab, onClick = { onSelectDay(day); scopeName = DateScope.DAY.name })
                            .semantics { contentDescription = dateText(day, "EEEE d MMMM yyyy", zone) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                            val color = if (chosen) p.onAccent else p.textSecondary
                            Text(weekdays[java.util.Calendar.getInstance(zone).apply { timeInMillis = day }.get(java.util.Calendar.DAY_OF_WEEK) - 1], style = MaterialTheme.typography.bodySmall, color = color)
                            Text(dateText(day, "d", zone), style = MaterialTheme.typography.titleMedium, color = if (chosen) p.onAccent else p.textPrimary)
                            Box(Modifier.size(5.dp).background(if (hasTasks) { if (chosen) p.onAccent else p.vaultAccent } else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(3.dp)))
                        }
                    }
                }
                }
                Spacer(Modifier.height(12.dp))
                TasksViewSelector(false, {}, onGroups)
                if (active) Text(summary, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                }
                HorizontalDivider(color = p.borderSubtle)
            }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else if (state.failed) item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.tasks_action_failed), color = p.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.dashboard_retry)) }
                }
            }
            else if (sections.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                    Text(stringResource(R.string.tasks_empty_hint), style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                }
            }
            sections.forEach { (section, rows) ->
                item(key = section.name) {
                    Text(stringResource(when (section) {
                        TimelineSection.OVERDUE -> R.string.tasks_overdue
                        TimelineSection.SELECTED_DAY -> if (dayBounds(anchor, zone).first == dayBounds(state.now, zone).first) R.string.tasks_today else R.string.tasks_selected_day
                        TimelineSection.UPCOMING -> R.string.tasks_upcoming
                        TimelineSection.EARLIER -> R.string.tasks_earlier
                        TimelineSection.UNDATED -> R.string.tasks_undated
                    }), Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp).semantics { heading() }, style = MaterialTheme.typography.labelMedium, color = p.textTertiary)
                }
                items(rows, key = { it.task.id }) { row ->
                    ManagedTaskRow(row.task, taskMetadata(row.itemName, row.task.dueAt, zone), { onEdit(row) },
                        { onStatus(row.task, it) }, { onDelete(row.task) }, busy = busy,
                        overdue = section == TimelineSection.OVERDUE, dense = true, leadingStripe = true,
                        modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
    if (filters) TimelineFilterPanel(filter, dateText(anchor, "d MMM yyyy", zone), active,
        onScope = { value -> scopeName = value.name; if (value == DateScope.ALL) onSelectDay(null) },
        onStatus = { statusName = it?.name }, onNotes = { withNotes = it }, onDue = { withDue = it },
        onReset = { onSelectDay(null); scopeName = DateScope.ALL.name; statusName = null; withNotes = false; withDue = false },
        onSearch = { filters = false; onSearch() }, onDismiss = { filters = false })
    if (calendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = pickerDate(anchor))
        DatePickerDialog(onDismissRequest = { calendar = false },
            confirmButton = { TextButton(onClick = {
                picker.selectedDateMillis?.let { onSelectDay(mergeDate(it, anchor)); scopeName = DateScope.DAY.name }
                calendar = false
            }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { calendar = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(picker) }
    }
}

internal fun statusLabel(status: TaskStatus): Int = when (status) {
    TaskStatus.PENDING -> R.string.status_pending
    TaskStatus.COMPLETED -> R.string.status_completed
    TaskStatus.SNOOZED -> R.string.status_snoozed
}

internal fun dateText(time: Long, pattern: String, zone: TimeZone): String =
    SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = zone }.format(java.util.Date(time))

internal fun weekRangeLabel(week: List<Long>, zone: TimeZone): String {
    val first = week.first()
    val last = week.last()
    return if (dateText(first, "yyyy-MM", zone) == dateText(last, "yyyy-MM", zone))
        dateText(first, "MMMM yyyy", zone)
    else "${dateText(first, "MMM yyyy", zone)} – ${dateText(last, "MMM yyyy", zone)}"
}

internal fun taskMetadata(group: String, due: Long?, zone: TimeZone = TimeZone.getDefault()): String =
    if (due == null) group else "$group · ${dateText(due, "d MMM yyyy HH:mm", zone)}"
