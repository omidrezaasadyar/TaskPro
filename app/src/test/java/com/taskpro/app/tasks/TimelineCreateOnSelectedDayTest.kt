package com.taskpro.app.tasks

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.taskpro.app.data.*
import com.taskpro.app.notification.ReminderScheduler
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.dashboard.DashboardMoment
import com.taskpro.app.ui.screens.tasks.*
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h1000dp-mdpi")
class TimelineCreateOnSelectedDayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var model: TasksViewModel
    private val now = Instant.parse("2026-10-17T10:00:00Z").toEpochMilli()

    @Before fun setup() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        db.itemDao().insert(Item(1, "Synthetic group"))
        model = TasksViewModel(TaskRepository(db.itemDao(), db.taskDao(), ReminderScheduler(app)),
            SavedStateHandle(), flowOf(DashboardMoment(now, "UTC")))
    }
    @After fun close() { db.close() }

    @Test fun futureDayNewTaskSavesOnThatDayWithoutOpeningSetDate() {
        compose.setContent {
            TaskProTheme(ThemeMode.DARK) {
                TimelineRoute({}, {}, { _, _ -> }, model)
            }
        }
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Sunday 18 October 2026").performClick()
        compose.onNodeWithText("New task").performClick()
        compose.onNodeWithText("Set date").assertDoesNotExist()
        compose.onNodeWithText("Task").performTextInput("Future fixture")
        compose.onNodeWithText("Choose a group").performScrollTo().performClick()
        compose.onNodeWithText("Synthetic group").performClick()
        compose.onNodeWithText("Add").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Future fixture").assertExists()
        val saved = runBlocking { db.taskDao().getAll() }
        assertEquals(1, saved.size)
        assertEquals("Future fixture", saved.single().title)
        val selected = model.selectedDay.value!!
        val (start, end) = dayBounds(selected, TimeZone.getTimeZone("UTC"))
        assertTrue(saved.single().dueAt!! in start until end)
    }
}
