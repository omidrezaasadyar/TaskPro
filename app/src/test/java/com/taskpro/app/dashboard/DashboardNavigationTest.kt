package com.taskpro.app.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import com.taskpro.app.MainActivity
import com.taskpro.app.TaskProApp
import com.taskpro.app.data.*
import com.taskpro.app.ui.components.shared.WorkspaceTab
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.navigation.dashboardTab
import com.taskpro.app.vault.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h850dp-mdpi")
class DashboardNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var db: AppDatabase
    @Before fun setup() { db = AppDatabase.getInstance(compose.activity) }
    @After fun close() {
        compose.activityRule.scenario.close(); db.close()
        AppDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
    private fun await(text: String) { compose.waitUntil(10_000) {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(20, java.util.concurrent.TimeUnit.MILLISECONDS)
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    } }
    private fun home() {
        await("Open instantly")
        compose.onNode(hasText("Tasks") and hasClickAction()).performClick()
        await("Home")
        compose.onNodeWithText("Home").performClick()
        await("Scheduled today")
    }

    @Test fun chooserAndExistingGroupsRemainAccessibleAndHomeRestoresAfterSettings() {
        runBlocking { db.itemDao().insert(Item(1, "Synthetic group")) }
        home()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        await("Scheduled today")
        compose.activityRule.scenario.recreate()
        await("Scheduled today")
        compose.onNode(hasText("Tasks") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performScrollTo().performClick()
        await("Groups")
        compose.onNodeWithText("Groups").performClick()
        await("Synthetic group")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithContentDescription("Workspaces").performClick()
        await("Open instantly")
        assertEquals(2, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test fun homeVaultCardKeepsFirstUseAndLockedAuthenticationGates() {
        home()
        compose.onNode(hasText("Vault") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performScrollTo().performClick()
        await("New master password")
        val vault = (compose.activity.application as TaskProApp).container.vaultRepository
        assertNull(vault.state.value.document)
        runBlocking { vault.unlock("synthetic dashboard master".toCharArray(), create = true); vault.confirmRecoverySaved() }
        vault.lock()
        compose.onNodeWithText("Home").performClick()
        await("Scheduled today")
        compose.onNode(hasText("Vault") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performScrollTo().performClick()
        await("Vault is locked")
        assertNull(vault.state.value.document)
    }

    @Test fun homeSearchFindsNotesAndDoesNotExposeUnlockedVaultMetadata() {
        val vault = (compose.activity.application as TaskProApp).container.vaultRepository
        runBlocking {
            db.itemDao().insert(Item(1, "Public task group"))
            db.taskDao().insert(Task(1, 1, "Synthetic task", notes = "Needle in task notes"))
            vault.unlock("synthetic dashboard master".toCharArray(), create = true)
            vault.confirmRecoverySaved()
            val category = VaultCategory(name = "Synthetic confidential group")
            vault.edit { it.copy(categories = listOf(category), records = listOf(VaultRecord(categoryId = category.id,
                title = "Synthetic confidential record", values = mapOf("username" to "synthetic-private-user")))) }
        }
        home()
        compose.onNodeWithText("Synthetic confidential record").assertDoesNotExist()
        compose.onNodeWithText("synthetic-private-user").assertDoesNotExist()
        compose.onNodeWithText("Synthetic confidential group").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("needle")
        await("Synthetic task")
        compose.onNodeWithText("Synthetic confidential record").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextReplacement("confidential")
        await("No matching tasks")
        assertEquals(0, compose.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Test fun snoozedUpcomingOpensExistingStatusListWithoutChangingTask() {
        runBlocking {
            db.itemDao().insert(Item(1, "Upcoming group"))
            db.taskDao().insert(Task(1, 1, "Synthetic future snoozed", dueAt = System.currentTimeMillis() + 3_600_000,
                status = TaskStatus.SNOOZED, position = 7))
        }
        home()
        compose.onNodeWithText("Synthetic future snoozed").performScrollTo().performClick()
        await("Synthetic future snoozed")
        compose.onNode(hasText("Tasks") and isSelected()).assertExists()
        assertEquals(TaskStatus.SNOOZED, runBlocking { db.taskDao().getById(1)!!.status })
        assertEquals(7, runBlocking { db.taskDao().getById(1)!!.position })
    }

    @Test fun tabMappingPreservesTaskDescendantsAndSettingsOrigin() {
        listOf(Screen.Home.route, Screen.ItemDetail.route, Screen.ItemStatusList.route, Screen.Search.route).forEach {
            assertEquals(WorkspaceTab.Tasks, dashboardTab(it))
        }
        assertEquals(WorkspaceTab.Home, dashboardTab(Screen.Dashboard.route))
        assertEquals(WorkspaceTab.Vault, dashboardTab(Screen.Vault.route))
        assertEquals(WorkspaceTab.Vault, dashboardTab(Screen.Settings.route, Screen.Vault.route))
        assertEquals(WorkspaceTab.Tasks, dashboardTab(Screen.Settings.route, Screen.ItemDetail.route))
    }
}
