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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.taskpro.app.data.AppDatabase
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.vault.VaultScreen
import com.taskpro.app.ui.screens.vault.VaultLogin
import com.taskpro.app.ui.theme.TaskProTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
class VaultAuthVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val files = TemporaryFolder()
    private val previousLocale = Locale.getDefault()
    private lateinit var db: AppDatabase
    private lateinit var repo: VaultRepository

    @After fun close() {
        if (::repo.isInitialized) repo.lock()
        if (::db.isInitialized) db.close()
        Locale.setDefault(previousLocale)
        RuntimeEnvironment.setFontScale(1f)
    }

    @Test fun lockedDarkEnglish() = render("locked-dark-en")
    @Test fun lockedLightPersian() = render("locked-light-fa", rtl = true, dark = false)
    @Test @Config(qualifiers = "fa-rIR-w320dp-h640dp-mdpi")
    fun lockedNarrowLargePersian() = render("locked-large-fa", rtl = true, width = 320, height = 640, scale = 1.6f)
    @Test fun masterEntryDarkEnglish() = render("master-dark-en", state = "master")
    @Test fun recoveryEntryDarkEnglish() = render("recovery-dark-en", state = "recovery")
    @Test fun firstRunDarkEnglish() = render("first-run-dark-en", state = "first-run")
    @Test fun deviceSetupLightEnglish() = render("device-setup-light-en", dark = false, state = "device")

    @Suppress("DEPRECATION")
    private fun render(name: String, rtl: Boolean = false, dark: Boolean = true, state: String = "locked",
                       width: Int = 360, height: Int = 778, scale: Float = 1f) {
        val locale = Locale(if (rtl) "fa" else "en")
        Locale.setDefault(locale)
        RuntimeEnvironment.setFontScale(scale)
        val config = Configuration(compose.activity.resources.configuration).apply {
            setLocale(locale); fontScale = scale; screenWidthDp = width; screenHeightDp = height
        }
        compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        repo = VaultRepository(db.vaultDao()) { System.nanoTime() / 1_000_000 }
        if (state != "first-run") runBlocking {
            repo.unlock("synthetic visual master".toCharArray(), create = true)
            repo.confirmRecoverySaved()
            repo.lock()
        }
        val device = if (state == "device") VaultDeviceUnlock(app, repo, File(files.root, "device.json")) else null
        compose.setContent {
            CompositionLocalProvider(LocalContext provides compose.activity, LocalConfiguration provides config,
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(1f, scale)) {
                TaskProTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Box(Modifier.requiredSize(width.dp, height.dp).testTag("authCapture")) {
                        if (state == "first-run") VaultLogin(false, false, null, null, {}, { _, _ -> })
                        else VaultScreen(repo, VaultClipboard(compose.activity), {}, {}, device)
                    }
                }
            }
        }
        compose.waitForIdle()
        if (state == "first-run") {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("New master password").fetchSemanticsNodes().isNotEmpty() }
        } else {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(if (rtl) "گاوصندوق قفل است" else "Vault is locked").fetchSemanticsNodes().isNotEmpty() }
        }
        if (state != "first-run") assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        when (state) {
            "master", "recovery" -> compose.onNodeWithText("Master password").performClick()
            "device" -> compose.onNodeWithContentDescription("Unlock settings").performClick()
        }
        if (state == "recovery") compose.onNodeWithText("Forgot Password?").performClick()
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("authCapture").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle {
            val view = if (state == "recovery") checkNotNull(ShadowDialog.getLatestDialog().window).decorView
                else compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = if (state == "recovery") bitmap else Bitmap.createBitmap(bitmap,
                bounds.left.toInt(), bounds.top.toInt(), bounds.width.toInt(), bounds.height.toInt())
            val samples = HashSet<Int>()
            for (y in 0 until output.height step 16) for (x in 0 until output.width step 16) samples += output.getPixel(x, y)
            assertTrue(output.width >= 300 && output.height >= 500 && samples.size > 8)
            val file = File("build/reports/phase05d-4/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(file.length() > 10_000)
            if (output !== bitmap) output.recycle()
            bitmap.recycle()
        }
    }
}
