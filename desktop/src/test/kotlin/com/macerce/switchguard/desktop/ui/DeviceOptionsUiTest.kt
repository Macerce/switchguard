package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.AppGraph
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceOptionsUiTest {
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

    private fun openOptions() {
        compose.onAllNodesWithContentDescription("Options…")[0].performClick()
        compose.onNodeWithText("Options…").performClick()
    }

    @Test
    fun ruleChoiceInOptionsIsSavedThroughTheEngine() {
        openOptions()
        compose.onNodeWithTag("rule-${EventType.TURNED_OFF.name}-${AlertAction.IGNORE.name}").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.rulesFor(firstId).actionFor(EventType.TURNED_OFF) == AlertAction.IGNORE }
    }

    @Test
    fun monitorSwitchInOptionsTurnsMonitoringOff() {
        openOptions()
        compose.onNodeWithText("Monitor this device").performClick()

        compose.waitUntil(5_000) { !graph.store.rulesFor(firstId).monitored }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Not monitored").assertExists()
    }

    @Test
    fun smallCardSwitchMakesThatCardSmall() {
        openOptions()
        compose.onNodeWithText("Small card").performScrollTo().performClick()

        compose.waitUntil(5_000) { firstId in graph.store.deviceLayout.compact }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag("card-small-$firstId").assertExists()
    }

    @Test
    fun generalCardSizeSmallMakesEveryCardSmall() {
        compose.onNodeWithTag("card-size").performClick()
        compose.onNodeWithText("Small").performClick()

        compose.waitUntil(5_000) { graph.store.cardSize == "small" }
        for (d in graph.store.snapshots) compose.onNodeWithTag("card-small-${d.id}").assertExists()
    }

    @Test
    fun demoSimulationLivesInOptionsNotOnTheCard() {
        compose.onNodeWithText("Switch from outside").assertDoesNotExist()

        openOptions()
        compose.onNodeWithText("Switch from outside").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.snapshots.first { it.id == firstId }.switches[0] == false }
    }

    @Test
    fun demoSwitchGoesThroughTheEngine() {
        compose.onNodeWithTag("demo-switch").performClick()

        compose.waitUntil(5_000) { !graph.store.demoMode && graph.store.snapshots.none { Demo.isDemo(it.id) } }
    }
}
