package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.AlarmScreen
import com.macerce.switchguard.desktop.ui.Snapshots
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/**
 * Uyarılar listesi, düzenleyici ve alarm ekranının görsel örnekleri (açık + koyu, Türkçe, demo cihazları);
 * `build/snapshots/alerts-*.png`. Karşılaştırma yapmaz, gözle bakmak içindir.
 */
class AutomationsSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "tr"
        graph.store.enableDemo(tr.demoNames())
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun forBoth(name: String, width: Int = 1100, height: Int = 760, content: @Composable () -> Unit) {
        for (dark in listOf(false, true)) {
            val out = Snapshots.render("alerts-$name-" + if (dark) "dark" else "light", width, height, dark = dark, content = content)
            assertTrue(Files.size(out) > 1000)
        }
    }

    @Test
    fun list() {
        // Demo'nun iki örneğine ek olarak cihaz açan (gecikmeli) ve kapalı duran birer uyarı.
        graph.store.automations = graph.store.automations + listOf(
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
        forBoth("list") { AutomationsScreen(graph, tr) }
    }

    @Test
    fun noAlerts() {
        graph.store.automations = emptyList()
        forBoth("empty") { AutomationsScreen(graph, tr) }
    }

    @Test
    fun newEditor() {
        forBoth("editor-new", height = 1500) { editor(null) }
    }

    @Test
    fun editingChannelPowerAlarm() {
        // Demo: 4 kanallı pompa panelinde kanal gücü düşünce alarm (kanal düğmeleri, iki adımlayıcı, şu anki güç).
        val pump = graph.store.automations.first { it.id == "demo-auto-pump" }
        forBoth("editor-pump", height = 1500) { editor(pump) }
        forBoth("editor-pump-top") { editor(pump) }
    }

    @Test
    fun editingTurnOnWithDelay() {
        val a = Automation(
            id = "x-light", name = "Dondurucu ısınınca ışığı yak", enabled = true,
            trigger = Trigger(TriggerKind.POWER_ABOVE, Demo.FREEZER, watts = 1500.0),
            action = AutoAction(ActionKind.TURN_ON, Demo.PUMPS, 2), delayMinutes = 5,
        )
        forBoth("editor-turn-on", height = 1500) { editor(a) }
    }

    @Test
    fun alarm() {
        val lines = listOf(
            "21:58  Pompa paneli · Kanal 1 kapandı",
            "22:03  Dondurucu çevrimdışı",
            "22:05  " + tr["test_alarm_line_silent"],
            "22:07  Uyarı “Pompa boşa dönüyor”: Pompa paneli · Kanal 1 açıkken gücü 3 W",
        )
        forBoth("alarm") { AlarmScreen(lines, tr, onDismiss = {}) }
        // Çok satır: liste kayar, başlık ve düğme yerinde kalır.
        val many = (1..30).map { "22:%02d  Pompa paneli · Kanal ${it % 4 + 1} değişti".format(it) }
        Snapshots.render("alerts-alarm-many", dark = true) { AlarmScreen(many, tr, onDismiss = {}) }
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
    )
}
