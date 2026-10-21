package com.taskpro.app.vault

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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
class VaultGroupDetailTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        repo.unlock("synthetic master phrase".toCharArray(), create = true)
        repo.confirmRecoverySaved()
    }

    @After fun close() { repo.lock(); db.close() }

    private fun screen(document: VaultDocument) {
        runBlocking { repo.edit { document } }
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            VaultScreen(repo, VaultClipboard(RuntimeEnvironment.getApplication()), {}, {})
        } }
        compose.waitForIdle()
    }

    private fun folder(name: String) = compose.onNode(hasText(name) and hasClickAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))

    @Test fun nestedGroupShowsImmediateChildrenDirectRecordsAndEmptyState() {
        val fixture = VaultDashboardFixtures.document()
        screen(fixture)
        folder("Banks").performClick()
        compose.onNodeWithText("Items: 1 · Subgroups: 1").assertExists()
        compose.onNodeWithText("Subgroups").assertExists()
        compose.onNodeWithText("Bank login").assertExists()
        folder("Savings").performScrollTo().performClick()
        compose.onNode(hasText("Banks / Savings") and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        compose.onNodeWithText("Savings login").assertExists()
        compose.onNodeWithText("Bank login").assertDoesNotExist()
        folder("Services").performScrollTo().performClick()
        compose.onNodeWithText("This group is empty").assertExists()
        compose.onNodeWithText("Add an item or create a subgroup here.").assertExists()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithText("Savings login").assertExists()
        assertEquals(fixture, repo.state.value.document)
    }

    @Test fun detailKeepsEveryFieldAndMasksAllSecretTypesFromSemantics() {
        val group = VaultCategory(id = "group", name = "Accounts", icon = CategoryIcon.PERSON)
        val record = VaultRecord(id = "record", categoryId = group.id, title = "Synthetic account",
            values = mapOf("url" to "https://synthetic.example.invalid", "username" to "synthetic-user",
                "email" to "person@example.invalid", "password" to "synthetic-password",
                "phone" to "+10000000000", "reference" to "REF-123", "notes" to "First line\nSecond line"),
            custom = listOf(CustomField(id = "secret", label = "Private field", value = "synthetic-private", type = FieldType.SECRET),
                CustomField(id = "public", label = "Public field", value = "Visible value")), tags = listOf("work"))
        screen(VaultDocument(listOf(group), listOf(record)))
        compose.onNodeWithText("Synthetic account").performClick()
        compose.onNodeWithText("Sign-in details").assertExists()
        compose.onNodeWithText("Account details").assertExists()
        compose.onNodeWithText("Custom fields").assertExists()
        compose.onNodeWithText("synthetic-user").assertExists()
        compose.onNodeWithText("person@example.invalid").assertExists()
        compose.onNodeWithText("REF-123").assertExists()
        compose.onNodeWithText("First line\nSecond line").assertExists()
        compose.onNodeWithText("Visible value").assertExists()
        compose.onNodeWithText("synthetic-password").assertDoesNotExist()
        compose.onNodeWithText("synthetic-private").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show Password").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Password, Revealed visually").assertExists()
        compose.onNodeWithText("synthetic-password").assertDoesNotExist()
        compose.onNodeWithContentDescription("Open Website URL").assertExists()
        compose.onNodeWithText("Vaurivo tries to clear its unchanged clipboard entry", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNodeWithText("synthetic-password").assertDoesNotExist()
        assertEquals(record, repo.state.value.document!!.records.single())
    }

    @Test fun favoriteAndOverflowActionsAreIndependentOfEditAndPreserveIdentity() {
        val record = VaultRecord(id = "stable-id", title = "Synthetic account")
        screen(VaultDocument(records = listOf(record)))
        compose.onNodeWithText("Synthetic account").performClick()
        compose.onNode(hasContentDescription("Favorite") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.records.single().favorite }
        assertEquals(record.id, repo.state.value.document!!.records.single().id)
        compose.onNodeWithText("Record title").assertDoesNotExist()
        compose.onNode(hasContentDescription("Menu") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Duplicate").performClick()
        compose.onNodeWithText("Record title").assertExists()
        compose.onNodeWithContentDescription("Cancel").performClick()
        assertEquals(1, repo.state.value.document!!.records.size)
        compose.onNodeWithContentDescription("Edit").performClick()
        compose.onNodeWithText("Record title").performTextReplacement("Renamed account")
        compose.onNodeWithText("Save to Vault").performClick()
        compose.waitUntil(10_000) { repo.state.value.document!!.records.single().title == "Renamed account" }
        assertEquals(record.id, repo.state.value.document!!.records.single().id)
        assertTrue(repo.state.value.document!!.records.single().favorite)
    }
}
