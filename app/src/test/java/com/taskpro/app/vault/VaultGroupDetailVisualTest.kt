package com.taskpro.app.vault

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.WindowManager
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
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.settings.ThemeMode
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
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h778dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VaultGroupDetailVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository
    private val previousLocale = Locale.getDefault()

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        repo.unlock("synthetic visual master".toCharArray(), create = true)
        repo.confirmRecoverySaved()
    }

    @After fun close() { repo.lock(); db.close(); Locale.setDefault(previousLocale); RuntimeEnvironment.setFontScale(1f) }

    @Test fun groupDarkEnglish() = render("group-dark-en", detail = false)
    @Test fun groupDarkPersian() = render("group-dark-fa", detail = false, rtl = true)
    @Test fun groupLightEnglish() = render("group-light-en", detail = false, dark = false)
    @Test fun groupNarrowLargePersian() = render("group-large-fa", detail = false, rtl = true, width = 320, height = 640, scale = 1.6f)
    @Test fun detailDarkEnglish() = render("detail-dark-en", detail = true)
    @Test fun detailDarkPersian() = render("detail-dark-fa", detail = true, rtl = true)
    @Test fun detailLightEnglish() = render("detail-light-en", detail = true, dark = false)
    @Test fun detailLargeEnglish() = render("detail-large-en", detail = true, scale = 1.6f)

    @Suppress("DEPRECATION")
    private fun render(name: String, detail: Boolean, rtl: Boolean = false, dark: Boolean = true,
        width: Int = 360, height: Int = 778, scale: Float = 1f) {
        val locale = Locale(if (rtl) "fa" else "en")
        Locale.setDefault(locale)
        RuntimeEnvironment.setFontScale(scale)
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(locale); fontScale = scale }
        compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        val fixture = VaultDashboardFixtures.document(rtl)
        val record = fixture.records[1].copy(values = fixture.records[1].values + mapOf(
            "url" to "https://synthetic.example.invalid", "email" to "person@example.invalid"))
        runBlocking { repo.edit { fixture.copy(records = fixture.records.map { if (it.id == record.id) record else it }) } }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides compose.activity, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(1f, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("phase05d2Capture")) {
                        VaultScreen(repo, VaultClipboard(compose.activity), {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        val group = if (rtl) "بانک‌ها" else "Banks"
        compose.onNode(hasText(group) and hasClickAction() and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)).performClick()
        if (detail) {
            val title = if (rtl) "ورود به بانک" else "Bank login"
            compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("synthetic-secret-1").assertDoesNotExist()
            compose.runOnIdle {
                val dialog = checkNotNull(ShadowDialog.getLatestDialog())
                assertTrue(dialog.isShowing)
                val view = checkNotNull(dialog.window).decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                save(name, bitmap)
                bitmap.recycle()
            }
        } else {
            compose.onNodeWithText(if (rtl) "زیرگروه‌ها" else "Subgroups").assertExists()
            val bounds = compose.onNodeWithTag("phase05d2Capture").fetchSemanticsNode().boundsInWindow
            compose.runOnIdle {
                val view = compose.activity.window.decorView
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                val cropped = Bitmap.createBitmap(bitmap, bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
                save(name, cropped)
                cropped.recycle(); bitmap.recycle()
            }
        }
    }

    private fun save(name: String, bitmap: Bitmap) {
        assertTrue(bitmap.width >= 300 && bitmap.height >= 500)
        val file = File("build/reports/phase05d-2/$name.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 10_000)
        val samples = HashSet<Int>()
        for (y in 0 until bitmap.height step 16) for (x in 0 until bitmap.width step 16) samples += bitmap.getPixel(x, y)
        assertTrue("Capture is blank", samples.size > 8)
    }
}
