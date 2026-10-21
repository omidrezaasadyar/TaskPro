package com.taskpro.app.vault

import android.content.ClipboardManager
import android.content.Intent
import android.view.WindowManager
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class VaultExternalIntentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private val sampleUrl = "https://synthetic.example.invalid/account"

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        repo.unlock("synthetic master phrase".toCharArray(), create = true)
        repo.confirmRecoverySaved()
    }
    @After fun close() { repo.lock(); db.close() }

    private fun openRecord(values: Map<String, String> = emptyMap(), custom: List<CustomField> = emptyList()) {
        runBlocking { repo.edit { it.copy(records = listOf(VaultRecord(title = "Synthetic account", values = values, custom = custom))) } }
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) {
            VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {})
        } }
        compose.onNodeWithText("Synthetic account").performClick()
    }

    private fun assertNoExternalAction() {
        compose.onAllNodesWithContentDescription("Open website").assertCountEquals(0)
        assertNull(shadowOf(compose.activity).nextStartedActivity)
    }

    @Test fun standardWebsiteOpensOnlyViewIntentWithoutChangingVault() {
        openRecord(values = mapOf("url" to sampleUrl))
        val before = runBlocking { db.vaultDao().get() }
        compose.onNodeWithContentDescription("Open website").performScrollTo().performClick()
        val intent = checkNotNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(sampleUrl, intent.dataString)
        assertTrue(intent.extras == null || intent.extras!!.isEmpty)
        assertEquals(before, runBlocking { db.vaultDao().get() })
        assertNotNull(repo.state.value.document)
    }

    @Test fun nonSensitiveCustomUrlAllowsHttpWebsiteOpening() {
        val url = "http://synthetic.example.invalid/login"
        openRecord(custom = listOf(CustomField(label = "Public website", value = url, type = FieldType.URL)))
        compose.onNodeWithContentDescription("Open website").performScrollTo().performClick()
        val intent = checkNotNull(shadowOf(compose.activity).nextStartedActivity)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(url, intent.dataString)
    }

    @Test fun passwordUrlCannotOpenWhenMaskedRevealedOrCopiedAndLockStillClearsIt() {
        openRecord(values = mapOf("password" to sampleUrl))
        compose.onNodeWithText(sampleUrl).assertDoesNotExist()
        assertNoExternalAction()
        compose.onNodeWithContentDescription("Show or hide value").performScrollTo().performClick()
        compose.onNodeWithText(sampleUrl).assertExists()
        assertNoExternalAction()
        compose.onNodeWithContentDescription("Copy").performScrollTo().performClick()
        val manager = compose.activity.getSystemService(ClipboardManager::class.java)
        compose.runOnIdle { assertEquals(sampleUrl, manager.primaryClip!!.getItemAt(0).text.toString()) }
        assertNoExternalAction()
        compose.onNodeWithContentDescription("Show or hide value").performClick()
        compose.onNodeWithText(sampleUrl).assertDoesNotExist()
        assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Lock vault").performClick()
        compose.onNodeWithText("Vault is locked").assertExists()
        compose.onNodeWithText("Synthetic account").assertDoesNotExist()
        compose.onNodeWithText(sampleUrl).assertDoesNotExist()
        assertNull(repo.state.value.document)
        assertNoExternalAction()
    }

    @Test fun sensitiveCustomUrlCannotOpenBeforeOrAfterReveal() {
        openRecord(custom = listOf(CustomField(label = "Sensitive URL", value = sampleUrl, type = FieldType.URL, sensitive = true)))
        compose.onNodeWithText(sampleUrl).assertDoesNotExist()
        assertNoExternalAction()
        compose.onNodeWithContentDescription("Show or hide value").performScrollTo().performClick()
        compose.onNodeWithText(sampleUrl).assertExists()
        assertNoExternalAction()
    }

    @Test fun secretTypeCannotOpenEvenWithSensitiveFlagFalse() {
        openRecord(custom = listOf(CustomField(label = "Secret value", value = sampleUrl, type = FieldType.SECRET, sensitive = false)))
        compose.onNodeWithText(sampleUrl).assertDoesNotExist()
        assertNoExternalAction()
        compose.onNodeWithContentDescription("Show or hide value").performScrollTo().performClick()
        compose.onNodeWithText(sampleUrl).assertExists()
        assertNoExternalAction()
    }

    @Test fun nonUrlFieldsDoNotExposeExternalActionsForUrlShapedValues() {
        openRecord(values = listOf("username", "email", "phone", "reference", "notes").associateWith { sampleUrl },
            custom = listOf(
                CustomField(label = "Text", value = sampleUrl, type = FieldType.TEXT),
                CustomField(label = "Notes", value = sampleUrl, type = FieldType.NOTES),
                CustomField(label = "Email", value = "$sampleUrl/user@example.invalid", type = FieldType.EMAIL),
                CustomField(label = "Number", value = "42", type = FieldType.NUMBER),
                CustomField(label = "Date", value = "2026-10-20", type = FieldType.DATE)
            ))
        assertNoExternalAction()
    }
}
