package com.macerce.switchguard.desktop.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Cihazlar ekranının davranışı (motor başlatılmaz; anahtar komutları izleme kapalıyken ayara doğrudan yazılır). */
class DevicesScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show(demo: Boolean = true) {
        if (demo) graph.store.enableDemo(graph.strings().demoNames())
        compose.setContent {
            SwitchGuardTheme("light") {
                CompositionLocalProvider(LocalStrings provides graph.strings()) { DevicesScreen(graph, graph.strings()) }
            }
        }
    }

    private fun switchOf(id: String, channel: Int = 0) = graph.store.snapshots.first { it.id == id }.switches[channel]

    @Test
    fun clickingATileOpensTheDevicePage() {
        show()
        compose.onNodeWithText(graph.strings().demoNames().freezer).performClick()
        assertEquals(Demo.FREEZER, graph.appState.openDevice.value)
    }

    @Test
    fun tilePowerButtonSwitchesTheDeviceThroughTheEngine() {
        show()
        assertEquals(false, switchOf(Demo.LIGHT))
        compose.onNode(hasContentDescription("Power") and hasAnyAncestor(hasTestTag("tile-${Demo.LIGHT}"))).performClick()
        compose.waitUntil(5_000) { switchOf(Demo.LIGHT) == true }
    }

    @Test
    fun channelButtonsSwitchOneChannel() {
        show()
        assertEquals(false, switchOf(Demo.PUMPS, 1))
        compose.onNodeWithTag("channel-${Demo.PUMPS}-1").performClick()
        compose.waitUntil(5_000) { switchOf(Demo.PUMPS, 1) == true }
        assertEquals(true, switchOf(Demo.PUMPS, 0))
    }

    @Test
    fun editModeSetsPriorityAndCardSize() {
        show()
        compose.onNodeWithTag("edit-layout").performClick()
        compose.onNodeWithText("Arrange devices").assertExists()
        // Düzenlemede koruma kartı gizlenir.
        compose.onNodeWithTag("guard-card").assertDoesNotExist()

        compose.onNodeWithTag("priority-${Demo.LIGHT}").performClick()
        compose.waitUntil(5_000) { Demo.LIGHT in graph.store.deviceLayout.priority }

        compose.onNodeWithTag("devices-grid").performScrollToNode(hasTestTag("size-${Demo.FREEZER}"))
        compose.onNodeWithTag("size-${Demo.FREEZER}").performClick()
        compose.waitUntil(5_000) { Demo.FREEZER in graph.store.deviceLayout.large }

        compose.onNodeWithTag("edit-done").performClick()
        compose.onNodeWithTag("guard-card").assertExists()
    }

    @Test
    fun newCategoryFromTheEditBarAssignsTheDevice() {
        show()
        compose.onNodeWithTag("edit-layout").performClick()
        compose.onNodeWithTag("category-${Demo.LIGHT}").performClick()
        compose.onNodeWithText("New category").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Garden")
        compose.onNodeWithText("Save").performClick()

        compose.waitUntil(5_000) { graph.store.deviceLayout.categories.any { it.name == "Garden" } }
        val layout = graph.store.deviceLayout
        assertEquals(layout.categories.first { it.name == "Garden" }.id, layout.categoryOf[Demo.LIGHT])
        // Kategori başlığı ve kartın kategori düğmesi.
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Garden")).fetchSemanticsNodes().size == 2 }
    }

    @Test
    fun withoutAnAccountTheCardLeadsToSettingsOrTheDemo() {
        show(demo = false)
        compose.onNodeWithTag("guard-card").assertDoesNotExist()
        compose.onNodeWithTag("add-account").performClick()
        assertEquals(AppState.Tab.SETTINGS, graph.appState.tab.value)

        compose.onNodeWithTag("try-demo").performClick()
        compose.waitUntil(5_000) { graph.store.demoMode && graph.store.snapshots.any { Demo.isDemo(it.id) } }
        compose.onNodeWithTag("guard-card").assertExists()
    }

    @Test
    fun sessionAndBlindWarningsAreShown() {
        show()
        graph.appState.setWarning("Signed in elsewhere")
        graph.appState.setBlind("Monitoring is blind")
        compose.onNodeWithTag("session-card").assertExists()
        compose.onNodeWithTag("blind-card").assertExists()
        compose.onNodeWithText("Signed in elsewhere").assertExists()

        compose.onNodeWithText("Sign in again").performClick()
        assertEquals(AppState.Tab.SETTINGS, graph.appState.tab.value)

        graph.appState.setWarning(null)
        graph.appState.setBlind(null)
        compose.waitForIdle()
        compose.onNodeWithTag("session-card").assertDoesNotExist()
        compose.onNodeWithTag("blind-card").assertDoesNotExist()
    }

    @Test
    fun guardCardFollowsTheMonitoringSetting() {
        show()
        compose.onNodeWithTag("monitoring-start").assertExists()
        compose.onNodeWithTag("monitoring-stop").assertDoesNotExist()
        compose.onNodeWithText("Devices").assertExists()

        // Motor başlatılmadan yalnızca ayar: kart "durdur" düğmesine geçer.
        graph.store.monitoringEnabled = true
        compose.waitForIdle()
        compose.onNodeWithTag("monitoring-stop").assertExists()
        compose.onNodeWithTag("monitoring-start").assertDoesNotExist()
    }
}
