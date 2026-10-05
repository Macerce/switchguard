package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineGraceTest {

    private val off = Change.Connection("a", "A", from = true, to = false)

    @Test
    fun `sure dolmadan geri gelirse hic olay yok`() {
        val g = OfflineGrace()
        g.hold(off, now = 0)
        assertTrue(g.cameBack("a"))
        assertTrue(g.due(now = 999_999).isEmpty())
    }

    @Test
    fun `sure dolunca offline olayi verilir, bir kez`() {
        val g = OfflineGrace()
        g.hold(off, now = 1_000, graceMs = 60_000)
        assertTrue(g.due(now = 30_000).isEmpty())
        assertEquals(listOf(off), g.due(now = 61_000))
        assertTrue(g.due(now = 120_000).isEmpty())
    }

    @Test
    fun `beklemede olmayan cihaz icin cameBack false`() {
        assertFalse(OfflineGrace().cameBack("x"))
    }

    @Test
    fun `sonraki kontrol zamani`() {
        val g = OfflineGrace()
        assertEquals(null, g.nextDueAt())
        g.hold(off, now = 5_000, graceMs = 10_000)
        assertEquals(15_000L, g.nextDueAt())
    }
}
