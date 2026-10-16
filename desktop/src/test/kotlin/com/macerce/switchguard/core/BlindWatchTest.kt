package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlindWatchTest {
    private val min = 60_000L
    private val limit = 10 * min

    @Test
    fun `esik dolmadan uyarmaz, dolunca bir kez uyarir`() {
        val w = BlindWatch()
        assertNull(w.update(blind = true, now = 0, thresholdMs = limit))
        assertNull(w.update(blind = true, now = 9 * min, thresholdMs = limit))
        assertEquals(BlindWatch.Signal.Blind(since = 0), w.update(blind = true, now = 10 * min, thresholdMs = limit))
        assertNull(w.update(blind = true, now = 30 * min, thresholdMs = limit))
    }

    @Test
    fun `uyaridan sonra duzelince kesinti suresiyle bildirir`() {
        val w = BlindWatch()
        w.update(true, 5 * min, limit)
        w.update(true, 15 * min, limit)
        assertEquals(BlindWatch.Signal.Recovered(from = 5 * min, to = 40 * min), w.update(false, 40 * min, limit))
        assertNull(w.update(false, 41 * min, limit))
    }

    @Test
    fun `kisa kopma uyari da duzelme de uretmez`() {
        val w = BlindWatch()
        w.update(true, 0, limit)
        assertNull(w.update(false, 3 * min, limit))
        // Yeni kopma sayaci bastan baslatir.
        w.update(true, 4 * min, limit)
        assertNull(w.update(true, 13 * min, limit))
        assertEquals(BlindWatch.Signal.Blind(since = 4 * min), w.update(true, 14 * min, limit))
    }

    @Test
    fun `uyari kapaliysa hic uyarmaz`() {
        val w = BlindWatch()
        w.update(true, 0, 0)
        assertNull(w.update(true, 600 * min, 0))
        assertNull(w.update(false, 601 * min, 0))
    }
}
