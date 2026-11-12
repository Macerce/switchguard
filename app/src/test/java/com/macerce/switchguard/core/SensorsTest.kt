package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorsTest {

    private fun p(json: String) = JSONObject(json)

    @Test fun th16ReadsCurrentTemperatureAndHumidity() {
        val s = Sensors.parse(15, p("""{"switch":"on","currentTemperature":"23.46","currentHumidity":"55"}"""))
        assertEquals(23.5, s[SensorKind.TEMPERATURE]!!, 0.001)
        assertEquals(55.0, s[SensorKind.HUMIDITY]!!, 0.001)
    }

    @Test fun th16WithoutProbeGivesNothing() {
        val s = Sensors.parse(15, p("""{"switch":"on","currentTemperature":"unavailable","currentHumidity":"unavailable"}"""))
        assertTrue(s.isEmpty())
    }

    @Test fun zigbeeHundredths() {
        val s = Sensors.parse(1770, p("""{"temperature":2350,"humidity":4812,"battery":87}"""))
        assertEquals(23.5, s[SensorKind.TEMPERATURE]!!, 0.001)
        assertEquals(48.1, s[SensorKind.HUMIDITY]!!, 0.001)
        assertEquals(87.0, s[SensorKind.BATTERY]!!, 0.001)
    }

    @Test fun snzb02wdRawValues() {
        val s = Sensors.parse(7033, p("""{"temperature":21.7,"humidity":60}"""))
        assertEquals(21.7, s[SensorKind.TEMPERATURE]!!, 0.001)
        assertEquals(60.0, s[SensorKind.HUMIDITY]!!, 0.001)
    }

    @Test fun correctionIsAdded() {
        val s = Sensors.parse(7038, p("""{"temperature":2000,"tempCorrection":-1.5}"""))
        assertEquals(18.5, s[SensorKind.TEMPERATURE]!!, 0.001)
    }

    @Test fun unknownZigbeeScaledHeuristically() {
        val s = Sensors.parse(9999, p("""{"temperature":2210}"""))
        assertEquals(22.1, s[SensorKind.TEMPERATURE]!!, 0.001)
    }

    @Test fun wifiDoorUsesSwitchAsDoorAndIsNotARelay() {
        val s = Sensors.parse(102, p("""{"switch":"on","battery":2.9}"""))
        assertEquals(1.0, s[SensorKind.DOOR]!!, 0.0)
        assertTrue(SensorKind.BATTERY !in s) // DW2'de volt
        val body = """{"data":{"thingList":[{"itemData":{"deviceid":"d1","name":"Kapı","online":true,"extra":{"uiid":102},"params":{"switch":"off"}}}]}}"""
        val d = DeviceParser.parseThingList(body).single()
        assertTrue(d.switches.isEmpty())
        assertTrue(d.sensorOnly)
        assertEquals(false, d.sensorActive(SensorKind.DOOR))
    }

    @Test fun zigbeeDoorMotionWater() {
        assertEquals(1.0, Sensors.parse(7003, p("""{"lock":1}"""))[SensorKind.DOOR]!!, 0.0)
        assertEquals(0.0, Sensors.parse(2026, p("""{"motion":0}"""))[SensorKind.MOTION]!!, 0.0)
        assertEquals(1.0, Sensors.parse(7016, p("""{"human":1}"""))[SensorKind.MOTION]!!, 0.0)
        assertEquals(1.0, Sensors.parse(7019, p("""{"water":1}"""))[SensorKind.WATER]!!, 0.0)
        assertTrue(SensorKind.WATER !in Sensors.parse(25, p("""{"water":1}""")))
    }

    @Test fun wsUpdateMergesAndKeepsDoorOutOfSwitches() {
        val door = DeviceSnapshot("d1", "Kapı", true, emptyMap(), uiid = 102, sensors = mapOf(SensorKind.DOOR to 0.0))
        val after = WsMessages.apply(door, WsMessages.parse("""{"action":"update","deviceid":"d1","params":{"switch":"on"}}""") as WsEvent.Update)
        assertTrue(after.switches.isEmpty())
        assertEquals(true, after.sensorActive(SensorKind.DOOR))

        val th = DeviceSnapshot("t1", "TH", true, mapOf(0 to true), uiid = 15, sensors = mapOf(SensorKind.TEMPERATURE to 20.0, SensorKind.HUMIDITY to 50.0))
        val th2 = WsMessages.apply(th, WsMessages.parse("""{"action":"update","deviceid":"t1","params":{"currentTemperature":"21.0"}}""") as WsEvent.Update)
        assertEquals(21.0, th2.sensors[SensorKind.TEMPERATURE]!!, 0.0)
        assertEquals(50.0, th2.sensors[SensorKind.HUMIDITY]!!, 0.0)
    }

    @Test fun snapshotJsonRoundTrip() {
        val d = DeviceSnapshot("t1", "TH", true, mapOf(0 to true), uiid = 15, sensors = mapOf(SensorKind.TEMPERATURE to 21.5, SensorKind.DOOR to 1.0))
        assertEquals(d.sensors, DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(d))).single().sensors)
    }

    // ------------------------------------------------ otomasyon

    private fun dev(temp: Double?, door: Double? = null, online: Boolean = true) = DeviceSnapshot(
        "s", "Sensör", online, emptyMap(), uiid = 7014,
        sensors = buildMap { temp?.let { put(SensorKind.TEMPERATURE, it) }; door?.let { put(SensorKind.DOOR, it) } },
    )

    @Test fun sensorAboveFiresOnceAndRearmsAfterHysteresis() {
        val a = Automation("a", "Sıcak", true, Trigger(TriggerKind.SENSOR_ABOVE, "s", watts = 26.0, sensor = SensorKind.TEMPERATURE), AutoAction(ActionKind.NOTIFY))
        val e = AutomationEngine()
        var t = 1_000_000L
        fun step(v: Double, prev: Double): Int {
            t += 60_000
            return e.onStates(mapOf("s" to dev(prev)), mapOf("s" to dev(v)), listOf(a), t).size
        }
        assertEquals(0, step(25.0, 24.0))
        assertEquals(1, step(26.5, 25.0))
        assertEquals(0, step(27.0, 26.5))
        assertEquals(0, step(25.8, 27.0)) // histerezis içinde: yeniden kurulmaz
        assertEquals(0, step(26.2, 25.8))
        assertEquals(0, step(25.4, 26.2)) // 0,5 °C geri döndü → kuruldu
        assertEquals(1, step(26.1, 25.4))
    }

    @Test fun sensorIgnoredWhileOffline() {
        val a = Automation("a", "Soğuk", true, Trigger(TriggerKind.SENSOR_BELOW, "s", watts = 5.0, sensor = SensorKind.TEMPERATURE), AutoAction(ActionKind.ALARM))
        val fired = AutomationEngine().onStates(mapOf("s" to dev(10.0)), mapOf("s" to dev(2.0, online = false)), listOf(a), 1_000_000L)
        assertTrue(fired.isEmpty())
    }

    @Test fun doorOpenedFiresOnEdgeOnly() {
        val a = Automation("a", "Kapı", true, Trigger(TriggerKind.SENSOR_ACTIVE, "s", sensor = SensorKind.DOOR), AutoAction(ActionKind.ALARM))
        val e = AutomationEngine()
        assertEquals(1, e.onStates(mapOf("s" to dev(null, 0.0)), mapOf("s" to dev(null, 1.0)), listOf(a), 1_000_000L).size)
        assertEquals(0, e.onStates(mapOf("s" to dev(null, 1.0)), mapOf("s" to dev(null, 1.0)), listOf(a), 1_100_000L).size)
        val closed = a.copy(id = "b", trigger = a.trigger.copy(kind = TriggerKind.SENSOR_CLEAR))
        assertEquals(1, e.onStates(mapOf("s" to dev(null, 1.0)), mapOf("s" to dev(null, 0.0)), listOf(closed), 1_200_000L).size)
    }

    @Test fun sensorAlertQuickRules() {
        val door = SensorAlerts.simple("d", "Kapı", SensorKind.DOOR, ActionKind.ALARM)!!
        assertEquals("salert:d:DOOR", door.id)
        assertEquals(TriggerKind.SENSOR_ACTIVE, door.trigger.kind)
        assertEquals(AutomationTab.ALERTS, AutomationTab.tabOf(door))
        assertEquals(null, SensorAlerts.simple("d", "Kapı", SensorKind.DOOR, null))
        val bat = SensorAlerts.simple("d", "Pil", SensorKind.BATTERY, ActionKind.NOTIFY)!!
        assertEquals(TriggerKind.SENSOR_BELOW, bat.trigger.kind)
        assertEquals(SensorAlerts.LOW_BATTERY, bat.trigger.watts, 0.0)
        val hi = SensorAlerts.bound("d", "Dondurucu", SensorKind.TEMPERATURE, SensorAlerts.Bound.MAX, -15.0, ActionKind.ALARM)!!
        assertEquals("salert:d:TEMPERATURE:MAX", hi.id)
        assertEquals(TriggerKind.SENSOR_ABOVE, hi.trigger.kind)
        assertEquals(null, SensorAlerts.bound("d", "x", SensorKind.TEMPERATURE, SensorAlerts.Bound.MIN, null, ActionKind.ALARM))
        assertEquals(ActionKind.ALARM, SensorAlerts.actionOf(listOf(hi), hi.id))
        assertEquals(null, SensorAlerts.actionOf(listOf(hi.copy(enabled = false)), hi.id))
        assertEquals(-15.0, SensorAlerts.valueOf(listOf(hi), hi.id)!!, 0.0)
    }

    @Test fun sensorOnlyDeviceKind() {
        assertEquals(DeviceKind.SENSOR, DeviceKinds.of(dev(22.0)))
        assertEquals(DeviceKind.DOOR, DeviceKinds.of(dev(null, 0.0)))
    }

    @Test fun automationJsonKeepsSensor() {
        val a = Automation("a", "Nem", true, Trigger(TriggerKind.SENSOR_ABOVE, "s", watts = 70.0, sensor = SensorKind.HUMIDITY), AutoAction(ActionKind.TURN_ON, "fan"))
        assertEquals(a, Automation.listFromJson(Automation.listToJson(listOf(a))).single())
    }
}
