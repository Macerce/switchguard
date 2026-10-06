package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationFormTest {
    private val plain = DeviceSnapshot("lamp", "Lamba", online = true, switches = mapOf(0 to true))
    private val meter = DeviceSnapshot("pump", "Pompa", online = true, switches = mapOf(0 to true), power = 120.0)
    private val fourCh = DeviceSnapshot("panel", "Panel", online = true, switches = mapOf(0 to true, 1 to true, 2 to false, 3 to false))
    private val devices = listOf(plain, meter, fourCh)

    @Test
    fun newFormStartsWithTheFirstDeviceAndThePhoneDefaults() {
        val f = AutomationForm.from(null, devices)

        assertEquals("lamp", f.triggerDevice)
        assertEquals("lamp", f.actionDevice)
        assertEquals(TriggerKind.TURNED_ON, f.triggerKind)
        assertEquals(ActionKind.NOTIFY, f.actionKind)
        assertEquals("30", f.minutes)
        assertEquals("1000", f.watts)
        assertEquals("0", f.delay)
    }

    @Test
    fun powerEventsAreOfferedOnlyForMeteringDevices() {
        val power = setOf(TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW, TriggerKind.CHANNEL_POWER_LOW)

        assertTrue((AutomationForm.kindsFor(plain) intersect power).isEmpty())
        assertTrue(TriggerKind.POWER_ABOVE in AutomationForm.kindsFor(meter))
        assertTrue(TriggerKind.CHANNEL_POWER_LOW in AutomationForm.kindsFor(meter)) // tek kanallı ve güç ölçen: kanal gücü de var
        assertFalse(TriggerKind.POWER_ABOVE in AutomationForm.kindsFor(null))
    }

    @Test
    fun anEventTheDeviceCannotReportFallsBackToTurnedOn() {
        val f = AutomationForm.from(null, devices).copy(triggerDevice = "lamp", triggerKind = TriggerKind.POWER_ABOVE)

        assertEquals(TriggerKind.TURNED_ON, f.normalized(devices).triggerKind)
    }

    @Test
    fun validation() {
        val ok = AutomationForm.from(null, devices).copy(name = "Pompa uzun açık")
        assertTrue(ok.isValid(devices))

        assertFalse(ok.copy(name = "  ").isValid(devices))
        assertFalse(ok.copy(triggerDevice = "missing").isValid(devices))
        assertFalse(ok.copy(triggerKind = TriggerKind.STAYED_ON, minutes = "0").isValid(devices))
        assertTrue(ok.copy(triggerKind = TriggerKind.STAYED_ON, minutes = "45").isValid(devices))
        assertFalse(ok.copy(triggerDevice = "pump", triggerKind = TriggerKind.POWER_ABOVE, watts = "abc").isValid(devices))
        assertTrue(ok.copy(triggerDevice = "pump", triggerKind = TriggerKind.POWER_ABOVE, watts = "1,5").isValid(devices))
        assertFalse(ok.copy(actionKind = ActionKind.TURN_OFF, actionDevice = "missing").isValid(devices))
        assertFalse(ok.copy(delay = "-1").isValid(devices))
    }

    @Test
    fun buildKeepsTheChannelOnlyForChannelEvents() {
        val f = AutomationForm.from(null, devices).copy(
            name = " Panel koptu ", triggerDevice = "panel", triggerKind = TriggerKind.WENT_OFFLINE, triggerChannel = 2,
            actionKind = ActionKind.ALARM, delay = "3",
        )

        val a = f.build(id = "x", enabled = true)

        assertEquals("Panel koptu", a.name)
        assertEquals(Trigger(TriggerKind.WENT_OFFLINE, "panel", channel = 0), a.trigger)
        assertEquals(AutoAction(ActionKind.ALARM), a.action)
        assertEquals(3, a.delayMinutes)
    }

    @Test
    fun buildForChannelPowerLowUsesItsOwnMinutesAndGrace() {
        val f = AutomationForm.from(null, devices).copy(
            name = "Pompa boşa döndü", triggerDevice = "pump", triggerKind = TriggerKind.CHANNEL_POWER_LOW,
            watts = "20.5", lowMinutes = "4", grace = "2", actionKind = ActionKind.TURN_OFF, actionDevice = "panel", actionChannel = 1,
        )

        val a = f.build(id = "y", enabled = false)

        assertEquals(Trigger(TriggerKind.CHANNEL_POWER_LOW, "pump", channel = 0, minutes = 4, watts = 20.5, graceMinutes = 2), a.trigger)
        assertEquals(AutoAction(ActionKind.TURN_OFF, "panel", 1), a.action)
        assertEquals(false, a.enabled)
    }

    @Test
    fun existingAutomationRoundTrips() {
        val a = Automation(
            id = "a1", name = "Uzun açık", enabled = false,
            trigger = Trigger(TriggerKind.STAYED_ON, "panel", channel = 3, minutes = 30),
            action = AutoAction(ActionKind.TURN_OFF, "panel", 3), delayMinutes = 5,
        )

        assertEquals(a, AutomationForm.from(a, devices).build(a.id, a.enabled))
    }
}
