package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasmotaEsphomeTest {
    private val status0 = """{"Status":{"Module":1,"DeviceName":"Salon Priz","FriendlyName":["Salon"],"Power":1},
        "StatusNET":{"Hostname":"tasmota-1A2B3C","IPAddress":"192.168.1.40","Mac":"A4:CF:12:1A:2B:3C"},
        "StatusSNS":{"ENERGY":{"Power":42,"Voltage":229,"Current":0.19}},"StatusSTS":{"POWER":"ON"}}"""

    @Test
    fun tasmotaInfoAndStatus() {
        val i = TasmotaProtocol.parseInfo(status0)!!
        assertEquals("tasmota-a4cf121a2b3c", i.id)
        assertEquals("Salon Priz", i.model)
        assertEquals(1, i.channels)
        val r = TasmotaProtocol.parseStatus(status0, 1)!!
        assertEquals(mapOf(0 to true), r.switches)
        assertEquals(42.0, r.power!!, 0.01)
        assertEquals(229.0, r.voltage!!, 0.01)
        val multi = """{"StatusNET":{"Mac":"AA"},"StatusSTS":{"POWER1":"ON","POWER2":"OFF"},"StatusSNS":{"ENERGY":{"Power":[10,5]}}}"""
        assertEquals(2, TasmotaProtocol.parseInfo(multi)!!.channels)
        val m = TasmotaProtocol.parseStatus(multi, 2)!!
        assertEquals(mapOf(0 to true, 1 to false), m.switches)
        assertEquals(mapOf(0 to 10.0, 1 to 5.0), m.channelPower)
        assertNull(TasmotaProtocol.parseInfo("""{"WARNING":"Need user=<username>&password=<password>"}"""))
        assertTrue(TasmotaProtocol.needsPassword("""{"WARNING":"Need user=<username>&password=<password>"}"""))
    }

    @Test
    fun tasmotaPaths() {
        assertEquals("/cm?cmnd=Power2%20On", TasmotaProtocol.switchPath(1, true, null))
        assertEquals("/cm?user=admin&password=a%26b&cmnd=Status%200", TasmotaProtocol.statusPath(ShellyAuth.Credentials("admin", "a&b")))
        assertTrue(TasmotaProtocol.isSwitchOk("""{"POWER":"ON"}""", 0, true))
        assertTrue(TasmotaProtocol.isSwitchOk("""{"POWER2":"OFF"}""", 1, false))
        assertFalse(TasmotaProtocol.isSwitchOk("""{"POWER2":"ON"}""", 1, false))
    }

    @Test
    fun esphomeEvents() {
        val sse = """
            event: ping
            data: {"title":"Mutfak Röle","comment":"","ota":true,"log":true,"lang":"en"}

            event: state
            data: {"id":"switch-relay_1","name":"Relay 1","value":true,"state":"ON"}

            event: state
            data: {"id":"light-lamba","name_id":"light/Tavan Lamba","value":false,"state":"OFF"}

            event: state
            data: {"id":"sensor-guc","value":12.5,"state":"12.5 W"}

            event: state
            data: {"id":"sensor-sicaklik","value":21.0,"state":"21.0 °C"}
        """.trimIndent()
        val e = EsphomeProtocol.parseEvents(sse)!!
        assertEquals("Mutfak Röle", e.title)
        assertEquals(listOf("switch/relay_1", "light/Tavan%20Lamba"), e.switches)
        assertEquals("sensor/guc", e.power)
        assertEquals(e.switches, EsphomeProtocol.entitiesFromJson(e.toJson()).switches)
        assertEquals("esphome-mutfak-röle", EsphomeProtocol.idFor("Mutfak Röle", "1.2.3.4"))
        assertNull(EsphomeProtocol.parseEvents("<html></html>"))
        assertEquals(true, EsphomeProtocol.parseOn("""{"id":"switch-relay_1","value":true,"state":"ON"}"""))
        assertEquals(false, EsphomeProtocol.parseOn("""{"id":"light-x","state":"OFF"}"""))
        assertEquals("/switch/relay_1/turn_on", EsphomeProtocol.switchPath("switch/relay_1", true))
    }
}
