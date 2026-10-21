package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EwelinkRegionTest {
    @Test
    fun `bilinen bolgeler kabul edilir`() {
        EwelinkRegion.ALL.forEach { assertEquals(it, EwelinkRegion.parse(it)) }
        assertEquals("eu", EwelinkRegion.parse(" EU "))
    }

    @Test
    fun `sunucu adresini degistirebilecek degerler reddedilir`() {
        listOf("evil.com/#", "eu.evil.com", "eu/", "e u", "", "xx").forEach { assertNull(it, EwelinkRegion.parse(it)) }
        assertNull(EwelinkRegion.parse(null))
    }
}
