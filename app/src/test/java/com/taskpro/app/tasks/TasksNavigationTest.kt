package com.taskpro.app.tasks

import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.*
import com.taskpro.app.MainActivity
import com.taskpro.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TasksNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var db: AppDatabase
    @Before fun setup() = runBlocking {
        db = AppDatabase.getInstance(compose.activity)
        db.itemDao().insert(Item(9, "Synthetic group"))
        db.taskDao().insertAll(listOf(Task(20, 9, "First manual", "Notes", position = 0), Task(21, 9, "Second manual", position = 1)))
        Unit
    }
    @After fun close() {
        compose.activityRule.scenario.close(); db.close()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
    private fun await(text: String) { try { compose.waitUntil(10_000) {
        shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.MILLISECONDS)
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }; compose.mainClock.advanceTimeBy(500)
        shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.MILLISECONDS)
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
        println("Awaiting $text. Synthetic test UI: " + compose.onRoot().printToString())
        throw e
    } }
    private fun enter() {
        await("Open instantly")
        compose.onNode(hasText("Tasks") and hasClickAction()).performClick()
        await("Timeline")
    }
    private fun group() {
        enter(); compose.onNodeWithText("Groups").performClick()
        await("Synthetic group"); compose.onNodeWithText("Synthetic group").performClick(); await("First manual")
    }
    private fun button(text: String) = compose.onNode(hasText(text) and hasClickAction())
    @Test fun timelineIsDefaultAndGroupsIsOneTapWithPredictableBackAndTaskTab() {
        enter()
        compose.onNodeWithText("Timeline").assertIsSelected()
        compose.onNodeWithText("Groups").performClick()
        await("Synthetic group")
        compose.onNodeWithText("Groups").assertIsSelected()
        compose.onNode(hasText("Tasks") and isSelected()).assertExists()
        compose.onNodeWithText("Synthetic group").performClick(); await("First manual")
        compose.onNodeWithText("Tasks").performClick(); await("Timeline")
        compose.onNodeWithText("Timeline").assertIsSelected()
        compose.onNodeWithText("Groups").performClick(); await("Synthetic group")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        await("Timeline"); compose.onNodeWithText("Timeline").assertIsSelected()
    }
    @Test fun groupRenameUsesExistingViewModelAndCountsRemainLive() {
        enter(); compose.onNodeWithText("Groups").performClick(); await("Synthetic group")
        compose.onNodeWithContentDescription("Options for Synthetic group").performClick()
        button("Rename group").performClick()
        compose.mainClock.advanceTimeBy(500)
        shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.MILLISECONDS)
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetText) { it(androidx.compose.ui.text.AnnotatedString("Renamed group")) }
        button("Save").performClick()
        await("Renamed group")
        val item = runBlocking { db.itemDao().getAll().single() }
        assertEquals(9L, item.id); assertEquals("Renamed group", item.name)
        assertEquals(2, runBlocking { db.taskDao().getAll().size })
    }
    @Test fun groupDeleteRequiresConfirmationAndCancelKeepsTasks() {
        enter(); compose.onNodeWithText("Groups").performClick(); await("Synthetic group")
        compose.onNodeWithContentDescription("Options for Synthetic group").performClick()
        button("Delete").performClick()
        button("Cancel").performClick()
        assertEquals(2, runBlocking { db.taskDao().getAll().size })
        compose.onNodeWithContentDescription("Options for Synthetic group").performClick()
        button("Delete").performClick()
        button("Delete").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.MILLISECONDS)
            runBlocking { db.taskDao().getAll().isEmpty() }
        }
        assertTrue(runBlocking { db.itemDao().getAll().isEmpty() })
    }
    @Test fun groupCreationAppearsImmediatelyAndKeepsExistingTasks() {
        enter(); compose.onNodeWithText("Groups").performClick(); await("Synthetic group")
        button("New group").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Created group")
        button("Add").performClick(); await("Created group")
        assertEquals(2, runBlocking { db.itemDao().getAll().size })
        assertEquals(2, runBlocking { db.taskDao().getAll().size })
        assertEquals("Synthetic group", runBlocking { db.itemDao().getAll().single { it.id == 9L }.name })
    }
    @Test fun timelineCreatesNewTaskOnlyInExplicitlyChosenGroup() {
        enter(); await("New task"); button("New task").performClick(); await("Choose a group")
        compose.onNodeWithText("Task").performTextInput("Created in Timeline")
        button("Add").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Choose a group").performScrollTo().performClick()
        compose.onNode(hasText("Synthetic group") and hasClickAction() and hasAnyAncestor(isPopup())).performClick()
        button("Add").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idleFor(20, TimeUnit.MILLISECONDS)
            runBlocking { db.taskDao().getAll().size == 3 }
        }
        val created = runBlocking { db.taskDao().getAll().single { it.title == "Created in Timeline" } }
        assertEquals(9L, created.itemId); assertEquals(2, created.position)
        assertEquals(TaskStatus.PENDING, created.status)
    }
    @Test fun pendingEditPreservesIdAndGroupAndCancelLeavesRoomUntouched() {
        group(); compose.onNodeWithText("First manual").performClick(); await("Edit task")
        compose.onNode(hasSetTextAction() and hasText("First manual")).performTextReplacement("Edited task")
        button("Cancel").performScrollTo().performClick(); button("Discard changes").performClick()
        assertEquals("First manual", runBlocking { db.taskDao().getById(20)!!.title })
        compose.onNodeWithText("First manual").performClick(); await("Edit task")
        compose.onNode(hasSetTextAction() and hasText("First manual")).performTextReplacement("Edited task")
        button("Save").performScrollTo().performClick(); await("Edited task")
        val saved = runBlocking { db.taskDao().getById(20)!! }
        assertEquals(9L, saved.itemId); assertEquals(0, saved.position)
        assertEquals("Notes", saved.notes); assertEquals(2, runBlocking { db.taskDao().getAll().size })
    }
    @Test fun accessibleManualMovePersistsOnlyPositionsAndDoesNotOpenEditor() {
        group()
        compose.onNodeWithContentDescription("Options for First manual").performClick()
        button("Move down").performClick()
        compose.waitUntil(10_000) { runBlocking { db.taskDao().getById(20)!!.position == 1 } }
        assertEquals(0, runBlocking { db.taskDao().getById(21)!!.position })
        compose.onNodeWithText("Edit task").assertDoesNotExist()
    }
    @Test fun longPressDragReleaseDoesNotOpenEditing() {
        group()
        val start = compose.onNodeWithText("First manual").fetchSemanticsNode().boundsInRoot.center
        val end = compose.onNodeWithText("Second manual").fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(start); advanceEventTime(700)
            moveTo(start.copy(y = start.y + 1), delayMillis = 16)
            moveTo(end, delayMillis = 500); up()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Edit task").assertDoesNotExist()
        compose.waitUntil(10_000) { runBlocking { db.taskDao().getById(20)!!.position == 1 } }
        assertEquals(0, runBlocking { db.taskDao().getById(21)!!.position })
    }
}
