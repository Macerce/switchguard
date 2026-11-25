package com.macerce.switchguard

import com.macerce.switchguard.core.StandbyControl
import com.macerce.switchguard.core.StandbyControl.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StandbyControlTest {
    @Test
    fun roundTripKeepsNonDefaultModes() {
        val m = mapOf("dev:a:b:0" to Mode.CONFIRM, "dev:x:1" to Mode.NONE)
        assertEquals(m, StandbyControl.parse(StandbyControl.format(m)))
    }

    @Test
    fun toggleIsDefaultAndNotStored() {
        val m = StandbyControl.with(mapOf("dev:a:0" to Mode.NONE), "dev:a:0", Mode.TOGGLE)
        assertEquals(emptyMap<String, Mode>(), m)
        assertNull(StandbyControl.format(m))
    }

    @Test
    fun badLinesAreIgnored() {
        assertEquals(mapOf("dev:a:0" to Mode.CONFIRM), StandbyControl.parse("junk\ndev:a:0\tCONFIRM\ndev:b:0\tWHAT\n"))
    }

    @Test
    fun brightnessRules() {
        assertNull(StandbyControl.brightness(night = true, idle = true, idleDim = true, alarm = true))
        assertEquals(StandbyControl.NIGHT_BRIGHTNESS, StandbyControl.brightness(night = true, idle = true, idleDim = false, alarm = false)!!, 0f)
        assertEquals(StandbyControl.IDLE_BRIGHTNESS, StandbyControl.brightness(night = true, idle = false, idleDim = false, alarm = false)!!, 0f)
        assertEquals(StandbyControl.IDLE_BRIGHTNESS, StandbyControl.brightness(night = false, idle = true, idleDim = true, alarm = false)!!, 0f)
        assertNull(StandbyControl.brightness(night = false, idle = true, idleDim = false, alarm = false))
        assertNull(StandbyControl.brightness(night = false, idle = false, idleDim = true, alarm = false))
    }
}
