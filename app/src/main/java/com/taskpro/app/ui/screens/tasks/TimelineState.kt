package com.taskpro.app.ui.screens.tasks

import com.taskpro.app.data.TaskStatus
import com.taskpro.app.data.TaskWithItemName
import java.util.Calendar
import java.util.TimeZone

enum class DateScope { ALL, DAY, UNDATED }
enum class TimelineSection { OVERDUE, SELECTED_DAY, UPCOMING, EARLIER, UNDATED }

data class TimelineFilter(
    val scope: DateScope = DateScope.ALL,
    val selectedDay: Long? = null,
    val status: TaskStatus? = null,
    val withNotes: Boolean = false,
    val withDueDate: Boolean = false
)

internal fun dayBounds(instant: Long, zone: TimeZone): Pair<Long, Long> {
    val c = Calendar.getInstance(zone).apply {
        timeInMillis = instant
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = c.timeInMillis
    c.add(Calendar.DAY_OF_MONTH, 1)
    return start to c.timeInMillis
}

internal fun shiftDay(instant: Long, days: Int, zone: TimeZone): Long =
    Calendar.getInstance(zone).apply { timeInMillis = instant; add(Calendar.DAY_OF_MONTH, days) }.timeInMillis

internal fun weekDays(selected: Long, zone: TimeZone): List<Long> =
    (-3..3).map { shiftDay(dayBounds(selected, zone).first, it, zone) }

/** A read-only, mutually exclusive partition. No ordering is written to Room. */
fun timelineSections(rows: List<TaskWithItemName>, filter: TimelineFilter, now: Long, zone: TimeZone):
    Map<TimelineSection, List<TaskWithItemName>> {
    val (start, end) = dayBounds(filter.selectedDay ?: now, zone)
    val buckets = linkedMapOf<TimelineSection, MutableList<TaskWithItemName>>()
    rows.asSequence().filter { row ->
        val t = row.task
        (filter.status == null || t.status == filter.status) &&
            (!filter.withNotes || t.notes.isNotBlank()) && (!filter.withDueDate || t.dueAt != null) &&
            when (filter.scope) {
                DateScope.ALL -> true
                DateScope.UNDATED -> t.dueAt == null
                DateScope.DAY -> t.dueAt?.let { it >= start && it < end } == true
            }
    }.sortedWith(compareBy<TaskWithItemName> { it.task.dueAt ?: Long.MAX_VALUE }.thenBy { it.task.id })
        .forEach { row ->
            val t = row.task
            val key = when {
                t.dueAt == null -> TimelineSection.UNDATED
                t.status != TaskStatus.COMPLETED && t.dueAt < now -> TimelineSection.OVERDUE
                t.dueAt < start -> TimelineSection.EARLIER
                t.dueAt < end -> TimelineSection.SELECTED_DAY
                else -> TimelineSection.UPCOMING
            }
            buckets.getOrPut(key) { mutableListOf() }.add(row)
        }
    return TimelineSection.entries.mapNotNull { key -> buckets[key]?.let { key to it.toList() } }.toMap()
}
