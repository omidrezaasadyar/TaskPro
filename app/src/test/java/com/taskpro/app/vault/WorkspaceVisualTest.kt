package com.taskpro.app.vault

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.navigation.Screen
import com.taskpro.app.ui.screens.WorkspaceScreen
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.*
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
@Config(sdk = [28], qualifiers = "w1000dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkspaceVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun referenceDark() = render("dark-en-360", 360, 778)
    @Test fun widerPhone() = render("dark-en-400", 400, 850)
    @Test fun compactPhone() = render("dark-en-320", 320, 640)
    @Test fun narrowLargeType() = render("dark-en-280-large", 280, 480, scale = 2f)
    @Test fun landscape() = render("dark-en-landscape", 600, 360)
    @Test fun expanded() = render("dark-en-expanded", 840, 600)
    @Test fun persian() = render("dark-fa-360", 360, 778, rtl = true)
    @Test fun persianLargeType() = render("dark-fa-320-large", 320, 640, rtl = true, scale = 2f)
    @Test fun light() = render("light-en-360", 360, 778, dark = false)
    @Test fun lightPersianLargeType() = render("light-fa-320-large", 320, 640, dark = false, rtl = true, scale = 2f)

    @OptIn(ExperimentalTestApi::class)
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    @Test fun keyboardFocusAndActivationReachBothCards() {
        val selected = mutableListOf<Screen>()
        compose.setContent { TaskProTheme(ThemeMode.DARK) { WorkspaceScreen(onSelect = selected::add) } }
        val tasks = compose.onNode(hasText("Tasks") and hasClickAction())
        tasks.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        tasks.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(Screen.Home), selected)
        tasks.performKeyInput { pressKey(Key.Tab) }
        compose.onNode(hasText("Vault") and hasClickAction()).assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(Screen.Home, Screen.Vault), selected)
    }

    private fun render(name: String, width: Int, height: Int, rtl: Boolean = false, scale: Float = 1f, dark: Boolean = true) {
        val configuration = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply {
            setLocale(Locale(if (rtl) "fa" else "en"))
        }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration)
        val destinations = mutableListOf<Screen>()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("chooser")) {
                        WorkspaceScreen(onSelect = destinations::add)
                    }
                }
            }
        }
        compose.waitForIdle()
        capture(name)
        val cards = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals("Only the two real destinations are interactive", 2, cards.size)
        cards.forEach {
            assertTrue(it.layoutInfo.coordinates.size.width >= 48)
            assertTrue(it.layoutInfo.coordinates.size.height >= 48)
        }
        val paired = width >= 340 && scale <= 1.3f
        // boundsInRoot are clipped for off-screen items; use layout coordinates for geometry.
        val positions = cards.map { it.layoutInfo.coordinates.localToRoot(androidx.compose.ui.geometry.Offset.Zero) }
        if (paired) {
            assertEquals(positions[0].y, positions[1].y, 1f)
            assertTrue(if (rtl) positions[0].x > positions[1].x else positions[0].x < positions[1].x)
        } else assertTrue(positions[0].y < positions[1].y)
        listOf(R.string.workspace_tasks to Screen.Home, R.string.vault_title to Screen.Vault).forEach { (id, destination) ->
            val card = compose.onNode(hasText(context.getString(id)) and hasClickAction())
            card.performScrollTo().assertIsDisplayed().performClick()
            assertEquals(destination, destinations.last())
            assertEquals("A single card tap invokes exactly one destination", destinations.size, if (destination == Screen.Home) 1 else 2)
        }
        if (scale > 1 || height < 500) capture("$name-scrolled")
        // Every localized text must fit its actual paragraph width and line height.
        compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach { result ->
                    assertFalse("$name vertically clips ${result.layoutInput.text}", result.didOverflowHeight)
                    (0 until result.lineCount).forEach { line ->
                        assertTrue("$name horizontally clips ${result.layoutInput.text}",
                            result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1)
                    }
                }
            }
        assertTrue(compose.onAllNodes(isHeading()).fetchSemanticsNodes().isNotEmpty())
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach {
            assertNotNull(it.config.getOrNull(SemanticsProperties.Role))
        }
    }

    private fun capture(name: String) {
        val bounds = compose.onNodeWithTag("chooser").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val file = File("build/reports/workspace/$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue("Android render must be opaque/nonblank", cropped.getPixel(0, 0) != 0)
            cropped.recycle(); bitmap.recycle()
        }
    }
}
