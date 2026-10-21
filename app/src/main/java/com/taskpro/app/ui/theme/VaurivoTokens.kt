package com.taskpro.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object VaurivoSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 20.dp
    val section = 24.dp
    val sectionLarge = 32.dp
    val sectionExtraLarge = 40.dp
    val display = 48.dp
}

object VaurivoLayout {
    val compactScreenMargin = 16.dp
    val expandedScreenMargin = 24.dp
    val cardPadding = 16.dp
    val densePadding = 12.dp
    val minimumTouchTarget = 48.dp
    val primaryActionHeight = 56.dp
    val iconSize = 24.dp
    val compactIconSize = 20.dp
    val navigationHeight = 80.dp
}

object VaurivoShapes {
    val utilityRadius = 8.dp
    val controlRadius = 12.dp
    val cardRadius = 16.dp
    val sheetRadius = 24.dp
    val utility = RoundedCornerShape(utilityRadius)
    val control = RoundedCornerShape(controlRadius)
    val card = RoundedCornerShape(cardRadius)
    val sheet = RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius)
    val material = Shapes(
        extraSmall = utility,
        small = control,
        medium = card,
        large = card,
        extraLarge = RoundedCornerShape(sheetRadius)
    )
}

object VaurivoStroke {
    val border = 1.dp
    val focus = 2.dp
    val focusOffset = 2.dp
}

object VaurivoElevation {
    val card = 0.dp
    val overlay = 4.dp
}

// Durations are unscaled; consumers must honor the system animation scale.
object VaurivoMotion {
    const val PressMillis = 120
    const val FastMillis = 160
    const val StandardMillis = 180
    const val EntranceMillis = 220
    const val SheetMillis = 240
    const val PulseMillis = 2000
    val enterEasing = LinearOutSlowInEasing
    val stateEasing = FastOutSlowInEasing
    val progressEasing = LinearEasing
}
