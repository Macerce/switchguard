package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccountsSectionTest {
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

    private fun tuyaConnected() {
        graph.store.tuyaAccessId = "id"
        graph.store.tuyaSecret = "secret"
    }

    @Test
    fun disconnectAfterConfirmClearsTuya() {
        tuyaConnected()
        compose.setContent { SettingsScreen(graph, graph.strings()) }

        compose.onNodeWithText("Disconnect").performScrollTo().performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()

        assertEquals("", graph.store.tuyaAccessId)
        assertEquals("", graph.store.tuyaSecret)
    }

    @Test
    fun cancellingTheDisconnectKeepsTuya() {
        tuyaConnected()
        compose.setContent { SettingsScreen(graph, graph.strings()) }

        compose.onNodeWithText("Disconnect").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()

        assertEquals("id", graph.store.tuyaAccessId)
    }

    @Test
    fun tuyaLiveAsksBeforeTurningOn() {
        tuyaConnected()
        compose.setContent { SettingsScreen(graph, graph.strings()) }

        compose.onNodeWithText("Tuya live updates on this computer").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(false, graph.store.tuyaLive) // önce uyarı

        compose.onNodeWithText("Turn on").performClick()
        compose.waitForIdle()
        assertEquals(true, graph.store.tuyaLive)
    }

    @Test
    fun withoutTuyaThereIsAConnectButtonAndNoLiveSwitch() {
        compose.setContent { SettingsScreen(graph, graph.strings()) }

        compose.onNodeWithText("Connect").performScrollTo()
        compose.onNodeWithText("Tuya live updates on this computer").assertDoesNotExist()
    }
}
