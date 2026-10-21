package com.taskpro.app.vault

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VaultDashboardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private var clock = 1000L
    private val password = "synthetic master phrase"
    private val fixtures = VaultDashboardFixtures.document()
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { clock }
        repo.unlock(password.toCharArray(), create = true); repo.confirmRecoverySaved()
        repo.edit { fixtures }
    }
    @After fun close() { repo.lock(); db.close() }
    private fun screen() {
        val clipboard = VaultClipboard(compose.activity)
        compose.setContent { TaskProTheme(ThemeMode.DARK) { VaultScreen(repo, clipboard, {}, {}) } }
        compose.waitForIdle()
    }
    private fun button(text: String) = compose.onNode(hasText(text) and hasClickAction())
    private fun folder(text: String) = compose.onNode(hasText(text) and hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
    private fun topMenu(label: String) { compose.onNode(hasScrollToNodeAction()).performScrollToIndex(0); compose.onNodeWithContentDescription("Menu").performClick(); button(label).performClick() }
    private fun folderMenu(name: String, action: String) {
        compose.onNodeWithContentDescription("Options for $name").performScrollTo().performClick()
        button(action).performClick()
    }
    private fun scrollTo(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun query(value: String) { compose.onNode(hasScrollToNodeAction()).performScrollToIndex(0); compose.onNode(hasSetTextAction() and hasContentDescription("Search")).performTextReplacement(value) }
    private fun waitFor(check: () -> Boolean) { compose.waitUntil(10_000, check); compose.waitForIdle() }

    @Test fun actualOverviewAndDirectFolderCountsUpdateAfterAuthorizedChanges() {
        screen()
        compose.onNodeWithText("6 groups · 5 items").assertExists()
        compose.onNodeWithContentDescription("6 Total Groups, 5 Total Saved Items").assertExists()
        folder("Banks").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Items: 1 · Subgroups: 1"))
        runBlocking { repo.edit { it.copy(categories = it.categories + VaultCategory(name = "Added live"),
            records = it.records + VaultRecord(title = "Added item")) } }
        compose.onNodeWithText("7 groups · 6 items").assertExists()
        scrollTo("Added live"); compose.onNodeWithText("Added live").assertIsDisplayed()
        assertEquals(fixtures.categories.take(4), repo.state.value.document!!.categories.take(4))
    }
    @Test fun nestedFoldersParentAllRecordsAndUncategorizedRemainReachable() {
        screen(); folder("Banks").performClick()
        folder("Savings").performScrollTo().performClick()
        compose.onNode(hasText("Banks / Savings") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        scrollTo("Savings login"); button("Savings login").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
        compose.onNode(hasText("Banks") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        compose.activity.onBackPressedDispatcher.onBackPressed(); compose.waitForIdle()
        topMenu("Categories")
        compose.onNode(hasText("Uncategorized") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        scrollTo("Unfiled account"); button("Unfiled account").assertIsDisplayed()
        compose.onNodeWithText("Bank login").assertDoesNotExist()
        scrollTo("All records"); button("All records").performClick()
        scrollTo("Bank login"); button("Bank login").assertIsDisplayed()
    }
    @Test fun favoritesNavigateWithoutChangingUsageTimestampsAndFavoriteActionIsIndependent() {
        screen(); scrollTo("Personal email")
        val before = repo.state.value.document!!.records
        button("Personal email").performClick()
        compose.onNodeWithText("synthetic-user-0").assertExists()
        assertEquals(before, repo.state.value.document!!.records)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNode(hasContentDescription("Favorite") and hasAnyAncestor(hasText("Personal email"))).performClick()
        waitFor { repo.state.value.document!!.records.none { it.favorite } }
        compose.onNodeWithText("Record title").assertDoesNotExist()
        assertEquals(before.map { it.id }, repo.state.value.document!!.records.map { it.id })
    }
    @Test fun searchUsesExistingAllowedFieldsAndNeverPreviewsSecrets() {
        screen(); query("private-custom-0")
        compose.onNodeWithText("Personal email").assertDoesNotExist()
        query("private-note-0"); compose.onNodeWithText("Personal email").assertDoesNotExist()
        query("synthetic-secret-0"); compose.onNodeWithText("Personal email").assertDoesNotExist()
        query("synthetic-user-2"); scrollTo("Savings login"); button("Savings login").assertIsDisplayed()
        compose.onNode(hasText("synthetic-user-2") and !hasSetTextAction()).assertDoesNotExist()
        query("Services"); folder("Services").assertExists()
        compose.onNodeWithText("Search across all groups").assertExists()
    }
    @Test fun tagsFavoritesSortAndResetKeepTheirExistingSemantics() {
        screen(); compose.onNodeWithContentDescription("Filters and sorting").performClick()
        compose.onNodeWithText("Filter by tag").performTextInput("work")
        button("Sort: Name").performClick(); button("Date created").performClick()
        button("OK").performClick()
        scrollTo("Business portal"); button("Business portal").assertIsDisplayed()
        compose.onNodeWithText("Personal email").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filters and sorting").performScrollTo().performClick()
        button("Reset filters").performClick()
        button("Favorites only").performClick(); button("OK").performClick()
        scrollTo("Personal email"); button("Personal email").assertIsDisplayed()
        compose.onNodeWithText("Business portal").assertDoesNotExist()
    }
    @Test fun addItemKeepsSelectedGroupAndDoesNotDuplicateOnSave() {
        screen(); folder("Banks").performClick(); folder("Savings").performScrollTo().performClick()
        button("Add Item").performClick()
        compose.onNodeWithText("Record title").performTextInput("New nested account")
        compose.onNodeWithText("Save to Vault").performClick()
        waitFor { repo.state.value.document!!.records.size == 6 }
        assertEquals(VaultDashboardFixtures.id("savings"), repo.state.value.document!!.records.single { it.title == "New nested account" }.categoryId)
    }
    @Test fun groupRenameColorIconParentAndReorderPreserveIdentityAndRecords() {
        screen(); folderMenu("Banks", "Edit")
        compose.onNodeWithText("Category name").performTextReplacement("Renamed bank")
        compose.onNode(hasSetTextAction() and hasText("Color (#RRGGBB)")).performTextReplacement("010203")
        button("Icon: Banks").performClick(); button("Servers").performClick()
        button("Parent category: All records").performClick()
        compose.onNode(hasText("Business") and hasClickAction() and hasAnyAncestor(isPopup())).performClick()
        button("Save").performClick()
        waitFor { repo.state.value.document!!.categories.any { it.name == "Renamed bank" } }
        val renamed = repo.state.value.document!!.categories.single { it.name == "Renamed bank" }
        assertEquals(VaultDashboardFixtures.id("banks"), renamed.id)
        assertEquals(0xff010203.toInt(), renamed.color); assertEquals(CategoryIcon.SERVER, renamed.icon)
        assertEquals(VaultDashboardFixtures.id("business"), renamed.parentId)
        assertEquals(fixtures.records, repo.state.value.document!!.records)
        folderMenu("Business", "Move up")
        waitFor { repo.state.value.document!!.categories.single { it.name == "Business" }.position < 3 }
        assertEquals(fixtures.records, repo.state.value.document!!.records)
    }
    @Test fun deletingGroupWithConfirmationPreservesSubtreeRecordsAsUncategorized() {
        screen(); val before = repo.state.value.document!!.records
        folderMenu("Banks", "Delete")
        button("Cancel").performClick(); assertEquals(6, repo.state.value.document!!.categories.size)
        folderMenu("Banks", "Delete"); button("Delete").performClick()
        waitFor { repo.state.value.document!!.categories.size == 3 }
        assertEquals(before.map { it.id }, repo.state.value.document!!.records.map { it.id })
        assertTrue(repo.state.value.document!!.records.filter { it.title in listOf("Bank login", "Savings login") }.all { it.categoryId == null })
        assertEquals(before.map { it.values }, repo.state.value.document!!.records.map { it.values })
        assertEquals(before.map { it.custom }, repo.state.value.document!!.records.map { it.custom })
        assertEquals(before.map { it.tags }, repo.state.value.document!!.records.map { it.tags })
    }
    @Test fun allFourSortModesOrderTheSameAllowedSearchResults() {
        screen(); query("login")
        val modes = listOf("Name", "Recently modified", "Date created", "Category")
        modes.forEachIndexed { i, label ->
            compose.onNodeWithContentDescription("Filters and sorting").performScrollTo().performClick()
            button("Sort: ${if (i == 0) "Name" else modes[i - 1]}").performClick()
            compose.onNode(hasText(label) and hasClickAction() and hasAnyAncestor(isPopup())).performClick()
            button("OK").performClick()
            val bank = button("Bank login").fetchSemanticsNode().boundsInRoot.top
            val savings = button("Savings login").fetchSemanticsNode().boundsInRoot.top
            assertEquals(label, i == 2, savings < bank)
        }
    }
    @Test fun newGroupUsesSelectedParentAndKeepsExistingCustomGroups() {
        screen(); folder("Banks").performClick(); button("New group").performClick()
        compose.onNodeWithText("Category name").performTextInput("Another child")
        button("Save").performClick()
        waitFor { repo.state.value.document!!.categories.size == 7 }
        val added = repo.state.value.document!!.categories.single { it.name == "Another child" }
        assertEquals(VaultDashboardFixtures.id("banks"), added.parentId)
        java.util.UUID.fromString(added.id)
        assertEquals(fixtures.categories, repo.state.value.document!!.categories.filterNot { it.id == added.id })
        folder("Another child").assertIsDisplayed()
    }
    @Test fun optionalStarterGroupsRemainOptionalAndUseExistingCreationFlow() {
        runBlocking { repo.edit { VaultDocument() } }
        screen(); assertTrue(repo.state.value.document!!.categories.isEmpty())
        topMenu("Categories")
        button(compose.activity.getString(com.taskpro.app.R.string.vault_starter)).performClick()
        waitFor { repo.state.value.document!!.categories.size == 5 }
        assertTrue(repo.state.value.document!!.categories.all { it.parentId == null })
        assertTrue(repo.state.value.document!!.records.isEmpty())
        repo.state.value.document!!.categories.forEach { java.util.UUID.fromString(it.id) }
    }
    @Test fun capturedSearchFavoriteAndCreateCallbacksCannotAffectANewSession() {
        screen()
        val input = compose.onNode(hasSetTextAction() and hasContentDescription("Search"))
        val staleSearch = input.fetchSemanticsNode().config[SemanticsActions.SetText].action!!
        val staleNew = button("Add Item").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        scrollTo("Personal email")
        val staleFavorite = compose.onNode(hasContentDescription("Favorite") and hasAnyAncestor(hasText("Personal email")))
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        val before = repo.state.value.document
        repo.lock(); compose.waitForIdle(); runBlocking { repo.unlock(password.toCharArray()) }; compose.waitForIdle()
        compose.runOnIdle { staleSearch(AnnotatedString("Bank")); staleNew(); staleFavorite() }
        compose.onNode(hasSetTextAction() and hasContentDescription("Search")).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("Record title").assertDoesNotExist()
        assertEquals(before, repo.state.value.document)
    }
    @Test fun lockAndExpiredSessionRemoveMetadataAndInvalidateCapturedActions() {
        screen()
        val stale = folder("Banks").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.onNodeWithContentDescription("Lock vault").performClick()
        compose.onNodeWithText("6 groups · 5 items").assertDoesNotExist()
        compose.onNodeWithText("Banks").assertDoesNotExist()
        runBlocking { repo.unlock(password.toCharArray()) }
        compose.runOnIdle { stale() }
        compose.onNodeWithText("Savings").assertDoesNotExist()
        assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        clock += 301_000; repo.checkInactivity(); compose.waitForIdle()
        compose.onNodeWithText("6 groups · 5 items").assertDoesNotExist()
        compose.onNodeWithText("Personal email").assertDoesNotExist()
    }

}
