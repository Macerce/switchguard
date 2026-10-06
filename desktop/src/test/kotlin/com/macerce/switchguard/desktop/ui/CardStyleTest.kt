package com.macerce.switchguard.desktop.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CardStyleTest {
    @Test
    fun aDeviceMarkedSmallIsSmallWhateverTheGeneralSize() {
        assertEquals(CardStyle.SMALL, CardStyle.of("large", compact = true))
        assertEquals(CardStyle.SMALL, CardStyle.of("medium", compact = true))
    }

    @Test
    fun otherwiseTheGeneralSizeApplies() {
        assertEquals(CardStyle.LARGE, CardStyle.of("large", compact = false))
        assertEquals(CardStyle.MEDIUM, CardStyle.of("medium", compact = false))
        assertEquals(CardStyle.SMALL, CardStyle.of("small", compact = false))
        assertEquals(CardStyle.LARGE, CardStyle.of("bilinmeyen", compact = false))
    }

    @Test
    fun smallerSizesFitMoreCardsInARow() {
        val large = CardStyle.gridMinWidth("large")
        val medium = CardStyle.gridMinWidth("medium")
        val small = CardStyle.gridMinWidth("small")
        assertEquals(true, large > medium && medium > small)
    }
}
