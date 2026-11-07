package com.taskpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpro.app.R
import com.taskpro.app.ui.theme.LocalVaurivoColors
import com.taskpro.app.ui.theme.VaurivoSpacing
import java.util.Calendar
import java.util.TimeZone

enum class IntroMode { NONE, FULL, SHORT }

internal fun chooseIntroMode(lastFullDay: Int, today: Int, canAnimate: Boolean): IntroMode =
    when {
        !canAnimate -> IntroMode.NONE
        lastFullDay == today -> IntroMode.SHORT
        else -> IntroMode.FULL
    }

internal fun introDayKey(now: Long, zone: TimeZone): Int = Calendar.getInstance(zone).run {
    timeInMillis = now
    get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
}

internal data class IntroFrame(
    val check: Float, val morph: Float, val body: Float, val shackle: Float,
    val rise: Float, val title: Float, val tagline: Float, val exit: Float
)

private fun smooth(value: Float): Float {
    val x = value.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

internal fun introFrameAt(ms: Float, mode: IntroMode): IntroFrame {
    if (mode == IntroMode.SHORT) return IntroFrame(1f, 1f, 1f, 1f, 1f,
        smooth((ms - 100f) / 360f), 0f, smooth((ms - 780f) / 220f))
    val handoff = 1f - smooth(ms / 120f)
    val morph = maxOf(handoff, smooth((ms - 900f) / 600f))
    val trace = 1f - (1f - ms / 500f).coerceIn(0f, 1f).let { it * it * it }
    return IntroFrame(maxOf(handoff, trace), morph,
        maxOf(handoff, smooth((ms - 900f) / 540f)),
        maxOf(handoff, smooth((ms - 1020f) / 470f)),
        smooth((ms - 1480f) / 600f),
        smooth((ms - 2020f) / 350f),
        smooth((ms - 2220f) / 290f),
        smooth((ms - 2780f) / 220f))
}

private fun segment(path: Path, fraction: Float): Path {
    val measure = PathMeasure().apply { setPath(path, false) }
    return Path().also { measure.getSegment(0f, measure.length * fraction.coerceIn(0f, 1f), it, true) }
}

private fun checkPath(morph: Float) = Path().apply {
    fun between(a: Float, b: Float) = a + (b - a) * morph
    moveTo(between(34f, 43f), between(65f, 84f))
    lineTo(between(50f, 55f), between(81f, 95f))
    lineTo(between(83f, 79f), between(43f, 70f))
}

private fun shacklePath() = Path().apply {
    moveTo(38f, 55f); lineTo(38f, 39f)
    cubicTo(38f, 24f, 48f, 13f, 60f, 13f)
    cubicTo(72f, 13f, 82f, 24f, 82f, 39f)
    lineTo(82f, 55f)
}

@Composable
fun LaunchIntro(onFinished: () -> Unit, mode: IntroMode = IntroMode.FULL) {
    val colors = LocalVaurivoColors.current
    val progress = remember { Animatable(0f) }
    val duration = if (mode == IntroMode.SHORT) 1_000 else 3_000
    var finished by remember { mutableStateOf(false) }
    fun finish() { if (!finished) { finished = true; onFinished() } }
    LaunchedEffect(mode) {
        if (mode == IntroMode.NONE) finish()
        else { progress.animateTo(1f, tween(duration, easing = LinearEasing)); finish() }
    }
    BackHandler(mode != IntroMode.NONE) { finish() }
    Box(Modifier.fillMaxSize().background(colors.canvas)
        .graphicsLayer { alpha = 1f - introFrameAt(progress.value * duration, mode).exit }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.Button, onClick = ::finish), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ms = progress.value * duration
            val frame = introFrameAt(ms, mode)
            val px = 1.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val iconCenter = center.copy(y = center.y - 64f * frame.rise * px)
            val waveShift = kotlin.math.sin(ms / 470f) * 7f * px
            val upper = Path().apply {
                moveTo(-40f * px, size.height * .29f + waveShift)
                cubicTo(size.width * .28f, size.height * .22f + waveShift,
                    size.width * .55f, size.height * .33f + waveShift,
                    size.width + 40f * px, size.height * .25f + waveShift)
            }
            drawPath(upper, colors.vaultAccent.copy(alpha = .045f), style = Stroke(1f * px))
            val lower = Path().apply {
                moveTo(-40f * px, size.height * .73f - waveShift)
                cubicTo(size.width * .3f, size.height * .66f - waveShift,
                    size.width * .62f, size.height * .8f - waveShift,
                    size.width + 40f * px, size.height * .69f - waveShift)
            }
            drawPath(lower, colors.tasksAccent.copy(alpha = .05f), style = Stroke(1f * px))
            if (mode == IntroMode.FULL) {
                for (i in 0..2) {
                    val start = 500f + 140f * i
                    if (ms in start..(start + 650f)) {
                        val phase = ((ms - start) / 650f).coerceIn(0f, 1f)
                        drawCircle(colors.vaultAccent, (42f + 95f * smooth(phase)) * px,
                            iconCenter, .35f * (1f - phase), style = Stroke(1.5f * px))
                    }
                    val inwardStart = 1500f + 100f * i
                    if (ms in inwardStart..(inwardStart + 420f)) {
                        val phase = ((ms - inwardStart) / 420f).coerceIn(0f, 1f)
                        drawCircle(colors.vaultAccent, (132f - 76f * smooth(phase)) * px,
                            iconCenter, .28f * (1f - phase), style = Stroke(1.5f * px))
                    }
                }
                if (ms in 1840f..2090f) {
                    val phase = (ms - 1840f) / 250f
                    drawCircle(colors.vaultAccent, (56f + 18f * phase) * px, iconCenter,
                        .2f * (1f - phase), style = Stroke(2f * px))
                }
            }
            withTransform({
                translate(iconCenter.x - 60f * px, iconCenter.y - 65f * px)
                scale(px, px, pivot = Offset.Zero)
            }) {
                val body = Path().apply {
                    addRoundRect(RoundRect(Rect(27f, 54f, 93f, 116f), CornerRadius(14f)))
                }
                drawPath(body, colors.tasksAccent.copy(alpha = .075f * frame.body))
                drawPath(segment(body, frame.body), colors.tasksAccent, style = Stroke(4f))
                drawPath(segment(shacklePath(), frame.shackle), colors.tasksAccent,
                    style = Stroke(5f, cap = StrokeCap.Round))
                drawPath(segment(checkPath(frame.morph), frame.check), colors.tasksAccent,
                    style = Stroke(5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Text(stringResource(R.string.app_name),
            Modifier.align(Alignment.Center).offset(y = 104.dp)
                .graphicsLayer {
                    val value = introFrameAt(progress.value * duration, mode).title
                    alpha = value; translationY = (1f - value) * 9.dp.toPx()
                },
            style = MaterialTheme.typography.headlineMedium, letterSpacing = 1.1.sp,
            color = colors.textPrimary)
        if (mode == IntroMode.FULL) Text(stringResource(R.string.intro_tagline),
            Modifier.align(Alignment.Center).offset(y = 140.dp)
                .padding(horizontal = VaurivoSpacing.section)
                .graphicsLayer {
                    val value = introFrameAt(progress.value * duration, mode).tagline
                    alpha = value; translationY = (1f - value) * 7.dp.toPx()
                },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary, textAlign = TextAlign.Center)
        Text(stringResource(R.string.intro_skip),
            Modifier.align(Alignment.BottomCenter).padding(bottom = VaurivoSpacing.section)
                .graphicsLayer { alpha = 1f - introFrameAt(progress.value * duration, mode).title },
            style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
