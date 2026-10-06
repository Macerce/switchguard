package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        compose.setContent { SettingsScreen(graph, graph.strings()) }
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun autostartSwitchWritesTheSetting() {
        // Test JVM'i kurulu uygulama değil (jpackage.app-path yok): kayıt defterine dokunulmaz, yalnızca ayar yazılır.
        compose.onNodeWithText("Start with Windows").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(false, graph.store.autostart)
    }

    @Test
    fun pollIntervalChoiceIsSaved() {
        compose.onNodeWithText("Backup check interval").performScrollTo().performClick()
        compose.onNodeWithText("Every 60 s").performClick()
        compose.waitForIdle()

        assertEquals(60, graph.store.pollIntervalSec)
    }

    @Test
    fun themeChoiceIsSaved() {
        compose.onNodeWithText("Theme").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()

        assertEquals("dark", graph.store.theme)
    }

    @Test
    fun volumeSliderStaysWithinLimits() {
        compose.onNodeWithTag("alarm-volume").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitForIdle()

        assertEquals(0.05f, graph.store.alarmVolume)
    }

    @Test
    fun quietHoursSwitchAndStartAreSaved() {
        compose.onNodeWithText("Quiet hours").performScrollTo().performClick()
        compose.onNodeWithText("Starts").performScrollTo().performClick()
        compose.onNodeWithText("22:00").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(true, graph.store.quietHours.enabled)
        assertEquals(22 * 60, graph.store.quietHours.startMinute)
    }
}
