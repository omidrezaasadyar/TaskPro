package com.taskpro.app.ui.screens.reminder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpro.app.R
import com.taskpro.app.util.DateFormat

private val PopupTop = Color(0xFF6D5DF6)
private val PopupBottom = Color(0xFF4F46E5)

/**
 * The "very special" full-screen reminder. A vivid indigo gradient, a pulsing,
 * gently swinging bell, the task title, the current time, and three big
 * actions: Done, Snooze 10 min, Dismiss.
 */
@Composable
fun ReminderPopupScreen(
    title: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "reminder")

    // Pulsing glow behind the bell.
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Bell swing.
    val swing by transition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swing"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PopupTop, PopupBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading
            Text(
                text = stringResource(R.string.reminder_popup_heading),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(28.dp))

            // Pulsing halo + swinging bell
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(180.dp)
                        .scale(pulse)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                )
                Box(
                    Modifier
                        .size(140.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                )
                Icon(
                    painter = painterResource(R.drawable.illustration_reminder_bell),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(swing)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Task card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = DateFormat.formatTime(System.currentTimeMillis()),
                        style = MaterialTheme.typography.titleMedium,
                        color = PopupBottom
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Primary: Done
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PopupBottom
                )
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.reminder_done),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Secondary actions side by side
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedPopupButton(
                    icon = Icons.Default.Snooze,
                    label = stringResource(R.string.reminder_snooze_10),
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f)
                )
                OutlinedPopupButton(
                    icon = Icons.Default.Close,
                    label = stringResource(R.string.reminder_dismiss),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OutlinedPopupButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}
