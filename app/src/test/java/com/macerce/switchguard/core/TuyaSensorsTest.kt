package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuyaSensorsTest {
    @Test
    fun temperatureHumiditySensorWithScalesFromSpecification() {
        val v = TuyaParser.sensors(
            mapOf("va_temperature" to 235, "va_humidity" to 53, "battery_percentage" to 87),
            mapOf("va_temperature" to 1, "va_humidity" to 0),
        )
        assertEquals(mapOf(SensorKind.TEMPERATURE to 23.5, SensorKind.HUMIDITY to 53.0, SensorKind.BATTERY to 87.0), v)
    }

    @Test
    fun zigbeeCodesAndMissingScales() {
        // Ürün tanımı okunamadı: sıcaklık onda bir, nem 1000'e kadar onda bir sayılır.
        val v = TuyaParser.sensors(mapOf("temp_current" to -52, "humidity_value" to 615, "battery_state" to "low"), emptyMap())
        assertEquals(-5.2, v[SensorKind.TEMPERATURE]!!, 0.001)
        assertEquals(61.5, v[SensorKind.HUMIDITY]!!, 0.001)
        assertEquals(10.0, v[SensorKind.BATTERY]!!, 0.001)
    }

    @Test
    fun doorWaterAndMotion() {
        assertEquals(1.0, TuyaParser.sensors(mapOf("doorcontact_state" to true), emptyMap())[SensorKind.DOOR])
        assertEquals(0.0, TuyaParser.sensors(mapOf("doorcontact_state" to false), emptyMap())[SensorKind.DOOR])
        assertEquals(1.0, TuyaParser.sensors(mapOf("watersensor_state" to "alarm"), emptyMap())[SensorKind.WATER])
        assertEquals(0.0, TuyaParser.sensors(mapOf("watersensor_state" to "normal"), emptyMap())[SensorKind.WATER])
        assertEquals(1.0, TuyaParser.sensors(mapOf("pir" to "pir"), emptyMap())[SensorKind.MOTION])
        assertEquals(0.0, TuyaParser.sensors(mapOf("pir" to "none"), emptyMap())[SensorKind.MOTION])
        assertEquals(1.0, TuyaParser.sensors(mapOf("presence_state" to "presence"), emptyMap())[SensorKind.MOTION])
    }

    @Test
    fun nonsenseValuesAreDropped() {
        val v = TuyaParser.sensors(mapOf("va_temperature" to 99999, "battery_percentage" to 250, "va_humidity" to 5000), mapOf("va_temperature" to 1))
        assertTrue(v.isEmpty())
    }

    @Test
    fun sensorOnlyDeviceFromStatusAndPartialMessages() {
        val device = JSONObject().put("id", "s1").put("name", "Salon").put("isOnline", true)
        val status = JSONArray()
            .put(JSONObject().put("code", "va_temperature").put("value", 221))
            .put(JSONObject().put("code", "va_humidity").put("value", 48))
            .put(JSONObject().put("code", "battery_percentage").put("value", 90))
        val scales = mapOf("va_temperature" to 1)
        val snap = TuyaParser.snapshot(device, status, scales)

        assertTrue(snap.sensorOnly)
        assertEquals(22.1, snap.sensors[SensorKind.TEMPERATURE]!!, 0.001)

        // Mesaj servisi yalnız değişen değeri gönderir: diğerleri korunur.
        val after = TuyaParser.apply(snap, mapOf("va_temperature" to 245), scales)
        assertEquals(24.5, after.sensors[SensorKind.TEMPERATURE]!!, 0.001)
        assertEquals(48.0, after.sensors[SensorKind.HUMIDITY]!!, 0.001)
        assertEquals(90.0, after.sensors[SensorKind.BATTERY]!!, 0.001)
    }

    @Test
    fun plugWithoutSensorsStaysAPlug() {
        val device = JSONObject().put("id", "p1").put("name", "Priz").put("isOnline", true)
        val status = JSONArray().put(JSONObject().put("code", "switch_1").put("value", true)).put(JSONObject().put("code", "cur_power").put("value", 1180))
        val snap = TuyaParser.snapshot(device, status, mapOf("cur_power" to 1))

        assertFalse(snap.hasSensors)
        assertEquals(118.0, snap.power!!, 0.001)
    }
}
