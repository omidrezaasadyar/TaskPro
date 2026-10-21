package com.taskpro.app.vault

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowManager
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
import androidx.room.Room
import com.taskpro.app.R
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.components.shared.BottomNavigationBar
import com.taskpro.app.ui.components.shared.WorkspaceTab
import com.taskpro.app.ui.screens.vault.VaultScreen
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w1000dp-h1400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VaultDashboardVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private val previousLocale = Locale.getDefault()
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { 1000L }
        repo.unlock("synthetic visual master".toCharArray(), create = true); repo.confirmRecoverySaved()
    }
    @After fun close() { repo.lock(); db.close(); Locale.setDefault(previousLocale); RuntimeEnvironment.setFontScale(1f) }
    @Test fun dashboardDarkEnglish() = render("vault-dashboard-dark-en")
    @Test fun dashboardDarkPersian() = render("vault-dashboard-dark-fa", rtl = true)
    @Test fun dashboardLight() = render("vault-dashboard-light-en", dark = false)
    @Test fun nestedFolder() = render("vault-folder-nested", nested = true)
    @Test fun narrowLargeEnglish() = render("vault-dashboard-en-large", width = 320, height = 640, scale = 2f)
    @Test fun narrowLargePersian() = render("vault-dashboard-fa-large", width = 320, height = 640, scale = 2f, rtl = true)
    @Test fun landscape() = render("vault-dashboard-landscape", width = 640, height = 360)
    @Test fun wide() = render("vault-dashboard-wide", width = 1000, height = 850)
    @Test fun referenceSized() = render("vault-dashboard-reference-sized", height = 723)
    @Test fun emptyVault() = render("vault-dashboard-empty", empty = true)

    @Suppress("DEPRECATION")
    private fun render(name: String, width: Int = 360, height: Int = 778, scale: Float = 1f,
        rtl: Boolean = false, dark: Boolean = true, nested: Boolean = false, empty: Boolean = false) {
        val locale = Locale(if (rtl) "fa" else "en"); Locale.setDefault(locale)
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(locale) }
        compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        val context = compose.activity
        val clipboard = VaultClipboard(context)
        val fixture = VaultDashboardFixtures.document(rtl)
        runBlocking { repo.edit { if (empty) VaultDocument() else fixture } }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(1f, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("vaultCapture")) {
                        Scaffold(bottomBar = { BottomNavigationBar(WorkspaceTab.Vault, {}, vaultLocked = false) },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                            Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                                // Use the real repository gate and real secure window, not an active=true preview override.
                                VaultScreen(repo, clipboard, {}, {})
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        if (empty) {
            compose.onNodeWithText("0 groups · 0 items").assertExists()
            capture(name)
            verify(name)
            return
        }
        if (nested) {
            compose.onNode(hasText("Banks") and hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).performClick()
            compose.onNode(hasText("Savings") and hasClickAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).performScrollTo().performClick()
            compose.onNode(hasText("Banks / Savings") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        }
        capture(name)
        verify(name)
        val last = if (nested) "Savings login" else fixture.records.single { it.title == if (rtl) "یادداشت شخصی" else "Unfiled account" }.title
        compose.onAllNodes(hasScrollToNodeAction()).onLast().performScrollToNode(hasText(last))
        compose.onNodeWithText(last).assertIsDisplayed()
        val row = compose.onNode(hasText(last) and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val action = compose.onNode(hasText(context.getString(R.string.vault_add_item)) and hasClickAction()).fetchSemanticsNode().boundsInRoot
        assertTrue("Final row covered by Add Item", row.bottom <= action.top)
        capture("$name-scrolled")
        verify(name)
    }
    private fun verify(name: String) {
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { it.config.getOrNull(SemanticsProperties.Disabled) == null }.forEach {
            assertTrue("$name short target: ${it.config}", it.layoutInfo.coordinates.size.height >= 48)
            assertTrue("$name narrow target: ${it.config}", it.layoutInfo.coordinates.size.width >= 48)
        }
        compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.config.contains(SemanticsActions.GetTextLayoutResult) }.forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
                results.forEach { result ->
                    assertFalse("$name clips height: ${result.layoutInput.text}", result.didOverflowHeight)
                    if (result.layoutInput.text.none { Character.getType(it) == Character.NON_SPACING_MARK.toInt() })
                        (0 until result.lineCount).forEach { line -> assertTrue("$name clips width: ${result.layoutInput.text}",
                            result.getLineRight(line) - result.getLineLeft(line) <= result.size.width + 1) }
                }
            }
    }
    private fun capture(name: String) {
        val bounds = compose.onNodeWithTag("vaultCapture").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val file = File("build/reports/vault-dashboard/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(cropped.getPixel(0, 0) != 0)
            cropped.recycle(); bitmap.recycle()
        }
    }
}
