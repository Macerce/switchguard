package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.AppGraph
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalTestApi::class)
class DeviceRulesUiTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private lateinit var firstId: String

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
        firstId = graph.store.deviceLayout.sorted(graph.store.snapshots).first().id
        compose.setContent { DevicesScreen(graph, graph.strings()) }
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun ruleChoiceInTheDialogIsSavedThroughTheEngine() {
        compose.onAllNodesWithContentDescription("Rules…")[0].performClick()
        compose.onNodeWithText("Rules…").performClick()
        compose.onNodeWithTag("rule-${EventType.TURNED_OFF.name}-${AlertAction.IGNORE.name}").performClick()

        compose.waitUntil(5_000) { graph.store.rulesFor(firstId).actionFor(EventType.TURNED_OFF) == AlertAction.IGNORE }
    }

    @Test
    fun monitoredChipTurnsMonitoringOffForThatDevice() {
        compose.onAllNodesWithText("Monitored")[0].performClick()

        compose.waitUntil(5_000) { !graph.store.rulesFor(firstId).monitored }
    }

    @Test
    fun demoSwitchGoesThroughTheEngine() {
        compose.onNodeWithTag("demo-switch").performClick()

        compose.waitUntil(5_000) { !graph.store.demoMode && graph.store.snapshots.none { Demo.isDemo(it.id) } }
    }
}
