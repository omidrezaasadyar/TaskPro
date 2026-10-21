package com.taskpro.app.ui.theme

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.taskpro.app.R
import com.taskpro.app.settings.*
import com.taskpro.app.ui.components.GradientBackground
import com.taskpro.app.ui.components.LocalBackgroundStyle
import com.taskpro.app.ui.components.ProvideBackgroundStyle
import kotlinx.coroutines.runBlocking
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
@Config(sdk = [28], qualifiers = "w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeFoundationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun darkEnglish() = render("dark-en", true, false, 1f)
    @Test fun lightEnglish() = render("light-en", false, false, 1f)
    @Test fun darkPersianLarge() = render("dark-fa-large", true, true, 2f)
    @Test fun lightPersianLarge() = render("light-fa-large", false, true, 2f)
    @Test fun darkPersianMedium() = render("dark-fa-medium", true, true, 1.3f)
    @Test fun lightEnglishMedium() = render("light-en-medium", false, false, 1.5f)

    @Test fun systemOverridesAndAllPersistedBackgroundPreferencesArePreserved() {
        val settings = SettingsRepository(RuntimeEnvironment.getApplication())
        assertEquals(SettingsSnapshot(ThemeMode.SYSTEM, BackgroundStyle.GRADIENT, null),
            runBlocking { settings.snapshot() })
        val mode = mutableStateOf(ThemeMode.SYSTEM)
        val systemDark = mutableStateOf(false)
        val background = mutableStateOf(BackgroundStyle.GRADIENT)
        var observedDark = true
        var observedStyle: BackgroundStyle? = null
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (systemDark.value) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides config) {
                TaskProTheme(mode.value) {
                    ProvideBackgroundStyle(background.value) {
                        val dark = LocalAppDarkTheme.current
                        val style = LocalBackgroundStyle.current
                        SideEffect { observedDark = dark; observedStyle = style }
                        GradientBackground(Modifier.requiredSize(320.dp, 400.dp).testTag("background")) {}
                    }
                }
            }
        }
        ThemeMode.entries.forEach { theme ->
            BackgroundStyle.entries.forEach { style ->
                val snapshot = SettingsSnapshot(theme, style, 123456789L)
                runBlocking { settings.replace(snapshot) }
                listOf(false, true).forEach { night ->
                    compose.runOnIdle { mode.value = theme; background.value = style; systemDark.value = night }
                    compose.runOnIdle {
                        assertEquals(when (theme) {
                            ThemeMode.SYSTEM -> night
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                        }, observedDark)
                        assertEquals(style, observedStyle)
                    }
                    assertEquals(snapshot, runBlocking { settings.snapshot() })
                    val bitmap = capture("background", "${theme.name}-${style.name}-$night")
                    val canvas = if (observedDark) VaurivoDarkColors.canvas else VaurivoLightColors.canvas
                    if (style == BackgroundStyle.PLAIN) assertEquals(canvas.toArgb(), bitmap.getPixel(2, 2))
                    else assertNotEquals("Selected background must not be forced to plain", canvas.toArgb(), bitmap.getPixel(2, 2))
                    bitmap.recycle()
                }
            }
        }
    }

    private fun render(name: String, dark: Boolean, rtl: Boolean, fontScale: Float) {
        val configuration = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply {
            setLocale(Locale(if (rtl) "fa" else "en"))
            // Explicit app selection must override the opposite device theme.
            uiMode = if (dark) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration)
        val palette = if (dark) VaurivoDarkColors else VaurivoLightColors
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)
            ) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    assertEquals(dark, LocalAppDarkTheme.current)
                    assertEquals(palette, LocalVaurivoColors.current)
                    val scheme = MaterialTheme.colorScheme
                    assertEquals(palette.canvas, scheme.background)
                    assertEquals(palette.surface, scheme.surface)
                    assertEquals(palette.surfaceRaised, scheme.surfaceContainerHighest)
                    assertEquals(palette.textPrimary, scheme.onSurface)
                    assertEquals(palette.textSecondary, scheme.onSurfaceVariant)
                    assertEquals(palette.tasksAccent, scheme.primary)
                    assertEquals(palette.vaultAccent, scheme.secondary)
                    assertEquals(palette.onAccent, scheme.onPrimary)
                    assertEquals(palette.onAccent, scheme.onSecondary)
                    assertEquals(palette.outlineInteractive, scheme.outline)
                    assertEquals(palette.error, scheme.error)
                    assertEquals(Color.Transparent, scheme.surfaceTint)
                    assertEquals(VaurivoShapes.material, MaterialTheme.shapes)
                    assertEquals(AppTypography, MaterialTheme.typography)
                    Column(Modifier.requiredSize(320.dp, 800.dp).background(scheme.background)
                        .testTag("sample").verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(context.getString(R.string.app_name), style = MaterialTheme.typography.headlineLarge,
                            color = scheme.onBackground, modifier = Modifier.fillMaxWidth().testTag("heading"))
                        Surface(color = scheme.surface, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(context.getString(R.string.workspace_tasks), style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.fillMaxWidth().testTag("tasks"))
                                Text(context.getString(R.string.vault_title), style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().testTag("vault"))
                                OutlinedTextField(value = "sample@example.test", onValueChange = {},
                                    modifier = Modifier.fillMaxWidth(), singleLine = false,
                                    shape = VaurivoShapes.control)
                            }
                        }
                        Button(onClick = {}, shape = VaurivoShapes.control,
                            modifier = Modifier.fillMaxWidth().heightIn(min = VaurivoLayout.primaryActionHeight).testTag("action")) {
                            Text(context.getString(R.string.vault_title), modifier = Modifier.fillMaxWidth().testTag("action-label"),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(palette.canvas, palette.surface, palette.surfaceRaised,
                                palette.tasksAccent, palette.vaultAccent).forEachIndexed { index, color ->
                                Box(Modifier.size(40.dp).background(color).testTag("swatch-$index"))
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        listOf("heading", "tasks", "vault", "action-label").forEach { tag ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("$name/$tag clips text: size=${layout.size}, paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height}, lines=${layout.lineCount}, width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
        }
        compose.onNodeWithTag("action").assertHeightIsAtLeast(56.dp)
        val screenshot = capture("sample", name)
        assertEquals(palette.canvas.toArgb(), screenshot.getPixel(1, 1))
        listOf(palette.canvas, palette.surface, palette.surfaceRaised, palette.tasksAccent, palette.vaultAccent)
            .forEachIndexed { index, color ->
                val bounds = compose.onNodeWithTag("swatch-$index").fetchSemanticsNode().boundsInWindow
                val root = compose.onNodeWithTag("sample").fetchSemanticsNode().boundsInWindow
                assertEquals("$name swatch $index", color.toArgb(), screenshot.getPixel(
                    (bounds.center.x - root.left).toInt(), (bounds.center.y - root.top).toInt()))
            }
        screenshot.recycle()
    }

    private fun capture(tag: String, name: String): Bitmap {
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        lateinit var cropped: Bitmap
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(full))
            cropped = Bitmap.createBitmap(full, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val output = File("build/reports/theme-foundation/$name.png")
            output.parentFile!!.mkdirs()
            output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            full.recycle()
        }
        return cropped
    }
}
