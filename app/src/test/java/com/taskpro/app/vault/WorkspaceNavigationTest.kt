package com.taskpro.app.vault

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.taskpro.app.MainActivity
import com.taskpro.app.TaskProApp
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class WorkspaceNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun moduleSwitchPreservesTasksAndVaultSession() {
        val app = compose.activity.application as TaskProApp
        val database = AppDatabase.getInstance(app)
        val vault = app.container.vaultRepository
        runBlocking {
            database.itemDao().insert(Item(42, "Preserved group"))
            database.taskDao().insert(Task(42, 42, "Preserved task", notes = "Preserved notes"))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tasks").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Tasks").onLast().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Groups").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Groups").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Preserved group").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Preserved group").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Preserved task").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Vault").performClick()
        runBlocking { vault.unlock("synthetic master phrase".toCharArray(), create = true) }
        runBlocking { vault.confirmRecoverySaved() }
        compose.onNodeWithContentDescription("Lock vault").assertExists()
        compose.onNodeWithText("Tasks").performClick()
        compose.waitForIdle()
        assertNotNull(vault.state.value.document)
        assertEquals(0, compose.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE)
        runBlocking {
            assertEquals("Preserved task", database.taskDao().getAll().single().title)
            assertEquals("Preserved notes", database.taskDao().getAll().single().notes)
        }
    }
}
