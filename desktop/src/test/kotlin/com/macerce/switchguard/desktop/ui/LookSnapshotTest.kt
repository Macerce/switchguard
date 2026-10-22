package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.SceneAction
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/**
 * Bütün pencerenin (kenar çubuğu, sekme, durum çubuğu) görsel örnekleri: her sekme, açık + koyu, 1280×800;
 * `build/snapshots/look-*.png`. Karşılaştırma yapmaz, gözle bakmak içindir.
 */
class LookSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.apply {
            language = "tr"
            enableDemo(tr.demoNames())
            scenes = listOf(
                Scene("s1", "Gece", listOf(SceneAction(Demo.LIGHT, 0, false), SceneAction(Demo.PUMPS, 1, false))),
                Scene("s2", "Sabah", listOf(SceneAction(Demo.LIGHT, 0, true))),
            )
            automations = automations + Automation(
                id = "x-sun", name = "Gün batınca ışığı yak", enabled = true,
                trigger = Trigger(TriggerKind.SUNSET, "", minutes = -15),
                action = AutoAction(ActionKind.TURN_ON, Demo.LIGHT),
            )
        }
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun everyTab() {
        for (tab in AppState.Tab.entries) {
            for (dark in listOf(false, true)) {
                graph.appState.select(tab)
                val out = Snapshots.render("look-${tab.name.lowercase()}-" + if (dark) "dark" else "light", 1280, 800, dark = dark) {
                    AppNav(graph, tr)
                }
                assertTrue(Files.size(out) > 1000)
            }
        }
    }
}
