package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AutomationsScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
        graph.store.automations = emptyList() // demo kendi örnek otomasyonlarını ekler; boş listeden başla
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show() = compose.setContent { AutomationsScreen(graph, graph.strings()) }

    @Test
    fun emptyListShowsExamples() {
        show()

        compose.onNodeWithText("No alerts yet").assertExists()
    }

    @Test
    fun addingAnAutomationSavesIt() {
        show()

        compose.onNodeWithTag("add-automation").performClick()
        compose.onNodeWithText("Name").performTextInput("Pump turned off")
        compose.onNodeWithTag("pick-event").performScrollTo().performClick()
        compose.onNodeWithText("Turns Off").performClick() // olay listesi: her kelime büyük harfle
        compose.onNodeWithTag("pick-action").performScrollTo().performClick()
        compose.onNodeWithText("Ring alarm").performClick()
        compose.onNodeWithText("Save").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.size == 1 }
        val a = graph.store.automations.single()
        assertEquals("Pump turned off", a.name)
        assertEquals(ActionKind.ALARM, a.action.kind)
        assertEquals(TriggerKind.TURNED_OFF, a.trigger.kind)
        assertEquals(true, a.enabled)
    }

    @Test
    fun theSwitchOnTheCardTurnsAnAutomationOff() {
        val a = sample()
        graph.store.automations = listOf(a)
        show()

        compose.onNodeWithTag("enabled-${a.id}").performClick()

        compose.waitUntil(5_000) { graph.store.automations.single().enabled == false }
    }

    @Test
    fun deleteAsksFirst() {
        val a = sample()
        graph.store.automations = listOf(a)
        show()

        compose.onNodeWithText(a.name).performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete this alert?").assertExists()
        compose.onNodeWithTag("confirm-delete").performClick()

        compose.waitUntil(5_000) { graph.store.automations.isEmpty() }
    }

    private fun sample(): Automation {
        val device = graph.store.snapshots.first()
        return Automation(
            id = "a1", name = "Long on", enabled = true,
            trigger = Trigger(TriggerKind.STAYED_ON, device.id, channel = 0, minutes = 30),
            action = AutoAction(ActionKind.NOTIFY),
        )
    }
}
