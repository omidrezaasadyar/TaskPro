package com.taskpro.app.ui.screens.reminder

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReminderPopupScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun phone() = verify("phone", 360, 800)
    @Test fun narrowWithLargeText() = verify("narrow-large", 280, 480, fontScale = 2f)
    @Test fun persianWithLargeText() = verify("rtl-large", 320, 640, fontScale = 2f, rtl = true)
    @Test fun landscapeWithLongTitle() = verify("landscape", 720, 320,
        title = "A long task title with English and فارسی content that must wrap without covering the actions. ".repeat(3))
    @Test fun tabletDark() = verify("tablet-dark", 800, 1000, dark = true)

    private fun verify(
        name: String, width: Int, height: Int, fontScale: Float = 1f,
        rtl: Boolean = false, dark: Boolean = false, title: String = "Review project proposal"
    ) {
        val configuration = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply {
            setLocale(Locale(if (rtl) "fa" else "en"))
        }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration)
        val clicked = mutableListOf<String>()
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)
            ) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("reminder")) {
                        ReminderPopupScreen(title,
                            onDone = { clicked.add("done") },
                            onSnooze = { clicked.add("snooze") },
                            onDismiss = { clicked.add("dismiss") })
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(400)
        capture(name)
        listOf(R.string.reminder_done, R.string.reminder_snooze_10, R.string.reminder_dismiss).forEach { resource ->
            val label = context.getString(resource)
            val action = compose.onNodeWithText(label)
            action.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(56.dp)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("$name: $label is clipped", layouts.single().hasVisualOverflow)
            if (resource == R.string.reminder_snooze_10) capture("$name-actions")
            action.performClick()
        }
        assertEquals(listOf("done", "snooze", "dismiss"), clicked)
    }

    private fun capture(name: String) {
        val bounds = compose.onNodeWithTag("reminder").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(full))
            val cropped = Bitmap.createBitmap(full, bounds.left.toInt(), bounds.top.toInt(),
                bounds.width.toInt(), bounds.height.toInt())
            val output = File("build/reports/reminder/$name.png")
            output.parentFile!!.mkdirs()
            output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            cropped.recycle()
            full.recycle()
        }
    }
}
