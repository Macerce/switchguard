package com.macerce.ewelinkalarm.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeDetectorTest {

    private fun dev(id: String, online: Boolean = true, vararg ch: Pair<Int, Boolean>) =
        DeviceSnapshot(id, "Cihaz $id", online, if (ch.isEmpty()) mapOf(0 to false) else ch.toMap())

    private fun map(vararg d: DeviceSnapshot) = d.associateBy { it.id }

    @Test
    fun `ilk calistirmada onceki durum yoksa degisiklik yok`() {
        val changes = ChangeDetector.detect(emptyMap(), map(dev("a")), setOf("a"))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `ayni durum degisiklik uretmez`() {
        val s = map(dev("a", true, 0 to true))
        assertTrue(ChangeDetector.detect(s, s, setOf("a")).isEmpty())
    }

    @Test
    fun `acik kapali degisimi yakalanir`() {
        val changes = ChangeDetector.detect(
            map(dev("a", true, 0 to true)), map(dev("a", true, 0 to false)), setOf("a")
        )
        assertEquals(listOf(Change.Switch("a", "Cihaz a", 0, false, from = true, to = false)), changes)
    }

    @Test
    fun `baglanti degisimi yakalanir`() {
        val changes = ChangeDetector.detect(
            map(dev("a", true)), map(dev("a", false)), setOf("a")
        )
        assertEquals(listOf(Change.Connection("a", "Cihaz a", from = true, to = false)), changes)
    }

    @Test
    fun `cok kanalli cihazda sadece degisen kanal raporlanir`() {
        val changes = ChangeDetector.detect(
            map(dev("a", true, 0 to true, 1 to false)),
            map(dev("a", true, 0 to true, 1 to true)),
            setOf("a")
        )
        assertEquals(listOf(Change.Switch("a", "Cihaz a", 1, true, from = false, to = true)), changes)
    }

    @Test
    fun `izlenmeyen cihaz yok sayilir`() {
        val changes = ChangeDetector.detect(
            map(dev("a", true), dev("b", true)), map(dev("a", false), dev("b", false)), setOf("b")
        )
        assertEquals(1, changes.size)
        assertEquals("b", changes[0].deviceId)
    }

    @Test
    fun `yeni eklenen cihaz alarm uretmez`() {
        val changes = ChangeDetector.detect(map(dev("a")), map(dev("a"), dev("b")), setOf("a", "b"))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `metin turkce ve anlasilir`() {
        assertEquals(
            "Salon: AÇIK → KAPALI",
            Change.Switch("x", "Salon", 0, false, from = true, to = false).describe()
        )
        assertEquals(
            "Salon (K2): KAPALI → AÇIK",
            Change.Switch("x", "Salon", 1, true, from = false, to = true).describe()
        )
        assertEquals(
            "Salon: ONLINE → OFFLINE",
            Change.Connection("x", "Salon", from = true, to = false).describe()
        )
    }
}
