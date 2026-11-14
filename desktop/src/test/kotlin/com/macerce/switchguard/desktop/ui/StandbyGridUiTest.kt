package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.StandbyGrid
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Bekleme ekranı widget ızgarası: sürüklenen widget boş yere konur, diğerleri yerinden kaymaz; dolu yere bırakılan
 * widget eski yerine döner; kaldırınca diğerleri yerinde kalır.
 */
class StandbyGridUiTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "tr"
        graph.store.enableDemo(tr.demoNames())
        compose.setContent {
            Box(Modifier.size(1280.dp, 800.dp)) { StandbyBoardScreen(graph, tr, onOpenApp = {}, onClose = {}) }
        }
        compose.onNodeWithTag("standby").performClick()
        compose.onNodeWithTag("standby-edit").performClick()
        compose.waitForIdle()
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun bounds(id: String): Rect = compose.onNodeWithTag("widget-$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun drag(id: String, by: Offset) {
        compose.onNodeWithTag("widget-$id", useUnmergedTree = true).performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(by.x / 10, by.y / 10)) }
            up()
        }
        compose.waitForIdle()
    }

    private val light = StandbyGrid.device(Demo.LIGHT, 0)
    private val freezer = StandbyGrid.device(Demo.FREEZER, 0)

    @Test
    fun dragToFreeSpotKeepsOthersInPlace() {
        val others = listOf(StandbyGrid.CLOCK, freezer, StandbyGrid.WEATHER).associateWith { bounds(it) }
        drag(light, Offset(0f, 700f))
        // Görünür alanın dışına indi: kayıtlı konuma bakılır.
        val moved = StandbyGrid.parse(graph.store.standbyGrid).items.first { it.id == light }
        assertTrue("ışık aşağı inmeli: $moved", moved.y >= 10)
        others.forEach { (id, r) -> assertEquals("$id kaymamalı", r, bounds(id)) }
    }

    @Test
    fun dropOnOccupiedSpotReturnsHome() {
        val before = bounds(freezer)
        val clock = bounds(StandbyGrid.CLOCK)
        drag(freezer, Offset(clock.center.x - before.center.x, clock.center.y - before.center.y))
        assertEquals(before, bounds(freezer))
        assertEquals(clock, bounds(StandbyGrid.CLOCK))
    }

    @Test
    fun removeKeepsOthersInPlace() {
        val keep = bounds(freezer)
        val clock = bounds(StandbyGrid.CLOCK)
        compose.onNodeWithTag("widget-$light", useUnmergedTree = true).assertExistsAndRemove()
        assertEquals(keep, bounds(freezer))
        assertEquals(clock, bounds(StandbyGrid.CLOCK))
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsAndRemove() {
        val r = fetchSemanticsNode().boundsInRoot
        // × düğmesi widget'ın sol üst köşesinde.
        compose.onRoot().performTouchInput { click(Offset(r.left + 18f, r.top + 18f)) }
        compose.waitForIdle()
        assertTrue(graph.store.standbyGrid.orEmpty().substringAfter('|').contains(light))
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onRoot() =
        onNode(androidx.compose.ui.test.isRoot())
}
