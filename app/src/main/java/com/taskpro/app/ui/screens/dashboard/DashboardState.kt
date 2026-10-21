package com.taskpro.app.ui.screens.dashboard

import com.taskpro.app.data.TaskStatus
import com.taskpro.app.data.TaskWithItemName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.util.Calendar
import java.util.TimeZone

data class DashboardMoment(val now: Long, val zoneId: String) {
    val zone: TimeZone get() = TimeZone.getTimeZone(zoneId)
}

data class DashboardSummary(
    val moment: DashboardMoment,
    val scheduled: Int,
    val completed: Int,
    val active: Int,
    val overdue: Int,
    val futureCount: Int,
    val upcoming: List<TaskWithItemName>
) {
    val fraction: Float? get() = if (scheduled == 0) null else completed.toFloat() / scheduled
}

data class DashboardUiState(val summary: DashboardSummary? = null, val loading: Boolean = true, val failed: Boolean = false)

fun localDayBounds(moment: DashboardMoment): Pair<Long, Long> {
    val calendar = Calendar.getInstance(moment.zone).apply {
        timeInMillis = moment.now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    // Calendar-day addition preserves DST days; never assume a day is 24 elapsed hours.
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return start to calendar.timeInMillis
}

fun dashboardSummary(rows: List<TaskWithItemName>, moment: DashboardMoment): DashboardSummary {
    val (start, end) = localDayBounds(moment)
    val scheduled = rows.filter { it.task.dueAt?.let { due -> due >= start && due < end } == true }
    val active = rows.filter { it.task.status != TaskStatus.COMPLETED }
    val future = active.filter { it.task.dueAt?.let { due -> due > moment.now } == true }
        .sortedWith(compareBy<TaskWithItemName> { it.task.dueAt }.thenBy { it.task.id })
    return DashboardSummary(moment, scheduled.size, scheduled.count { it.task.status == TaskStatus.COMPLETED },
        active.size, active.count { it.task.dueAt?.let { due -> due < moment.now } == true },
        future.size, future.take(3))
}

fun dashboardClock(): Flow<DashboardMoment> = flow {
    // Collected only while Home is observed. Also catches wall-clock and time-zone changes.
    while (currentCoroutineContext().isActive) {
        emit(DashboardMoment(System.currentTimeMillis(), TimeZone.getDefault().id))
        delay(1_000)
    }
}
