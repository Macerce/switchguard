package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorHistoryTest {
    @Test fun writesOnChangeOrHeartbeat() {
        val k = SensorKind.TEMPERATURE
        assertTrue(SensorHistory.shouldWrite(k, null, 21.0, 0))
        val last = SensorSample(0, 21.0)
        // Küçük değişim ya da çok sık: yazılmaz.
        assertFalse(SensorHistory.shouldWrite(k, last, 21.05, 60_000))
        assertFalse(SensorHistory.shouldWrite(k, last, 22.0, 10_000))
        assertTrue(SensorHistory.shouldWrite(k, last, 21.1, 60_000))
        // Değişmese de 10 dk'da bir.
        assertTrue(SensorHistory.shouldWrite(k, last, 21.0, SensorHistory.EVERY_MS))
        assertFalse(SensorHistory.recordable(SensorKind.DOOR))
        assertTrue(SensorHistory.recordable(SensorKind.HUMIDITY))
    }

    @Test fun statsAndBuckets() {
        val samples = listOf(SensorSample(0, 20.0), SensorSample(10, 22.0), SensorSample(60, 18.0), SensorSample(99, 19.0), SensorSample(100, 50.0))
        val s = SensorHistory.stats(samples, 0, 100, 4)!!
        assertEquals(18.0, s.min.value, 0.0)
        assertEquals(60L, s.min.time)
        assertEquals(22.0, s.max.value, 0.0)
        assertEquals((20 + 22 + 18 + 19) / 4.0, s.avg, 1e-9)
        assertEquals(4, s.points.size)
        assertEquals(21.0, s.points[0]!!.avg, 1e-9)
        assertNull(s.points[1])
        assertEquals(18.0, s.points[2]!!.min, 0.0)
        assertEquals(19.0, s.points[3]!!.max, 0.0)
        assertNull(SensorHistory.stats(emptyList(), 0, 100, 4))
    }
}
