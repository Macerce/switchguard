package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.BackupSection
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class BackupSectionTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private lateinit var file: Path

    @Before
    fun setUp() {
        graph = AppGraph(tmp.newFolder("data").toPath())
        graph.store.language = "en"
        file = tmp.root.toPath().resolve("Masaüstü yedek.sgbackup")
        compose.setContent {
            Column { BackupSection(graph, graph.strings(), chooseTarget = { file }, chooseSource = { file }) }
        }
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun waitUntilText(text: String) =
        compose.waitUntil(20_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun backUp(password: String = "parola123") {
        compose.onNodeWithText("Back up to a file…").performClick()
        compose.onNodeWithText("Password").performTextInput(password)
        compose.onNodeWithText("Password again").performTextInput(password)
        compose.onNodeWithText("OK").performClick()
        waitUntilText("Backup saved")
        compose.onNodeWithText("OK").performClick()
    }

    @Test
    fun backupThenRestoreBringsTheSettingsBack() {
        graph.store.tuyaAccessId = "tid"
        graph.store.tuyaSecret = "tsecret"
        graph.store.pollIntervalSec = 120
        backUp()
        assertTrue(Files.size(file) > 0)

        graph.store.pollIntervalSec = 15
        graph.store.disconnectTuya()

        compose.onNodeWithText("Restore from a file…").performClick()
        compose.onNodeWithText("Password").performTextInput("parola123")
        compose.onNodeWithText("OK").performClick()
        waitUntilText("All settings on this computer will be replaced")
        compose.onNodeWithText("Restore").performClick()
        waitUntilText("Settings restored.")

        assertEquals(120, graph.store.pollIntervalSec)
        assertEquals("tsecret", graph.store.tuyaSecret)
    }

    @Test
    fun shortPasswordIsRefused() {
        compose.onNodeWithText("Back up to a file…").performClick()
        compose.onNodeWithText("Password").performTextInput("kisa")
        compose.onNodeWithText("Password again").performTextInput("kisa")
        compose.onNodeWithText("OK").performClick()

        compose.onNodeWithText("Use at least 8 characters.").assertExists()
        assertTrue(!Files.exists(file))
    }

    @Test
    fun mismatchedPasswordsAreRefused() {
        compose.onNodeWithText("Back up to a file…").performClick()
        compose.onNodeWithText("Password").performTextInput("parola123")
        compose.onNodeWithText("Password again").performTextInput("parola124")
        compose.onNodeWithText("OK").performClick()

        compose.onNodeWithText("The passwords do not match.").assertExists()
    }

    @Test
    fun wrongPasswordOnRestoreIsExplained() {
        backUp()

        compose.onNodeWithText("Restore from a file…").performClick()
        compose.onNodeWithText("Password").performTextInput("yanlis-parola")
        compose.onNodeWithText("OK").performClick()

        waitUntilText("Wrong password or damaged file.")
    }
}
