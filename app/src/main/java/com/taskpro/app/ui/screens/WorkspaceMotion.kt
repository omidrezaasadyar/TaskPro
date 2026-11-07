package com.taskpro.app.ui.screens

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.taskpro.app.ui.theme.LocalAppDarkTheme
import com.taskpro.app.ui.theme.LocalVaurivoColors
import kotlin.math.PI
import kotlin.math.sin

/** Animation clocks exist only while Home is resumed and system animation is enabled. */
@Composable
internal fun rememberWorkspaceMotionActive(): Boolean {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var active by remember(owner) { mutableStateOf(false) }
    DisposableEffect(owner, context) {
        fun update() {
            val enabled = runCatching {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            }.getOrDefault(true)
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && enabled
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        owner.lifecycle.addObserver(observer)
        update()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return active
}

private val ringEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** The mark stays still; only three one-second-staggered circles change in the draw phase. */
@Composable
internal fun HeroRings(animated: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVaurivoColors.current
    val dark = LocalAppDarkTheme.current
    val transition = if (animated) rememberInfiniteTransition(label = "homeRings") else null
    val clock = transition?.animateFloat(0f, 3f,
        infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "ringCycle")
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE } }
    val dots = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL } }
    val mint = palette.vaultAccent.toArgb()
    val dotColor = palette.textTertiary.toArgb()
    Canvas(modifier.clearAndSetSemantics {}) {
        val native = drawContext.canvas.nativeCanvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val density = this.density
        dots.color = dotColor
        dots.alpha = 26
        val step = 18f * density
        var x = step / 2f
        while (x < size.width) {
            var y = step / 2f
            while (y < size.height) {
                native.drawCircle(x, y, 0.7f * density, dots)
                y += step
            }
            x += step
        }
        paint.color = mint
        if (clock == null) {
            paint.alpha = if (dark) 28 else 38
            paint.strokeWidth = density
            native.drawCircle(cx, cy, 92f * density, paint)
            native.drawCircle(cx, cy, 140f * density, paint)
        } else {
            val phase = clock.value // draw-phase read; no frame-by-frame recomposition
            for (index in 2 downTo 0) {
                val age = ((phase + index) % 3f) / 3f
                val radius = (60f + 100f * ringEasing.transform(age)) * density
                paint.alpha = ((if (dark) 0.35f else 0.45f) * (1f - age) * 255f).toInt()
                paint.strokeWidth = (1.5f - 0.75f * age) * density
                native.drawCircle(cx, cy, radius, paint)
            }
        }
    }
}

/** Three cached Android Paths keep the subtle bottom waves allocation-free per frame. */
@Composable
internal fun AmbientWaves(animated: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVaurivoColors.current
    val dark = LocalAppDarkTheme.current
    val transition = if (animated) rememberInfiniteTransition(label = "homeWaves") else null
    val first = transition?.animateFloat(0f, 1f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "lavenderWaveA")
    val second = transition?.animateFloat(0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "mintWave")
    val third = transition?.animateFloat(0f, 1f,
        infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "lavenderWaveB")
    val paths = remember { Array(3) { Path() } }
    val taskColor = palette.tasksAccent.toArgb()
    val vaultColor = palette.vaultAccent.toArgb()
    val paints = remember(taskColor, vaultColor, dark) {
        Array(3) { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE } }
    }
    val lastSize = remember(taskColor, vaultColor, dark) { floatArrayOf(-1f, -1f, -1f) }
    val phases = remember { FloatArray(3) }
    val colors = remember(taskColor, vaultColor) { intArrayOf(taskColor, vaultColor, taskColor) }
    val amplitudes = remember { floatArrayOf(14f, 18f, 10f) }
    val wavelengths = remember { floatArrayOf(1.2f, 1.5f, 1f) }
    val opacity = remember(dark) {
        if (dark) floatArrayOf(.10f, .11f, .09f) else floatArrayOf(.14f, .15f, .13f)
    }
    Canvas(modifier.clearAndSetSemantics {}) {
        val native = drawContext.canvas.nativeCanvas
        phases[0] = first?.value ?: 0f // state reads only in the draw phase
        phases[1] = second?.value ?: 0f
        phases[2] = third?.value ?: 0f
        val density = this.density
        if (lastSize[0] != size.width || lastSize[1] != size.height || lastSize[2] != density) {
            for (index in 0..2) {
                val color = colors[index]
                val alpha = (opacity[index] * 255f).toInt()
                paints[index].shader = LinearGradient(0f, size.height * .65f, 0f, size.height * .86f,
                    intArrayOf(color and 0x00FFFFFF, (alpha shl 24) or (color and 0x00FFFFFF)),
                    null, Shader.TileMode.CLAMP)
                paints[index].strokeWidth = density
            }
            lastSize[0] = size.width
            lastSize[1] = size.height
            lastSize[2] = density
        }
        for (index in 0..2) {
            val path = paths[index]
            path.reset()
            val baseline = size.height * .79f + index * 24f * density
            val wavelength = size.width * wavelengths[index]
            val direction = if (index == 1) -1f else 1f
            var x = 0f
            while (x <= size.width + 4f * density) {
                val angle = 2.0 * PI * (x / wavelength + direction * phases[index])
                val y = baseline + amplitudes[index] * density * sin(angle).toFloat()
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 4f * density
            }
            native.drawPath(path, paints[index])
        }
    }
}
