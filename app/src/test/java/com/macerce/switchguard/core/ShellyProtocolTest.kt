package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Örnekler Shelly API belgelerindeki gerçek yanıtlardan kısaltıldı. */
class ShellyProtocolTest {
    @Test
    fun gen1Info() {
        val i = ShellyProtocol.parseInfo("""{"type":"SHPLG-S","mac":"A4CF12F45F6A","auth":false,"fw":"20230913-114008/v1.14.0","num_outputs":1,"num_meters":1}""")!!
        assertEquals("shelly-a4cf12f45f6a", i.id)
        assertEquals(1, i.gen)
        assertEquals("SHPLG-S", i.model)
        assertEquals(1, i.channels)
        assertFalse(i.auth)
        assertEquals(2, ShellyProtocol.parseInfo("""{"type":"SHSW-25","mac":"aa:bb:cc:dd:ee:ff","auth":true,"num_outputs":2}""")!!.channels)
        assertEquals("shelly-aabbccddeeff", ShellyProtocol.parseInfo("""{"type":"SHSW-25","mac":"aa:bb:cc:dd:ee:ff","auth":true,"num_outputs":2}""")!!.id)
    }

    @Test
    fun gen2Info() {
        val i = ShellyProtocol.parseInfo("""{"name":null,"id":"shellyplus1pm-a8032ab12345","mac":"A8032AB12345","model":"SNSW-001P16EU","gen":2,"fw_id":"20230912-082210/1.0.3","ver":"1.0.3","app":"Plus1PM","auth_en":true,"auth_domain":"shellyplus1pm-a8032ab12345"}""")!!
        assertEquals("shelly-a8032ab12345", i.id)
        assertEquals(2, i.gen)
        assertEquals("Plus1PM", i.model)
        assertTrue(i.auth)
        assertEquals(3, ShellyProtocol.parseInfo("""{"mac":"X","gen":3,"model":"S3SW-001X16EU"}""")!!.gen)
        assertNull(ShellyProtocol.parseInfo("""{"id":"x"}"""))
        assertNull(ShellyProtocol.parseInfo("<html>"))
    }

    @Test
    fun gen1Status() {
        val plug = """{"wifi_sta":{"connected":true},"relays":[{"ison":true,"has_timer":false,"overpower":false}],"meters":[{"power":61.5,"is_valid":true,"total":4000}]}"""
        val r = ShellyProtocol.parseStatus(1, plug, 1)!!
        assertEquals(mapOf(0 to true), r.switches)
        assertEquals(61.5, r.power!!, 0.001)
        assertTrue(r.channelPower.isEmpty())

        val two = """{"relays":[{"ison":false},{"ison":true}],"meters":[{"power":0.0,"is_valid":true},{"power":12.0,"is_valid":true}]}"""
        val r2 = ShellyProtocol.parseStatus(1, two, 2)!!
        assertEquals(mapOf(0 to false, 1 to true), r2.switches)
        assertEquals(12.0, r2.power!!, 0.001)
        assertEquals(mapOf(0 to 0.0, 1 to 12.0), r2.channelPower)

        val em = """{"relays":[{"ison":true}],"emeters":[{"power":230.4,"voltage":229.1,"current":1.01,"is_valid":true}]}"""
        val r3 = ShellyProtocol.parseStatus(1, em, 1)!!
        assertEquals(230.4, r3.power!!, 0.001)
        assertEquals(229.1, r3.voltage!!, 0.001)

        // Ölçmeyen Shelly 1: güç yok → enerji arayüzü çıkmaz.
        assertNull(ShellyProtocol.parseStatus(1, """{"relays":[{"ison":false}]}""", 1)!!.power)
        assertNull(ShellyProtocol.parseStatus(1, """{"lights":[]}""", 1))
    }

    @Test
    fun gen2Status() {
        val json = """{"ble":{},"switch:0":{"id":0,"source":"HTTP","output":true,"apower":8.9,"voltage":237.5,"current":0.068,"temperature":{"tC":53.3}},"sys":{"mac":"A8032AB12345"}}"""
        val r = ShellyProtocol.parseStatus(2, json, 1)!!
        assertEquals(mapOf(0 to true), r.switches)
        assertEquals(8.9, r.power!!, 0.001)
        assertEquals(237.5, r.voltage!!, 0.001)
        assertEquals(0.068, r.current!!, 0.0001)
        assertEquals(1, ShellyProtocol.channelsOf(json))

        val pro = """{"switch:0":{"output":true,"apower":10.0},"switch:1":{"output":false,"apower":0.0},"switch:2":{"output":true,"apower":5.0},"switch:3":{"output":false,"apower":0.0}}"""
        assertEquals(4, ShellyProtocol.channelsOf(pro))
        val r2 = ShellyProtocol.parseStatus(2, pro, 4)!!
        assertEquals(15.0, r2.power!!, 0.001)
        assertEquals(4, r2.channelPower.size)
        // Kayıtlı kanal sayısından fazlası atılır.
        assertEquals(2, ShellyProtocol.parseStatus(2, pro, 2)!!.switches.size)
        // Plus 1 (ölçmez).
        assertNull(ShellyProtocol.parseStatus(2, """{"switch:0":{"output":false}}""", 1)!!.power)
    }

    @Test
    fun commands() {
        assertEquals("/relay/1?turn=on", ShellyProtocol.switchPath(1, 1, true))
        assertEquals("/rpc/Switch.Set?id=0&on=false", ShellyProtocol.switchPath(2, 0, false))
        assertEquals("/status", ShellyProtocol.statusPath(1))
        assertEquals("/rpc/Shelly.GetStatus", ShellyProtocol.statusPath(3))
        assertTrue(ShellyProtocol.isSwitchOk(1, """{"ison":true,"has_timer":false}""", true))
        assertFalse(ShellyProtocol.isSwitchOk(1, """{"ison":false}""", true))
        assertTrue(ShellyProtocol.isSwitchOk(2, """{"was_on":false}""", true))
        assertFalse(ShellyProtocol.isSwitchOk(2, """{"code":401,"message":"unauthorized"}""", true))
        assertFalse(ShellyProtocol.isSwitchOk(2, "bad", true))
    }

    @Test
    fun localDeviceKeepsBrand() {
        val d = LocalDevice("shelly-a1", "Isıtıcı", channels = 1, ip = "192.168.1.9", brand = LocalDevice.BRAND_SHELLY, gen = 2)
        assertEquals(listOf(d), LocalDevice.listFromJson(LocalDevice.listToJson(listOf(d))))
        // Eski kayıtlar (marka yok) Sonoff sayılır.
        assertEquals(LocalDevice.BRAND_SONOFF, LocalDevice.listFromJson("""[{"id":"s","name":"x"}]""").single().brand)
        val snap = LocalDevice.merge(emptyList(), listOf(d)) { LocalReading(mapOf(0 to true), power = 40.0, voltage = 230.0) }.single()
        assertTrue(snap.hasEnergy)
        assertEquals(40.0, snap.power!!, 0.001)
        // Görülmeyince güç gösterilmez.
        assertFalse(LocalDevice.merge(listOf(snap), listOf(d)) { null }.single().hasEnergy)
        assertTrue(ShellyProtocol.isShellyServiceName("ShellyPlus1PM-a8032ab12345"))
        assertFalse(ShellyProtocol.isShellyServiceName("eWeLink_1000aa"))
    }

    @Test
    fun staConfig() {
        assertEquals("/settings/sta?enabled=1&ssid=Ev%20Wi-Fi&key=a%26b", ShellyProtocol.staConfigPath(1, "Ev Wi-Fi", "a&b"))
        val p2 = ShellyProtocol.staConfigPath(2, "Ev", "x\"y")
        assertTrue(p2.startsWith("/rpc/WiFi.SetConfig?config="))
        val cfg = org.json.JSONObject(java.net.URLDecoder.decode(p2.substringAfter("config="), "UTF-8")).getJSONObject("sta")
        assertEquals("x\"y", cfg.getString("pass"))
        assertTrue(ShellyProtocol.isStaConfigOk(1, """{"enabled":true,"ssid":"Ev"}""", "Ev"))
        assertTrue(ShellyProtocol.isStaConfigOk(2, """{"restart_required":true}""", "Ev"))
        assertFalse(ShellyProtocol.isStaConfigOk(2, """{"code":-103,"message":"x"}""", "Ev"))
        assertTrue(ShellyProtocol.isSetupSsid("ShellyPlus1PM-A8032AB12345"))
        assertFalse(ShellyProtocol.isSetupSsid("Ev"))
    }
}
