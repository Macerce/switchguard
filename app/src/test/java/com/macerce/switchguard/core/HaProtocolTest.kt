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
}
