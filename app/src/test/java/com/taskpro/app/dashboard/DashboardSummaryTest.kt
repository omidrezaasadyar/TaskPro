package com.taskpro.app.dashboard

import com.taskpro.app.data.*
import com.taskpro.app.ui.screens.dashboard.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DashboardSummaryTest {
    private fun at(value: String) = Instant.parse(value).toEpochMilli()
    private val moment = DashboardMoment(at("2026-10-17T12:00:00Z"), "UTC")
    private fun row(id: Long, due: Long?, status: TaskStatus = TaskStatus.PENDING, position: Int = 4) =
        TaskWithItemName(Task(id, 1, "Synthetic $id", dueAt = due, status = status, position = position), "Group")

    @Test fun emptyDayHasNoMisleadingFractionOrPercentage() {
        val summary = dashboardSummary(emptyList(), moment)
        assertEquals(0, summary.scheduled)
        assertNull(summary.fraction)
        assertTrue(summary.upcoming.isEmpty())
    }

    @Test fun scheduledTodayIncludesAllStatusesButOnlyCompletedInNumerator() {
        val rows = listOf(row(1, moment.now), row(2, moment.now, TaskStatus.SNOOZED),
            row(3, moment.now, TaskStatus.COMPLETED), row(4, null), row(5, null, TaskStatus.COMPLETED))
        val summary = dashboardSummary(rows, moment)
        assertEquals(3, summary.scheduled)
        assertEquals(1, summary.completed)
        assertEquals(1f / 3, summary.fraction!!, 0.0001f)
        assertEquals(3, summary.active)
    }

    @Test fun dayUsesInclusiveStartExclusiveNextMidnight() {
        val (start, end) = localDayBounds(moment)
        val summary = dashboardSummary(listOf(row(1, start - 1), row(2, start), row(3, end - 1), row(4, end)), moment)
        assertEquals(2, summary.scheduled)
        assertEquals(at("2026-10-17T00:00:00Z"), start)
        assertEquals(at("2026-10-18T00:00:00Z"), end)
    }

    @Test fun midnightReclassifiesWithoutRewritingDueDates() {
        val rows = listOf(row(1, at("2026-10-17T23:59:59Z")), row(2, at("2026-10-18T00:00:00Z"), TaskStatus.COMPLETED))
        val before = dashboardSummary(rows, DashboardMoment(at("2026-10-17T23:59:58Z"), "UTC"))
        val after = dashboardSummary(rows, DashboardMoment(at("2026-10-18T00:00:00Z"), "UTC"))
        assertEquals(0, before.completed)
        assertEquals(1, after.completed)
        assertEquals(1, after.overdue)
        assertEquals(at("2026-10-18T00:00:00Z"), rows[1].task.dueAt)
    }

    @Test fun timezoneChangeReclassifiesTheSameEpoch() {
        val now = at("2026-10-17T23:30:00Z")
        val rows = listOf(row(1, at("2026-10-17T03:00:00Z")))
        assertEquals(1, dashboardSummary(rows, DashboardMoment(now, "UTC")).scheduled)
        assertEquals(0, dashboardSummary(rows, DashboardMoment(now, "Asia/Muscat")).scheduled)
    }

    @Test fun daylightSavingDaysAreNotAssumedToBe24Hours() {
        val spring = localDayBounds(DashboardMoment(at("2026-03-08T12:00:00Z"), "America/New_York"))
        val autumn = localDayBounds(DashboardMoment(at("2026-11-01T12:00:00Z"), "America/New_York"))
        assertEquals(23 * 3_600_000L, spring.second - spring.first)
        assertEquals(25 * 3_600_000L, autumn.second - autumn.first)
    }

    @Test fun upcomingExcludesCompletedUndatedPastAndExactlyNow() {
        val summary = dashboardSummary(listOf(row(1, null), row(2, moment.now - 1), row(3, moment.now),
            row(4, moment.now + 1, TaskStatus.COMPLETED), row(5, moment.now + 2, TaskStatus.SNOOZED)), moment)
        assertEquals(listOf(5L), summary.upcoming.map { it.task.id })
        assertEquals(1, summary.futureCount)
        assertEquals(1, summary.overdue)
    }

    @Test fun upcomingTiesUseStableIdsAndDoNotAlterManualPositions() {
        val rows = listOf(row(9, moment.now + 2, position = 0), row(5, moment.now + 1, position = 9),
            row(2, moment.now + 1, position = 7), row(1, moment.now + 3))
        val summary = dashboardSummary(rows, moment)
        assertEquals(listOf(2L, 5L, 9L), summary.upcoming.map { it.task.id })
        assertEquals(4, summary.futureCount)
        assertEquals(listOf(0, 9, 7, 4), rows.map { it.task.position })
    }

    @Test fun undatedTasksRemainInTheActiveSummaryButNotScheduledToday() {
        val summary = dashboardSummary(listOf(row(1, null), row(2, null, TaskStatus.SNOOZED)), moment)
        assertEquals(2, summary.active)
        assertEquals(0, summary.scheduled)
        assertNull(summary.fraction)
    }
}
