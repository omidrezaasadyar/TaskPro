package com.taskpro.app.tasks

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.*
import com.taskpro.app.data.*
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.item.AddTaskSheet
import com.taskpro.app.ui.screens.tasks.ManagedTaskRow
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class TaskEditorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun button(text: String) = compose.onNode(hasText(text) and hasClickAction())

    @Test fun existingValuesArePrefilledAndCleanCancelDoesNotSave() {
        var saves = 0; var closed = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(Task(15, 2, "Existing title", "Existing notes", 1234567890000),
            onDismiss = { closed++ }, onSave = { _, _, _, _ -> saves++ }) } }
        compose.onNode(hasSetTextAction() and hasText("Existing title")).assertExists()
        compose.onNode(hasSetTextAction() and hasText("Existing notes")).assertExists()
        button("Cancel").performScrollTo().performClick()
        assertEquals(1, closed); assertEquals(0, saves)
        compose.onNodeWithText("Discard changes?").assertDoesNotExist()
    }
    @Test fun changedDraftRequiresDiscardOrKeepEditingAndNeverAutosaves() {
        var saves = 0; var closed = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(Task(15, 2, "Existing", "Notes"),
            onDismiss = { closed++ }, onSave = { _, _, _, _ -> saves++ }) } }
        compose.onNode(hasSetTextAction() and hasText("Existing")).performTextReplacement("Changed")
        button("Cancel").performScrollTo().performClick()
        button("Keep editing").performClick()
        assertEquals(0, closed); assertEquals(0, saves)
        button("Cancel").performScrollTo().performClick()
        button("Discard changes").performClick()
        assertEquals(1, closed); assertEquals(0, saves)
    }
    @Test fun sheetDismissOnChangedDraftUsesSameDiscardGuard() {
        var saves = 0; var closed = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(onDismiss = { closed++ }, onSave = { _, _, _, _ -> saves++ }) } }
        compose.onNodeWithText("Task").performTextInput("Draft")
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).onFirst()
            .performSemanticsAction(SemanticsActions.Dismiss) { it() }
        compose.onNodeWithText("Discard changes?").assertExists()
        assertEquals(0, saves); assertEquals(0, closed)
    }
    @Test fun creationRequiresExplicitGroupSelection() {
        var saved: Long? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(onDismiss = {},
            groups = listOf(Item(1, "First group"), Item(2, "Chosen group")),
            onSave = { group, _, _, _ -> saved = group }) } }
        compose.onNodeWithText("Task").performTextInput("New task")
        button("Add").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Choose a group").performScrollTo().performClick()
        button("Chosen group").performClick()
        button("Add").performScrollTo().performClick()
        compose.waitForIdle(); assertEquals(2L, saved)
    }
    @Test fun duplicateSaveIsBlockedWhileDatabaseIsBusy() {
        val finish = CompletableDeferred<Unit>()
        var saves = 0; var closed = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(Task(4, 1, "Existing"),
            onDismiss = { closed++ }, onSave = { _, _, _, _ -> saves++; finish.await() }) } }
        button("Save").performScrollTo().performClick()
        button("Save").assertIsNotEnabled().performClick()
        assertEquals(1, saves); assertEquals(0, closed)
        compose.runOnIdle { finish.complete(Unit) }
        compose.waitForIdle(); assertEquals(1, closed)
    }
    @Test fun failedSaveKeepsDraftAndCanBeRetriedWithoutDuplication() {
        var attempt = 0; var closed = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(Task(4, 1, "Existing"),
            onDismiss = { closed++ }, onSave = { _, title, _, _ ->
                assertEquals("Changed", title); attempt++; if (attempt == 1) error("Synthetic save failure")
            }) } }
        compose.onNode(hasSetTextAction() and hasText("Existing")).performTextReplacement("Changed")
        button("Save").performScrollTo().performClick()
        compose.onNodeWithText("Could not save.", substring = true).assertExists()
        assertEquals(0, closed)
        button("Save").performScrollTo().performClick()
        assertEquals(2, attempt); assertEquals(1, closed)
    }
    @Test fun clearingDueDateSavesNullWithoutChangingOtherFields() {
        var saved = false
        compose.setContent { TaskProTheme(ThemeMode.DARK) { AddTaskSheet(Task(4, 1, "Existing", "Notes", 1234567890000),
            onDismiss = {}, onSave = { _, title, notes, due ->
                assertEquals("Existing", title); assertEquals("Notes", notes); assertNull(due); saved = true
            }) } }
        compose.onNodeWithContentDescription("Clear reminder").performScrollTo().performClick()
        button("Save").performScrollTo().performClick(); assertTrue(saved)
    }
    @Test fun bodyCheckboxAndOverflowAreIndependent() = independentActions(false)
    @Test fun denseTimelineBodyCheckboxAndOverflowAreIndependent() = independentActions(true)
    private fun independentActions(dense: Boolean) {
        var edits = 0; val statuses = mutableListOf<TaskStatus>(); var deletes = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            ManagedTaskRow(Task(4, 1, "Synthetic row"), "Group", { edits++ }, { statuses.add(it) }, { deletes++ }, dense = dense)
        } }
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach {
            assertTrue(it.layoutInfo.coordinates.size.height >= 48)
            assertTrue(it.layoutInfo.coordinates.size.width >= 48)
        }
        compose.onNodeWithText("Synthetic row").performClick()
        assertEquals(1, edits); assertTrue(statuses.isEmpty())
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        assertEquals(listOf(TaskStatus.COMPLETED), statuses); assertEquals(1, edits)
        compose.onNodeWithContentDescription("Options for Synthetic row").performClick()
        button("Snooze").performClick()
        assertEquals(listOf(TaskStatus.COMPLETED, TaskStatus.SNOOZED), statuses)
        assertEquals(1, edits); assertEquals(0, deletes)
    }
    @Test fun draggingDisablesEditCompletionAndOverflow() {
        var calls = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            ManagedTaskRow(Task(4, 1, "Synthetic row"), "Group", { calls++ }, { calls++ }, { calls++ }, dragging = true)
        } }
        compose.onNodeWithText("Synthetic row").performClick()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        compose.onNodeWithContentDescription("Options for Synthetic row").performClick()
        assertEquals(0, calls)
    }
    @Test fun completedStatusListDoesNotPretendBodyIsEditable() {
        var status: TaskStatus? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            ManagedTaskRow(Task(4, 1, "Completed row", status = TaskStatus.COMPLETED), "Group", null, { status = it }, {})
        } }
        compose.onNodeWithText("Completed row").assertIsNotEnabled()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).performClick()
        assertEquals(TaskStatus.PENDING, status)
    }
}
