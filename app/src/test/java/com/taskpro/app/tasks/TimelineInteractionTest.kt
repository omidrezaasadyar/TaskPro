package com.taskpro.app.tasks

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.taskpro.app.data.*
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.tasks.*
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h1000dp-mdpi")
class TimelineInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-17T10:00:00Z").toEpochMilli()
    private val rows = listOf(
        TaskWithItemName(Task(1, 1, "Today pending", notes = "Memo", dueAt = now + 60000), "Group"),
        TaskWithItemName(Task(2, 1, "Tomorrow pending", dueAt = now + 86_400_000), "Group"),
        TaskWithItemName(Task(3, 1, "Undated pending"), "Group"),
        TaskWithItemName(Task(4, 1, "Completed fixture", dueAt = now, status = TaskStatus.COMPLETED), "Group"),
        TaskWithItemName(Task(5, 1, "Snoozed fixture", dueAt = now, status = TaskStatus.SNOOZED), "Group"))
    private fun screen() { compose.setContent { TaskProTheme(ThemeMode.DARK) {
        var selected by rememberSaveable { mutableStateOf<Long?>(null) }
        TimelineScreen(TasksState(rows, emptyList(), now, "UTC", false), {}, {}, {}, {}, { _, _ -> }, {}, {},
            selected, { selected = it })
    } } }
    private fun button(text: String) = compose.onNode(hasText(text) and hasClickAction())
    private fun menu(text: String) = compose.onNode(hasText(text) and hasClickAction() and hasAnyAncestor(isPopup()))
    private fun openFilters() { compose.onNodeWithContentDescription("Task filters").performClick() }
    private fun closeFilters() { compose.onNodeWithContentDescription("Close filters").performClick() }
    private fun choose(label: String, value: String) { button(label).performClick(); menu(value).performClick() }
    private fun date(value: String) { openFilters(); choose("Dates", value); closeFilters() }
    private fun status(value: String) { openFilters(); choose("Status", value); closeFilters() }
    @Test fun dateHeaderShowsActualDailyProgressAndSwitchesWithWeek() {
        screen()
        compose.onNodeWithText("3 tasks · 1 done").assertExists()
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Sunday 18 October 2026").performClick()
        compose.onNodeWithText("1 task · 0 done").assertExists()
        compose.onNodeWithText("Tomorrow pending").assertExists()
    }
    @Test fun weekSwipeTodayAndMonthLabelTrackSelectedDay() {
        screen()
        compose.onNodeWithText("October 2026").assertExists()
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Saturday, 24 Oct").assertExists()
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeRight() }
        compose.onNodeWithText("Saturday, 17 Oct").assertExists()
        button("Today").performClick()
        compose.onNodeWithText("Today pending").assertExists()
    }
    @Test fun rtlSwipeRightMovesToNextWeek() {
        compose.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            TaskProTheme(ThemeMode.DARK) {
                var selected by rememberSaveable { mutableStateOf<Long?>(null) }
                TimelineScreen(TasksState(rows, emptyList(), now, "UTC", false), {}, {}, {}, {}, { _, _ -> }, {}, {},
                    selected, { selected = it })
            }
        } }
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeRight() }
        compose.onNodeWithText("Saturday, 24 Oct").assertExists()
    }
    @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    @Test fun narrowWeekStripStillPagesByWeek() {
        screen()
        compose.onNodeWithTag("tasksWeekStrip").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Saturday, 24 Oct").assertExists()
    }
    @Test fun undatedTodayAllDatesAndWeekControlsWork() {
        screen()
        date("Undated")
        compose.onNodeWithText("Undated pending").assertExists()
        compose.onNodeWithText("Today pending").assertDoesNotExist()
        button("Today").performClick()
        compose.onNodeWithText("Today pending").assertExists()
        compose.onNodeWithText("Undated pending").assertDoesNotExist()
        compose.onNodeWithContentDescription("Choose date").performClick(); button("Next week").performClick()
        compose.onNodeWithText("No tasks in this view").assertExists()
        compose.onNodeWithContentDescription("Choose date").performClick(); button("Previous week").performClick()
        compose.onNodeWithText("Today pending").assertExists()
        date("All dates")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Tomorrow pending"))
        compose.onNodeWithText("Tomorrow pending").assertIsDisplayed()
    }
    @Test fun realStatusFiltersExposeCompletedAndSnoozedAndReversibleNotesFilter() {
        screen()
        status("Completed")
        compose.onNodeWithText("Completed fixture").assertExists()
        compose.onNodeWithText("Today pending").assertDoesNotExist()
        status("Snoozed")
        compose.onNodeWithText("Snoozed fixture").assertExists()
        status("All statuses")
        openFilters()
        button("With notes").performClick()
        closeFilters()
        compose.onNodeWithText("Today pending").assertExists()
        compose.onNodeWithText("Snoozed fixture").assertDoesNotExist()
        openFilters()
        button("With notes").assertIsOn().performClick()
        button("Has due date").performClick()
        choose("Dates", "Undated")
        closeFilters()
        compose.onNodeWithText("No tasks in this view").assertExists()
    }
    @Test fun defaultHeaderHasNoLargeFilterControlsAndResetRestoresAllDates() {
        screen()
        button("All dates").assertDoesNotExist()
        button("Status").assertDoesNotExist()
        button("Undated").assertDoesNotExist()
        openFilters()
        compose.onNodeWithContentDescription("Reset filters").assertIsNotEnabled()
        choose("Dates", "Undated")
        choose("Status", "Pending")
        button("With notes").performClick()
        button("Has due date").performClick()
        closeFilters()
        compose.onNodeWithText("Undated · Pending · With notes · Has due date").assertExists()
        compose.onNodeWithContentDescription("Task filters").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "Active filters: Undated · Pending · With notes · Has due date"))
        openFilters()
        button("With notes").assertIsOn()
        button("Has due date").assertIsOn()
        compose.onNodeWithContentDescription("Reset filters").performClick()
        button("With notes").assertIsOff()
        button("Has due date").assertIsOff()
        button("Dates").assertTextContains("All dates")
        button("Status").assertTextContains("All statuses")
        closeFilters()
        compose.onNodeWithContentDescription("Task filters").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "All dates"))
        compose.onNodeWithText("Today pending").assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Undated pending"))
        compose.onNodeWithText("Undated pending").assertIsDisplayed()
    }
    @Test fun selectedDayScopeStatusAndTogglesSurviveSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { TaskProTheme(ThemeMode.DARK) {
            var selected by rememberSaveable { mutableStateOf<Long?>(null) }
            TimelineScreen(TasksState(rows, emptyList(), now, "UTC", false), {}, {}, {}, {}, { _, _ -> }, {}, {},
                selected, { selected = it })
        } }
        compose.onNodeWithContentDescription("Saturday 17 October 2026").performClick()
        openFilters()
        choose("Status", "Pending")
        button("With notes").performClick()
        closeFilters()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Selected date · 17 Oct 2026 · Pending · With notes").assertExists()
        compose.onNodeWithText("Today pending").assertExists()
        compose.onNodeWithText("Tomorrow pending").assertDoesNotExist()
        compose.onNodeWithText("Completed fixture").assertDoesNotExist()
        openFilters()
        button("With notes").assertIsOn()
        button("Dates").assertTextContains("Selected date · 17 Oct 2026")
    }
    @Test fun filterPanelRetainsSearchAction() {
        var searches = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            var selected by rememberSaveable { mutableStateOf<Long?>(null) }
            TimelineScreen(TasksState(rows, emptyList(), now, "UTC", false), {}, { searches++ }, {}, {}, { _, _ -> }, {}, {},
                selected, { selected = it })
        } }
        openFilters()
        button("Search").performClick()
        assertEquals(1, searches)
        compose.onNodeWithContentDescription("Close filters").assertDoesNotExist()
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardCanSwitchToGroupsWithoutSelectingTask() {
        var groups = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { TasksViewSelector(false, {}, { groups++ }) } }
        compose.onNodeWithText("Groups").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithText("Groups").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, groups)
    }
}
