package com.taskpro.app.tasks

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.taskpro.app.settings.ThemeMode
import com.taskpro.app.ui.screens.LaunchIntro
import com.taskpro.app.ui.screens.IntroMode
import com.taskpro.app.ui.screens.introDayKey
import com.taskpro.app.ui.screens.introFrameAt
import com.taskpro.app.ui.screens.chooseIntroMode
import com.taskpro.app.ui.theme.TaskProTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import java.util.TimeZone

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
        compose.setContent { TaskProTheme(ThemeMode.LIGHT) { LaunchIntro({ finished++ }, IntroMode.NONE) } }
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun fullMotionHasExpectedMilestones() {
        assertEquals(1f, introFrameAt(0f, IntroMode.FULL).body)
        assertEquals(1f, introFrameAt(0f, IntroMode.FULL).morph)
        assertEquals(1f, introFrameAt(500f, IntroMode.FULL).check)
        assertEquals(1f, introFrameAt(1_500f, IntroMode.FULL).morph)
        assertEquals(1f, introFrameAt(2_550f, IntroMode.FULL).tagline)
        assertEquals(1f, introFrameAt(3_000f, IntroMode.FULL).exit)
        assertEquals(0f, introFrameAt(0f, IntroMode.SHORT).tagline)
        assertEquals(1f, introFrameAt(1_000f, IntroMode.SHORT).exit)
    }

    @Test fun calendarDayKeyChangesAtLocalMidnight() {
        val zone = TimeZone.getTimeZone("Asia/Muscat")
        val midnight = Calendar.getInstance(zone).apply {
            set(2026, Calendar.NOVEMBER, 8, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2026311, introDayKey(midnight - 1, zone))
        assertEquals(2026312, introDayKey(midnight, zone))
        assertEquals(IntroMode.FULL, chooseIntroMode(2026311, 2026312, true))
        assertEquals(IntroMode.SHORT, chooseIntroMode(2026312, 2026312, true))
        assertEquals(IntroMode.NONE, chooseIntroMode(2026311, 2026312, false))
    }

    @Test fun darkIntroFramesCompleteInThreeSeconds() = renderFrames(ThemeMode.DARK, "dark")
    @Test fun lightIntroFramesCompleteInThreeSeconds() = renderFrames(ThemeMode.LIGHT, "light")
    @Test fun shortIntroCompletesInOneSecond() {
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { TaskProTheme(ThemeMode.DARK) { LaunchIntro({ finished++ }, IntroMode.SHORT) } }
        compose.mainClock.advanceTimeBy(750)
        compose.runOnIdle { assertEquals(0, finished) }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertEquals(1, finished) }
    }

    private fun renderFrames(theme: ThemeMode, name: String) {
        var finished = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { TaskProTheme(theme) { LaunchIntro({ finished++ }) } }
        compose.mainClock.advanceTimeByFrame()
        capture("$name-start")
        compose.mainClock.advanceTimeBy(2_500)
        compose.onNodeWithText("Vaurivo").assertExists()
        capture("$name-finish")
        compose.mainClock.advanceTimeBy(550)
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
