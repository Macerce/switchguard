package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuyaListenerCheckTest {
    private val plug = DeviceSnapshot("p", "Priz", online = true, switches = mapOf(0 to true), cloud = Cloud.TUYA, power = 40.0)

    @Test
    fun switchOrConnectionChangeNotSeenOnSocketIsAMiss() {
        assertTrue(TuyaListenerCheck.missed(listOf(plug), listOf(plug.copy(switches = mapOf(0 to false)))))
        assertTrue(TuyaListenerCheck.missed(listOf(plug), listOf(plug.copy(online = false))))
    }

    @Test
    fun powerChangesAndNewDevicesAreNotMisses() {
        assertFalse(TuyaListenerCheck.missed(listOf(plug), listOf(plug.copy(power = 900.0))))
        assertFalse(TuyaListenerCheck.missed(listOf(plug), listOf(plug, plug.copy(id = "new"))))
        assertFalse(TuyaListenerCheck.missed(listOf(plug), listOf(plug)))
    }

    @Test
    fun twoMissesInARowMeanStandbyAndNotifyOnce() {
        val c = TuyaListenerCheck()

        assertFalse(c.onSync(missed = true))
        assertFalse(c.standby)
        // İkinci kaçırma: artık yedek; haber bir kez verilir.
        assertTrue(c.onSync(missed = true))
        assertTrue(c.standby)
        assertFalse(c.onSync(missed = true))
        assertTrue(c.standby)
    }

    @Test
    fun syncWithoutChangesSaysNothing() {
        val c = TuyaListenerCheck()
        c.onSync(missed = true)

        assertFalse(c.onSync(missed = false))
        assertEquals(1, c.misses)
        assertTrue(c.onSync(missed = true))
    }

    @Test
    fun socketEventMeansThisDeviceReceives() {
        val c = TuyaListenerCheck()
        c.onSync(true)
        c.onSync(true)

        c.onEvent()

        assertFalse(c.standby)
        // Diğer cihaz kapanınca Failover bu cihaza geçer; olaylar gelir ve yeniden iki kaçırma gerekir.
        assertFalse(c.onSync(true))
    }
}
