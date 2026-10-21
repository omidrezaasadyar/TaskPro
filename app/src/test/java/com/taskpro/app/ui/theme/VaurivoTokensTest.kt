package com.taskpro.app.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class VaurivoTokensTest {
    @Test fun palettesMatchApprovedOpaqueValuesAndScrimOpacity() {
        fun opaque(p: VaurivoColors) = listOf(p.canvas, p.surface, p.surfaceRaised,
            p.borderSubtle, p.outlineInteractive, p.textPrimary, p.textSecondary, p.textTertiary,
            p.disabledContainer, p.disabledContent, p.tasksAccent, p.tasksContainer,
            p.vaultAccent, p.vaultContainer, p.onAccent, p.success, p.warning, p.error,
            p.focusTasks, p.focusVault)
        fun expected(values: String) = values.split(" ").map { Color(0xFF000000L or it.toLong(16)) }
        assertEquals(expected("0C1110 131B19 1B2522 26322E 71827A E7EEEC A5B3AC 8C9D94 1A211E 708079 ADB8FF 24283B 5EDFB2 19362C 0C1110 5EDFB2 F0C06B FF8A7A C7CEFF 8CEBC9"), opaque(VaurivoDarkColors))
        assertEquals(expected("F4F7F5 FFFFFF EAF0EC D2DCD6 718279 17231D 4D6256 596D61 E4EBE6 77857C 4654A6 E8ECFF 087556 DDF3E9 FFFFFF 147346 8A5700 B33C32 4654A6 087556"), opaque(VaurivoLightColors))
        assertEquals(Color.Black.copy(alpha = .56f), VaurivoDarkColors.scrim)
        assertEquals(Color.Black.copy(alpha = .32f), VaurivoLightColors.scrim)
    }

    @Test fun activeTextAndRequiredOutlinesMeetContrastTargets() {
        listOf(VaurivoDarkColors, VaurivoLightColors).forEach { p ->
            listOf(p.canvas, p.surface, p.surfaceRaised).forEach { background ->
                listOf(p.textPrimary, p.textSecondary, p.textTertiary, p.tasksAccent,
                    p.vaultAccent, p.success, p.warning, p.error).forEach { foreground ->
                    assertContrast(foreground, background, 4.5)
                }
                listOf(p.outlineInteractive, p.focusTasks, p.focusVault).forEach {
                    assertContrast(it, background, 3.0)
                }
            }
            listOf(p.tasksContainer, p.vaultContainer).forEach {
                assertContrast(p.textPrimary, it, 4.5)
                assertContrast(p.textSecondary, it, 4.5)
            }
            listOf(p.tasksAccent, p.vaultAccent, p.success, p.error).forEach {
                assertContrast(p.onAccent, it, 4.5)
            }
        }
    }

    @Test fun typographyUsesBundledFontZeroTrackingAndApprovedRoleSizes() {
        val roles = with(AppTypography) { listOf(displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall, titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall) }
        assertEquals(listOf(28, 28, 28, 24, 24, 20, 20, 16, 14, 16, 14, 12, 14, 12, 12),
            roles.map { it.fontSize.value.toInt() })
        assertEquals(listOf(36, 36, 36, 32, 32, 28, 28, 24, 20, 24, 22, 18, 20, 18, 18),
            roles.map { it.lineHeight.value.toInt() })
        roles.forEach {
            assertEquals(VazirFontFamily, it.fontFamily)
            assertEquals(0.sp, it.letterSpacing)
            assertTrue(it.fontSize.value >= 12)
        }
    }

    @Test fun geometryAndElevationMatchReferenceContracts() {
        assertEquals(listOf(4, 8, 12, 16, 20, 24, 32, 40, 48).map { it.dp },
            with(VaurivoSpacing) { listOf(extraSmall, small, medium, large, extraLarge,
                section, sectionLarge, sectionExtraLarge, display) })
        assertEquals(16.dp, VaurivoLayout.compactScreenMargin)
        assertEquals(24.dp, VaurivoLayout.expandedScreenMargin)
        assertEquals(16.dp, VaurivoLayout.cardPadding)
        assertEquals(12.dp, VaurivoLayout.densePadding)
        assertEquals(48.dp, VaurivoLayout.minimumTouchTarget)
        assertEquals(56.dp, VaurivoLayout.primaryActionHeight)
        assertEquals(80.dp, VaurivoLayout.navigationHeight)
        assertEquals(24.dp, VaurivoLayout.iconSize)
        assertEquals(20.dp, VaurivoLayout.compactIconSize)
        val size = Size(200f, 300f)
        val density = Density(1f)
        assertEquals(8f, VaurivoShapes.utility.topStart.toPx(size, density), 0f)
        assertEquals(12f, VaurivoShapes.control.topStart.toPx(size, density), 0f)
        assertEquals(16f, VaurivoShapes.card.topStart.toPx(size, density), 0f)
        assertEquals(24f, VaurivoShapes.sheet.topStart.toPx(size, density), 0f)
        assertEquals(24f, VaurivoShapes.sheet.topEnd.toPx(size, density), 0f)
        assertEquals(0f, VaurivoShapes.sheet.bottomStart.toPx(size, density), 0f)
        assertEquals(0f, VaurivoShapes.sheet.bottomEnd.toPx(size, density), 0f)
        assertEquals(1.dp, VaurivoStroke.border)
        assertEquals(2.dp, VaurivoStroke.focus)
        assertEquals(2.dp, VaurivoStroke.focusOffset)
        assertEquals(0.dp, VaurivoElevation.card)
        assertEquals(4.dp, VaurivoElevation.overlay)
    }

    @Test fun motionTokensHaveApprovedDurationsAndBoundedEasings() {
        assertEquals(listOf(120, 160, 180, 220, 240, 2000), with(VaurivoMotion) {
            listOf(PressMillis, FastMillis, StandardMillis, EntranceMillis, SheetMillis, PulseMillis)
        })
        listOf(VaurivoMotion.enterEasing, VaurivoMotion.stateEasing, VaurivoMotion.progressEasing).forEach { easing ->
            assertEquals(0f, easing.transform(0f), 0f)
            assertEquals(1f, easing.transform(1f), 0f)
            (0..100).forEach { assertTrue(easing.transform(it / 100f) in 0f..1f) }
        }
    }

    @Test fun storedAndImplicitTaskGroupColorsRemainUnchanged() {
        val expected = listOf(0xFF4F46E5, 0xFF0D9488, 0xFFD81B60, 0xFF8E24AA, 0xFF1E88E5,
            0xFFF4511E, 0xFF43A047, 0xFF6D4C41, 0xFF00897B, 0xFF3949AB).map { Color(it) }
        assertEquals(expected, ItemAccentColors)
        expected.indices.forEach { assertEquals(expected[it], itemAccentColor(it.toLong(), null)) }
        assertEquals(Color(0xFF123456), itemAccentColor(42, 0xFF123456.toInt()))
    }

    private fun assertContrast(foreground: Color, background: Color, minimum: Double) {
        fun luminance(color: Color): Double {
            fun linear(channel: Float): Double = channel.toDouble().let {
                if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4)
            }
            return .2126 * linear(color.red) + .7152 * linear(color.green) + .0722 * linear(color.blue)
        }
        val a = luminance(foreground)
        val b = luminance(background)
        val ratio = (max(a, b) + .05) / (min(a, b) + .05)
        assertTrue("$foreground on $background: $ratio < $minimum", ratio >= minimum)
    }
}
