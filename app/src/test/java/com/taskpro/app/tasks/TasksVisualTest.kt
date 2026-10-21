package com.taskpro.app.tasks

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
import com.taskpro.app.data.*
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.components.shared.BottomNavigationBar
import com.taskpro.app.ui.components.shared.WorkspaceTab
import com.taskpro.app.ui.screens.home.GroupsScreen
import com.taskpro.app.ui.screens.tasks.*
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
class TasksVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-17T10:00:00Z").toEpochMilli()
    private val oldLocale = Locale.getDefault()
    @After fun locale() { Locale.setDefault(oldLocale); RuntimeEnvironment.setFontScale(1f) }
    @Test fun timelineDarkEnglish() = render("timeline-dark-en")
    @Test fun referenceSizedTimeline() = render("timeline-reference-sized", height = 723)
    @Test fun timelineDarkPersian() = render("timeline-dark-fa", rtl = true)
    @Test fun groupsDarkEnglish() = render("groups-dark-en", groups = true)
    @Test fun groupsDarkPersian() = render("groups-dark-fa", groups = true, rtl = true)
    @Test fun timelineLight() = render("timeline-light-en", dark = false)
    @Test fun timelineNarrowLarge() = render("timeline-en-large", width = 320, height = 640, scale = 2f)
    @Test fun timelinePersianLarge() = render("timeline-fa-large", width = 320, height = 640, scale = 2f, rtl = true)
    @Test fun timelineLandscape() = render("timeline-landscape", width = 640, height = 360)
    @Test fun groupsLarge() = render("groups-fa-large", groups = true, rtl = true, width = 320, height = 640, scale = 2f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h778dp-mdpi")
    fun filterPanelEnglish() = render("timeline-filters-en", panel = true)
    @Test @Config(qualifiers = "fa-rIR-w360dp-h778dp-mdpi")
    fun filterPanelPersian() = render("timeline-filters-fa", rtl = true, panel = true)
    @Test @Config(qualifiers = "en-rUS-w320dp-h640dp-mdpi")
    fun filterPanelLarge() = render("timeline-filters-large", width = 320, height = 640, scale = 2f, panel = true)

    private fun render(name: String, groups: Boolean = false, width: Int = 360, height: Int = 778,
        scale: Float = 1f, rtl: Boolean = false, dark: Boolean = true, panel: Boolean = false) {
        val locale = Locale(if (rtl) "fa" else "en")
        Locale.setDefault(locale)
        if (panel) RuntimeEnvironment.setFontScale(scale)
        val config = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply { setLocale(locale) }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(config)
        val groupNames = if (rtl) listOf("امور مالی", "برنامه‌ریزی", "کارهای شخصی") else listOf("Finance", "Planning", "Personal")
        val titles = if (rtl) listOf("تمدید حساب بانکی", "تماس با دفتر کرمان", "آماده‌سازی گزارش ماهانه", "به‌روزرسانی ایمیل", "بررسی یادداشت‌ها")
            else listOf("Renew bank account", "Call Kerman office", "Prepare monthly report", "Update email", "Review notes")
        val rows = listOf(
            TaskWithItemName(Task(1, 1, titles[0], dueAt = now - 86_400_000), groupNames[0]),
            TaskWithItemName(Task(2, 2, titles[1], dueAt = now + 90 * 60_000), groupNames[1]),
            TaskWithItemName(Task(3, 2, titles[2], dueAt = now + 4 * 3_600_000), groupNames[1]),
            TaskWithItemName(Task(4, 3, titles[3], dueAt = now + 2 * 3_600_000, status = TaskStatus.COMPLETED), groupNames[2]),
            TaskWithItemName(Task(5, 3, titles[4], dueAt = null), groupNames[2]),
            TaskWithItemName(Task(6, 2, if (rtl) "بررسی هفتهٔ آینده" else "Review next week", dueAt = now + 2 * 86_400_000), groupNames[1]))
        val items = groupNames.mapIndexed { i, title ->
            val tasks = rows.filter { it.task.itemId == i + 1L }.map { it.task }
            ItemWithTaskCounts(Item(i + 1L, title), tasks.count { it.status == TaskStatus.PENDING },
                tasks.count { it.status == TaskStatus.COMPLETED }, tasks.count { it.status == TaskStatus.SNOOZED })
        }
        var opens = 0
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("tasksCapture")) {
                        Scaffold(bottomBar = { BottomNavigationBar(WorkspaceTab.Tasks, {}) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                            Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                                if (groups) GroupsScreen(items, { opens++ }, {}, {}, {}, {}, {}, {}, {})
                                else TimelineScreen(TasksState(rows, items, now, "UTC", false), {}, {}, {}, { opens++ }, { _, _ -> }, {}, {})
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        if (panel) {
            compose.onNodeWithContentDescription(context.getString(com.taskpro.app.R.string.tasks_filters)).performClick()
            compose.waitForIdle()
            capture(name, dialog = true)
            verifyLayout(name)
            compose.onNodeWithText(context.getString(com.taskpro.app.R.string.tasks_due)).performScrollTo().performClick()
            compose.onNodeWithContentDescription(context.getString(com.taskpro.app.R.string.tasks_close_filters)).performScrollTo().performClick()
            compose.waitForIdle()
            capture("$name-active")
            return
        }
        capture(name)
        val title = if (groups) groupNames.last() else titles.last()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).assertIsDisplayed().performClick()
        assertEquals(1, opens)
        capture("$name-scrolled")
        verifyLayout(name)
    }

    private fun verifyLayout(name: String) {
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { it.config.getOrNull(SemanticsProperties.Disabled) == null }.forEach {
            assertTrue("$name target height ${it.layoutInfo.coordinates.size}: ${it.config}", it.layoutInfo.coordinates.size.height >= 48)
            assertTrue("$name target width ${it.layoutInfo.coordinates.size}: ${it.config}", it.layoutInfo.coordinates.size.width >= 48)
        }
        compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach { result ->
                    assertFalse("$name clips height: ${result.layoutInput.text}", result.didOverflowHeight)
                    // Native paragraph intrinsic bounds are not visible ink bounds. In RTL,
                    // combining-mark bounds are unreliable; those captions also get pixel review.
                    if (result.layoutInput.text.none { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }) {
                        (0 until result.lineCount).forEach { line -> assertTrue("$name clips width: ${result.layoutInput.text}",
                            result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1) }
                    }
                }
            }
    }

    private fun capture(name: String, dialog: Boolean = false) {
        val bounds = compose.onNodeWithTag("tasksCapture").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            if (dialog) {
                val sheet = requireNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
                val decor = requireNotNull(sheet.window).decorView
                assertTrue("Sheet window not rendered", decor.width > 0 && decor.height > 0)
                val activityLocation = IntArray(2).also(view::getLocationOnScreen)
                val dialogLocation = IntArray(2).also(decor::getLocationOnScreen)
                val canvas = Canvas(bitmap)
                canvas.translate((dialogLocation[0] - activityLocation[0]).toFloat(), (dialogLocation[1] - activityLocation[1]).toFloat())
                decor.draw(canvas)
            }
            val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val file = File("build/reports/tasks/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(cropped.getPixel(0, 0) != 0)
            bitmap.recycle(); cropped.recycle()
        }
    }
}
