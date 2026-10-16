package com.macerce.switchguard.desktop.ui.devices

import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import com.macerce.switchguard.desktop.i18n.Strings
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Cihaz ekranlarının arayüzden bağımsız yardımcıları: süre/güç/gün metinleri, enerji özeti, bulut işlemleri. */
class DeviceHelpersTest {
    @get:Rule val tmp = TemporaryFolder()

    private val tr = Strings.load("tr")
    private val en = Strings.load("en")

    @Test
    fun relativeAgoRoundsToMinutesUnderAnHour() {
        assertEquals("az önce", relativeAgo(30_000, tr))
        assertEquals("3 dk önce", relativeAgo(3 * 60_000 + 40_000, tr))
        assertEquals("3 min ago", relativeAgo(3 * 60_000 + 40_000, en))
        assertTrue(relativeAgo(2 * 3_600_000 + 10 * 60_000, tr).endsWith("önce"))
    }

    @Test
    fun wattsUseTheLanguageNumberFormat() {
        assertEquals("118 W", formatWatts(118.4, Locale.forLanguageTag("tr")))
        assertEquals("1,25 kW", formatWatts(1250.0, Locale.forLanguageTag("tr")))
        assertEquals("1.25 kW", formatWatts(1250.0, Locale.ENGLISH))
    }

    @Test
    fun daysLabelNamesCommonSetsAndStartsOnMonday() {
        assertEquals(tr["days_every"], daysLabel((0..6).toSet(), tr))
        assertEquals(tr["days_weekdays"], daysLabel(setOf(1, 2, 3, 4, 5), tr))
        assertEquals(tr["days_weekend"], daysLabel(setOf(0, 6), tr))
        assertEquals("Mon, Wed, Sun", daysLabel(setOf(0, 1, 3), en))
    }

    @Test
    fun formatMinuteUsesTheLocalClock() {
        assertEquals("07:05", formatMinute(7 * 60 + 5, Locale.forLanguageTag("tr")))
        assertTrue(formatMinute(19 * 60, Locale.ENGLISH).startsWith("7:00"))
    }

    @Test
    fun energySummaryCoversSevenDaysAndTheMonth() {
        MetricsLog(tmp.root.toPath().resolve("metrics.db")).use { metrics ->
            val zone = ZoneId.of("Europe/Istanbul")
            val now = LocalDate.of(2026, 10, 16).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            val d = DeviceSnapshot("p", "Priz", online = true, switches = mapOf(0 to true), power = 100.0)
            // Dünden bu yana sabit 100 W: dün 24 saat (2,4 kWh), bugün 12 saat (1,2 kWh).
            val start = LocalDate.of(2026, 10, 15).atStartOfDay(zone).toInstant().toEpochMilli()
            var prev = emptyMap<String, DeviceSnapshot>()
            var t = start
            while (t <= now) {
                metrics.record(prev, listOf(d), t)
                prev = mapOf(d.id to d)
                t += 10 * 60_000L // EnergyMath.MAX_GAP_MS altında
            }
            val data = loadEnergy(metrics, d.id, now, zone)
            assertEquals(7, data.days.size)
            assertEquals(LocalDate.of(2026, 10, 10).atStartOfDay(zone).toInstant().toEpochMilli(), data.days.first().first)
            assertEquals(1.2, data.days.last().second, 0.01)
            assertEquals(2.4, data.days[5].second, 0.01)
            assertEquals(0.0, data.days[0].second, 0.0001)
            assertEquals(3.6, data.month, 0.01)
            assertTrue(data.hasData)
        }
    }

    private class FakeEwelink(var timers: JSONArray) : DeviceActions.Ewelink {
        var written: JSONArray? = null
        override fun deviceParams(deviceId: String) = JSONObject().put("switch", "on").put("timers", timers)
        override fun setTimers(deviceId: String, timers: JSONArray) { written = timers; this.timers = timers }
        override fun requestLiveEnergy(device: DeviceSnapshot) = Unit
    }

    private fun store() = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)

    @Test
    fun savingATimerKeepsTimersTheAppDoesNotKnow() = runBlocking {
        val once = JSONObject("""{"mId":"x","type":"once","at":"2026-10-20T10:00:00.000Z","do":{"switch":"off"}}""")
        val fake = FakeEwelink(JSONArray().put(once))
        val actions = DeviceActions(store(), ewelink = { fake })
        val plug = DeviceSnapshot("1", "Priz", online = true, switches = mapOf(0 to false), cloud = Cloud.EWELINK)

        val loaded = actions.loadTimers(plug.id).getOrThrow()
        assertEquals(1, loaded.size)
        assertEquals(null, loaded[0].timer)

        val saved = actions.saveTimer(plug, "new", RepeatTimer("new", true, 7, 30, setOf(1, 2), 0, true)).getOrThrow()
        assertEquals(2, saved.size)
        assertEquals("x", fake.written!!.getJSONObject(0).getString("mId"))
        assertEquals(7, saved[1].timer!!.hour)

        // Silme: yalnızca o kayıt gider.
        val afterDelete = actions.saveTimer(plug, "new", null).getOrThrow()
        assertEquals(1, afterDelete.size)
    }

    @Test
    fun rawDataOfADemoDeviceNeedsNoCloud() = runBlocking {
        val actions = DeviceActions(store(), ewelink = { error("no cloud for demo") }, tuyaRaw = { error("no cloud for demo") })
        val demo = DeviceSnapshot("demo-light", "Işık", online = true, switches = mapOf(0 to false), cloud = Cloud.DEMO)
        val text = actions.rawParams(demo).getOrThrow()
        assertTrue(text, text.contains("demo-light"))
    }

    @Test
    fun cloudErrorsComeBackAsFailures() = runBlocking {
        val actions = DeviceActions(store(), ewelink = { throw java.io.IOException("offline") })
        assertTrue(actions.loadTimers("1").isFailure)
        assertTrue(actions.requestLiveEnergy(DeviceSnapshot("1", "P", true, mapOf(0 to true))).isFailure)
    }
}
