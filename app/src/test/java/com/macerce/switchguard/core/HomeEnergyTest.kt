package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeEnergyTest {
    private fun summary(vararg kwh: Double, covered: Long = 3_600_000) = EnergySummary(
        EnergyPeriod.DAY, 0, kwh.size * 3_600_000L,
        kwh.mapIndexed { i, k -> EnergyBucket(i * 3_600_000L, (i + 1) * 3_600_000L, k, null, null, null, if (k > 0) covered else 0, cost = k * 2) },
    )

    @Test
    fun sumsDevicesAndShares() {
        val h = HomeEnergy.build(EnergyPeriod.DAY, 0, 7_200_000, mapOf("a" to summary(1.0, 0.0), "b" to summary(1.0, 2.0)), emptySet())
        assertEquals(4.0, h.total.kwh, 1e-9)
        assertEquals(8.0, h.total.cost, 1e-9)
        assertEquals(listOf("b", "a"), h.shares.map { it.id })
        assertEquals(0.75, h.shares.first().share, 1e-9)
        // Aynı saatte iki cihaz: kapsanan süre bir saat, ortalama güç 2 kW.
        assertEquals(3_600_000L, h.total.buckets[0].coveredMs)
        assertEquals(2000.0, h.total.buckets[0].avgWatts!!, 1e-6)
        assertNull(h.untrackedKwh)
    }

    @Test
    fun meterIsTotalAndRestIsUntracked() {
        val h = HomeEnergy.build(EnergyPeriod.DAY, 0, 3_600_000, mapOf("m" to summary(5.0), "p" to summary(1.5)), setOf("m"))
        assertEquals(5.0, h.total.kwh, 1e-9)
        assertEquals(3.5, h.untrackedKwh!!, 1e-9)
        assertEquals(listOf("p"), h.shares.map { it.id })
        assertTrue(HomeEnergy.isMeter(DeviceSnapshot("m", "EM", true, emptyMap(), power = 10.0)))
    }
}
