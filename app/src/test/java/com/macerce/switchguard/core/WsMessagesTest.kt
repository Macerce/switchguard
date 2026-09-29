package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WsMessagesTest {

    private val single = DeviceSnapshot("d1", "Salon", true, mapOf(0 to true))
    private val multi = DeviceSnapshot("d2", "Priz", true, mapOf(0 to true, 1 to false))

    @Test
    fun `handshake yaniti heartbeat suresini verir`() {
        val e = WsMessages.parse("""{"error":0,"apikey":"k","config":{"hb":1,"hbInterval":145},"sequence":"1"}""")
        assertEquals(WsEvent.HandshakeOk(145), e)
    }

    @Test
    fun `handshake hatasi`() {
        val e = WsMessages.parse("""{"error":406,"sequence":"1"}""")
        assertEquals(WsEvent.HandshakeFailed(406), e)
    }

    @Test
    fun `pong taninir`() {
        assertEquals(WsEvent.Pong, WsMessages.parse("pong"))
    }

    @Test
    fun `tek kanal update uygulanir`() {
        val e = WsMessages.parse("""{"action":"update","deviceid":"d1","params":{"switch":"off"}}""") as WsEvent.Update
        assertEquals(single.copy(switches = mapOf(0 to false)), WsMessages.apply(single, e))
    }

    @Test
    fun `cok kanal kismi update birlestirilir`() {
        val e = WsMessages.parse(
            """{"action":"update","deviceid":"d2","params":{"switches":[{"switch":"on","outlet":1}]}}"""
        ) as WsEvent.Update
        assertEquals(multi.copy(switches = mapOf(0 to true, 1 to true)), WsMessages.apply(multi, e))
    }

    @Test
    fun `sysmsg online durumunu gunceller`() {
        val e = WsMessages.parse("""{"action":"sysmsg","deviceid":"d1","params":{"online":false}}""") as WsEvent.Update
        assertEquals(single.copy(online = false), WsMessages.apply(single, e))
    }

    @Test
    fun `ilgisiz parametreler durumu degistirmez`() {
        val e = WsMessages.parse("""{"action":"update","deviceid":"d1","params":{"rssi":-60}}""") as WsEvent.Update
        assertEquals(single, WsMessages.apply(single, e))
    }

    @Test
    fun `bozuk mesaj null`() {
        assertNull(WsMessages.parse("{bozuk"))
    }
}
