package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsViewTest {

    private fun auto(id: String, kind: ActionKind) = Automation(
        id = id, name = id, enabled = true,
        trigger = Trigger(TriggerKind.TURNED_OFF, "dev"),
        action = if (kind.usesDevice) AutoAction(kind, "other") else AutoAction(kind),
    )

    private val texts = DeviceAlertSummary.Texts(
        notMonitored = "Not monitored",
        none = "Monitored · nothing",
        alarm = { "Alarm: $it" },
        notify = { "Notification: $it" },
        event = {
            when (it) {
                EventType.TURNED_OFF -> "off"
                EventType.TURNED_ON -> "on"
                EventType.WENT_OFFLINE -> "offline"
                EventType.CAME_ONLINE -> "online"
            }
        },
    )

    @Test
    fun `alarm ve bildirim uyarilar sekmesinde, ac kapa otomasyon sekmesinde`() {
        assertEquals(AutomationTab.ALERTS, AutomationTab.tabOf(auto("a", ActionKind.ALARM)))
        assertEquals(AutomationTab.ALERTS, AutomationTab.tabOf(auto("n", ActionKind.NOTIFY)))
        assertEquals(AutomationTab.AUTOMATIONS, AutomationTab.tabOf(auto("on", ActionKind.TURN_ON)))
        assertEquals(AutomationTab.AUTOMATIONS, AutomationTab.tabOf(auto("off", ActionKind.TURN_OFF)))
    }

    @Test
    fun `her islem turu tam olarak bir sekmenin duzenleyicisinde`() {
        for (kind in ActionKind.entries) {
            val tabs = AutomationTab.entries.filter { kind in it.actions }
            assertEquals(listOf(AutomationTab.tabOf(kind)), tabs)
        }
    }

    @Test
    fun `liste ikiye ayrilir, sira korunur, kayip yok`() {
        val list = listOf(
            auto("1", ActionKind.TURN_ON), auto("2", ActionKind.ALARM), auto("3", ActionKind.NOTIFY),
            auto("4", ActionKind.TURN_OFF), auto("5", ActionKind.ALARM),
        )
        assertEquals(listOf("2", "3", "5"), AutomationTab.ALERTS.of(list).map { it.id })
        assertEquals(listOf("1", "4"), AutomationTab.AUTOMATIONS.of(list).map { it.id })
    }

    @Test
    fun `demo ornekleri uyarilara duser`() {
        val names = Demo.Names("Freezer", "Pumps", "Light", "Pump stalled", "Light left on")
        val demo = Demo.automations(names)
        assertEquals(demo, AutomationTab.ALERTS.of(demo))
        assertTrue(AutomationTab.AUTOMATIONS.of(demo).isEmpty())
    }

    @Test
    fun `varsayilan kurallar - uc olayda alarm, cevrimicinde bildirim`() {
        val s = DeviceAlertSummary.of(DeviceRules(), monitored = true)
        assertEquals(listOf(EventType.TURNED_OFF, EventType.TURNED_ON, EventType.WENT_OFFLINE), s.alarm)
        assertEquals(listOf(EventType.CAME_ONLINE), s.notify)
        assertEquals(listOf("Alarm: off, on, offline", "Notification: online"), s.lines(texts))
        assertFalse(s.critical)
        assertFalse(s.customSound)
    }

    @Test
    fun `izlenmeyen cihaz tek satir - kurallari olsa da`() {
        val s = DeviceAlertSummary.of(DeviceRules(), monitored = false)
        assertEquals(listOf("Not monitored"), s.lines(texts))
    }

    @Test
    fun `hicbir olay uyarmiyorsa izleniyor ama uyari yok`() {
        val rules = EventType.entries.fold(DeviceRules()) { r, t -> r.with(t, AlertAction.IGNORE) }
        assertEquals(listOf("Monitored · nothing"), DeviceAlertSummary.of(rules, monitored = true).lines(texts))
    }

    @Test
    fun `yalniz bildirim varsa alarm satiri yok`() {
        val rules = DeviceRules()
            .with(EventType.TURNED_OFF, AlertAction.NOTIFY)
            .with(EventType.TURNED_ON, AlertAction.IGNORE)
            .with(EventType.WENT_OFFLINE, AlertAction.NOTIFY)
            .with(EventType.CAME_ONLINE, AlertAction.IGNORE)
        assertEquals(listOf("Notification: off, offline"), DeviceAlertSummary.of(rules, monitored = true).lines(texts))
    }

    @Test
    fun `yalniz alarm varsa bildirim satiri yok`() {
        val rules = DeviceRules().with(EventType.CAME_ONLINE, AlertAction.ALARM)
        assertEquals(listOf("Alarm: off, on, offline, online"), DeviceAlertSummary.of(rules, monitored = true).lines(texts))
    }

    @Test
    fun `kritik ve ozel ses ozete tasinir`() {
        val s = DeviceAlertSummary.of(DeviceRules(critical = true, soundUri = "content://ses"), monitored = true)
        assertTrue(s.critical)
        assertTrue(s.customSound)
    }
}
