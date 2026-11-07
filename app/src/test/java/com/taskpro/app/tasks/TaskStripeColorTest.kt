package com.taskpro.app.tasks

import com.taskpro.app.ui.components.shared.taskStripeColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaskStripeColorTest {
    @Test fun colorIsStableBySavedTaskIdAcrossRenders() {
        val id = 928L
        assertEquals(taskStripeColor(id, true), taskStripeColor(id, true))
        assertEquals(taskStripeColor(id, false), taskStripeColor(id, false))
        assertNotEquals(taskStripeColor(id, true), taskStripeColor(id + 1, true))
    }

    @Test fun lightAndDarkHaveMatchingPalettePositionsWithDistinctContrast() {
        assertNotEquals(taskStripeColor(1, true), taskStripeColor(1, false))
        assertEquals(taskStripeColor(1, true), taskStripeColor(7, true))
    }
}
