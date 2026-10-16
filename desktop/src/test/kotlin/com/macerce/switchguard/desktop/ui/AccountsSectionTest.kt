package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.SettingsPage
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
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Disconnect").performScrollTo().performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()

        assertEquals("", graph.store.tuyaAccessId)
        assertEquals("", graph.store.tuyaSecret)
    }

    @Test
    fun cancellingTheDisconnectKeepsTuya() {
        tuyaConnected()
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Disconnect").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()

        assertEquals("id", graph.store.tuyaAccessId)
    }

    @Test
    fun tuyaLiveAsksBeforeTurningOn() {
        tuyaConnected()
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.MONITORING) }

        compose.onNodeWithText("Tuya live updates on this computer").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(false, graph.store.tuyaLive) // önce uyarı

        compose.onNodeWithText("Turn on").performClick()
        compose.waitForIdle()
        assertEquals(true, graph.store.tuyaLive)
    }

    @Test
    fun withoutTuyaThereIsAConnectButton() {
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Connect").performScrollTo()
        compose.onNodeWithText("Disconnect").assertDoesNotExist()
    }

    @Test
    fun apiCredentialsAreSaved() {
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("API credentials").performScrollTo().performClick()
        compose.onNodeWithText("App ID").performTextInput("my-app")
        compose.onNodeWithText("App Secret").performTextInput("my-secret")
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()

        assertEquals("my-app", graph.store.appId)
        assertEquals("my-secret", graph.store.appSecret)
        assertEquals("https://127.0.0.1", graph.store.redirectUrl)
    }

    @Test
    fun removingTheEwelinkAccountAfterConfirmDeletesKeysAndSession() {
        graph.store.appId = "app"
        graph.store.appSecret = "secret"
        graph.store.accessToken = "token"
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Remove account").performScrollTo().performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()

        assertEquals("", graph.store.appId)
        assertEquals("", graph.store.accessToken)
    }

    @Test
    fun signedInAccountShowsTheMaskedEmail() {
        graph.store.appId = "app"
        graph.store.appSecret = "secret"
        graph.store.accessToken = "token"
        graph.store.accountEmail = "macerce@gmail.com"
        graph.store.region = "eu"
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("ma***@gmail.com").performScrollTo()
        compose.onNodeWithText("Region: EU").assertExists()
        compose.onNodeWithText("Sign in again").assertExists()
        compose.onNodeWithText("App ID: app…").assertExists()
    }

    @Test
    fun withoutKeysTheAccountRowAsksForThem() {
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Not signed in").assertExists()
        compose.onNodeWithText("Not set").assertExists()
        compose.onNodeWithText("Sign in with eWeLink").assertDoesNotExist()
        compose.onNodeWithText("Remove account").assertDoesNotExist()
    }

    @Test
    fun withKeysButNoSessionThereIsASignInButton() {
        graph.store.appId = "app"
        graph.store.appSecret = "secret"
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ACCOUNTS) }

        compose.onNodeWithText("Sign in with eWeLink").assertExists()
        compose.onNodeWithText("Sign out").assertDoesNotExist()
    }
}
