package com.macerce.switchguard.data

import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class EventLogTest {
    @get:Rule val tmp = TemporaryFolder()

    private val opened = mutableListOf<EventLog>()

    private fun file(): Path = tmp.root.toPath().resolve("events.db")

    private fun log(): EventLog = EventLog(file()).also { opened += it }

    /** Windows açık veritabanı dosyasını silemez; geçici klasör temizlenmeden önce bağlantıları kapat. */
    @After fun closeAll() = opened.forEach { it.close() }

    @Test
    fun addAndRecentNewestFirst() {
        val log = log()
        assertTrue(log.recent().isEmpty())
        log.add("d1", "Pump", EventKind.USER, "first", "", time = 1_000)
        log.add("d1", "Pump", EventKind.SHORT_DROP, "third", "NOTIFY", EventArgs.ShortDrop, time = 3_000)
        log.add("d2", "Light", EventKind.TEST, "second", "ALARM", time = 2_000)

        val all = log.recent()
        assertEquals(listOf("third", "second", "first"), all.map { it.text })
        assertEquals(listOf(3_000L, 2_000L, 1_000L), all.map { it.time })
        val third = all[0]
        assertEquals("d1", third.deviceId)
        assertEquals("Pump", third.deviceName)
        assertEquals(EventKind.SHORT_DROP, third.kind)
        assertEquals("NOTIFY", third.action)
        assertEquals(EventArgs.ShortDrop, EventArgs.parse(third.args))
        assertNull(all[2].args)
        assertEquals(listOf("third", "second"), log.recent(limit = 2).map { it.text })
    }

    @Test
    fun keepsAtMost2000Rows() {
        val log = log()
        for (i in 0 until 2005) log.add("d1", "Pump", EventKind.USER, "e$i", "", time = i.toLong())

        val all = log.recent(limit = 5000)
        assertEquals(EventLog.MAX_ROWS, all.size)
        assertEquals("e2004", all.first().text)
        assertEquals("e5", all.last().text)
        assertFalse(all.any { it.text in listOf("e0", "e1", "e2", "e3", "e4") })
    }

    @Test
    fun unseenAlertCountCountsOnlyAlarmAndNotify() {
        val log = log()
        assertEquals(0L, log.latestId())
        assertEquals(0, log.unseenAlertCount(0))

        log.add("d1", "Pump", EventKind.USER, "a", "", time = 1)
        log.add("d1", "Pump", EventKind.USER, "b", "ALARM", time = 2)
        val afterB = log.latestId()
        log.add("d1", "Pump", EventKind.USER, "c", "NOTIFY", time = 3)
        log.add("d1", "Pump", EventKind.USER, "d", "IGNORE", time = 4)
        log.add("d1", "Pump", EventKind.SYSTEM, "e", "", time = 5)

        assertEquals(2, log.unseenAlertCount(0))
        assertEquals(1, log.unseenAlertCount(afterB))
        assertEquals(0, log.unseenAlertCount(log.latestId()))
        assertEquals(5L, log.latestId())
    }

    @Test
    fun recentFiltersByDevice() {
        val log = log()
        log.add("d1", "Pump", EventKind.USER, "a", "", time = 1)
        log.add("d2", "Light", EventKind.USER, "b", "", time = 2)
        log.add("d1", "Pump", EventKind.USER, "c", "", time = 3)

        assertEquals(listOf("c", "a"), log.recent("d1").map { it.text })
        assertEquals(listOf("b"), log.recent("d2").map { it.text })
        assertTrue(log.recent("nope").isEmpty())
        assertEquals(3, log.recent().size)
    }

    @Test
    fun turkishNamesRoundTrip() {
        val name = "Çamaşır Makinesi İğüşöç ıİ"
        val text = "Şu anda “Çalışıyor” → Kapalı; ’tırnak’ ve 'apostrof' %s"
        val args = EventArgs.Switch(channel = 0, multi = false, from = true, to = false)
        log().add("dev-ç", name, EventKind.USER, text, "NOTIFY", args, time = 42)
        opened.forEach { it.close() }
        opened.clear()

        val back = log().recent().single()   // aynı dosya, yeni bağlantı: kalıcılık da sınanır
        assertEquals("dev-ç", back.deviceId)
        assertEquals(name, back.deviceName)
        assertEquals(text, back.text)
        assertEquals(args, EventArgs.parse(back.args))
        assertTrue(back.text(Strings.load("tr").eventStrings()).contains(name))
        assertEquals(listOf(back), log().recent("dev-ç"))
    }

    @Test
    fun versionBumpsOnAddAndClear() {
        val log = log()
        val v0 = log.version.value
        log.add("d1", "Pump", EventKind.USER, "a", "", time = 1)
        assertEquals(v0 + 1, log.version.value)
        log.clear()
        assertEquals(v0 + 2, log.version.value)
        assertTrue(log.recent().isEmpty())
    }

    @Test
    fun injectionLikeValuesAreStoredLiterally() {
        val log = log()
        val evil = "x'); DROP TABLE events; --"
        log.add(evil, evil, EventKind.USER, evil, "", time = 1)
        assertEquals(evil, log.recent(evil).single().deviceName)
        assertEquals(1, log.recent().size)
    }
}
