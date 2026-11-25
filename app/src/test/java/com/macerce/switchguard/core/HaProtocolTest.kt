package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HaProtocolTest {
    @Test
    fun normalize() {
        assertEquals("http://192.168.1.5:8123", HaProtocol.normalize("192.168.1.5"))
        assertEquals("http://192.168.1.5:8124", HaProtocol.normalize("192.168.1.5:8124/"))
        assertEquals("http://homeassistant.local:8123", HaProtocol.normalize("homeassistant.local:8123"))
        assertEquals("https://ev.duckdns.org", HaProtocol.normalize("ev.duckdns.org"))
        assertEquals("https://x.ui.nabu.casa", HaProtocol.normalize("https://x.ui.nabu.casa/"))
        assertEquals("", HaProtocol.normalize("  "))
    }

    @Test
    fun states() {
        val list = HaProtocol.parseStates("""[
            {"entity_id":"switch.pompa","state":"on","attributes":{"friendly_name":"Pompa"}},
            {"entity_id":"sensor.sicaklik","state":"21","attributes":{}},
            {"entity_id":"light.salon","state":"unavailable","attributes":{"friendly_name":"Salon"}},
            {"entity_id":"input_boolean.tatil","state":"off","attributes":{}}]""")
        assertEquals(listOf("input_boolean.tatil", "switch.pompa", "light.salon"), list.map { it.id })
        assertTrue(list.first { it.id == "switch.pompa" }.on)
        assertFalse(list.first { it.id == "light.salon" }.available)
        assertEquals("/api/services/light/turn_off", HaProtocol.servicePath("light.salon", false))
        assertEquals("ha-switch.pompa", HaProtocol.idFor("switch.pompa"))
        val s = HaProtocol.Server("192.168.1.5", "", "tok")
        assertEquals(s, HaProtocol.serverFromJson(s.toJson()))
        assertNull(HaProtocol.serverFromJson("""{"l":"","r":"","t":"x"}"""))
    }

    @Test
    fun websocket() {
        assertEquals("ws://192.168.1.5:8123/api/websocket", HaProtocol.wsUrl("192.168.1.5"))
        assertEquals("wss://ev.duckdns.org/api/websocket", HaProtocol.wsUrl("ev.duckdns.org"))
        assertEquals(HaProtocol.WsMessage.AuthRequired, HaProtocol.parseWs("""{"type":"auth_required","ha_version":"2026.11"}"""))
        assertEquals(HaProtocol.WsMessage.AuthOk, HaProtocol.parseWs("""{"type":"auth_ok"}"""))
        assertEquals(HaProtocol.WsMessage.AuthInvalid("bad"), HaProtocol.parseWs("""{"type":"auth_invalid","message":"bad"}"""))
        assertEquals(HaProtocol.WsMessage.Result(1, true), HaProtocol.parseWs("""{"id":1,"type":"result","success":true}"""))
        val ev = """{"id":1,"type":"event","event":{"event_type":"state_changed","data":{"entity_id":"switch.priz",
            "new_state":{"entity_id":"switch.priz","state":"on","attributes":{"friendly_name":"Priz"}}}}}"""
        val m = HaProtocol.parseWs(ev) as HaProtocol.WsMessage.StateChanged
        assertEquals("switch.priz", m.entityId)
        assertEquals(true, m.state?.on)
        val removed = HaProtocol.parseWs("""{"type":"event","event":{"data":{"entity_id":"switch.x","new_state":null}}}""")
        assertEquals(HaProtocol.WsMessage.StateChanged("switch.x", null), removed)
        assertEquals(2, HaProtocol.parseWsBatch("[$ev,{\"type\":\"auth_ok\"}]").size)
        assertEquals(HaProtocol.WsMessage.Other, HaProtocol.parseWs("bozuk"))
        assertTrue(HaProtocol.wsAuth("t").contains("\"access_token\":\"t\""))
    }
}
