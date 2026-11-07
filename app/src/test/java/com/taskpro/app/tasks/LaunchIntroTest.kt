package com.taskpro.app.tasks

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.LaunchIntro
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h778dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchIntroTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun tapSkipsWithoutWaitingForAnimation() {
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { TaskProTheme(ThemeMode.DARK) { LaunchIntro({ finished++ }) } }
        compose.onNodeWithText("Tap to skip").performClick()
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun disabledAnimationFinishesImmediately() {
        var finished = 0
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) { LaunchIntro({ finished++ }, animate = false) } }
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun darkIntroFramesCompleteWithinTwoSeconds() = renderFrames(ThemeMode.DARK, "dark")
    @Test fun lightIntroFramesCompleteWithinTwoSeconds() = renderFrames(ThemeMode.LIGHT, "light")

    private fun renderFrames(theme: ThemeMode, name: String) {
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { TaskProTheme(theme) { LaunchIntro({ finished++ }) } }
        compose.mainClock.advanceTimeByFrame()
        capture("$name-start")
        compose.mainClock.advanceTimeBy(1_350)
        compose.onNodeWithText("Vaurivo").assertExists()
        capture("$name-finish")
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(1, finished) }
    }

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val file = File("build/reports/launch-intro/$name.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
