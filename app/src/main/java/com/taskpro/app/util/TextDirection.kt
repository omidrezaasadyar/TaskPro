package com.taskpro.app.util

/**
 * Heuristic: returns true if [text] should be laid out right-to-left, i.e. its
 * first strong directional character is in the Arabic/Persian/Hebrew range.
 * Used to align Persian task text to the right and Latin text to the left.
 */
fun isRtlText(text: String): Boolean {
    for (ch in text) {
        when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT ->
                return false
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ->
                return true
            else -> { /* neutral char — keep scanning */ }
        }
    }
    return false
}
