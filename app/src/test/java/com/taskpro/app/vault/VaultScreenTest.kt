package com.taskpro.app.vault

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.key.Key
import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.WorkspaceScreen
import com.taskpro.app.ui.screens.vault.VaultScreen
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w400dp-h850dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VaultScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val testName = org.junit.rules.TestName()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        // Compose advances Android's virtual clock while waiting for dialog layout.
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        repo.unlock("synthetic master phrase".toCharArray(), create = true)
        if (testName.methodName != "automaticSetupRequiresSavedConfirmation") repo.confirmRecoverySaved()
        repo.edit { it.copy(categories = listOf(VaultCategory(id = "cat", name = "Business", icon = CategoryIcon.BUSINESS)),
            records = listOf(VaultRecord(id = "rec", title = "Example account", categoryId = "cat", favorite = true,
                values = mapOf("username" to "example-user", "password" to "synthetic-hidden-password")))) }
    }
    @After fun close() { repo.lock(); db.close() }
    private fun screen() {
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) {
            VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {})
        } }
        compose.waitForIdle()
        val ready = when {
            repo.state.value.document == null -> "Vault is locked"
            repo.state.value.pendingRecovery != null -> "I have saved my recovery key safely"
            else -> "Add Item"
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(ready).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    @Test fun listsRecordsMasksSecretsAndManualLockRemovesContent() {
        screen()
        compose.onNodeWithText("Example account").assertIsDisplayed().performClick()
        compose.onNodeWithText("synthetic-hidden-password").assertDoesNotExist()
        compose.onNodeWithText("example-user").assertExists()
        compose.onNodeWithContentDescription("Show Password").performClick()
        compose.onNodeWithContentDescription("Password, Revealed visually").assertExists()
        compose.onNodeWithText("synthetic-hidden-password").assertDoesNotExist()
        compose.onNodeWithContentDescription("Hide Password").performClick()
        compose.onNodeWithText("synthetic-hidden-password").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Lock vault").performClick()
        compose.onNodeWithText("Vault is locked").assertExists()
        compose.onNodeWithText("Example account").assertDoesNotExist()
        assertNull(repo.state.value.document)
    }
    @Test fun createAndEditCategoryThroughUi() {
        screen()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.onAllNodesWithContentDescription("New category").onLast().performClick()
        compose.onNodeWithText("Category name").performTextInput("Personal")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.categories.any { it.name == "Personal" } }
        val id = repo.state.value.document!!.categories.single { it.name == "Personal" }.id
        compose.onNodeWithText("Personal").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Categories").performClick()
        compose.onAllNodesWithContentDescription("Menu").onLast().performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Category name").performTextReplacement("Renamed")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.categories.any { it.id == id && it.name == "Renamed" } }
    }
    @Test fun createRecordAndSaveSameIdOnEdit() {
        screen()
        compose.onNodeWithText("Add Item").performClick()
        compose.onNodeWithText("Record title").performTextInput("New account")
        compose.onNodeWithText("Save to Vault").performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.records.size == 2 }
        val id = repo.state.value.document!!.records.single { it.title == "New account" }.id
        compose.onAllNodesWithContentDescription("Edit").onLast().performClick()
        compose.onNodeWithText("Record title").performTextReplacement("Updated account")
        compose.onNodeWithText("Save to Vault").performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.records.any { it.id == id && it.title == "Updated account" } }
        assertEquals(2, repo.state.value.document!!.records.size)
    }
    @Test fun rendersWorkspaceAndVaultScreens() {
        screen()
        capture("vault-phone")
    }
    @Test fun automaticSetupRequiresSavedConfirmation() {
        screen()
        compose.onNodeWithText("Add Item").assertDoesNotExist()
        compose.onNodeWithText("1 group · 1 item").assertDoesNotExist()
        compose.onNodeWithText("Example account").assertDoesNotExist()
        compose.onNodeWithText("Business").assertDoesNotExist()
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Cancel").assertIsNotEnabled()
        compose.onNodeWithText("I have saved my recovery key safely").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(10_000) { repo.state.value.pendingRecovery == null }
        compose.onNodeWithText("Add Item").assertIsDisplayed()
    }
    @Test fun foldersNavigateToChildrenAndSearchAcrossGroups() {
        runBlocking { repo.edit { it.copy(categories = it.categories + VaultCategory(id = "child", name = "Servers", parentId = "cat")) } }
        screen()
        compose.onNodeWithText("2 groups · 1 item").assertIsDisplayed()
        compose.onNodeWithText("Servers").assertDoesNotExist()
        compose.onAllNodesWithText("Business").onFirst().performClick()
        compose.onNodeWithText("Servers").assertIsDisplayed()
        compose.onNodeWithText("Example account").assertIsDisplayed()
        compose.onNodeWithText("Servers").performClick()
        compose.onNodeWithText("Example account").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasContentDescription("Search")).performTextInput("Example")
        compose.onNodeWithText("Example account").assertIsDisplayed()
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardShortcutsCreateAndLock() {
        screen()
        compose.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.N); keyUp(Key.CtrlLeft) }
        compose.onNodeWithText("Record title").assertExists()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onRoot().performKeyInput { keyDown(Key.CtrlLeft); pressKey(Key.L); keyUp(Key.CtrlLeft) }
        compose.onNodeWithText("Vault is locked").assertExists()
        assertEquals(1, runBlocking { db.vaultDao().get() }?.let { encrypted ->
            val plain = VaultCrypto.open(encrypted.envelope, "synthetic master phrase".toCharArray(), "vault")
            try { VaultDocument.decode(plain).records.size } finally { plain.fill(0) }
        })
    }
    @Test fun workspaceSelectionIsFunctional() {
        var selected = ""
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) { WorkspaceScreen { selected = it.route } } }
        compose.onNodeWithText("Vault").performClick()
        assertEquals("vault", selected)
        capture("workspace")
    }
    @Test fun loginRecoveryResetsPasswordWithoutLosingRecords() {
        val recovery = runBlocking { repo.setupRecovery("synthetic master phrase".toCharArray()) }
        repo.lock(); screen()
        compose.onNodeWithText("Master password").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Vaurivo | v${com.taskpro.app.BuildConfig.VERSION_NAME}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Vaurivo | v${com.taskpro.app.BuildConfig.VERSION_NAME}").assertExists()
        capture("vault-login-light")
        compose.onNodeWithText("Forgot Password?").performClick()
        compose.onNodeWithText("Recovery key").performTextInput(recovery)
        compose.onNodeWithText("New master password").performTextInput("recovered synthetic master")
        compose.onNodeWithText("Confirm password").performTextInput("recovered synthetic master")
        val beforeRecovery = repo.state.value.generation
        compose.onNodeWithText("Reset master password").performScrollTo().performClick()
        compose.waitUntil(10_000) { repo.state.value.generation > beforeRecovery &&
            compose.onAllNodesWithText("Reset master password").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Master password").performClick()
        compose.onNode(hasText("Master password") and hasSetTextAction()).performTextInput("recovered synthetic master")
        compose.onNodeWithText("Unlock").performClick()
        compose.waitUntil(10_000) { repo.state.value.document != null }
        assertEquals("Example account", repo.state.value.document!!.records.single().title)
    }
    @Test fun recoverySetupFromSecurityRequiresCurrentPassword() {
        screen()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Vault security").performClick()
        compose.onNodeWithText("Recovery Key Management").performScrollTo().performClick()
        compose.onNodeWithText("Current master password").performTextInput("synthetic master phrase")
        compose.onNodeWithText("Generate recovery key").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("I have saved my recovery key safely").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(org.json.JSONObject(runBlocking { db.vaultDao().get() }!!.envelope).has("recovery"))
    }
    @Test @Config(qualifiers = "fa-rIR-w320dp-h640dp-mdpi") fun loginCompactRtlLargeText() {
        repo.lock()
        compose.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
            TaskProTheme(ThemeMode.DARK) { VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {}) }
        } }
        val forgot = compose.activity.getString(com.taskpro.app.R.string.vault_forgot)
        compose.onNodeWithText(compose.activity.getString(com.taskpro.app.R.string.unlock_master)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(forgot).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(forgot).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vaurivo | v${com.taskpro.app.BuildConfig.VERSION_NAME}").performScrollTo().assertIsDisplayed()
        capture("vault-login-rtl-large")
    }
    @Test fun workspaceDarkPreview() {
        compose.setContent { TaskProTheme(ThemeMode.DARK) { WorkspaceScreen {} } }
        capture("workspace-dark")
    }
    @Test @Config(qualifiers = "fa-rIR-w320dp-h640dp-mdpi") fun dashboardCompactRtlLargeText() {
        compose.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
            TaskProTheme(ThemeMode.DARK) { VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {}) }
        } }
        compose.onNodeWithText(compose.activity.getString(com.taskpro.app.R.string.vault_add_item)).assertIsDisplayed()
        capture("vault-dashboard-rtl-large")
    }
    @Test @Config(qualifiers = "fa-rIR-w1000dp-h850dp-mdpi") fun rendersRtlTablet() {
        compose.setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            TaskProTheme(ThemeMode.DARK) { VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {}) }
        } }
        compose.waitForIdle()
        capture("vault-rtl-tablet")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/workspace/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
