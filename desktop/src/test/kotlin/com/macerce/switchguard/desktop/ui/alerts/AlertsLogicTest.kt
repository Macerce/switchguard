package com.macerce.switchguard.desktop.ui.alerts

import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.automations.AutomationForm
import com.macerce.switchguard.desktop.ui.automations.AutomationMode
import com.macerce.switchguard.desktop.ui.automations.inMode
import com.macerce.switchguard.desktop.ui.settings.windowsIssues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uyarılar / Otomasyon ayrımının ve Uyarılar sekmesi özetlerinin arayüzsüz mantığı (telefon 2.13 ile aynı kurallar). */
class AlertsLogicTest {
    private val en = Strings.load("en")
    private val tr = Strings.load("tr")

    // ---------------------------------------------------------------- hangi kayıt hangi sekmede

    @Test
    fun alarmAndNotifyAreAlertsTurnOnAndOffAreAutomations() {
        assertEquals(AutomationMode.ALERT, AutomationMode.of(ActionKind.ALARM))
        assertEquals(AutomationMode.ALERT, AutomationMode.of(ActionKind.NOTIFY))
        assertEquals(AutomationMode.AUTOMATION, AutomationMode.of(ActionKind.TURN_ON))
        assertEquals(AutomationMode.AUTOMATION, AutomationMode.of(ActionKind.TURN_OFF))
        // Her işlem tam olarak bir modda.
        assertEquals(ActionKind.entries.toSet(), AutomationMode.entries.flatMap { it.actions }.toSet())
        assertEquals(ActionKind.entries.size, AutomationMode.entries.sumOf { it.actions.size })
    }

    @Test
    fun theListIsSplitByActionAndKeepsItsOrder() {
        val list = listOf(
            auto("a", ActionKind.TURN_ON), auto("b", ActionKind.ALARM), auto("c", ActionKind.NOTIFY), auto("d", ActionKind.TURN_OFF),
        )
        assertEquals(listOf("b", "c"), list.inMode(AutomationMode.ALERT).map { it.id })
        assertEquals(listOf("a", "d"), list.inMode(AutomationMode.AUTOMATION).map { it.id })
    }

    @Test
    fun aNewRecordStartsWithTheModesActionAndAnExistingOneKeepsItsOwn() {
        val devices = listOf(DeviceSnapshot("d1", "Pump", online = true, switches = mapOf(0 to false), cloud = Cloud.DEMO))
        assertEquals(ActionKind.ALARM, AutomationForm.from(null, devices, AutomationMode.ALERT).actionKind)
        assertEquals(ActionKind.TURN_ON, AutomationForm.from(null, devices, AutomationMode.AUTOMATION).actionKind)
        val existing = auto("x", ActionKind.TURN_OFF).copy(action = AutoAction(ActionKind.TURN_OFF, "d1"))
        assertEquals(ActionKind.TURN_OFF, AutomationForm.from(existing, devices, AutomationMode.ALERT).actionKind)
        // Düzenleyici yalnızca modun işlemlerini gösterir: başka moddaki işlem varsayılana döner.
        assertEquals(ActionKind.ALARM, AutomationForm(actionKind = ActionKind.ALARM).inMode(AutomationMode.ALERT).actionKind)
        assertEquals(ActionKind.TURN_ON, AutomationForm(actionKind = ActionKind.ALARM).inMode(AutomationMode.AUTOMATION).actionKind)
    }

    // ---------------------------------------------------------------- cihaz uyarıları özeti

    @Test
    fun defaultRulesRingForThreeEventsAndNotifyWhenBackOnline() {
        assertEquals(
            DeviceAlertSummary.Monitored(
                alarm = listOf(EventType.TURNED_OFF, EventType.TURNED_ON, EventType.WENT_OFFLINE),
                notify = listOf(EventType.CAME_ONLINE),
            ),
            deviceAlertSummary(DeviceRules()),
        )
        assertEquals(
            listOf(
                DeviceAlertLine("Alarm: Kapanınca, Açılınca, Çevrimdışı olunca", AlertAction.ALARM),
                DeviceAlertLine("Bildirim: Çevrimiçi olunca", AlertAction.NOTIFY),
            ),
            deviceAlertLines(DeviceRules(), tr),
        )
    }

    @Test
    fun onlyTheLinesThatHaveEventsAreShown() {
        val notifyOnly = DeviceRules()
            .with(EventType.TURNED_OFF, AlertAction.IGNORE).with(EventType.TURNED_ON, AlertAction.NOTIFY)
            .with(EventType.WENT_OFFLINE, AlertAction.IGNORE).with(EventType.CAME_ONLINE, AlertAction.IGNORE)
        assertEquals(listOf(DeviceAlertLine("Notification: Turns on", AlertAction.NOTIFY)), deviceAlertLines(notifyOnly, en))

        val alarmOnly = DeviceRules().with(EventType.CAME_ONLINE, AlertAction.IGNORE)
        assertEquals(listOf(AlertAction.ALARM), deviceAlertLines(alarmOnly, en).map { it.action })
    }

    @Test
    fun notMonitoredAndNothingToSayAreSingleLines() {
        assertEquals(DeviceAlertSummary.NotMonitored, deviceAlertSummary(DeviceRules(monitored = false)))
        assertEquals(listOf(DeviceAlertLine("İzlenmiyor", null)), deviceAlertLines(DeviceRules(monitored = false, category = DeviceCategory.CRITICAL), tr))

        val silent = EventType.entries.fold(DeviceRules()) { r, t -> r.with(t, AlertAction.IGNORE) }
        assertEquals(DeviceAlertSummary.Monitored(emptyList(), emptyList()), deviceAlertSummary(silent))
        assertEquals(listOf(DeviceAlertLine("Monitored · no alarm or notification", null)), deviceAlertLines(silent, en))
    }

    // ---------------------------------------------------------------- durum kartı

    @Test
    fun statusCardFollowsMonitoringAndWindowsSettings() {
        assertEquals(AlertReadiness.MONITORING_OFF, alertReadiness(monitoring = false, issues = emptyList()))
        assertEquals(AlertReadiness.MONITORING_OFF, alertReadiness(monitoring = false, issues = windowsIssues(false, false)))
        assertEquals(AlertReadiness.WARNING, alertReadiness(monitoring = true, issues = windowsIssues(autostart = true, alwaysAwake = false)))
        assertEquals(AlertReadiness.READY, alertReadiness(monitoring = true, issues = windowsIssues(autostart = true, alwaysAwake = true)))
    }

    @Test
    fun windowsIssuesNameEachSwitchThatIsOff() {
        assertTrue(windowsIssues(autostart = true, alwaysAwake = true).isEmpty())
        assertEquals(listOf("desktop_settings_windows_no_autostart"), windowsIssues(autostart = false, alwaysAwake = true))
        assertEquals(
            listOf("desktop_settings_windows_no_autostart", "desktop_settings_windows_may_sleep"),
            windowsIssues(autostart = false, alwaysAwake = false),
        )
    }

    private fun auto(id: String, kind: ActionKind) = Automation(
        id = id, name = id, enabled = true,
        trigger = Trigger(TriggerKind.TURNED_ON, "d1"),
        action = AutoAction(kind),
    )
}
