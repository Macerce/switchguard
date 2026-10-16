package com.macerce.switchguard.desktop.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.AppGraph
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

/** Geçmiş sekmesinin davranışı: süzgeç, temizleme ve CSV dışa aktarma (dosya penceresi yerine geçici yol). */
class HistoryScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private var target: Path? = null
    private var askedName: String? = null

    @Before
    fun setUp() {
        graph = AppGraph(tmp.newFolder("data").toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
        val now = System.currentTimeMillis()
        graph.eventLog.add(Demo.LIGHT, "Garden light", EventType.TURNED_ON.name, "Garden light: OFF → ON", "ALARM", time = now - 60_000)
        graph.eventLog.add(Demo.PUMPS, "Pump panel", EventType.WENT_OFFLINE.name, "Pump panel: ONLINE → OFFLINE", "NOTIFY", time = now - 30_000)
        compose.setContent {
            SwitchGuardTheme("light") {
                CompositionLocalProvider(LocalStrings provides graph.strings()) {
                    HistoryScreen(graph, chooseTarget = { name, _ -> askedName = name; target })
                }
            }
        }
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun showsEventsUnderTodayAndFiltersByDevice() {
        waitForText("Garden light: OFF → ON")
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Pump panel: ONLINE → OFFLINE").assertExists()

        compose.onNodeWithTag("filter-${Demo.LIGHT}").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Pump panel: ONLINE → OFFLINE")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Garden light: OFF → ON").assertExists()
    }

    @Test
    fun clearAsksFirstThenEmptiesTheLog() {
        waitForText("Garden light: OFF → ON")
        compose.onNodeWithTag("history-clear").performClick()
        compose.onNodeWithTag("history-clear-confirm").performClick()
        waitForText("No events yet")
        assertEquals(0L, graph.eventLog.latestId())
    }

    @Test
    fun exportWritesTheChosenRangeToTheChosenFile() {
        waitForText("Garden light: OFF → ON")
        target = tmp.root.toPath().resolve("out.csv")
        compose.onNodeWithTag("history-export").performClick()
        compose.onNodeWithTag("range-DAY").performClick()

        compose.waitUntil(5_000) { Files.exists(target!!) }
        val csv = String(Files.readAllBytes(target!!), Charsets.UTF_8)
        assertTrue(csv, csv.contains("Garden light: OFF → ON") && csv.contains("Pump panel: ONLINE → OFFLINE"))
        assertTrue(askedName!!, askedName!!.startsWith("switchguard-") && askedName!!.endsWith(".csv"))
        waitForText("2 entries exported")
    }

    @Test
    fun cancellingTheFileWindowWritesNothing() {
        waitForText("Garden light: OFF → ON")
        target = null
        compose.onNodeWithTag("history-export").performClick()
        compose.onNodeWithTag("range-ALL").performClick()
        compose.waitForIdle()
        assertTrue(askedName != null)
        assertEquals("no file written", listOf("data"), tmp.root.list()!!.toList())
    }
}
