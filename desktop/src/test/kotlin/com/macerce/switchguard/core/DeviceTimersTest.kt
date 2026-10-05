package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceTimersTest {

    private val tr = 180 // UTC+3

    @Test
    fun `yerel saat UTC crona cevrilir`() {
        val t = RepeatTimer("x", true, 8, 30, setOf(1, 2, 3, 4, 5), 0, true)
        val json = DeviceTimers.toJson(t, multiChannel = false, utcOffsetMinutes = tr)
        assertEquals("30 5 * * 1,2,3,4,5", json.getString("at"))
        assertEquals("on", json.getJSONObject("do").getString("switch"))
        assertEquals(1, json.getInt("enabled"))
    }

    @Test
    fun `gece yarisini asan saat gunu de kaydirir`() {
        // Pazartesi 01:00 (TR) = Pazar 22:00 UTC
        val t = RepeatTimer("x", true, 1, 0, setOf(1), 0, false)
        val json = DeviceTimers.toJson(t, false, tr)
        assertEquals("0 22 * * 0", json.getString("at"))
        assertEquals(t, DeviceTimers.parse(JSONArray().put(json), tr).single().timer)
    }

    @Test
    fun `cok kanal zamanlayicisi gidis donus`() {
        val t = RepeatTimer("y", false, 23, 45, setOf(6), 2, true)
        val json = DeviceTimers.toJson(t, true, tr)
        assertEquals(2, json.getJSONObject("do").getJSONArray("switches").getJSONObject(0).getInt("outlet"))
        assertEquals(t, DeviceTimers.parse(JSONArray().put(json), tr).single().timer)
    }

    @Test
    fun `eski outlet bicimi ve yildiz gunler okunur`() {
        val raw = JSONObject("""{"mId":"z","type":"repeat","enabled":1,"at":"0 4 * * *","do":{"outlet":3,"switch":"off"}}""")
        assertEquals(RepeatTimer("z", true, 7, 0, DeviceTimers.ALL_DAYS, 3, false), DeviceTimers.parse(JSONArray().put(raw), tr).single().timer)
    }

    @Test
    fun `tanimsiz tur korunur, duzenleme sadece hedefi degistirir`() {
        val once = JSONObject("""{"mId":"o","type":"once","at":"2026-10-01T05:00:00.000Z","do":{"switch":"on"}}""")
        val rep = DeviceTimers.toJson(RepeatTimer("r", true, 9, 0, setOf(0), 0, true), false, tr)
        val entries = DeviceTimers.parse(JSONArray().put(once).put(rep), tr)
        assertNull(entries[0].timer)

        val edited = entries[1].timer!!.copy(hour = 10)
        val out = DeviceTimers.rebuild(entries, "r", edited, false, tr)
        assertEquals(2, out.length())
        assertEquals("o", out.getJSONObject(0).getString("mId"))
        assertEquals("0 7 * * 0", out.getJSONObject(1).getString("at"))

        assertEquals(1, DeviceTimers.rebuild(entries, "r", null, false, tr).length())
        val added = RepeatTimer("n", true, 6, 0, setOf(3), 0, false)
        assertEquals(3, DeviceTimers.rebuild(entries, "n", added, false, tr).length())
    }
}
