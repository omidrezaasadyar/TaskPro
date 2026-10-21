package com.taskpro.app.dashboard

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.compose.ui.input.key.Key
import com.taskpro.app.data.*
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.components.shared.BottomNavigationBar
import com.taskpro.app.ui.components.shared.WorkspaceTab
import com.taskpro.app.ui.screens.dashboard.*
import com.taskpro.app.ui.screens.search.TaskSearchState
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1000dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val moment = DashboardMoment(Instant.parse("2026-10-17T18:06:00Z").toEpochMilli(), "UTC")
    private fun summary(): DashboardSummary {
        val day = localDayBounds(moment).first
        val rows = (1..5).map { TaskWithItemName(Task(it.toLong(), 1, "Synthetic completed $it",
            dueAt = day + 9 * 3_600_000, status = TaskStatus.COMPLETED), "Operations") } + listOf(
            TaskWithItemName(Task(6, 1, "Synthetic overdue", dueAt = day + 10 * 3_600_000), "Operations"),
            TaskWithItemName(Task(7, 1, "Call office", dueAt = day + 18 * 3_600_000 + 30 * 60_000), "Operations"),
            TaskWithItemName(Task(8, 2, "Review monthly report", dueAt = day + 19 * 3_600_000 + 15 * 60_000), "Planning"))
        return dashboardSummary(rows, moment)
    }
    @Test fun darkEnglish() = render("home-dark-en")
    @Test fun darkPersian() = render("home-dark-fa", rtl = true)
    @Test fun lightEnglish() = render("home-light-en", dark = false)
    @Test fun narrowLargeEnglish() = render("home-en-large", width = 280, height = 640, scale = 2f)
    @Test fun narrowLargePersian() = render("home-fa-large", width = 320, height = 640, scale = 2f, rtl = true)
    @Test fun landscape() = render("home-landscape", width = 640, height = 360)

    private fun render(name: String, width: Int = 360, height: Int = 778, scale: Float = 1f,
                       rtl: Boolean = false, dark: Boolean = true) {
        val config = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply { setLocale(Locale(if (rtl) "fa" else "en")) }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(config)
        val opened = mutableListOf<Long>()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("dashboardCapture")) {
                        Scaffold(bottomBar = { BottomNavigationBar(WorkspaceTab.Home, {}) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                            Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                                DashboardScreen(DashboardUiState(summary(), loading = false), true, "", TaskSearchState(),
                                    {}, {}, {}, {}, {}, {}, {}, { opened.add(it.task.id) })
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        capture(name)
        compose.onNode(hasText(context.getString(com.taskpro.app.R.string.component_home)) and isSelected()).assertExists()
        compose.onNodeWithText("Review monthly report").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf(8L), opened)
        capture("$name-scrolled")
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { it.config.getOrNull(SemanticsProperties.Disabled) == null }.forEach {
            assertTrue("$name target height", it.layoutInfo.coordinates.size.height >= 48)
            assertTrue("$name target width", it.layoutInfo.coordinates.size.width >= 48)
        }
        compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach { result ->
                    assertFalse("$name clips height: ${result.layoutInput.text}", result.didOverflowHeight)
                    (0 until result.lineCount).forEach { line -> assertTrue("$name clips width: ${result.layoutInput.text}",
                        result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1) }
                }
            }
    }

    @Test fun emptyErrorAndLoadingDoNotInventZeroProgress() {
        val state = mutableStateOf(DashboardUiState())
        var retries = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            DashboardScreen(state.value, true, "", TaskSearchState(), {}, { retries++ }, {}, {}, {}, {}, {}, {})
        } }
        compose.onAllNodesWithText("Loading tasks").onFirst().assertExists()
        compose.onNodeWithText("0/0").assertDoesNotExist()
        compose.runOnIdle { state.value = DashboardUiState(loading = false, failed = true) }
        compose.onNodeWithText("Could not load task information.").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
        compose.runOnIdle { state.value = DashboardUiState(dashboardSummary(emptyList(), moment), loading = false) }
        compose.onNodeWithText("No tasks scheduled today").assertExists()
        compose.onNodeWithText("0/0").assertDoesNotExist()
        compose.onNodeWithText("No upcoming tasks").performScrollTo().assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @GraphicsMode(GraphicsMode.Mode.LEGACY)
    @Test fun dashboardKeyboardFocusCanActivateSettings() {
        var calls = 0
        compose.setContent { TaskProTheme(ThemeMode.DARK) {
            DashboardScreen(DashboardUiState(summary(), loading = false), true, "", TaskSearchState(),
                {}, {}, {}, {}, {}, { calls++ }, {}, {})
        } }
        val button = compose.onNodeWithContentDescription("Settings")
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, calls)
    }

    private fun capture(name: String) {
        val bounds = compose.onNodeWithTag("dashboardCapture").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val file = File("build/reports/dashboard/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(cropped.getPixel(0, 0) != 0)
            bitmap.recycle(); cropped.recycle()
        }
    }
}
