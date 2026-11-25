package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSensorsTest {
    @Test
    fun haSensorsAndClimate() {
        val list = HaProtocol.parseStates("""[
            {"entity_id":"sensor.salon_t","state":"71.6","attributes":{"friendly_name":"Salon","device_class":"temperature","unit_of_measurement":"°F"}},
            {"entity_id":"sensor.enerji","state":"5","attributes":{"device_class":"energy"}},
            {"entity_id":"binary_sensor.kapi","state":"on","attributes":{"friendly_name":"Kapı","device_class":"door"}},
            {"entity_id":"climate.klima","state":"cool","attributes":{"friendly_name":"Klima","current_temperature":24.5,"current_humidity":48}},
            {"entity_id":"switch.pompa","state":"off","attributes":{"friendly_name":"Pompa"}}]""")
        // Aç/kapa yapılabilenler önce, sonra sensörler; eşlenemeyen sensör (energy) yok.
        assertEquals(listOf("climate.klima", "switch.pompa", "binary_sensor.kapi", "sensor.salon_t"), list.map { it.id })
        val t = list.first { it.id == "sensor.salon_t" }
        assertEquals(22.0, t.reading()!!.sensors[SensorKind.TEMPERATURE]!!, 0.01)
        assertTrue(t.reading()!!.switches.isEmpty())
        assertEquals(1.0, list.first { it.id == "binary_sensor.kapi" }.reading()!!.sensors[SensorKind.DOOR])
        val c = list.first { it.id == "climate.klima" }.reading()!!
        assertEquals(mapOf(0 to true), c.switches)
        assertEquals(24.5, c.sensors[SensorKind.TEMPERATURE]!!, 0.01)
        assertEquals(48.0, c.sensors[SensorKind.HUMIDITY]!!, 0.01)
        assertFalse(HaProtocol.parseState("""{"entity_id":"climate.k","state":"off","attributes":{}}""")!!.on)
        assertNull(HaProtocol.parseState("""{"entity_id":"sensor.x","state":"unavailable","attributes":{"device_class":"humidity"}}""")!!.reading())
    }

    @Test
    fun shellyMetersAndSensors() {
        // Pro 3EM: röle yok, toplam güç.
        val em = ShellyProtocol.parseStatus(2, """{"em:0":{"total_act_power":1520.4,"a_voltage":229.1,"total_current":7.2}}""", 0)!!
        assertTrue(em.switches.isEmpty())
        assertEquals(1520.4, em.power!!, 0.01)
        assertEquals(229.1, em.voltage!!, 0.01)
        // PM Mini.
        assertEquals(42.0, ShellyProtocol.parseStatus(2, """{"pm1:0":{"apower":42.0,"voltage":230}}""", 0)!!.power!!, 0.01)
        // Plus 1 + Add-on: röle + sıcaklık.
        val addon = ShellyProtocol.parseStatus(2, """{"switch:0":{"output":true},"temperature:0":{"tC":4.5}}""", 1)!!
        assertEquals(mapOf(0 to true), addon.switches)
        assertEquals(4.5, addon.sensors[SensorKind.TEMPERATURE]!!, 0.01)
        // Gen1 H&T ve Door/Window.
        val ht = ShellyProtocol.parseStatus(1, """{"tmp":{"tC":21.3,"is_valid":true},"hum":{"value":55},"bat":{"value":80}}""", 0)!!
        assertEquals(80.0, ht.sensors[SensorKind.BATTERY]!!, 0.01)
        val dw = ShellyProtocol.parseStatus(1, """{"sensor":{"state":"open"},"lux":{"value":120},"bat":{"value":90}}""", 0)!!
        assertEquals(1.0, dw.sensors[SensorKind.DOOR])
        // Gen1 3EM: fazlar toplanır.
        val em3 = ShellyProtocol.parseStatus(1, """{"relays":[{"ison":false}],"emeters":[{"power":100,"voltage":230},{"power":50},{"power":25}]}""", 1)!!
        assertEquals(175.0, em3.power!!, 0.01)
        // Okunacak bir şey yoksa null.
        assertNull(ShellyProtocol.parseStatus(2, """{"sys":{}}""", 0))
    }

    @Test
    fun sensorOnlyLocalDevice() {
        val d = LocalDevice("ha-sensor.t", "Salon", channels = 0, brand = LocalDevice.BRAND_HA)
        val snap = d.snapshot(LocalReading(emptyMap(), sensors = mapOf(SensorKind.TEMPERATURE to 21.0)), null)
        assertTrue(snap.sensorOnly)
        assertTrue(snap.switches.isEmpty())
        // Görülmeyince son bilinen değer kalır, çevrimdışı.
        val off = d.snapshot(null, snap)
        assertFalse(off.online)
        assertEquals(21.0, off.sensors[SensorKind.TEMPERATURE])
        assertEquals(0, LocalDevice.listFromJson(LocalDevice.listToJson(listOf(d))).single().channels)
        val meter = LocalDevice("shelly-x", "Ev", channels = 0, brand = LocalDevice.BRAND_SHELLY).snapshot(LocalReading(emptyMap(), power = 900.0), null)
        assertTrue(meter.sensorOnly)
    }
}
