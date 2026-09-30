package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyTest {

    private fun parse(uiid: Int, json: String) = EnergyParser.parse(uiid, JSONObject(json))

    @Test
    fun `pow r2 ondalikli metin`() {
        assertEquals(Energy(123.45, 229.5, 0.54), parse(32, """{"power":"123.45","voltage":"229.5","current":"0.54"}"""))
    }

    @Test
    fun `pow r3 yuzde bir tam sayi, gerilimden olcek bulunur`() {
        assertEquals(Energy(123.45, 229.5, 0.54), parse(0, """{"power":12345,"voltage":22950,"current":54}"""))
    }

    @Test
    fun `gerilim yoksa bilinen uiid ile olceklenir`() {
        assertEquals(Energy(12.0, null, null), parse(190, """{"power":1200}"""))
    }

    @Test
    fun `dual r3 kanal gucleri toplanir`() {
        val e = parse(126, """{"actPow_00":1000,"actPow_01":2550,"voltage_00":23000,"current_00":10,"current_01":20}""")
        assertEquals(35.5, e.power!!, 1e-9)
        assertEquals(230.0, e.voltage!!, 1e-9)
        assertEquals(0.3, e.current!!, 1e-9)
    }

    @Test
    fun `spm kanal gucleri ayri ayri okunur`() {
        val e = parse(130, """{"actPow_00":1000,"actPow_02":5025}""")
        assertEquals(mapOf(0 to 10.0, 2 to 50.25), e.channelPowers)
        assertEquals(60.25, e.power!!, 1e-9)
    }

    @Test
    fun `olcum yapmayan cihazda enerji yok`() {
        assertTrue(parse(7, """{"switches":[{"switch":"on","outlet":0}],"rssi":-50}""").isEmpty)
    }

    @Test
    fun `thing listesinde enerji algilanir, digerlerinde null kalir`() {
        val body = """
        {"error":0,"data":{"thingList":[
          {"itemData":{"deviceid":"p","name":"Pompa","online":true,"extra":{"uiid":32},
             "params":{"switch":"on","power":"850.5","voltage":"230","current":"3.7"}}},
          {"itemData":{"deviceid":"l","name":"Lamba","online":true,"extra":{"uiid":1},"params":{"switch":"off"}}}
        ]}}"""
        val (pump, lamp) = DeviceParser.parseThingList(body)
        assertTrue(pump.hasEnergy)
        assertEquals(850.5, pump.power!!, 1e-9)
        assertEquals(32, pump.uiid)
        assertFalse(lamp.hasEnergy)
        assertNull(lamp.voltage)
        assertEquals(listOf(pump, lamp), DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(pump, lamp))))
    }

    @Test
    fun `websocket guc guncellemesi uygulanir, eksik alanlar korunur`() {
        val before = DeviceSnapshot("p", "Pompa", true, mapOf(0 to true), uiid = 190, power = 10.0, voltage = 230.0, current = 0.1)
        val e = WsMessages.parse("""{"action":"update","deviceid":"p","params":{"power":5000}}""") as WsEvent.Update
        assertEquals(before.copy(power = 50.0), WsMessages.apply(before, e))
    }

    @Test
    fun `kwh sabit guc`() {
        val h = 3_600_000L
        // 1000 W, 2 saat → 2 kWh (her 10 dk'da ölçüm).
        val samples = (0 until 12).map { PowerSample(it * 10 * 60_000L, 1000.0) }
        assertEquals(2.0, EnergyMath.kwh(samples, 0, 2 * h), 1e-9)
    }

    @Test
    fun `kwh uzun bosluk sayilmaz ve aralik disi kirpilir`() {
        val min = 60_000L
        val samples = listOf(PowerSample(0, 600.0), PowerSample(120 * min, 600.0))
        // İlk ölçüm en fazla 15 dk sayılır; ikincisi aralık sonuna (130. dk) kadar 10 dk.
        assertEquals(600.0 * 25 / 60 / 1000, EnergyMath.kwh(samples, 0, 130 * min), 1e-9)
        // Aralık 5. dakikadan başlarsa ilk ölçümün 10 dk'sı kalır.
        assertEquals(600.0 * 20 / 60 / 1000, EnergyMath.kwh(samples, 5 * min, 130 * min), 1e-9)
    }

    @Test
    fun `kullanim sayisi ve suresi`() {
        val s = listOf(StateSample(10, true), StateSample(20, false), StateSample(30, true), StateSample(35, true))
        assertEquals(Usage(2, 10 + 70), UsageMath.usage(false, s, 0, 100))
        // Başta açıksa açılış sayılmaz ama süre baştan başlar.
        assertEquals(Usage(0, 50), UsageMath.usage(true, listOf(StateSample(50, false)), 0, 100))
    }
}
