package com.taskpro.app.vault

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.vault.RecoveryForm
import com.taskpro.app.ui.screens.vault.RecoverySetup
import com.taskpro.app.ui.screens.vault.VaultLogin
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h778dp-mdpi")
class VaultAuthPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun masterPasswordRemainsAbsentFromSemanticsWhenRevealed() {
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            VaultLogin(true, false, null, null, {}, { _, _ -> })
        } }
        compose.onNodeWithText("Master password").performTextInput("synthetic-only-password")
        compose.onNodeWithText("synthetic-only-password").assertDoesNotExist()
        compose.onNodeWithContentDescription("Master password, Hidden").assertExists()
        compose.onNodeWithContentDescription("Show Master password").performClick()
        compose.onNodeWithContentDescription("Master password, Revealed visually").assertExists()
        compose.onNodeWithText("synthetic-only-password").assertDoesNotExist()
    }

    @Test fun recoveryKeyDisplayIsRedactedFromSemanticsAndRequiresAcknowledgment() {
        var closed = false
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) {
            RecoverySetup(false, { closed = true }, {}, initialKey = "synthetic-recovery-material", generate = { _, _ -> })
        } }
        compose.onNodeWithText("synthetic-recovery-material").assertDoesNotExist()
        compose.onNodeWithContentDescription("Recovery key displayed. Use Copy to save it securely.").assertExists()
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithText("I have saved my recovery key safely").performClick()
        compose.onNodeWithText("OK").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(closed) }
    }

    @Test fun recoveryEntryKeepsResetDisabledUntilKeyAndMatchingPasswordArePresent() {
        var recovered = false
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            RecoveryForm(false, null, {}, { _, _ -> recovered = true })
        } }
        compose.onNodeWithText("Reset master password").assertIsNotEnabled()
        compose.onNodeWithText("Recovery key").performTextInput("synthetic-key")
        compose.onNodeWithText("New master password").performTextInput("synthetic new password")
        compose.onNodeWithText("Confirm password").performTextInput("different password")
        compose.onNodeWithText("Reset master password").assertIsNotEnabled()
        compose.runOnIdle { assertFalse(recovered) }
    }
}
