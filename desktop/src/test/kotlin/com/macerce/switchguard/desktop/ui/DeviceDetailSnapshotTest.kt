package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.devices.DeviceActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.test.ExperimentalTestApi
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/**
 * Cihaz sayfasının görsel örnekleri (açık + koyu, TR): demo dondurucu (enerji, 7 gün), pompa paneli (kanal düğmeleri)
 * ve zamanlayıcılı bir eWeLink prizi (sahte bulut). `build/snapshots/detail-*.png`.
 */
@OptIn(ExperimentalTestApi::class)
class DeviceDetailSnapshotTest {
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

    /** Son 7 gün boyunca saatte bir güç ölçümü: enerji çubukları dolsun. */
    private fun recordWeek(device: DeviceSnapshot) {
        val now = System.currentTimeMillis()
        var previous = emptyMap<String, DeviceSnapshot>()
        var t = now - 7 * 24 * 3_600_000L
        var i = 0
        while (t <= now) {
            val watts = if (i % 24 in 8..20) 90.0 + (i % 7) * 15 else 4.0
            val d = device.copy(power = watts)
            graph.metrics.record(previous, listOf(d), t)
            previous = mapOf(d.id to d)
            t += 3_600_000L
            i++
        }
    }

    private fun events(id: String, name: String) {
        val now = System.currentTimeMillis()
        val log = graph.eventLog
        log.add(id, name, EventType.TURNED_OFF.name, "$name: AÇIK → KAPALI", "ALARM", time = now - 30 * 60_000L)
        log.add(id, name, EventKind.USER, "$name: KAPALI → AÇIK", "", time = now - 25 * 60_000L)
        log.add(id, name, EventType.WENT_OFFLINE.name, "$name: ÇEVRİMİÇİ → ÇEVRİMDIŞI", "NOTIFY", time = now - 20 * 60_000L)
    }

    private fun render(name: String, id: String, actions: DeviceActions = DeviceActions(graph.store), height: Int = 760) {
        for (dark in listOf(false, true)) {
            val out = SettledSnapshots.render("$name-" + if (dark) "dark" else "light", 1100, height, dark) {
                DeviceDetailScreen(graph, id, onBack = {}, actions = actions)
            }
            assertTrue(Files.size(out) > 1000)
        }
    }

    @Test
    fun freezerWithEnergy() {
        val freezer = graph.store.snapshots.first { it.id == Demo.FREEZER }
        recordWeek(freezer)
        events(freezer.id, freezer.name)
        render("detail-freezer", Demo.FREEZER)
        render("detail-freezer-tall", Demo.FREEZER, height = 2400)
    }

    @Test
    fun pumpPanel() {
        render("detail-pumps", Demo.PUMPS, height = 1100)
    }

    @Test
    fun ewelinkPlugWithTimers() {
        val plug = DeviceSnapshot("1000abc", "Akıllı priz", online = true, switches = mapOf(0 to false), uiid = 1, cloud = Cloud.EWELINK)
        graph.store.snapshots = graph.store.snapshots + plug
        graph.engine.updateRules(plug.id) { it.copy(critical = true, soundUri = "builtin:alarm_beep") }
        val timers = JSONArray()
            .put(JSONObject("""{"mId":"a","type":"repeat","coolkit_timer_type":"repeat","enabled":1,"at":"30 4 * * 1,2,3,4,5","do":{"switch":"on"}}"""))
            .put(JSONObject("""{"mId":"b","type":"repeat","coolkit_timer_type":"repeat","enabled":0,"at":"0 20 * * 0,6","do":{"switch":"off"}}"""))
            .put(JSONObject("""{"mId":"c","type":"once","at":"2026-10-20T10:00:00.000Z","do":{"switch":"off"}}"""))
        val fake = object : DeviceActions.Ewelink {
            override fun deviceParams(deviceId: String) = JSONObject().put("switch", "off").put("timers", timers)
            override fun setTimers(deviceId: String, timers: JSONArray) = Unit
            override fun requestLiveEnergy(device: DeviceSnapshot) = Unit
        }
        Thread.sleep(300) // motor kuralı yazsın
        val actions = DeviceActions(graph.store, ewelink = { fake })
        render("detail-ewelink", plug.id, actions, height = 1500)
        for (dark in listOf(false, true)) {
            InteractiveSnapshots.render("detail-timer-dialog-" + if (dark) "dark" else "light", 1100, 760, dark, content = {
                DeviceDetailScreen(graph, plug.id, onBack = {}, actions = actions)
            }) {
                waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText(tr["days_weekdays"])).fetchSemanticsNodes().isNotEmpty() }
                onNodeWithText("07:30 · " + tr["auto_action_turn_on"]).performClick()
            }
        }
    }
}
