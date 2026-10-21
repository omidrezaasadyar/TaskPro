package com.taskpro.app.vault

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.vault.RecordEditor
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h778dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VaultEditorVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val previousLocale = Locale.getDefault()

    @After fun reset() { Locale.setDefault(previousLocale); RuntimeEnvironment.setFontScale(1f) }

    @Test fun addDarkEnglish() = render("add-dark-en")
    @Test fun addDarkPersian() = render("add-dark-fa", rtl = true)
    @Test fun editDarkEnglish() = render("edit-dark-en", edit = true)
    @Test fun generatorDarkEnglish() = render("generator-dark-en", generator = true)
    @Test fun addLightEnglish() = render("add-light-en", dark = false)
    @Test @Config(qualifiers = "fa-rIR-w320dp-h640dp-mdpi")
    fun addNarrowLargePersian() = render("add-large-fa", rtl = true, width = 320, height = 640, scale = 1.6f)

    @Suppress("DEPRECATION")
    private fun render(name: String, rtl: Boolean = false, dark: Boolean = true, edit: Boolean = false,
                       generator: Boolean = false,
                       width: Int = 360, height: Int = 778, scale: Float = 1f) {
        val locale = Locale(if (rtl) "fa" else "en")
        Locale.setDefault(locale)
        RuntimeEnvironment.setFontScale(scale)
        val config = Configuration(compose.activity.resources.configuration).apply {
            setLocale(locale); fontScale = scale; screenWidthDp = width; screenHeightDp = height
        }
        compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        val category = VaultCategory(id = "synthetic-group", name = if (rtl) "حساب‌ها" else "Accounts")
        val existing = VaultRecord(id = "synthetic-item", categoryId = category.id,
            title = if (rtl) "ورود آزمایشی" else "Synthetic login",
            values = mapOf("username" to "demo-user", "password" to "synthetic-secret"))
        val document = VaultDocument(categories = listOf(category), records = if (edit) listOf(existing) else emptyList())
        val draft = if (edit) existing else VaultRecord(categoryId = category.id)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides compose.activity, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(1f, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    RecordEditor(draft, document, false, {}) {}
                }
            }
        }
        compose.waitForIdle()
        if (generator) {
            compose.onNodeWithText("Generate password").performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
            compose.waitForIdle()
        }
        compose.runOnIdle {
            val dialog = checkNotNull(ShadowDialog.getLatestDialog())
            assertTrue(dialog.isShowing)
            val view = checkNotNull(dialog.window).decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            assertTrue(bitmap.width >= 300 && bitmap.height >= 500)
            val samples = HashSet<Int>()
            for (y in 0 until bitmap.height step 16) for (x in 0 until bitmap.width step 16) samples += bitmap.getPixel(x, y)
            assertTrue("Capture is blank", samples.size > 8)
            val file = File("build/reports/phase05d-3/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(file.length() > 10_000)
            bitmap.recycle()
        }
    }
}
