package com.taskpro.app.tasks

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.taskpro.app.data.*
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.ui.screens.item.mergeDate
import com.taskpro.app.ui.screens.item.pickerDate
import com.taskpro.app.ui.screens.tasks.*
import com.taskpro.app.util.ShareUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.TimeZone
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TasksDataTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository
    private val app get() = RuntimeEnvironment.getApplication()
    private val alarms get() = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        repo = TaskRepository(db.itemDao(), db.taskDao(), ReminderScheduler(app))
        db.itemDao().insert(Item(1, "Synthetic group")); Unit
    }
    @After fun close() { db.close() }

    @Test fun editUpdatesSameIdAndLatestPositionWithoutDuplicates() = runBlocking {
        val original = Task(12, 1, "Original", "Notes", position = 4, createdAt = 9876)
        db.taskDao().insert(original)
        db.taskDao().updatePendingPosition(12, 1, 9)
        repo.editPendingTask(12, "Edited", "Updated", null)
        val saved = db.taskDao().getAll().single()
        assertEquals(original.copy(title = "Edited", notes = "Updated", position = 9), saved)
    }
    @Test fun staleEditorRejectsStatusTransitionAndDeletedTask() = runBlocking {
        val original = Task(1, 1, "Original", position = 5)
        db.taskDao().insert(original)
        repo.setStatus(original, TaskStatus.COMPLETED)
        val completed = db.taskDao().getById(1)!!
        assertTrue(runCatching { repo.editPendingTask(1, "Wrong", "", null) }.isFailure)
        assertEquals(completed, db.taskDao().getById(1))
        repo.deleteTask(completed)
        assertTrue(runCatching { repo.editPendingTask(1, "Wrong", "", null) }.isFailure)
        assertTrue(db.taskDao().getAll().isEmpty())
    }
    @Test fun staleReorderCannotUndoEditsOrReopenCompletedTasks() = runBlocking {
        val a = Task(1, 1, "Old A", position = 0)
        val b = Task(2, 1, "Old B", position = 1)
        db.taskDao().insertAll(listOf(a, b))
        repo.editPendingTask(1, "New A", "New notes", 12345)
        repo.setStatus(b, TaskStatus.COMPLETED)
        val completed = db.taskDao().getById(2)!!
        repo.reorder(listOf(b, a))
        assertEquals(completed, db.taskDao().getById(2))
        assertEquals("New A", db.taskDao().getById(1)!!.title)
        assertEquals("New notes", db.taskDao().getById(1)!!.notes)
        assertEquals(1, db.taskDao().getById(1)!!.position)
        assertEquals(12345L, db.taskDao().getById(1)!!.dueAt)
    }
    @Test fun timelineCreationRequiresExplicitGroupAndPreservesGroupContext() = runBlocking {
        db.itemDao().insert(Item(2, "Second"))
        val model = TasksViewModel(repo)
        assertTrue(runCatching { model.save(null, null, "New", "", null) }.isFailure)
        assertTrue(db.taskDao().getAll().isEmpty())
        model.save(null, 2, "New", "Notes", null)
        assertEquals(2L, db.taskDao().getAll().single().itemId)
        assertEquals(TaskStatus.PENDING, db.taskDao().getAll().single().status)
    }
    @Test fun createEditClearCompleteReopenAndListSnoozeKeepReminderContract() = runBlocking {
        val due = System.currentTimeMillis() + 86_400_000
        repo.addTask(1, "Reminder", "", due)
        val task = db.taskDao().getAll().single()
        assertEquals(due, alarms.scheduledAlarms.single().triggerAtMs)
        @Suppress("DEPRECATION") // This shadow exposes PendingIntent identity only through the field.
        val intent = shadowOf(alarms.scheduledAlarms.single().operation).savedIntent
        assertEquals(ReminderScheduler.ACTION_REMINDER, intent.action)
        assertEquals(task.id, intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1))
        repo.editPendingTask(task.id, "Edited reminder", "", due + 60_000)
        assertEquals(due + 60_000, alarms.scheduledAlarms.single().triggerAtMs)
        repo.setStatusById(task.id, TaskStatus.COMPLETED)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        repo.setStatusById(task.id, TaskStatus.PENDING)
        assertEquals(due + 60_000, alarms.scheduledAlarms.single().triggerAtMs)
        repo.setStatusById(task.id, TaskStatus.SNOOZED)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertEquals(due + 60_000, db.taskDao().getById(task.id)!!.dueAt)
        repo.setStatusById(task.id, TaskStatus.PENDING)
        repo.editPendingTask(task.id, "Cleared", "", null)
        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertNull(db.taskDao().getById(task.id)!!.dueAt)
    }
    @Test fun popupTenMinuteSnoozeRearmsUnlikeListSnoozeAndBootRestoresIt() = runBlocking {
        repo.addTask(1, "Popup", "", null)
        val id = db.taskDao().getAll().single().id
        val before = System.currentTimeMillis()
        repo.snoozeByIdForMinutes(id, 10)
        val snoozed = db.taskDao().getById(id)!!
        assertEquals(TaskStatus.SNOOZED, snoozed.status)
        assertTrue(snoozed.dueAt!! >= before + 600_000)
        assertEquals(snoozed.dueAt, alarms.scheduledAlarms.single().triggerAtMs)
        ReminderScheduler(app).cancel(id)
        repo.rescheduleAll()
        assertEquals(snoozed.dueAt, alarms.scheduledAlarms.single().triggerAtMs)
    }
    @Test fun renamedGroupsInvalidateProjectionAndDeletionCancelsAlarmsAndCascades() = runBlocking {
        repo.addTask(1, "Task", "Notes", System.currentTimeMillis() + 86_400_000)
        repo.updateItem(Item(1, "Renamed"))
        assertEquals("Renamed", repo.observeDashboardTasks().first().single().itemName)
        assertEquals(1, repo.observeItemsWithCounts().first().single().pendingCount)
        repo.deleteItem(Item(1, "Renamed"))
        assertTrue(repo.observeDashboardTasks().first().isEmpty())
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }
    @Test fun groupSharingStillUsesTextChooserWithNotesAndStatuses() {
        ShareUtil.share(app, "Synthetic group", listOf(Task(1, 1, "Synthetic task", "Synthetic notes", status = TaskStatus.SNOOZED)))
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action); assertEquals("text/plain", send.type)
        assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Synthetic notes"))
        assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Synthetic task"))
    }
    @Test fun datePickerUtcDayMapsToLocalDateAndKeepsExistingTime() {
        val old = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val existing = Instant.parse("2026-10-17T23:45:00Z").toEpochMilli()
            val picked = Instant.parse("2026-11-02T00:00:00Z").toEpochMilli()
            val saved = mergeDate(picked, existing)
            assertEquals(Instant.parse("2026-11-03T00:45:00Z").toEpochMilli(), saved)
            assertEquals(picked, pickerDate(saved))
        } finally { TimeZone.setDefault(old) }
    }
}
