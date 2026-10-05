package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpectedChangesTest {

    private fun sw(to: Boolean, ch: Int = 0) = Change.Switch("a", "A", ch, false, from = !to, to = to)

    @Test
    fun `uygulamadan yapilan degisiklik bir kez yutulur`() {
        val e = ExpectedChanges()
        e.expect("a", 0, true, now = 0)
        assertTrue(e.consume(sw(true), now = 1_000))
        assertFalse(e.consume(sw(true), now = 2_000))
    }

    @Test
    fun `farkli yon veya kanal yutulmaz`() {
        val e = ExpectedChanges()
        e.expect("a", 0, true, now = 0)
        assertFalse(e.consume(sw(false), now = 1_000))
        assertFalse(e.consume(sw(true, ch = 1), now = 1_000))
    }

    @Test
    fun `suresi dolan beklenti yutmaz`() {
        val e = ExpectedChanges()
        e.expect("a", 0, true, now = 0)
        assertFalse(e.consume(sw(true), now = ExpectedChanges.WINDOW_MS + 1))
    }

    @Test
    fun `baglanti degisikligi asla yutulmaz`() {
        val e = ExpectedChanges()
        e.expect("a", 0, true, now = 0)
        assertFalse(e.consume(Change.Connection("a", "A", true, false), now = 10))
    }
}
