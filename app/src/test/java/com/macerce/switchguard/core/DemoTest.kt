package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoTest {
    private val names = Demo.Names("Freezer", "Pumps", "Light", "Pump stalled", "Light left on")
    private val devices = Demo.devices(names).associateBy { it.id }

    @Test fun devicesAreDemoCloudAndMetered() {
        assertTrue(devices.values.all { it.cloud == Cloud.DEMO && Demo.isDemo(it.id) })
        assertEquals(118.0, devices.getValue(Demo.FREEZER).power!!, 0.01)
        val pumps = devices.getValue(Demo.PUMPS)
        assertEquals(740.0, pumps.channelPowerOf(0)!!, 0.01)
        assertEquals(0.0, pumps.channelPowerOf(1)!!, 0.01)
        assertEquals(795.0, pumps.power!!, 0.01)
        assertTrue(pumps.hasChannelPower)
    }

    @Test fun switchingOffZeroesPower() {
        val off = Demo.recompute(devices.getValue(Demo.FREEZER).copy(switches = mapOf(0 to false)))
        assertEquals(0.0, off.power!!, 0.0)
    }

    @Test fun stalledChannelDrawsAlmostNothing() {
        val stalled = Demo.recompute(devices.getValue(Demo.PUMPS), stalled = setOf(0))
        assertEquals(Demo.STALLED_WATTS, stalled.channelPowerOf(0)!!, 0.0)
        // Örnek otomasyonun eşiğinin altında kalmalı.
        val auto = Demo.automations(names).first { it.trigger.kind == TriggerKind.CHANNEL_POWER_LOW }
        assertTrue(stalled.channelPowerOf(0)!! < auto.trigger.watts)
    }

    @Test fun jitterStaysClose() {
        val j = Demo.recompute(devices.getValue(Demo.FREEZER), random = kotlin.random.Random(1))
        assertTrue(j.power!! in 114.0..122.0)
    }

    @Test fun plainSwitchHasNoEnergy() {
        assertEquals(null, devices.getValue(Demo.LIGHT).power)
        assertEquals(devices.getValue(Demo.LIGHT), Demo.recompute(devices.getValue(Demo.LIGHT)))
    }
}
