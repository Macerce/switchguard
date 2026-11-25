package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDiffTest {
    private val a = DeviceSnapshot("a", "Priz", true, mapOf(0 to true), power = 10.0, sensors = mapOf(SensorKind.TEMPERATURE to 20.0, SensorKind.DOOR to 0.0))

    @Test
    fun readingsOnly() {
        assertTrue(SnapshotDiff.onlyReadings(listOf(a), listOf(a.copy(power = 55.0, rssi = -60, sensors = a.sensors + (SensorKind.TEMPERATURE to 21.0)))))
    }

    @Test
    fun stateChangesWriteNow() {
        assertFalse(SnapshotDiff.onlyReadings(listOf(a), listOf(a.copy(switches = mapOf(0 to false)))))
        assertFalse(SnapshotDiff.onlyReadings(listOf(a), listOf(a.copy(online = false))))
        assertFalse(SnapshotDiff.onlyReadings(listOf(a), listOf(a.copy(sensors = a.sensors + (SensorKind.DOOR to 1.0)))))
        assertFalse(SnapshotDiff.onlyReadings(listOf(a), listOf(a.copy(power = null))))
        assertFalse(SnapshotDiff.onlyReadings(listOf(a), emptyList()))
    }
}
