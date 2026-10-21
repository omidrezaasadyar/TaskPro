package com.taskpro.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskpro.app.R
import com.taskpro.app.data.TaskWithItemName
import com.taskpro.app.ui.AppViewModelProvider
import com.taskpro.app.ui.components.shared.*
import com.taskpro.app.ui.screens.search.SearchViewModel
import com.taskpro.app.ui.screens.search.TaskSearchState
import com.taskpro.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

@Composable
fun DashboardRoute(vaultLocked: Boolean, onTasks: () -> Unit, onVault: () -> Unit,
                   onSettings: () -> Unit, onChooser: () -> Unit, onTask: (TaskWithItemName) -> Unit,
                   model: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
                   searchModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by model.state.collectAsStateWithLifecycle()
    val query by searchModel.query.collectAsStateWithLifecycle()
    val search by searchModel.state.collectAsStateWithLifecycle()
    DashboardScreen(state, vaultLocked, query, search, searchModel::onQueryChange, model::retry,
        searchModel::retry, onTasks, onVault, onSettings, onChooser, onTask)
}

@Composable
fun DashboardScreen(state: DashboardUiState, vaultLocked: Boolean, query: String, search: TaskSearchState,
                    onQuery: (String) -> Unit, onRetry: () -> Unit, onSearchRetry: () -> Unit,
                    onTasks: () -> Unit, onVault: () -> Unit, onSettings: () -> Unit, onChooser: () -> Unit,
                    onTask: (TaskWithItemName) -> Unit) {
    val p = LocalVaurivoColors.current
    val locale = LocalConfiguration.current.locales[0]
    val moment = state.summary?.moment ?: DashboardMoment(System.currentTimeMillis(), java.util.TimeZone.getDefault().id)
    BoxWithConstraints(Modifier.fillMaxSize().background(p.canvas).safeDrawingPadding()) {
        val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(VaurivoLayout.compactScreenMargin),
            verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
            DashboardHeader(moment, locale, stacked, onSettings, onChooser)
            SearchField(query, onQuery, stringResource(R.string.dashboard_search))
            if (query.isNotBlank()) {
                SectionHeader(stringResource(R.string.dashboard_results))
                when {
                    search.query != query.trim() || search.loading -> LoadingState(stringResource(R.string.component_busy))
                    search.failed -> ErrorState(stringResource(R.string.dashboard_search_error),
                        retryLabel = stringResource(R.string.dashboard_retry), onRetry = onSearchRetry)
                    search.results.isEmpty() -> EmptyState(stringResource(R.string.dashboard_no_results),
                        stringResource(R.string.dashboard_search_hint), Icons.Default.Search)
                    else -> DashboardTaskList(search.results, moment, locale, onTask)
                }
            } else {
                when {
                    state.loading -> LoadingState(stringResource(R.string.dashboard_loading))
                    state.failed -> ErrorState(stringResource(R.string.dashboard_error),
                        retryLabel = stringResource(R.string.dashboard_retry), onRetry = onRetry)
                    state.summary != null -> ProgressSummaryCard(state.summary, locale, stacked)
                }
                SectionHeader(stringResource(R.string.dashboard_workspaces))
                @Composable fun tasks(modifier: Modifier) {
                    WorkspaceCard(stringResource(R.string.workspace_tasks), state.summary?.let {
                        stringResource(R.string.dashboard_active, number(it.active, locale))
                    } ?: stringResource(R.string.chooser_tasks_subtitle), Icons.Default.Checklist,
                        ModuleAccent.Tasks, onTasks, modifier)
                }
                @Composable fun vault(modifier: Modifier) {
                    WorkspaceCard(stringResource(R.string.vault_title), stringResource(if (vaultLocked)
                        R.string.component_locked else R.string.dashboard_open_vault), Icons.Default.EnhancedEncryption,
                        ModuleAccent.Vault, onVault, modifier, locked = vaultLocked)
                }
                if (stacked) Column(verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    tasks(Modifier.fillMaxWidth()); vault(Modifier.fillMaxWidth())
                } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    tasks(Modifier.weight(1f).fillMaxHeight()); vault(Modifier.weight(1f).fillMaxHeight())
                }
                SectionHeader(stringResource(R.string.dashboard_up_next))
                if (state.loading) LoadingState(stringResource(R.string.dashboard_loading))
                else if (!state.failed) state.summary?.let { summary ->
                    if (summary.upcoming.isEmpty()) EmptyState(stringResource(R.string.dashboard_no_upcoming),
                        stringResource(R.string.dashboard_no_upcoming_hint), Icons.Default.EventAvailable)
                    else DashboardTaskList(summary.upcoming, summary.moment, locale, onTask)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(moment: DashboardMoment?, locale: Locale, stacked: Boolean,
                            onSettings: () -> Unit, onChooser: () -> Unit) {
    val p = LocalVaurivoColors.current
    @Composable fun title(modifier: Modifier) {
        Column(modifier) {
            if (moment != null) {
                val calendar = GregorianCalendar(moment.zone, locale).apply { timeInMillis = moment.now }
                val date = remember(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR), moment.zoneId, locale) {
                    SimpleDateFormat("EEEE, d MMM", locale).apply { this.calendar = GregorianCalendar(moment.zone, locale) }
                        .format(Date(moment.now))
                }
                Text(date, style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
                Text(stringResource(when (calendar.get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> R.string.dashboard_morning
                    in 12..17 -> R.string.dashboard_afternoon
                    else -> R.string.dashboard_evening
                }), Modifier.fillMaxWidth().semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = p.textPrimary)
            } else Text(stringResource(R.string.component_home), style = MaterialTheme.typography.headlineMedium,
                color = p.textPrimary, modifier = Modifier.semantics { heading() })
        }
    }
    @Composable fun actions() {
        Row {
            IconAction(stringResource(R.string.workspace_switch), Icons.Default.Apps, onChooser)
            IconAction(stringResource(R.string.settings), Icons.Default.Tune, onSettings)
        }
    }
    if (stacked) Column(Modifier.fillMaxWidth()) {
        title(Modifier.fillMaxWidth())
        Box(Modifier.align(Alignment.End)) { actions() }
    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        title(Modifier.weight(1f)); actions()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressSummaryCard(summary: DashboardSummary, locale: Locale, stacked: Boolean) {
    val p = LocalVaurivoColors.current
    val fraction = summary.fraction
    val fractionText = "${number(summary.completed, locale)}/${number(summary.scheduled, locale)}"
    val percentage = fraction?.let { NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }
        .format(summary.completed.toDouble() / summary.scheduled) }
    @Composable fun ring() {
        Box(Modifier.size(64.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            if (fraction == null) Icon(Icons.Default.EventAvailable, null, Modifier.size(32.dp), tint = p.tasksAccent)
            else {
                CircularProgressIndicator(progress = { fraction }, Modifier.fillMaxSize(), color = p.tasksAccent,
                    trackColor = p.surfaceRaised, strokeWidth = 6.dp)
                if (!stacked && fractionText.length <= 7) Text(fractionText, style = MaterialTheme.typography.titleSmall, color = p.textPrimary)
            }
        }
    }
    @Composable fun details(modifier: Modifier) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
            Text(stringResource(R.string.dashboard_scheduled), Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
            Text(if (percentage == null) stringResource(R.string.dashboard_none_scheduled)
                else stringResource(R.string.dashboard_progress, fractionText, percentage), Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall, color = p.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.small), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                Text(stringResource(R.string.dashboard_overdue, number(summary.overdue, locale)), color = if (summary.overdue > 0) p.error else p.textSecondary,
                    style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.dashboard_future, number(summary.futureCount, locale)), color = p.textSecondary,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Surface(shape = VaurivoShapes.card, color = p.surface, border = BorderStroke(VaurivoStroke.border, p.borderSubtle)) {
        if (stacked) Column(Modifier.fillMaxWidth().padding(VaurivoLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
            ring(); details(Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().padding(VaurivoLayout.cardPadding), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.large)) { ring(); details(Modifier.weight(1f)) }
    }
}

@Composable
private fun DashboardTaskList(rows: List<TaskWithItemName>, moment: DashboardMoment, locale: Locale,
                              onTask: (TaskWithItemName) -> Unit) {
    val p = LocalVaurivoColors.current
    Surface(shape = VaurivoShapes.card, color = p.surface, border = BorderStroke(VaurivoStroke.border, p.borderSubtle)) {
        Column(Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = VaurivoSpacing.large), color = p.borderSubtle)
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onTask(row) }
                    .heightIn(min = 72.dp).padding(VaurivoSpacing.medium), horizontalArrangement = Arrangement.spacedBy(VaurivoSpacing.medium)) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(24.dp), tint = p.textSecondary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaurivoSpacing.extraSmall)) {
                        Text(row.task.title, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Content), color = p.textPrimary)
                        Text(row.itemName, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content), color = p.textSecondary)
                        val due = row.task.dueAt
                        val date = remember(due, moment.zoneId, locale) { due?.let {
                            SimpleDateFormat("EEE, d MMM  HH:mm", locale).apply { calendar = GregorianCalendar(moment.zone, locale) }.format(Date(it))
                        } }
                        Text(date ?: stringResource(R.string.dashboard_undated), Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content), color = p.textSecondary)
                    }
                }
            }
        }
    }
}

private fun number(value: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)
