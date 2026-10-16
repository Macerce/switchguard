package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Kartın "Eğer / O zaman" satırları ve adımlayıcının adımları. */
class AutomationTextTest {
    private val tr = Strings.load("tr")
    private val lamp = DeviceSnapshot("lamp", "Lamba", online = true, switches = mapOf(0 to false))
    private val panel = DeviceSnapshot("panel", "Pompa paneli", online = true, switches = mapOf(0 to true, 1 to false))
    private val devices = listOf(lamp, panel)

    @Test
    fun cardLinesNameTheChannelTheEventAndTheAction() {
        val a = Automation(
            id = "a", name = "x", enabled = true,
            trigger = Trigger(TriggerKind.STAYED_ON, "panel", channel = 1, minutes = 30),
            action = AutoAction(ActionKind.TURN_ON, "lamp"), delayMinutes = 5,
        )

        val (whenText, thenText) = describe(a, devices, tr)

        assertEquals("Pompa paneli · Kanal 2 · şu süre açık kalınca: 30 dakika", whenText)
        assertEquals("Lamba: Aç · (5 dk sonra)", thenText)
    }

    @Test
    fun channelPowerLowWithoutGraceUsesTheShortForm() {
        val a = Automation(
            id = "a", name = "x", enabled = true,
            trigger = Trigger(TriggerKind.CHANNEL_POWER_LOW, "panel", channel = 0, minutes = 2, watts = 50.0, graceMinutes = 0),
            action = AutoAction(ActionKind.ALARM),
        )

        val (whenText, thenText) = describe(a, devices, tr)

        assertEquals(true, whenText.endsWith("(2 dk boyunca)"))
        assertEquals("Alarm çal", thenText)
    }

    @Test
    fun aRemovedDeviceIsNamedAsMissing() {
        val a = Automation(
            id = "a", name = "x", enabled = true,
            trigger = Trigger(TriggerKind.TURNED_ON, "gone"),
            action = AutoAction(ActionKind.NOTIFY),
        )

        assertEquals(tr["device_missing"] + " · açılınca", describe(a, devices, tr).first)
    }

    @Test
    fun stepsJumpToTheNextMeaningfulValue() {
        assertEquals(45, Steps.higher(30, Steps.MINUTES))
        assertEquals(20, Steps.lower(30, Steps.MINUTES))
        assertNull(Steps.lower(1, Steps.MINUTES)) // süre 0 olamaz
        assertNull(Steps.higher(1440, Steps.MINUTES))
        assertNull(Steps.lower(0, Steps.DELAY))
        // Listede olmayan eski değer: en yakın alt/üst adıma gidilir.
        assertEquals(10, Steps.higher(7, Steps.MINUTES))
        assertEquals(5, Steps.lower(7, Steps.MINUTES))
    }
}
