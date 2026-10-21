package com.taskpro.app.ui.components.shared

import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.theme.TaskProTheme
import com.taskpro.app.vault.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w600dp-h1200dp-mdpi")
class SecureComponentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val secret = "synthetic-only-DO-NOT-ANNOUNCE"

    private fun content(body: @Composable () -> Unit) {
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) { body() }
        } }
    }

    private fun assertNoSecretSemantics(value: String = secret) {
        compose.onAllNodes(SemanticsMatcher("all nodes") { true }, useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            node.config.forEach { entry -> assertFalse("Secret leaked in ${entry.key.name}", entry.value.toString().contains(value)) }
        }
        compose.onNodeWithText(value, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun sensitiveDetailNeverPublishesSecretSemanticsEvenWhenRevealed() {
        var generation by mutableStateOf(1)
        var active by mutableStateOf(true)
        var copied: String? = null
        content { SecureFieldCard("Synthetic key", secret, false, FieldType.SECRET, generation, active, onCopy = { copied = it }) }
        assertNoSecretSemantics()
        compose.onNodeWithContentDescription("Show Synthetic key").performClick()
        compose.onNodeWithContentDescription("Synthetic key, Revealed visually").assertExists()
        assertNoSecretSemantics()
        compose.onNodeWithContentDescription("Copy Synthetic key").performClick()
        assertEquals(secret, copied)
        compose.runOnIdle { generation++ }
        compose.onNodeWithContentDescription("Synthetic key, Hidden").assertExists()
        compose.onNodeWithContentDescription("Hide Synthetic key").assertDoesNotExist()
        compose.runOnIdle { active = false }
        compose.onNodeWithText("Synthetic key").assertDoesNotExist()
        compose.onNodeWithContentDescription("Copy Synthetic key").assertDoesNotExist()
    }

    @Test fun inputRetainsAccessibleEditingButRedactsTextAndLayoutBeforeAndAfterReveal() {
        var value by mutableStateOf("")
        var generation by mutableStateOf(1)
        content { SecureInputField("Synthetic password", value, { value = it }, generation, true, type = FieldType.SECRET) }
        compose.onNode(hasSetTextAction()).performTextReplacement(secret)
        assertEquals(secret, value)
        compose.onNode(hasSetTextAction()).performTextInput("!")
        assertEquals(secret + "!", value)
        assertNoSecretSemantics()
        compose.onNodeWithContentDescription("Show Synthetic password").performClick()
        assertNoSecretSemantics()
        val field = compose.onNodeWithContentDescription("Synthetic password, Revealed visually")
        field.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.GetTextLayoutResult))
        compose.runOnIdle { generation++ }
        compose.onNodeWithContentDescription("Synthetic password, Hidden").assertExists()
    }

    @Test fun everyCustomTypePreservesSensitivityAndUnavailableFieldsCannotEdit() {
        var type by mutableStateOf(FieldType.TEXT)
        var sensitive by mutableStateOf(true)
        var active by mutableStateOf(true)
        var busy by mutableStateOf(false)
        var changes = 0
        content { SecureInputField("Custom field", secret, { changes++ }, 1, active, type = type, sensitive = sensitive, busy = busy) }
        FieldType.entries.forEach { fieldType ->
            compose.runOnIdle { type = fieldType }
            assertNoSecretSemantics()
        }
        compose.runOnIdle { sensitive = false; type = FieldType.SECRET; busy = true }
        assertNoSecretSemantics()
        compose.onNodeWithContentDescription("Show Custom field").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.runOnIdle { active = false }
        compose.onNodeWithText("Custom field").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun externalActionUsesExactTypeSensitivityAndExistingUrlValidationWhitelist() {
        var type by mutableStateOf(FieldType.URL)
        var sensitive by mutableStateOf(false)
        var url by mutableStateOf("https://synthetic.example.invalid/path")
        val opened = mutableListOf<String>()
        content { SecureFieldCard("Synthetic website", url, sensitive, type, 1, true, onOpen = { opened.add(it) }) }
        compose.onNodeWithContentDescription("Open Synthetic website").performClick()
        assertEquals(listOf(url), opened)
        listOf("http://synthetic.example.invalid", "https://synthetic.example.invalid").forEach { valid ->
            compose.runOnIdle { url = valid }
            compose.onNodeWithContentDescription("Open Synthetic website").assertExists()
        }
        listOf("javascript:alert(1)", "https://user:password@synthetic.example.invalid", "https://", "ftp://synthetic.example.invalid").forEach { invalid ->
            compose.runOnIdle { url = invalid }
            compose.onNodeWithContentDescription("Open Synthetic website").assertDoesNotExist()
        }
        compose.runOnIdle { url = "https://synthetic.example.invalid" }
        FieldType.entries.forEach { fieldType ->
            compose.runOnIdle { type = fieldType; sensitive = false }
            if (fieldType != FieldType.URL) compose.onNodeWithContentDescription("Open Synthetic website").assertDoesNotExist()
            compose.runOnIdle { sensitive = true }
            compose.onNodeWithContentDescription("Open Synthetic website").assertDoesNotExist()
            compose.onNodeWithContentDescription("Show Synthetic website").performClick()
            compose.onNodeWithContentDescription("Open Synthetic website").assertDoesNotExist()
            assertNoSecretSemantics(url)
        }
        assertEquals(1, opened.size)
    }

    @Test fun copyDelegatesExactValueToExistingOwnedClipboard() {
        val context = RuntimeEnvironment.getApplication()
        val clipboard = VaultClipboard(context)
        val original = "  synthetic\nفارسی value  "
        content { SecureFieldCard("Notes", original, true, FieldType.NOTES, 1, true, onCopy = clipboard::copy) }
        compose.onNodeWithContentDescription("Copy Notes").performClick()
        val manager = context.getSystemService(ClipboardManager::class.java)
        assertEquals(original, manager.primaryClip!!.getItemAt(0).text.toString())
        assertTrue(manager.primaryClipDescription!!.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
        assertNoSecretSemantics(original)
        clipboard.clearOwned()
        assertFalse(manager.hasPrimaryClip())
    }

    @Test fun generatorNeverReplacesFormPasswordUntilExplicitUseAndCancelDoesNotApply() {
        var options by mutableStateOf(GeneratorOptions())
        var candidate by mutableStateOf<GeneratedPasswordCandidate?>(null)
        var current = "existing-synthetic-password"
        var generations = 0
        var cancellations = 0
        content { PasswordGeneratorPanel(options, candidate, { options = it }, {
            generations++; candidate = GeneratedPasswordCandidate("x".repeat(options.length), options, 1)
        }, { current = it }, { cancellations++ }, 1, true) }
        compose.onNodeWithText("Use Password").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Generate password").filterButton().performScrollTo().performClick()
        assertEquals("existing-synthetic-password", current)
        assertEquals(1, generations)
        assertNoSecretSemantics("x".repeat(24))
        compose.onNodeWithText("Use Password").performScrollTo().performClick()
        assertEquals("x".repeat(24), current)
        compose.onNodeWithContentDescription("Increase length").performScrollTo().performClick()
        compose.onNodeWithText("Use Password").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(1, cancellations)
        assertEquals("x".repeat(24), current)
    }

    @Test fun generatorRejectsOldSessionCandidatesAndAllClassesDisabled() {
        var generation by mutableStateOf(1)
        var active by mutableStateOf(true)
        var options by mutableStateOf(GeneratorOptions())
        val candidate = GeneratedPasswordCandidate("s".repeat(24), options, 1)
        content { PasswordGeneratorPanel(options, candidate, { options = it }, {}, {}, {}, generation, active) }
        compose.onNodeWithText("Use Password").performScrollTo().assertIsEnabled()
        compose.runOnIdle { generation++ }
        compose.onNodeWithText("Use Password").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Show Generated password").assertDoesNotExist()
        compose.runOnIdle { options = GeneratorOptions(lower = false, upper = false, digits = false, symbols = false) }
        compose.onNodeWithText("Select at least one character type.").performScrollTo().assertExists()
        compose.onNodeWithText("Generate password").filterButton().performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { active = false }
        compose.onNodeWithText("Use Password").assertDoesNotExist()
        assertEquals("GeneratedPasswordCandidate(redacted)", candidate.toString())
    }

    @Test fun generatorLengthBoundsAndBusyActionsAreGuarded() {
        var options by mutableStateOf(GeneratorOptions(length = 12))
        var busy by mutableStateOf(false)
        content { PasswordGeneratorPanel(options, null, { options = it }, {}, {}, {}, 1, true, busy = busy) }
        compose.onNodeWithContentDescription("Decrease length").assertIsNotEnabled()
        compose.runOnIdle { options = options.copy(length = 128) }
        compose.onNodeWithContentDescription("Increase length").assertIsNotEnabled()
        compose.runOnIdle { busy = true }
        compose.onNodeWithContentDescription("Lowercase").assertIsNotEnabled()
        compose.onNodeWithText("Generate password").filterButton().performScrollTo().assertIsNotEnabled()
    }

    @Test fun oldInputCallbackAndDisposedCopyCannotOperate() {
        var generation by mutableStateOf(1)
        var visible by mutableStateOf(true)
        var copies = 0
        var edits = 0
        content { if (visible) {
            SecureFieldCard("Secret", secret, true, FieldType.SECRET, generation, true, onCopy = { copies++ })
            SecureInputField("Input", "", { edits++ }, generation, true, type = FieldType.SECRET)
        } }
        val copy = compose.onNodeWithContentDescription("Copy Secret").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        val edit = compose.onNode(hasSetTextAction()).fetchSemanticsNode().config[SemanticsActions.SetText].action!!
        compose.runOnIdle { generation++ }
        compose.runOnIdle { assertFalse(edit(AnnotatedString(secret))) }
        compose.runOnIdle { visible = false }
        compose.runOnIdle { copy() }
        assertEquals(0, copies)
        assertEquals(0, edits)
    }

    @Test fun nonSecretTypesAndMultilineNotesRemainEditableWithoutValueNormalization() {
        var type by mutableStateOf(FieldType.TEXT)
        var value by mutableStateOf("")
        content { SecureInputField("Field", value, { value = it }, 1, true, type = type) }
        FieldType.entries.forEach { fieldType ->
            compose.runOnIdle { type = fieldType }
            val next = if (fieldType == FieldType.NOTES) "  Line one\nفارسی  " else "  synthetic value  "
            compose.onNode(hasSetTextAction()).performTextReplacement(next)
            assertEquals(next, value)
            if (fieldType == FieldType.SECRET) assertNoSecretSemantics(next)
        }
    }

    @Test fun phoneAndPinKeyboardsAreSupportedWithoutWeakeningSecretInputTypes() {
        assertEquals(KeyboardType.Phone, secureKeyboardType(FieldType.TEXT, false, KeyboardType.Phone))
        assertEquals(KeyboardType.NumberPassword, secureKeyboardType(FieldType.NUMBER, true, KeyboardType.NumberPassword))
        assertEquals(KeyboardType.Password, secureKeyboardType(FieldType.SECRET, false, KeyboardType.Email))
        assertEquals(KeyboardType.Password, secureKeyboardType(FieldType.URL, true, null))
        assertEquals(KeyboardType.Decimal, secureKeyboardType(FieldType.NUMBER, false, null))
        assertEquals(KeyboardType.Email, secureKeyboardType(FieldType.EMAIL, false, null))
        assertEquals(KeyboardType.Uri, secureKeyboardType(FieldType.URL, false, null))
    }

    private fun SemanticsNodeInteraction.filterButton(): SemanticsNodeInteraction =
        compose.onNode(hasText("Generate password") and hasClickAction())
}
