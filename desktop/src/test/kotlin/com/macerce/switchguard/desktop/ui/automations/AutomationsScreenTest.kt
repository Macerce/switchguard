package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.SwitchGuardTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Otomasyon sekmesi: yalnızca cihaz açan/kapatan kayıtlar; düzenleyici otomasyon modunda. */
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

    private fun show() = compose.setContent {
        SwitchGuardTheme("light") {
            CompositionLocalProvider(LocalStrings provides graph.strings()) { AutomationsScreen(graph, graph.strings()) }
        }
    }

    @Test
    fun emptyListShowsExamples() {
        show()

        compose.onNodeWithText("No automations yet").assertExists()
        compose.onNodeWithText(graph.strings()["automations_empty_desc"]).assertExists()
    }

    @Test
    fun alertsAreNotListedHere() {
        // Alarm/bildirim veren kayıt Uyarılar sekmesinde; burada yalnızca cihazı açan/kapatan.
        graph.store.automations = listOf(sample(), sample().copy(id = "n1", name = "Ring me", action = AutoAction(ActionKind.ALARM)))
        show()

        compose.onNodeWithText("Long on").assertExists()
        compose.onNodeWithText("Ring me").assertDoesNotExist()
    }

    @Test
    fun addingAnAutomationSavesIt() {
        show()

        compose.onNodeWithTag("add-automation").performClick()
        compose.onNodeWithText("New automation").assertExists()
        compose.onNodeWithTag("save-automation").performScrollTo().assertIsNotEnabled() // ad yokken kaydedilemez
        // Otomasyon modunda yalnızca cihazı aç / kapat sunulur.
        compose.onNodeWithTag("action-ALARM").assertDoesNotExist()
        compose.onNodeWithTag("action-NOTIFY").assertDoesNotExist()
        compose.onNodeWithTag("automation-name").performTextInput("Pump off at night")
        compose.onNodeWithTag("event-TURNED_OFF").performScrollTo().performClick()
        compose.onNodeWithTag("action-TURN_OFF").performScrollTo().performClick()
        compose.onNodeWithTag("save-automation").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.size == 1 }
        val a = graph.store.automations.single()
        assertEquals("Pump off at night", a.name)
        assertEquals(ActionKind.TURN_OFF, a.action.kind)
        assertEquals(graph.store.snapshots.first().id, a.action.deviceId)
        assertEquals(TriggerKind.TURNED_OFF, a.trigger.kind)
        assertEquals(true, a.enabled)
    }

    @Test
    fun stepperMovesToTheNextMeaningfulValue() {
        show()

        compose.onNodeWithTag("add-automation").performClick()
        compose.onNodeWithTag("automation-name").performTextInput("Long on")
        compose.onNodeWithTag("event-STAYED_ON").performScrollTo().performClick()
        compose.onNodeWithTag("minutes-value").assertTextEquals("30 minutes")
        compose.onNodeWithTag("minutes-plus").performScrollTo().performClick()
        compose.onNodeWithTag("minutes-value").assertTextEquals("45 minutes")
        compose.onNodeWithTag("delay-value").assertTextEquals("At once") // bekleme 0 = hemen
        compose.onNodeWithTag("delay-minus").assertIsNotEnabled()
        compose.onNodeWithTag("delay-plus").performScrollTo().performClick()
        compose.onNodeWithTag("save-automation").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.size == 1 }
        val a = graph.store.automations.single()
        assertEquals(45, a.trigger.minutes)
        assertEquals(1, a.delayMinutes)
    }

    @Test
    fun turningADeviceOnPicksTheTargetDeviceAndChannel() {
        show()

        compose.onNodeWithTag("add-automation").performClick()
        compose.onNodeWithTag("automation-name").performTextInput("Light on")
        compose.onNodeWithTag("action-TURN_ON").performScrollTo().performClick()
        compose.onNodeWithTag("action-device-${Demo.PUMPS}").performScrollTo().performClick()
        compose.onNodeWithTag("action-channel-2").performScrollTo().performClick() // numaralı düğme "3"
        compose.onNodeWithTag("automation-summary").performScrollTo()
        compose.onNodeWithText("Channel 3: Turn on", substring = true).assertExists() // özetteki "O zaman" satırı
        compose.onNodeWithTag("save-automation").performScrollTo().performClick()

        compose.waitUntil(5_000) { graph.store.automations.size == 1 }
        val action = graph.store.automations.single().action
        assertEquals(ActionKind.TURN_ON, action.kind)
        assertEquals(Demo.PUMPS, action.deviceId)
        assertEquals(2, action.channel)
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
        compose.onNodeWithTag("delete-automation").performClick()
        compose.onNodeWithText(graph.strings()["automation_delete_title"]).assertExists()
        compose.onNodeWithTag("confirm-delete").performClick()

        compose.waitUntil(5_000) { graph.store.automations.isEmpty() }
    }

    private fun sample(): Automation {
        val device = graph.store.snapshots.first()
        return Automation(
            id = "a1", name = "Long on", enabled = true,
            trigger = Trigger(TriggerKind.STAYED_ON, device.id, channel = 0, minutes = 30),
            action = AutoAction(ActionKind.TURN_OFF, device.id),
        )
    }
}
