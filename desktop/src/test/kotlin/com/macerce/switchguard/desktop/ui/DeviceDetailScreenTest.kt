package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.core.DeviceCategory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.devices.DeviceActions
import com.macerce.switchguard.desktop.ui.devices.TimeStepper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Cihaz sayfasının davranışı: kurallar motordan geçer, düğmeler cihazı değiştirir, zamanlayıcılar buluta yazılır. */
class DeviceDetailScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private var back = 0

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show(id: String, actions: DeviceActions = DeviceActions(graph.store)) {
        compose.setContent {
            SwitchGuardTheme("light") {
                CompositionLocalProvider(LocalStrings provides graph.strings()) {
                    DeviceDetailScreen(graph, id, onBack = { back++ }, actions = actions)
                }
            }
        }
    }

    private fun rules(id: String) = graph.store.rulesFor(id)

    @Test
    fun ruleChoiceIsSavedThroughTheEngine() {
        show(Demo.FREEZER)
        compose.onNodeWithTag("rule-${EventType.TURNED_OFF.name}-${AlertAction.IGNORE.name}").performScrollTo().performClick()
        compose.waitUntil(5_000) { rules(Demo.FREEZER).actionFor(EventType.TURNED_OFF) == AlertAction.IGNORE }
        // Öteki olaylar değişmez.
        assertEquals(AlertAction.ALARM, rules(Demo.FREEZER).actionFor(EventType.TURNED_ON))
    }

    @Test
    fun turningMonitoringOffHidesTheRules() {
        show(Demo.FREEZER)
        compose.onNodeWithText("When this happens").assertExists()
        compose.onNodeWithTag("monitor-switch").performScrollTo().performClick()
        compose.waitUntil(5_000) { !rules(Demo.FREEZER).monitored }
        compose.onNodeWithText("When this happens").assertDoesNotExist()
        compose.onNodeWithText("Not monitored").assertExists()
    }

    @Test
    fun categoryIsSaved() {
        show(Demo.LIGHT)
        compose.onNodeWithTag("category-IMPORTANT").performScrollTo().performClick()
        compose.waitUntil(5_000) { rules(Demo.LIGHT).category == DeviceCategory.IMPORTANT }
    }

    @Test
    fun bigPowerButtonSwitchesTheDevice() {
        show(Demo.LIGHT)
        compose.onNodeWithTag("hero-power").performClick()
        compose.waitUntil(5_000) { graph.store.snapshots.first { it.id == Demo.LIGHT }.switches[0] == true }
    }

    @Test
    fun demoSimulationGoesThroughTheEngine() {
        // Simülasyon izlemeyi başlatır: test sırasında alarm çalmasın diye bu cihazın olayları yalnızca kaydedilir.
        val quiet = EventType.entries.fold(DeviceRules()) { r, t -> r.with(t, AlertAction.IGNORE) }
        graph.store.setRules(Demo.LIGHT, quiet)
        show(Demo.LIGHT)
        compose.onNodeWithText("Lose connection").performScrollTo().performClick()
        compose.waitUntil(5_000) { graph.store.snapshots.first { it.id == Demo.LIGHT }.online.not() }
        graph.engine.stop()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun backArrowAndEscapeGoBack() {
        show(Demo.LIGHT)
        compose.onNode(hasContentDescription("Back")).performClick()
        assertEquals(1, back)
        compose.onRoot().performKeyInput { pressKey(Key.Escape) }
        compose.waitForIdle()
        assertEquals(2, back)
    }

    @Test
    fun missingDeviceShowsAMessage() {
        show("nope")
        compose.onNodeWithText(graph.strings()["device_missing"]).assertExists()
    }

    @Test
    fun ewelinkTimersAreListedAndANewOneIsWrittenToTheCloud() {
        val plug = DeviceSnapshot("1000abc", "Plug", online = true, switches = mapOf(0 to false), cloud = Cloud.EWELINK)
        graph.store.snapshots = graph.store.snapshots + plug
        var cloud = JSONArray().put(JSONObject("""{"mId":"a","type":"repeat","enabled":1,"at":"30 4 * * 1,2,3,4,5","do":{"switch":"on"}}"""))
        val fake = object : DeviceActions.Ewelink {
            override fun deviceParams(deviceId: String) = JSONObject().put("timers", cloud)
            override fun setTimers(deviceId: String, timers: JSONArray) { cloud = timers }
            override fun requestLiveEnergy(device: DeviceSnapshot) = Unit
        }
        show(plug.id, DeviceActions(graph.store, ewelink = { fake }))
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Weekdays")).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithTag("timer-add").performScrollTo().performClick()
        // 07:00 → 08:00 (saat oku), sonra kaydet.
        compose.onNodeWithTag("hour-up").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { cloud.length() == 2 }
        assertTrue(cloud.toString().contains("\"type\":\"repeat\""))
        assertFalse(cloud.getJSONObject(1).optString("mId").isEmpty())
    }

    @Test
    fun timeStepperWrapsAndAcceptsTyping() {
        var minute = 23 * 60 + 58
        compose.setContent {
            SwitchGuardTheme("light") {
                CompositionLocalProvider(LocalStrings provides graph.strings()) {
                    val state = remember { mutableIntStateOf(minute) }
                    TimeStepper(state.intValue) { state.intValue = it; minute = it }
                }
            }
        }
        compose.onNodeWithTag("hour-up").performClick()
        compose.waitForIdle()
        assertEquals(58, minute) // 23 → 0
        compose.onNodeWithTag("minute-down").performClick()
        compose.waitForIdle()
        assertEquals(57, minute)
        compose.onNodeWithTag("hour-field").performTextReplacement("7")
        compose.waitForIdle()
        assertEquals(7 * 60 + 57, minute)
    }
}
