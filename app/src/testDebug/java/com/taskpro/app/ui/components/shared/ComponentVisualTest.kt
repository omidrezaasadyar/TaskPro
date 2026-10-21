package com.taskpro.app.ui.components.shared

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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.theme.TaskProTheme
import com.taskpro.app.ui.theme.VaurivoDarkColors
import com.taskpro.app.ui.theme.VaurivoLightColors
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
class ComponentVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun darkWorkspace() = render("dark-workspace", 0)
    @Test fun darkVault() = render("dark-vault", 1)
    @Test fun lightVault() = render("light-vault", 1, dark = false)
    @Test fun persianVaultLarge() = render("fa-vault-large", 1, rtl = true, scale = 2f, width = 320)
    @Test fun narrowEnglishLarge() = render("en-workspace-large", 0, scale = 2f, width = 280)
    @Test fun darkGenerator() = render("dark-generator", 2)
    @Test fun darkStates() = render("dark-states", 3)
    @Test fun darkFolderPair() = render("dark-folder-pair", 4, width = 400)
    @Test fun persianGeneratorLarge() = render("fa-generator-large", 2, rtl = true, scale = 2f, width = 320)
    @Test fun lightPersianVaultLarge() = render("light-fa-vault-large", 1, dark = false, rtl = true, scale = 2f, width = 320)
    @Test fun generatedCandidateIsOnlyMaskedInScreenshots() = render("dark-generator-candidate", 2, generate = true)

    private fun render(name: String, page: Int, dark: Boolean = true, rtl: Boolean = false, scale: Float = 1f, width: Int = 360,
                       generate: Boolean = false) {
        val configuration = Configuration(RuntimeEnvironment.getApplication().resources.configuration).apply {
            setLocale(Locale(if (rtl) "fa" else "en"))
        }
        val context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(LocalDensity.current.density, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, 1100.dp).testTag("specimens")) {
                        SharedComponentSpecimens(page, Modifier.fillMaxSize())
                    }
                }
            }
        }
        compose.waitForIdle()
        if (page == 3) {
            val palette = if (dark) VaurivoDarkColors else VaurivoLightColors
            listOf(com.taskpro.app.R.string.vault_empty to palette.textPrimary,
                com.taskpro.app.R.string.component_busy to palette.textSecondary).forEach { (resource, expected) ->
                val results = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(context.getString(resource), useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                assertEquals("State text must not inherit black outside a Surface", expected, results.single().layoutInput.style.color)
            }
        }
        if (generate) {
            compose.onNode(hasText(context.getString(com.taskpro.app.R.string.vault_generate)) and hasClickAction()).performScrollTo().performClick()
            compose.waitForIdle()
        }
        capture(name)
        val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        nodes.filter { it.config.getOrNull(SemanticsProperties.Disabled) == null }.forEach { node ->
            assertTrue("$name: short target ${node.config}", node.layoutInfo.coordinates.size.height >= 48)
            assertTrue("$name: narrow target ${node.config}", node.layoutInfo.coordinates.size.width >= 48)
        }
        // Scroll each real command into view; measure the actual label layout, not an image proxy.
        val commands = compose.onAllNodes(hasClickAction(), useUnmergedTree = false)
            .fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }.distinct()
        commands.forEach { label ->
            val action = compose.onAllNodes(hasText(label) and hasClickAction()).onFirst()
            action.performScrollTo()
            val texts = compose.onAllNodes(hasText(label), useUnmergedTree = true).fetchSemanticsNodes()
            texts.filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach { result ->
                    assertFalse("$name/$label vertically clipped", result.didOverflowHeight)
                    (0 until result.lineCount).forEach { line ->
                        assertTrue("$name/$label horizontally clipped", result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1)
                    }
                }
            }
        }
        if (scale > 1f) capture("$name-scrolled")
    }

    private fun capture(name: String) {
        val bounds = compose.onNodeWithTag("specimens").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val output = File("build/reports/shared-components/$name.png")
            output.parentFile!!.mkdirs()
            output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(cropped.getPixel(0, 0) != 0)
            cropped.recycle(); bitmap.recycle()
        }
    }
}
