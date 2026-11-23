package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HueProtocolTest {
    @Test
    fun configAndPair() {
        val b = HueProtocol.parseConfig("""{"name":"Philips hue","bridgeid":"001788FFFE23ABCD","modelid":"BSB002","apiversion":"1.65.0"}""")!!
        assertEquals("001788fffe23abcd", b.id)
        assertEquals("hue-bridge-001788fffe23abcd", HueProtocol.bridgeKey(b.id))
        assertEquals("abc123" to null, HueProtocol.parsePair("""[{"success":{"username":"abc123"}}]"""))
        assertEquals(null to 101, HueProtocol.parsePair("""[{"error":{"type":101,"address":"","description":"link button not pressed"}}]"""))
        assertNull(HueProtocol.parseConfig("""{"name":"x"}"""))
    }

    @Test
    fun lights() {
        val l = HueProtocol.parseLights("""{"2":{"name":"Salon","type":"Extended color light","state":{"on":true,"reachable":true}},
            "1":{"name":"Priz","type":"On/Off plug-in unit","state":{"on":false,"reachable":false}},"3":{"name":"Sensör yok","state":{}}}""")!!
        assertEquals(listOf("1", "2"), l.map { it.num })
        assertFalse(l[0].reachable)
        assertTrue(l[1].on)
        assertEquals("001788" to "2", HueProtocol.parseExtra(HueProtocol.extraFor("001788", "2")))
        assertTrue(HueProtocol.isStateOk("""[{"success":{"/lights/2/state/on":true}}]"""))
        assertFalse(HueProtocol.isStateOk("""[{"error":{"type":1}}]"""))
        assertEquals(1, HueProtocol.errorType("""[{"error":{"type":1}}]"""))
    }
}
