package com.taskpro.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette
val Indigo = Color(0xFF4F46E5)
val IndigoDark = Color(0xFF6366F1)
val Teal = Color(0xFF0D9488)

// Status colours (shared across light & dark)
val StatusPending = Color(0xFFE53935)  // red   — to do
val StatusCompleted = Color(0xFF2E7D32) // green — done
val StatusSnoozed = Color(0xFFF57C00)  // orange — snoozed

// Soft container tints for status chips/cards
val PendingContainer = Color(0xFFFFEBEE)
val CompletedContainer = Color(0xFFE8F5E9)
val SnoozedContainer = Color(0xFFFFF3E0)

val PendingContainerDark = Color(0xFF4E2A2A)
val CompletedContainerDark = Color(0xFF24402A)
val SnoozedContainerDark = Color(0xFF4A3414)

/**
 * Palette used to give each item its own coloured stripe. Items are assigned a
 * colour deterministically (see [itemAccentColor]) so the same item always
 * keeps the same colour and adjacent items are easy to tell apart.
 */
val ItemAccentColors = listOf(
    Color(0xFF4F46E5), // indigo
    Color(0xFF0D9488), // teal
    Color(0xFFD81B60), // pink
    Color(0xFF8E24AA), // purple
    Color(0xFF1E88E5), // blue
    Color(0xFFF4511E), // deep orange
    Color(0xFF43A047), // green
    Color(0xFF6D4C41), // brown
    Color(0xFF00897B), // teal-green
    Color(0xFF3949AB), // indigo-blue
)

/**
 * Returns a stable accent colour for an item. If the item has an explicit
 * [colorArgb] it wins; otherwise one is picked from [ItemAccentColors] based on
 * the item id so it never changes.
 */
fun itemAccentColor(id: Long, colorArgb: Int?): Color =
    colorArgb?.let { Color(it) }
        ?: ItemAccentColors[((id % ItemAccentColors.size).toInt() + ItemAccentColors.size) % ItemAccentColors.size]
