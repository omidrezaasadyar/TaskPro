package com.taskpro.app.vault

import androidx.activity.ComponentActivity
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
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class VaultUnlockUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val files = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private lateinit var device: VaultDeviceUnlock
    @Before fun setup() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        repo.unlock("synthetic master phrase".toCharArray(), create = true)
        repo.confirmRecoverySaved(); repo.lock()
        val key = SecretKeySpec(VaultCrypto.randomBytes(32), "AES")
        device = VaultDeviceUnlock(context, repo, java.io.File(files.root, "device.json"), cipherProvider = { mode, _, _, nonce ->
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                if (nonce == null) init(mode, key) else init(mode, key, GCMParameterSpec(128, nonce))
            }
        })
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) { VaultScreen(repo, VaultClipboard(context), {}, {}, device) } }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Vault is locked").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun close() { repo.lock(); db.close() }
    @Test fun masterPasswordUnlockAndSecureWindow() {
        assertTrue(compose.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        compose.onNodeWithText("Master password").performClick()
        compose.onNode(hasText("Master password") and hasSetTextAction()).performTextInput("synthetic master phrase")
        compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) { repo.state.value.document != null }
        compose.onNodeWithText("Add Item").assertExists()
    }
    @Test fun lockedLandingRemainsVisibleUntilAnUnlockMethodIsSelected() {
        compose.onNodeWithText("Vault is locked").assertIsDisplayed()
        compose.onNodeWithText("Set up PIN").assertIsDisplayed()
        compose.onNode(hasText("Master password") and hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Master password").performClick()
        compose.onNode(hasText("Master password") and hasSetTextAction()).assertExists()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNode(hasText("Master password") and hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Vault is locked").assertIsDisplayed()
        assertNull(repo.state.value.document)
    }
    @Test fun setupPinThenUnlockThroughNumericKeypad() {
        compose.onNodeWithContentDescription("Unlock settings").performClick()
        compose.onNodeWithText("Existing master password").performTextInput("synthetic master phrase")
        compose.onNodeWithText("New PIN (4–6 digits)").performTextInput("123456")
        compose.onNodeWithText("Confirm PIN").performTextInput("123456")
        compose.onNodeWithText("Save PIN").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty() }
        (1..6).forEach { compose.onNodeWithText(it.toString()).performScrollTo().performClick() }
        compose.onNodeWithText("Unlock").performScrollTo().performClick()
        compose.waitUntil(10_000) { repo.state.value.document != null }
        assertEquals(0, repo.state.value.document!!.records.size)
    }
    @Test fun wrongMasterDuringSetupExplainsExistingPasswordRequirement() {
        compose.onNodeWithContentDescription("Unlock settings").performClick()
        compose.onNodeWithText("Existing master password").performTextInput("wrong synthetic password")
        compose.onNodeWithText("New PIN (4–6 digits)").performTextInput("1234")
        compose.onNodeWithText("Confirm PIN").performTextInput("1234")
        compose.onNodeWithText("Save PIN").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("(DU01)", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("(DU01)", substring = true)).performScrollTo().assertIsDisplayed()
        assertNull(repo.state.value.document)
        assertFalse(runBlocking { device.available() }.first)
    }
}
