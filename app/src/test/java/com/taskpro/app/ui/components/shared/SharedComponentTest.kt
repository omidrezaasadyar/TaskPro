package com.taskpro.app.ui.components.shared

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.taskpro.app.data.Task
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.theme.TaskProTheme
import com.taskpro.app.vault.VaultCategory
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w600dp-h1200dp-mdpi")
class SharedComponentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun content(body: @Composable () -> Unit) {
        compose.setContent { TaskProTheme(ThemeMode.DARK) { Column(Modifier.width(320.dp)) { body() } } }
    }

    @Test fun primaryButtonGuardsBusyAndDisabledWithoutChangingBounds() {
        val busy = mutableStateOf(false)
        val enabled = mutableStateOf(true)
        var calls = 0
        content { PrimaryActionButton("Save synthetic item", { calls++ }, Modifier.testTag("button"), busy = busy.value,
            enabled = enabled.value, unavailableReason = "Validation required") }
        val action = compose.onNodeWithText("Save synthetic item")
        action.assertHeightIsAtLeast(56.dp).performClick()
        val before = action.fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { busy.value = true }
        action.assertIsNotEnabled().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Working")).performClick()
        assertEquals(before, action.fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { busy.value = false; enabled.value = false }
        action.assertIsNotEnabled().performClick()
        compose.onNodeWithText("Validation required").assertExists()
        assertEquals(1, calls)
    }

    @Test fun searchEditsAndClearsOnlyItsSuppliedQuery() {
        var query by mutableStateOf("sample")
        content { SearchField(query, { query = it }, "Search tasks") }
        compose.onNode(hasSetTextAction()).performTextReplacement("new query")
        assertEquals("new query", query)
        compose.onNodeWithContentDescription("Clear search").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("", query)
    }

    @Test fun filterExposesSelectedAndDisabledStates() {
        var selected by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        content { FilterChip("Favorites", selected, { selected = !selected }, enabled = enabled) }
        val chip = compose.onNodeWithText("Favorites")
        val initialBounds = chip.fetchSemanticsNode().boundsInRoot
        chip.assertIsOff().assertHeightIsAtLeast(48.dp).performClick().assertIsOn()
        assertEquals(initialBounds, chip.fetchSemanticsNode().boundsInRoot)
        compose.runOnIdle { enabled = false }
        chip.assertIsNotEnabled().performClick()
        assertTrue(selected)
    }

    @Test fun taskChildCommandsNeverInvokeEditAndDragSuppressesEdit() {
        val calls = mutableListOf<String>()
        var dragging by mutableStateOf(false)
        content { TaskRow(Task(id = 77, itemId = 42, title = "Synthetic task"), { calls.add("edit") }, { calls.add("complete") },
            dragging = dragging, onSnooze = { calls.add("snooze") }, onStatus = { calls.add("status") },
            onMoveUp = { calls.add("up") }, onMoveDown = { calls.add("down") }, onMenu = { calls.add("menu") }) }
        compose.onNodeWithContentDescription("Synthetic task").performClick()
        listOf("Snooze", "Change status", "Move up", "Move down", "Options for Synthetic task").forEach {
            compose.onNodeWithContentDescription(it).assertHeightIsAtLeast(48.dp).performClick()
        }
        assertEquals(listOf("complete", "snooze", "status", "up", "down", "menu"), calls)
        compose.onNodeWithText("Synthetic task").performClick()
        assertEquals("edit", calls.last())
        compose.runOnIdle { dragging = true }
        compose.onNodeWithText("Synthetic task").performClick()
        assertEquals(7, calls.size)
    }

    @Test fun folderAndRecordChildActionsRemainIndependent() {
        val calls = mutableListOf<String>()
        var active by mutableStateOf(true)
        content {
            VaultGroupCard(VaultCategory(id = "group", name = "Synthetic group"), active,
                { calls.add("folder") }, { calls.add("menu") }, directItems = 1, subgroups = 2)
            VaultItemCard("Synthetic account", "Synthetic group", true, active,
                { calls.add("record") }, { calls.add("favorite") })
        }
        compose.onNodeWithText("1 direct item").assertExists()
        compose.onNodeWithText("2 subgroups").assertExists()
        compose.onNodeWithContentDescription("Options for Synthetic group").performClick()
        compose.onNodeWithContentDescription("Favorite").performClick()
        assertEquals(listOf("menu", "favorite"), calls)
        compose.onNodeWithText("Synthetic account").performClick()
        assertEquals("record", calls.last())
        compose.runOnIdle { active = false }
        compose.onNodeWithText("Synthetic account").assertDoesNotExist()
        compose.onNodeWithText("Synthetic group").assertDoesNotExist()
    }

    @Test fun bottomNavigationIsOnlySelectedPresentationAndCallback() {
        val calls = mutableListOf<WorkspaceTab>()
        var selected by mutableStateOf(WorkspaceTab.Home)
        content { BottomNavigationBar(selected, { calls.add(it); selected = it }) }
        compose.onNodeWithText("Home").assertIsSelected()
        compose.onNodeWithText("Vault").assertHeightIsAtLeast(80.dp).performClick().assertIsSelected()
        compose.onNodeWithText("Vault").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Locked"))
        assertEquals(listOf(WorkspaceTab.Vault), calls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardFocusAndEnterInvokeOneCommand() {
        var calls = 0
        content { IconAction("Synthetic action", Icons.Default.Check, { calls++ }) }
        val action = compose.onNodeWithContentDescription("Synthetic action")
        action.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        action.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, calls)
    }

    @Test fun confirmationSeparatesCancelConfirmAndBusy() {
        val calls = mutableListOf<String>()
        var busy by mutableStateOf(false)
        content { ConfirmationDialog("Delete synthetic item?", "Only this synthetic item will be removed.", "Delete item",
            { calls.add("confirm") }, { calls.add("cancel") }, busy = busy, destructive = true) }
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("cancel"), calls)
        compose.onNodeWithText("Delete item").performClick()
        assertEquals(listOf("cancel", "confirm"), calls)
        compose.runOnIdle { busy = true }
        compose.onNodeWithText("Delete item").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Cancel").assertIsNotEnabled().performClick()
        assertEquals(2, calls.size)
    }

    @Test fun statePrimitivesExposeHeadingTruthfulLoadingAndRealActions() {
        val calls = mutableListOf<String>()
        content {
            SectionHeader("Groups", actionLabel = "Create group", onAction = { calls.add("group") })
            EmptyState("Empty folder", "No items in this folder.", Icons.Default.Check,
                actionLabel = "Add item", onAction = { calls.add("add") })
            ErrorState("Synthetic storage error", retryLabel = "Retry", onRetry = { calls.add("retry") })
            LoadingState("Loading synthetic items")
        }
        compose.onNodeWithText("Groups").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Create group").performClick()
        compose.onNodeWithText("Add item").performClick()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf("group", "add", "retry"), calls)
        compose.onNodeWithText("Loading synthetic items").assertExists()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
    }

    @Test @Config(qualifiers = "fa-rIR-w320dp-h480dp-mdpi")
    fun longPersianConfirmationRemainsScrollableAtDoubleFontScale() {
        var open by mutableStateOf(true)
        var cancelled = false
        content { CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f),
            LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (open) ConfirmationDialog("حذف مورد نمونه؟", "این پیام آزمایشی است و هیچ دادهٔ واقعی حذف نخواهد شد. ".repeat(20),
                "حذف مورد آزمایشی", {}, { cancelled = true; open = false }, destructive = true)
        } }
        compose.onNodeWithText("حذف مورد آزمایشی").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(56.dp)
        val cancel = compose.onNodeWithText(compose.activity.getString(com.taskpro.app.R.string.cancel))
        cancel.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(cancelled)
        compose.onNodeWithText("حذف مورد نمونه؟").assertDoesNotExist()
    }
}
