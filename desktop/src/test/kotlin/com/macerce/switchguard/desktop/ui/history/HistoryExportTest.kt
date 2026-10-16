package com.macerce.switchguard.desktop.ui.history

import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.HistoryRange
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneId

class HistoryExportTest {
    @get:Rule val tmp = TemporaryFolder()

    private val tr = Strings.load("tr")
    private val en = Strings.load("en")
    private val BOM = Char(0xFEFF).toString()

    private fun event(id: Long, time: Long, kind: String, text: String, action: String) =
        LoggedEvent(id, time, "d1", "Pompa", kind, text, action)

    @Test
    fun turkishCsvUsesSemicolonsOldestFirstAndTranslatedActions() {
        val newer = event(2, 2_000_000, EventType.TURNED_OFF.name, "Pompa: AÇIK → KAPALI", "ALARM")
        val older = event(1, 1_000_000, EventKind.USER, "Pompa: KAPALI → AÇIK", "")
        val csv = HistoryExport.csv(listOf(newer, older), tr)
        val lines = csv.removePrefix(BOM).trimEnd().split("\r\n")

        assertTrue("BOM", csv.startsWith(BOM))
        assertEquals("Tarih;Saat;Cihaz;Olay;İşlem", lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1], lines[1].endsWith(";Pompa;Pompa: KAPALI → AÇIK;"))
        assertTrue(lines[2], lines[2].endsWith(";Pompa;Pompa: AÇIK → KAPALI;Alarm"))
    }

    @Test
    fun englishCsvUsesCommas() {
        val csv = HistoryExport.csv(listOf(event(1, 1_000_000, EventType.CAME_ONLINE.name, "Pump: back", "NOTIFY")), en)
        val lines = csv.removePrefix(BOM).trimEnd().split("\r\n")
        assertEquals("Date,Time,Device,Event,Action", lines[0].replace("\"", ""))
        assertTrue(lines[1], lines[1].endsWith(",Pompa,Pump: back,Notify"))
    }

    @Test
    fun fileNameHasTheLocalWordAndDate() {
        val now = LocalDate.of(2026, 10, 12).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000
        assertEquals("switchguard-gecmis-2026-10-12.csv", HistoryExport.fileName(tr, now))
    }

    @Test
    fun csvExtensionIsAddedOnlyWhenMissing() {
        assertEquals(Paths.get("a", "rapor.csv"), HistoryExport.withExtension(Paths.get("a", "rapor")))
        assertEquals(Paths.get("a", "rapor.CSV"), HistoryExport.withExtension(Paths.get("a", "rapor.CSV")))
    }

    @Test
    fun writeHonoursDeviceAndRange() {
        EventLog(tmp.root.toPath().resolve("events.db")).use { log ->
            val now = 10L * 24 * 3_600_000
            log.add("d1", "Pompa", EventType.TURNED_OFF.name, "eski", "ALARM", time = now - 3 * 24 * 3_600_000L)
            log.add("d1", "Pompa", EventType.TURNED_ON.name, "yeni", "ALARM", time = now - 3_600_000L)
            log.add("d2", "Işık", EventType.TURNED_ON.name, "başka", "NOTIFY", time = now - 60_000L)
            val out = tmp.root.toPath().resolve("out.csv")

            assertEquals(1, HistoryExport.write(out, log, "d1", HistoryRange.DAY, tr, now))
            val text = String(Files.readAllBytes(out), Charsets.UTF_8)
            assertTrue(text, text.contains("yeni") && !text.contains("eski") && !text.contains("başka"))

            assertEquals(3, HistoryExport.write(out, log, null, HistoryRange.ALL, tr, now))
        }
    }
}
