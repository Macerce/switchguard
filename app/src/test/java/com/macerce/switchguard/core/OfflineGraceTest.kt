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
        g.hold(off, now = 0, graceMs = 60_000)
        assertTrue(g.cameBack("a", now = 30_000))
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
        assertFalse(OfflineGrace().cameBack("x", now = 0))
    }

    @Test
    fun `sonraki kontrol zamani`() {
        val g = OfflineGrace()
        assertEquals(null, g.nextDueAt())
        g.hold(off, now = 5_000, graceMs = 10_000)
        assertEquals(15_000L, g.nextDueAt())
    }

    @Test
    fun `bekleyen kopma kaydedilip geri yuklenince suresi dolunca verilir`() {
        val g = OfflineGrace()
        g.hold(off, now = 1_000, graceMs = 60_000)
        val restored = OfflineGrace.fromJson(g.toJson())
        assertEquals(61_000L, restored.nextDueAt())
        assertTrue(restored.due(now = 30_000).isEmpty())
        assertEquals(listOf(off), restored.due(now = 61_000))
    }

    @Test
    fun `geri yuklenen bekleme cihaz donerse iptal olur`() {
        val g = OfflineGrace()
        g.hold(off, now = 0, graceMs = 60_000)
        assertTrue(OfflineGrace.fromJson(g.toJson()).cameBack("a", now = 30_000))
    }

    @Test
    fun `bozuk ya da bos kayit bos bekleme listesi verir`() {
        assertEquals(null, OfflineGrace.fromJson("bozuk").nextDueAt())
        assertEquals(null, OfflineGrace.fromJson(null).nextDueAt())
        assertEquals(null, OfflineGrace.fromJson(OfflineGrace().toJson()).nextDueAt())
    }

    @Test
    fun `bekleme suresi dolduktan sonra donus kisa kopma sayilmaz`() {
        val g = OfflineGrace()
        g.hold(off, now = 0, graceMs = 60_000)
        assertFalse(g.cameBack("a", now = 61_000))
        // Süresi dolan kopma hâlâ verilmeyi bekler (önce kopma alarmı, sonra dönüş).
        assertEquals(listOf(off), g.due(now = 61_000))
    }

    @Test
    fun `sure icinde donus kisa kopma sayilir`() {
        val g = OfflineGrace()
        g.hold(off, now = 0, graceMs = 60_000)
        assertTrue(g.cameBack("a", now = 59_000))
        assertTrue(g.due(now = 120_000).isEmpty())
    }
}
