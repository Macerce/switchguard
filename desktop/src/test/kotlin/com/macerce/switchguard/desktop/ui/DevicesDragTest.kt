package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import com.macerce.switchguard.desktop.AppGraph
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalTestApi::class)
class DevicesDragTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
        graph.store.enableDemo(graph.strings().demoNames())
        compose.setContent { DevicesScreen(graph, graph.strings()) }
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun currentOrder() = graph.store.deviceLayout.sorted(graph.store.snapshots).map { it.name }

    @Test
    fun holdingACardAndDraggingItSavesTheNewOrderOnce() {
        val before = currentOrder()
        val first = before.first()
        val startVersion = graph.store.version.value

        // İkinci kartın üstüne doğru sürükle (ızgara kaç sütun olursa olsun).
        val from = compose.onNodeWithText(first).fetchSemanticsNode().boundsInRoot.center
        val to = compose.onNodeWithText(before[1]).fetchSemanticsNode().boundsInRoot.center
        val step = (to - from) / 20f
        // Fare, basmadan önce kartın üstüne götürülür (test ortamında press() o anki konuma basar).
        val root = compose.onRoot()
        root.performMouseInput { moveTo(from); press() }
        compose.mainClock.advanceTimeBy(1_000) // basılı tut: uzun basma süresi dolar, kart kalkar
        repeat(24) {
            root.performMouseInput { moveBy(step) }
            compose.mainClock.advanceTimeBy(16)
        }
        root.performMouseInput { release() }
        compose.waitForIdle()
        val writes = graph.store.version.value - startVersion

        val after = currentOrder()
        assertNotEquals(before, after)
        assertEquals(before.toSet(), after.toSet())
        assertEquals("sıra bırakınca bir kez yazılır", 1, writes)
    }

    @Test
    fun aShortClickDoesNotMoveAnything() {
        val before = currentOrder()

        // Fare, basmadan önce kartın üstüne götürülür (test ortamında press() o anki konuma basar).
        val at = compose.onNodeWithText(before.first()).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performMouseInput { moveTo(at); press() }
        compose.mainClock.advanceTimeBy(80)
        compose.onRoot().performMouseInput { release() }
        compose.waitForIdle()

        assertEquals(before, currentOrder())
    }
}
