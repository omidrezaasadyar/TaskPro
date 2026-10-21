package com.taskpro.app.vault

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.vault.VaultScreen
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class VaultFirstRunTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        runBlocking { assertNull(db.vaultDao().get()) }
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
    }
    @After fun close() { repo.lock(); db.close() }
    private fun screen() {
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) {
            VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {})
        } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("New master password").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun freshInstallCreatesMasterThenRecoveryBeforeAnyUnlockSettings() {
        screen()
        compose.onNodeWithContentDescription("Unlock settings").assertDoesNotExist()
        compose.onNodeWithText("Existing master password").assertDoesNotExist()
        compose.onNodeWithText("New master password").performTextInput("synthetic first master")
        compose.onNodeWithText("Confirm password").performTextInput("synthetic first master")
        compose.onNode(hasText("Create master password") and hasClickAction()).performScrollTo().performClick()
        compose.waitUntil(10_000) { repo.state.value.pendingRecovery != null }
        compose.onNodeWithText("I have saved my recovery key safely").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(10_000) { repo.state.value.pendingRecovery == null }
        compose.onNodeWithText("Add Item").assertExists()
        compose.onNodeWithContentDescription("Lock vault").performClick()
        compose.onNodeWithText("Master password").performClick()
        compose.onNode(hasText("Master password") and hasSetTextAction()).performTextInput("synthetic first master")
        compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) { repo.state.value.document != null }
        assertTrue(repo.state.value.document!!.records.isEmpty())
    }
    @Test fun invalidPasswordCannotCreateAnEncryptedVault() {
        screen()
        compose.onNodeWithText("New master password").performTextInput("short")
        compose.onNodeWithText("Confirm password").performTextInput("short")
        compose.onNode(hasText("Create master password") and hasClickAction()).assertIsNotEnabled()
        assertNull(runBlocking { db.vaultDao().get() })
    }
    @Test fun existingVaultCanReachCreationAndCancelWithoutChanges() {
        runBlocking { repo.unlock("synthetic previous master".toCharArray(), create = true) }
        val original = runBlocking { db.vaultDao().get() }
        repo.lock()
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) {
            VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {})
        } }
        compose.onNodeWithText("Start a new empty vault").performClick()
        compose.onNodeWithText("New master password").performTextInput("synthetic next master")
        compose.onNodeWithText("Confirm password").performTextInput("synthetic next master")
        compose.onNode(hasText("Create master password") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Create a new empty vault?").assertExists()
        compose.onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(original, runBlocking { db.vaultDao().get() })
        assertNull(repo.state.value.document)
    }
    private fun existingVaultWithPicker(uri: android.net.Uri?) {
        runBlocking { repo.unlock("synthetic previous master".toCharArray(), create = true); repo.lock() }
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: androidx.core.app.ActivityOptionsCompat?) {
                dispatchResult(requestCode, if (uri == null) android.app.Activity.RESULT_CANCELED else android.app.Activity.RESULT_OK,
                    android.content.Intent().setData(uri))
            }
        }
        val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
            TaskProTheme(ThemeMode.LIGHT) { VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {}) }
        } }
        compose.onNodeWithText("Start a new empty vault").performClick()
        compose.onNodeWithText("New master password").performTextInput("synthetic next master")
        compose.onNodeWithText("Confirm password").performTextInput("synthetic next master")
        compose.onNode(hasText("Create master password") and hasClickAction()).performScrollTo().performClick()
    }
    @Test fun existingVaultCanCreateMasterAfterVerifiedExternalBackup() {
        val file = java.io.File.createTempFile("synthetic-vault-backup", ".json", compose.activity.cacheDir)
        existingVaultWithPicker(android.net.Uri.fromFile(file))
        val previous = runBlocking { db.vaultDao().get() }!!.envelope
        compose.onNodeWithText("Save backup and create vault").performClick()
        compose.waitUntil(10_000) { repo.state.value.document != null }
        val backup = com.taskpro.app.backup.WorkspaceBackupFormat.decode(file.readBytes(), "synthetic next master".toCharArray())
        assertEquals(previous, backup.vault)
        assertTrue(repo.state.value.document!!.records.isEmpty())
        assertNotNull(repo.state.value.pendingRecovery)
        compose.onNodeWithText("I have saved my recovery key safely").assertExists()
    }
    @Test fun cancelingExternalBackupCannotReplaceExistingVault() {
        existingVaultWithPicker(null)
        val previous = runBlocking { db.vaultDao().get() }
        compose.onNodeWithText("Save backup and create vault").performClick()
        compose.waitForIdle()
        assertEquals(previous, runBlocking { db.vaultDao().get() })
        assertNull(repo.state.value.document)
    }
}
