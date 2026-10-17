package com.macerce.switchguard.desktop.ui.alerts

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.ui.SwitchGuardTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Uyarılar sekmesi (telefon 2.13): durum kartı, cihaz uyarıları ve kuralları, koşullu uyarılar (yalnızca alarm/bildirim
 * kayıtları, düzenleyici uyarı modunda), uyarı ayarları. Test alarmı sahtedir (bilgisayarda gerçekten çalmasın).
 */
class AlertsScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private val started = mutableListOf<Pair<Boolean, Long>>()

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
        // Demo'nun iki uyarısına ek olarak cihaz açan bir otomasyon: Uyarılar'da görünmemeli.
        graph.store.automations = graph.store.automations + Automation(
            id = "x-on", name = "Light on when warm", enabled = true,
            trigger = Trigger(TriggerKind.POWER_ABOVE, Demo.FREEZER, watts = 1500.0),
            action = AutoAction(ActionKind.TURN_ON, Demo.LIGHT),
        )
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show(page: AlertsPage? = null) = compose.setContent {
        SwitchGuardTheme("light") {
            AlertsScreen(graph, graph.strings(), startPage = page, testAlarm = { silent, delay -> started += silent to delay })
        }
    }

    @Test
    fun homeListsDevicesConditionalAlertsAndSettings() {
        show()

        compose.onNodeWithText("Monitoring is off").assertExists()
        compose.onNodeWithTag("alerts-start").assertExists()
        compose.onNodeWithTag("device-alerts-${Demo.PUMPS}").performScrollTo()
        // Üç demo cihazı da varsayılan kurallarla: üç olayda alarm, çevrimiçi olunca bildirim.
        compose.onAllNodesWithText("Alarm: Turns off, Turns on, Goes offline", useUnmergedTree = true).assertCountEquals(3)
        compose.onAllNodesWithText("Notification: Comes online", useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithText(graph.strings().demoNames().stalledPump).performScrollTo()
        compose.onNodeWithText("Light on when warm").assertDoesNotExist() // otomasyon sekmesinde
        for (title in listOf("Alarm", "Quiet hours", "Tell someone else")) compose.onNodeWithText(title).performScrollTo()
    }

    @Test
    fun readyWhenMonitoringAndWindowsSettingsAreOn() {
        graph.store.monitoringEnabled = true
        show()

        compose.onNodeWithTag("alerts-status-ready").assertExists()
        compose.onNodeWithText("Alarm is ready").assertExists()
        compose.onNodeWithTag("alerts-start").assertDoesNotExist()
    }

    @Test
    fun windowsWarningOpensWindowsSettings() {
        graph.store.monitoringEnabled = true
        graph.store.alwaysAwake = false
        show()

        compose.onNodeWithTag("alerts-status-warning").assertExists()
        compose.onNodeWithText("The computer may go to sleep").performClick()
        compose.waitForIdle()

        assertEquals(AppState.Tab.SETTINGS, graph.appState.tab.value)
        assertEquals("WINDOWS", graph.appState.settingsOpen.value)
    }

    @Test
    fun testAlarmButtonStartsASilentAlarm() {
        show()

        compose.onNodeWithTag("alerts-test").performClick()
        compose.onNodeWithText("Silent (screen only)").performClick()
        compose.onNodeWithText("Start").performClick()
        compose.waitForIdle()

        assertEquals(listOf(true to 0L), started)
    }

    @Test
    fun deviceRowOpensItsRulesAndChangesGoThroughTheEngine() {
        show()

        compose.onNodeWithTag("device-alerts-${Demo.PUMPS}").performScrollTo().performClick()
        compose.onNodeWithText("Alert rules").assertExists()
        compose.onNodeWithTag("rule-TURNED_ON-IGNORE").performScrollTo().performClick()
        compose.waitUntil(5_000) { graph.store.rulesFor(Demo.PUMPS).actionFor(EventType.TURNED_ON) == AlertAction.IGNORE }
        compose.onNodeWithTag("critical-switch").performScrollTo().performClick()
        compose.waitUntil(5_000) { graph.store.rulesFor(Demo.PUMPS).critical }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Alarm: Turns off, Goes offline", useUnmergedTree = true).performScrollTo()
        compose.onNodeWithText("Critical device", useUnmergedTree = true).assertExists()
    }

    @Test
    fun turningMonitoringOffShowsNotMonitored() {
        show(AlertsPage.Rules(Demo.LIGHT))

        compose.onNodeWithTag("monitor-switch").performClick()
        compose.waitUntil(5_000) { !graph.store.rulesFor(Demo.LIGHT).monitored }
        compose.onNodeWithTag("rule-TURNED_ON-ALARM").assertDoesNotExist()
        compose.onNodeWithTag("monitor-switch").assertIsOff()
    }

    @Test
    fun addingAConditionalAlertOffersOnlyAlarmAndNotification() {
        show()

        compose.onNodeWithTag("add-alert").performScrollTo().performClick()
        compose.onNodeWithText("New alert").assertExists()
        compose.onNodeWithTag("action-TURN_ON").assertDoesNotExist()
        compose.onNodeWithTag("action-TURN_OFF").assertDoesNotExist()
        compose.onNodeWithTag("automation-name").performTextInput("Pump turned off")
        compose.onNodeWithTag("event-TURNED_OFF").performScrollTo().performClick()
        compose.onNodeWithTag("action-ALARM").performScrollTo().performClick()
        compose.onNodeWithTag("save-automation").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.any { it.name == "Pump turned off" } }
        val a = graph.store.automations.single { it.name == "Pump turned off" }
        assertEquals(ActionKind.ALARM, a.action.kind)
        assertEquals(TriggerKind.TURNED_OFF, a.trigger.kind)
        // Kaydedince sekmenin başına dönülür; yeni uyarı listede.
        compose.onNodeWithText("Pump turned off").performScrollTo()
    }

    @Test
    fun deletingAConditionalAlertAsksFirst() {
        show()

        compose.onNodeWithTag("automation-demo-auto-light").performScrollTo().performClick()
        compose.onNodeWithText("Edit alert").assertExists()
        compose.onNodeWithTag("delete-automation").performClick()
        compose.onNodeWithText("Delete this alert?").assertExists()
        compose.onNodeWithTag("confirm-delete").performClick()

        compose.waitUntil(5_000) { graph.store.automations.none { it.id == "demo-auto-light" } }
    }

    @Test
    fun theSwitchOnTheCardTurnsAnAlertOff() {
        show()

        compose.onNodeWithTag("enabled-demo-auto-pump").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.single { it.id == "demo-auto-pump" }.enabled == false }
    }

    @Test
    fun telegramFromTheAlarmPageGoesBackToAlarmAndTheTabReturnsHome() {
        show()

        compose.onNodeWithTag("alert-settings-alarm").performScrollTo().performClick()
        compose.onNodeWithText("Alarm sound").assertExists()
        compose.onNodeWithText("Tell someone else").performScrollTo().performClick()
        compose.onNodeWithText("Check token").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Alarm sound").assertExists()

        // Uyarılar sekmesine yeniden tıklamak başa döner.
        graph.appState.tab.value = AppState.Tab.ALERTS
        graph.appState.select(AppState.Tab.ALERTS)
        compose.waitForIdle()
        compose.onNodeWithText("Alarm sound").assertDoesNotExist()
        compose.onNodeWithText("Device alerts").assertExists()
    }

    @Test
    fun quietHoursPageOpensFromTheList() {
        show()

        compose.onNodeWithTag("alert-settings-quiet").performScrollTo().performClick()
        compose.onNodeWithText(graph.strings()["quiet_hours_desc"]).assertExists()
    }
}
