package com.taskpro.app.tasks

import com.taskpro.app.data.*
import com.taskpro.app.ui.screens.tasks.*
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.util.TimeZone

class TimelineStateTest {
    private val now = Instant.parse("2026-10-17T10:00:00Z").toEpochMilli()
    private val zone = TimeZone.getTimeZone("UTC")
    private fun row(id: Long, due: Long?, status: TaskStatus = TaskStatus.PENDING, notes: String = "") =
        TaskWithItemName(Task(id, 1, "Synthetic $id", notes, due, status, position = id.toInt()), "Group")
    private fun ids(rows: List<TaskWithItemName>, filter: TimelineFilter = TimelineFilter()) =
        timelineSections(rows, filter, now, zone).values.flatten().map { it.task.id }

    @Test fun allDatesPartitionsWithoutDuplicatesAndDoesNotChangeManualOrder() {
        val rows = listOf(row(8, null), row(7, now + 86_400_000), row(4, now - 1), row(3, now + 1), row(5, now - 86_400_000, TaskStatus.COMPLETED))
        val result = timelineSections(rows, TimelineFilter(), now, zone)
        assertEquals(listOf(4L), result[TimelineSection.OVERDUE]!!.map { it.task.id })
        assertEquals(listOf(3L), result[TimelineSection.SELECTED_DAY]!!.map { it.task.id })
        assertEquals(listOf(7L), result[TimelineSection.UPCOMING]!!.map { it.task.id })
        assertEquals(listOf(5L), result[TimelineSection.EARLIER]!!.map { it.task.id })
        assertEquals(5, result.values.flatten().map { it.task.id }.toSet().size)
        assertEquals(listOf(8, 7, 4, 3, 5), rows.map { it.task.position })
    }
    @Test fun selectedDayIncludesStartButNotNextMidnight() {
        val (start, end) = dayBounds(now, zone)
        val rows = listOf(row(1, start), row(2, end - 1), row(3, end), row(4, start - 1), row(5, null))
        assertEquals(listOf(1L, 2L), ids(rows, TimelineFilter(DateScope.DAY)))
    }
    @Test fun undatedIsAccessibleAcrossAllStatuses() {
        val rows = TaskStatus.entries.mapIndexed { i, status -> row(i.toLong(), null, status) } + row(9, now)
        assertEquals(listOf(0L, 1L, 2L), ids(rows, TimelineFilter(DateScope.UNDATED)))
    }
    @Test fun statusNotesAndDueFiltersComposeUsingRealFieldsOnly() {
        val rows = listOf(row(1, now, notes = "Note"), row(2, null, notes = "Note"), row(3, now, TaskStatus.SNOOZED, "Note"), row(4, now, notes = "  "))
        assertEquals(listOf(1L), ids(rows, TimelineFilter(status = TaskStatus.PENDING, withNotes = true, withDueDate = true)))
        assertEquals(listOf(3L), ids(rows, TimelineFilter(status = TaskStatus.SNOOZED)))
        assertTrue(ids(rows, TimelineFilter(status = TaskStatus.COMPLETED)).isEmpty())
    }
    @Test fun upcomingTiesSortByStableIdNotPosition() {
        assertEquals(listOf(2L, 3L, 9L), ids(listOf(row(9, now + 2), row(3, now + 1), row(2, now + 1))))
    }
    @Test fun completedIsNotOverdueButSnoozedRemainsAnUnfinishedTask() {
        val result = timelineSections(listOf(row(1, now - 1, TaskStatus.COMPLETED), row(2, now - 1, TaskStatus.SNOOZED)), TimelineFilter(), now, zone)
        assertEquals(listOf(2L), result[TimelineSection.OVERDUE]!!.map { it.task.id })
        assertEquals(listOf(1L), result[TimelineSection.SELECTED_DAY]!!.map { it.task.id })
    }
    @Test fun springAndAutumnDaysHaveCorrectRealDurations() {
        val ny = TimeZone.getTimeZone("America/New_York")
        listOf("2026-03-08T12:00:00Z" to 23L, "2026-11-01T12:00:00Z" to 25L).forEach { (iso, hours) ->
            val (start, end) = dayBounds(Instant.parse(iso).toEpochMilli(), ny)
            assertEquals(hours * 3_600_000, end - start)
            assertEquals(end, shiftDay(start, 1, ny))
        }
    }
    @Test fun timezoneChangesRecomputeDaysWithoutChangingInstants() {
        val instant = Instant.parse("2026-10-17T00:30:00Z").toEpochMilli()
        val rows = listOf(row(1, instant))
        val utc = timelineSections(rows, TimelineFilter(DateScope.DAY), now, zone)
        val la = timelineSections(rows, TimelineFilter(DateScope.DAY), now, TimeZone.getTimeZone("America/Los_Angeles"))
        assertEquals(1, utc.values.flatten().size); assertTrue(la.isEmpty())
        assertEquals(instant, rows.single().task.dueAt)
    }
    @Test fun sevenDayStripCrossesMonthAndYearBoundaries() {
        val selected = Instant.parse("2026-12-31T10:00:00Z").toEpochMilli()
        val days = weekDays(selected, zone)
        assertEquals(7, days.size); assertEquals(7, days.toSet().size)
        assertEquals(dayBounds(selected, zone).first, days[3])
        assertEquals(Instant.parse("2027-01-03T00:00:00Z").toEpochMilli(), days.last())
    }
}
