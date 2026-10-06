package com.macerce.switchguard.data

import com.macerce.switchguard.core.DeviceSnapshot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.sql.DriverManager

class MetricsLogTest {
    @get:Rule val tmp = TemporaryFolder()

    private val opened = mutableListOf<MetricsLog>()

    private fun file(): Path = tmp.root.toPath().resolve("metrics.db")

    private fun log(): MetricsLog = MetricsLog(file()).also { opened += it }

    /** Windows açık veritabanı dosyasını silemez; geçici klasör temizlenmeden önce bağlantıları kapat. */
    @After fun closeAll() = opened.forEach { it.close() }

    private val t0 = 1_700_000_000_000L
    private val hour = 3_600_000L
    private val day = 86_400_000L

    private fun plug(watts: Double?, on: Boolean = true, online: Boolean = true) =
        DeviceSnapshot("dev", "Plug", online, mapOf(0 to on), power = watts)

    /** Test için ham satır sayısı: ayrı bir bağlantıyla (WAL sayesinde açık bağlantıyla birlikte okunabilir). */
    private fun rows(table: String): Int =
        DriverManager.getConnection("jdbc:sqlite:${file()}").use { c ->
            c.createStatement().use { st -> st.executeQuery("SELECT COUNT(*) FROM $table").use { it.next(); it.getInt(1) } }
        }

    @Test
    fun constantPowerGivesKwh() {
        val log = log()
        var prev = emptyMap<String, DeviceSnapshot>()
        for (i in 0 until 60) {
            val d = plug(100.0)
            log.record(prev, listOf(d), t0 + i * MetricsLog.POWER_EVERY_MS)
            prev = mapOf(d.id to d)
        }
        assertEquals(0.1, log.kwh("dev", t0, t0 + hour), 0.001)
        assertTrue(log.hasPowerData("dev"))
        assertFalse(log.hasPowerData("other"))
    }

    @Test
    fun usageCountsOnTime() {
        val log = log()
        val off = plug(null, on = false)
        val on = plug(null, on = true)
        log.record(mapOf(off.id to off), listOf(on), t0)
        log.record(mapOf(on.id to on), listOf(off), t0 + hour)

        val u = log.usage("dev", 0, t0, t0 + 2 * hour)
        assertEquals(1, u.onCount)
        assertEquals(hour.toDouble(), u.onMillis.toDouble(), 1_000.0)
        // aralık, açık kalma sürerken başlıyorsa başlangıçtaki durum öncekinden bilinir
        assertEquals(hour / 2, log.usage("dev", 0, t0 + hour / 2, t0 + 2 * hour).onMillis)
    }

    @Test
    fun pruneRemovesOlderThan400Days() {
        val log = log()
        val now = t0 + 500 * day
        val old = now - (MetricsLog.KEEP_DAYS + 1) * day
        val fresh = now - (MetricsLog.KEEP_DAYS - 1) * day
        val off = plug(50.0, on = false)
        val on = plug(50.0, on = true)
        log.record(emptyMap(), listOf(off), old)
        log.record(mapOf(off.id to off), listOf(on), fresh)
        assertEquals(2, rows("power"))
        assertEquals(2, rows("state"))

        log.prune(now)

        assertEquals(1, rows("power"))
        assertEquals(1, rows("state"))
        assertEquals(listOf(fresh), log.powerSamples("dev", 0, now + 1).map { it.time })
    }

    @Test
    fun unchangedPowerIsNotRewrittenWithinTheInterval() {
        val log = log()
        val d = plug(100.0)
        val prev = mapOf(d.id to d)
        log.record(emptyMap(), listOf(d), t0)
        log.record(prev, listOf(d), t0 + 10_000)                                 // aynı değer, 10 sn: yazılmaz
        assertEquals(1, rows("power"))
        log.record(prev, listOf(plug(300.0)), t0 + 20_000)                       // belirgin değişim: yazılır
        assertEquals(2, rows("power"))
        log.record(prev, listOf(plug(300.0)), t0 + 20_000 + MetricsLog.POWER_EVERY_MS)  // süre doldu: yazılır
        assertEquals(3, rows("power"))
    }

    @Test
    fun offlineEnergyMeterWritesZeroWatts() {
        val log = log()
        log.record(emptyMap(), listOf(plug(100.0)), t0)
        log.record(emptyMap(), listOf(plug(100.0, online = false)), t0 + MetricsLog.POWER_EVERY_MS)
        assertEquals(listOf(100.0, 0.0), log.powerSamples("dev", t0, t0 + hour).map { it.watts })
    }

    @Test
    fun versionBumpsOnWritesAndClearEmptiesEverything() {
        val log = log()
        val v0 = log.version.value
        log.record(emptyMap(), listOf(plug(100.0)), t0)
        assertEquals(v0 + 1, log.version.value)
        log.record(mapOf("dev" to plug(100.0)), listOf(plug(100.0)), t0 + 1_000)   // değişen bir şey yok
        assertEquals(v0 + 1, log.version.value)
        log.clear()
        assertEquals(v0 + 2, log.version.value)
        assertEquals(0, rows("power"))
        assertEquals(0, rows("state"))
        assertFalse(log.hasPowerData("dev"))
    }

    @Test
    fun dataSurvivesReopen() {
        log().record(emptyMap(), listOf(plug(100.0)), t0)
        opened.forEach { it.close() }
        opened.clear()
        assertEquals(listOf(100.0), log().powerSamples("dev", t0, t0 + hour).map { it.watts })
    }
}
