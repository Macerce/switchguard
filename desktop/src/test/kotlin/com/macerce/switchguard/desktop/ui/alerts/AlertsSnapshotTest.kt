package com.macerce.switchguard.desktop.ui.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.AppNav
import com.macerce.switchguard.desktop.ui.Snapshots
import com.macerce.switchguard.desktop.ui.automations.AutomationEditor
import com.macerce.switchguard.desktop.ui.automations.AutomationMode
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/**
 * Uyarılar ve Otomasyon sekmelerinin görsel örnekleri (açık + koyu, Türkçe, demo cihazları, 1100×760 pencere ve uzun
 * sayfaların tamamı); `build/snapshots/tabs-*.png`. Karşılaştırma yapmaz, gözle bakmak içindir.
 */
class AlertsSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.apply {
            language = "tr"
            enableDemo(tr.demoNames())
            // Satırlar farklı görünsün: pompa paneli kritik ve kendi sesiyle, ışık yalnızca bildirim, dondurucu izlenmiyor.
            setRules(Demo.PUMPS, rulesFor(Demo.PUMPS).copy(critical = true, soundUri = "builtin:alarm_beep"))
            setRules(
                Demo.LIGHT,
                rulesFor(Demo.LIGHT).with(EventType.TURNED_ON, AlertAction.NOTIFY).with(EventType.TURNED_OFF, AlertAction.IGNORE)
                    .with(EventType.WENT_OFFLINE, AlertAction.IGNORE).with(EventType.CAME_ONLINE, AlertAction.IGNORE),
            )
            setRules(Demo.FREEZER, rulesFor(Demo.FREEZER).copy(monitored = false))
            quietHours = QuietHours(true, 23 * 60, 7 * 60)
            // Otomasyon sekmesinde de kayıt olsun: dondurucu ısınınca ışığı yak, pompa 2 uzun açık kalınca kapat.
            automations = automations + listOf(
                Automation(
                    id = "x-light", name = "Dondurucu ısınınca ışığı yak", enabled = true,
                    trigger = Trigger(TriggerKind.POWER_ABOVE, Demo.FREEZER, watts = 1500.0),
                    action = AutoAction(ActionKind.TURN_ON, Demo.LIGHT), delayMinutes = 5,
                ),
                Automation(
                    id = "x-off", name = "Pompa 2 uzun açık", enabled = false,
                    trigger = Trigger(TriggerKind.STAYED_ON, Demo.PUMPS, channel = 1, minutes = 45),
                    action = AutoAction(ActionKind.TURN_OFF, Demo.PUMPS, 1),
                ),
            )
        }
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun forBoth(name: String, width: Int = 1100, height: Int = 760, content: @Composable () -> Unit) {
        for (dark in listOf(false, true)) {
            val out = Snapshots.render("tabs-$name-" + if (dark) "dark" else "light", width, height, dark = dark, content = content)
            assertTrue(Files.size(out) > 1000)
        }
    }

    /** Sol menü (5 sekme) ve Uyarılar sekmesi; izleme açık, Windows ayarları tam: "Alarm hazır". */
    @Test
    fun alertsTabInTheWindow() {
        graph.store.monitoringEnabled = true
        graph.appState.select(AppState.Tab.ALERTS)
        forBoth("alerts-window") { AppNav(graph, tr) }
        forBoth("alerts-full", height = 1750) { AppNav(graph, tr) }
    }

    /** Durum kartının diğer halleri: izleme kapalı (+ Windows uyarısı) ve izleme açık ama bilgisayar uyuyabilir. */
    @Test
    fun statusCardStates() {
        graph.store.autostart = false
        forBoth("alerts-off", height = 520) { AlertsScreen(graph, tr, testAlarm = { _, _ -> }) }
        graph.store.monitoringEnabled = true
        graph.store.autostart = true
        graph.store.alwaysAwake = false
        forBoth("alerts-warning", height = 520) { AlertsScreen(graph, tr, testAlarm = { _, _ -> }) }
        graph.store.automations = graph.store.automations.filter { it.action.kind.usesDevice }
        forBoth("alerts-no-conditional", height = 1700) { AlertsScreen(graph, tr, testAlarm = { _, _ -> }) }
    }

    @Test
    fun deviceRulesPage() {
        forBoth("alert-rules") { AlertsScreen(graph, tr, startPage = AlertsPage.Rules(Demo.PUMPS)) }
        forBoth("alert-rules-full", height = 1100) { AlertsScreen(graph, tr, startPage = AlertsPage.Rules(Demo.PUMPS)) }
        forBoth("alert-rules-not-monitored") { AlertsScreen(graph, tr, startPage = AlertsPage.Rules(Demo.FREEZER)) }
    }

    @Test
    fun conditionalAlertEditor() {
        forBoth("alert-editor-pump", height = 1500) { AlertsScreen(graph, tr, startPage = AlertsPage.Editor("demo-auto-pump")) }
        forBoth("alert-editor-new") { AlertsScreen(graph, tr, startPage = AlertsPage.Editor(null)) }
        forBoth("alert-editor-new-full", height = 1300) { AlertsScreen(graph, tr, startPage = AlertsPage.Editor(null)) }
    }

    @Test
    fun alertSettingsPages() {
        forBoth("alert-settings-alarm") { AlertsScreen(graph, tr, startPage = AlertsPage.Alarm, testAlarm = { _, _ -> }) }
        forBoth("alert-settings-quiet") { AlertsScreen(graph, tr, startPage = AlertsPage.Quiet) }
        forBoth("alert-settings-telegram", height = 1300) { AlertsScreen(graph, tr, startPage = AlertsPage.Telegram) }
    }

    @Test
    fun automationTab() {
        graph.store.monitoringEnabled = true
        graph.appState.select(AppState.Tab.AUTOMATIONS)
        forBoth("automations-window") { AppNav(graph, tr) }
        forBoth("automation-editor-new", height = 1300) { editor(null) }
        forBoth("automation-editor-turn-on", height = 1300) { editor(graph.store.automations.first { it.id == "x-light" }) }
        graph.store.automations = graph.store.automations.filterNot { it.action.kind.usesDevice }
        forBoth("automations-empty") { AppNav(graph, tr) }
    }

    @Test
    fun settingsTab() {
        graph.appState.select(AppState.Tab.SETTINGS)
        forBoth("settings-window") { AppNav(graph, tr) }
    }

    @Composable
    private fun editor(initial: Automation?) = AutomationEditor(
        initial = initial,
        devices = graph.store.snapshots,
        strings = tr,
        modifier = Modifier,
        onSave = {},
        onDelete = {},
        onBack = {},
        newMode = AutomationMode.AUTOMATION,
    )
}
