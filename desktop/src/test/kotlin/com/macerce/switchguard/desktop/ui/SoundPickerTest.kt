package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.sound.ImportResult
import com.macerce.switchguard.desktop.sound.SoundImport
import com.macerce.switchguard.desktop.ui.settings.SettingsPage
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class SoundPickerTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.newFolder("data").toPath())
        graph.store.language = "en"
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun pickingABuiltInSoundSavesIt() {
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ALARM) }

        compose.onNodeWithText("Alarm sound").performScrollTo().performClick()
        compose.onNodeWithText("Beep").performClick()
        compose.waitForIdle()

        assertEquals("builtin:alarm_beep", graph.store.alarmSoundUri)
    }

    @Test
    fun notificationSoundIsSavedSeparately() {
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ALARM) }

        compose.onNodeWithText("Notification sound").performScrollTo().performClick()
        compose.onNodeWithText("Pulse").performClick()
        compose.waitForIdle()

        assertEquals("builtin:alarm_pulse", graph.store.notifySoundUri)
        assertEquals(null, graph.store.alarmSoundUri)
    }

    @Test
    fun importedSoundsAreListed() {
        val wav = tmp.root.toPath().resolve("kapı zili.wav")
        AudioSystem.write(
            AudioInputStream(ByteArrayInputStream(ByteArray(8_000 * 2) { (it % 50).toByte() }), AudioFormat(8_000f, 16, 1, true, false), 8_000),
            AudioFileFormat.Type.WAVE,
            wav.toFile(),
        )
        val imported = SoundImport(graph.soundsDir).import(wav)
        assertTrue(imported.toString(), imported is ImportResult.Ok)
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ALARM) }

        compose.onNodeWithText("Alarm sound").performScrollTo().performClick()
        compose.onNodeWithText("kapı zili").assertIsDisplayed().performClick()
        compose.waitForIdle()

        assertEquals((imported as ImportResult.Ok).id, graph.store.alarmSoundUri)
    }
}
