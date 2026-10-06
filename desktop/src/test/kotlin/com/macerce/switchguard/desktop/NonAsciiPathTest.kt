package com.macerce.switchguard.desktop

import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

/**
 * Windows kullanıcı adı Türkçe karakter ve boşluk içerebilir (`C:\Users\Kullanıcı Şükrü\AppData\Roaming\SwitchGuard`):
 * ayarlar, iki veritabanı ve günlük böyle bir klasörde çalışmalı.
 */
class NonAsciiPathTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var dir: Path

    @Before
    fun setUp() {
        dir = tmp.root.toPath().resolve("Kullanıcı Şükrü ğüİ").resolve("SwitchGuard")
    }

    @Test
    fun storeWorksInANonAsciiFolder() {
        val file = dir.resolve("settings.json")
        Store(file, PlainCodec).apply {
            appId = "uygulama"
            pendingAlarm = listOf("12:00  Rögar pompası: AÇIK → KAPALI")
        }

        val again = Store(file, PlainCodec)
        assertEquals("uygulama", again.appId)
        assertEquals(listOf("12:00  Rögar pompası: AÇIK → KAPALI"), again.pendingAlarm)
        assertTrue(Files.isRegularFile(file))
    }

    @Test
    fun eventLogWorksInANonAsciiFolder() {
        val file = dir.resolve("events.db")
        EventLog(file).use { it.add("dev", "Rögar", EventKind.TEST, "deneme ğüşİ", "ALARM", time = 1_000) }

        val events = EventLog(file).use { it.recent() }
        assertEquals(listOf("Rögar" to "deneme ğüşİ"), events.map { it.deviceName to it.text })
        assertTrue(Files.isRegularFile(file))
    }

    @Test
    fun metricsLogWorksInANonAsciiFolder() {
        val file = dir.resolve("metrics.db")
        val t0 = 1_700_000_000_000L
        val plug = DeviceSnapshot("dev", "Priz", online = true, switches = mapOf(0 to true), power = 100.0)
        MetricsLog(file).use { it.record(emptyMap(), listOf(plug), t0) }

        MetricsLog(file).use { assertTrue(it.hasPowerData("dev")) }
        assertTrue(Files.isRegularFile(file))
    }

    @Test
    fun logWorksInANonAsciiFolder() {
        Log.init(dir)
        Log.w("Test", "Şükrü'nün bilgisayarı")

        val text = String(Files.readAllBytes(dir.resolve("logs").resolve("switchguard.log")), UTF_8)
        assertTrue(text, text.contains("W/Test: Şükrü'nün bilgisayarı"))
    }
}
