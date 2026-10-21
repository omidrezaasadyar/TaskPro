package com.taskpro.app.vault

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.vault.RecordEditor
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h778dp-mdpi")
class VaultEditorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun addPrefillsSelectedGroupAndCancelDoesNotSave() {
        val group = VaultCategory(id = "group", name = "Synthetic group")
        val draft = VaultRecord(categoryId = group.id)
        val visible = mutableStateOf(true)
        var saved: VaultRecord? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            if (visible.value) RecordEditor(draft, VaultDocument(categories = listOf(group)), false,
                { visible.value = false }) { saved = it }
        } }
        compose.onNodeWithText("Add Item").assertExists()
        compose.onNodeWithText("Category: Synthetic group").assertExists()
        compose.onNodeWithText("Record title").performTextInput("Unsaved")
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Add Item").assertDoesNotExist()
        assertNull(saved)
    }

    @Test fun editKeepsIdentityFieldsAndSecretMaskedAfterSave() {
        val group = VaultCategory(id = "group", name = "Accounts")
        val original = VaultRecord(id = "stable-id", categoryId = group.id, title = "Existing",
            values = mapOf("username" to "original-user", "password" to "synthetic-secret"),
            custom = listOf(CustomField(id = "custom-id", label = "Private", value = "custom-secret", sensitive = true)),
            tags = listOf("work"), favorite = true, createdAt = 1_700_000_000_000)
        var saved: VaultRecord? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            RecordEditor(original, VaultDocument(listOf(group), listOf(original)), false, {}) { saved = it }
        } }
        compose.onNodeWithText("Edit Item").assertExists()
        compose.onNodeWithText("Existing").assertExists()
        compose.onNodeWithText("original-user").assertExists()
        compose.onNodeWithText("synthetic-secret").assertDoesNotExist()
        compose.onNodeWithText("custom-secret").assertDoesNotExist()
        compose.onNodeWithText("Record title").performTextReplacement("Changed")
        compose.onNodeWithText("Save to Vault").performClick()
        compose.runOnIdle {
            val result = checkNotNull(saved)
            assertEquals("stable-id", result.id)
            assertEquals("Changed", result.title)
            assertEquals(original.createdAt, result.createdAt)
            assertEquals(original.values, result.values)
            assertEquals(original.custom, result.custom)
            assertEquals(original.tags, result.tags)
            assertTrue(result.favorite)
        }
    }

    @Test fun generatorDoesNotReplacePasswordUntilExplicitUse() {
        val original = VaultRecord(title = "Existing", values = mapOf("password" to "synthetic-old-secret"))
        var saved: VaultRecord? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            RecordEditor(original, VaultDocument(records = listOf(original)), false, {}) { saved = it }
        } }
        compose.onNodeWithText("Generate password").performClick()
        compose.onNodeWithText("Save to Vault").performClick()
        compose.runOnIdle { assertEquals("synthetic-old-secret", saved?.values?.get("password")) }
    }

    @Test fun generatedPasswordAppliesOnlyAfterUse() {
        val original = VaultRecord(title = "Existing", values = mapOf("password" to "synthetic-old-secret"))
        var saved: VaultRecord? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            RecordEditor(original, VaultDocument(records = listOf(original)), false, {}) { saved = it }
        } }
        compose.onNodeWithText("Generate password").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
        compose.onNodeWithText("Use Password").assertIsNotEnabled()
        compose.onAllNodesWithText("Generate password").onLast().performClick()
        compose.onNodeWithText("Use Password").assertIsEnabled().performClick()
        compose.onNodeWithText("Save to Vault").performClick()
        compose.runOnIdle {
            val result = checkNotNull(saved)
            assertEquals(original.id, result.id)
            assertEquals(24, result.values.getValue("password").length)
            assertNotEquals("synthetic-old-secret", result.values.getValue("password"))
        }
    }

    @Test fun invalidTitleDoesNotCallSave() {
        var saved: VaultRecord? = null
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            RecordEditor(VaultRecord(), VaultDocument(), false, {}) { saved = it }
        } }
        compose.onNodeWithText("Save to Vault").performClick()
        compose.onNodeWithText("A title is required.", substring = true).assertExists()
        assertNull(saved)
    }
}
