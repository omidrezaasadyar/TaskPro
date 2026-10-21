package com.taskpro.app.vault

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.taskpro.app.MainActivity
import com.taskpro.app.TaskProApp
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.data.Item
import com.taskpro.app.data.Task
import com.taskpro.app.data.RestoreJournal
import com.taskpro.app.R
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class WorkspaceLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var database: AppDatabase

    @Before fun bindDatabase() { database = AppDatabase.getInstance(compose.activity) }

    @After fun releaseTestDatabase() {
        compose.activityRule.scenario.close()
        database.close()
        // Robolectric resets SQLite pointers between tests, but not this application singleton.
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tasks() {
        awaitText("Open instantly")
        compose.onNode(hasText("Tasks") and hasClickAction()).performClick()
        awaitText("Home")
    }

    private fun openGroup() {
        val db = AppDatabase.getInstance(compose.activity)
        runBlocking {
            db.itemDao().insert(Item(81, "Synthetic retained group"))
            db.taskDao().insert(Task(81, 81, "Synthetic retained task", notes = "Preserved notes"))
        }
        tasks()
        awaitText("Groups")
        compose.onNodeWithText("Groups").performClick()
        awaitText("Synthetic retained group")
        compose.onNodeWithText("Synthetic retained group").performClick()
        awaitText("Synthetic retained task")
    }

    @Test fun freshLaunchHasOnlyChooserActionsAndActualVersion() {
        awaitText("Open instantly")
        compose.onNodeWithText("Workspaces").assertDoesNotExist()
        compose.onNodeWithText("v${com.taskpro.app.BuildConfig.VERSION_NAME}").assertExists()
        assertEquals(2, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertNull((compose.activity.application as TaskProApp).container.vaultRepository.state.value.document)
    }

    @Test fun recreatedActivityRestoresTaskDestinationAndData() {
        openGroup()
        compose.activityRule.scenario.recreate()
        awaitText("Synthetic retained task")
        compose.onNodeWithText("Open instantly").assertDoesNotExist()
        runBlocking {
            val task = AppDatabase.getInstance(compose.activity).taskDao().getAll().single()
            assertEquals(81L, task.id)
            assertEquals("Preserved notes", task.notes)
        }
    }

    @Test fun foregroundAndRecentsReturnDoNotRestartChooser() {
        openGroup()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitText("Synthetic retained task")
        compose.onNodeWithText("Open instantly").assertDoesNotExist()
    }

    @Test fun explicitlyReopeningChooserRestoresExistingTaskStackOnSelection() {
        openGroup()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithContentDescription("Workspaces").performClick()
        awaitText("Open instantly")
        compose.onNode(hasText("Tasks") and hasClickAction()).performClick()
        awaitText("Synthetic retained task")
        assertEquals(81L, runBlocking { database.taskDao().getAll().single().id })
    }

    @Test fun newActivityLaunchShowsChooserRatherThanLastTask() {
        openGroup()
        val app = compose.activity.application
        compose.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText("Open instantly")
            compose.onNodeWithText("Synthetic retained task").assertDoesNotExist()
            runBlocking { assertEquals(81L, AppDatabase.getInstance(app).taskDao().getAll().single().id) }
        }
    }

    @Test fun chooserVaultSelectionUsesFirstRunSetupWithoutCreatingData() {
        awaitText("Open instantly")
        compose.onNode(hasText("Vault") and hasClickAction()).performClick()
        awaitText("New master password")
        val repository = (compose.activity.application as TaskProApp).container.vaultRepository
        assertNull(repository.state.value.document)
        assertFalse(repository.state.value.exists)
        compose.onNodeWithText("Add Item").assertDoesNotExist()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithContentDescription("Workspaces").performClick()
        awaitText("Open instantly")
    }

    @Test fun chooserNeverBypassesExistingVaultAuthentication() {
        val repository = (compose.activity.application as TaskProApp).container.vaultRepository
        runBlocking {
            repository.unlock("synthetic launch master".toCharArray(), create = true)
            repository.confirmRecoverySaved()
        }
        repository.lock()
        awaitText("Open instantly")
        compose.onNode(hasText("Vault") and hasClickAction()).performClick()
        awaitText("Vault is locked")
        assertNull(repository.state.value.document)
        compose.onNodeWithText("Add Item").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        awaitText("Vault is locked")
        compose.onNodeWithText("Open instantly").assertDoesNotExist()
        assertNull(repository.state.value.document)
    }

    @Test fun pendingRestoreFailureBlocksChooserUntilRecoverySucceeds() {
        openGroup()
        val app = compose.activity.application
        val error = app.getString(R.string.backup_recovery_error)
        compose.activityRule.scenario.close()
        runBlocking { database.restoreJournalDao().insert(RestoreJournal(theme = "UNSUPPORTED_TEST_VALUE",
            background = "PLAIN", lastBackupAt = null, oldTaskIds = "[]")) }
        ActivityScenario.launch(MainActivity::class.java).use {
            awaitText(error)
            compose.onNodeWithText("Open instantly").assertDoesNotExist()
            compose.onNodeWithText("Secure access").assertDoesNotExist()
            runBlocking { assertEquals(81L, database.taskDao().getAll().single().id) }
            // Repair only the synthetic test fixture; the production recovery path then replays it.
            runBlocking {
                database.restoreJournalDao().clear()
                database.restoreJournalDao().insert(RestoreJournal(theme = "SYSTEM", background = "PLAIN",
                    lastBackupAt = null, oldTaskIds = "[]"))
            }
            compose.onNodeWithText(app.getString(R.string.backup_retry)).performClick()
            awaitText("Open instantly")
            assertNull(runBlocking { database.restoreJournalDao().get() })
            runBlocking { assertEquals("Preserved notes", database.taskDao().getAll().single().notes) }
        }
    }
}
