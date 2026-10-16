package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.core.Category
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.i18n.Strings
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.ExperimentalTestApi
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/**
 * Cihazlar ekranının görsel örnekleri (açık + koyu, TR, demo cihazlarıyla): `build/snapshots/devices-*.png`.
 * Motor başlatılmaz; "canlı" görünüm için yalnızca izleme ayarı ve [LiveState] kurulur.
 */
@OptIn(ExperimentalTestApi::class)
class DevicesSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "tr"
    }

    @After
    fun tearDown() {
        LiveState.setConnection(ConnState.STOPPED)
        graph.shutdown()
    }

    private fun render(name: String, width: Int = 1100, height: Int = 760) {
        for (dark in listOf(false, true)) {
            val out = Snapshots.render("$name-" + if (dark) "dark" else "light", width, height, dark) {
                DevicesScreen(graph, tr)
            }
            assertTrue(Files.size(out) > 1000)
        }
    }

    @Test
    fun liveDemo() {
        graph.store.enableDemo(tr.demoNames())
        graph.store.monitoringEnabled = true
        LiveState.setConnection(ConnState.LIVE, "")
        render("devices-live")
    }

    @Test
    fun stoppedWithPriorityAndCategory() {
        graph.store.enableDemo(tr.demoNames())
        val pumps = Category("c1", "Pompalar")
        graph.store.deviceLayout = graph.store.deviceLayout
            .togglePriority(Demo.FREEZER)
            .addCategory(pumps)
            .assign(Demo.PUMPS, pumps.id)
        graph.appState.setBlind("İzleme 12 dk'dır çalışmıyor: cihazların durumu alınamıyor.\nİnternet bağlantısını ve bilgisayarın uykuda olmadığını kontrol edin.")
        render("devices-sections", height = 1000)
    }

    @Test
    fun narrowWindow() {
        graph.store.enableDemo(tr.demoNames())
        graph.store.monitoringEnabled = true
        LiveState.setConnection(ConnState.NO_INTERNET, "")
        render("devices-narrow", width = 900, height = 700)
    }

    @Test
    fun noAccount() {
        render("devices-no-account")
    }

    @Test
    fun editMode() {
        graph.store.enableDemo(tr.demoNames())
        val pumps = Category("c1", "Pompalar")
        graph.store.deviceLayout = graph.store.deviceLayout.togglePriority(Demo.LIGHT).addCategory(pumps).assign(Demo.PUMPS, pumps.id)
        for (dark in listOf(false, true)) {
            InteractiveSnapshots.render("devices-edit-" + if (dark) "dark" else "light", 1100, 900, dark, content = { DevicesScreen(graph, tr) }) {
                onNodeWithTag("edit-layout").performClick()
            }
        }
        InteractiveSnapshots.render("devices-categories-light", 1100, 760, content = { DevicesScreen(graph, tr) }) {
            onNodeWithTag("edit-layout").performClick()
            onNodeWithText(tr["categories_manage"]).performClick()
        }
    }

    /** Uygulamadaki gibi sol menüyle: Cihazlar, açık cihaz sayfası ve Geçmiş (1100×760 pencere). */
    @Test
    fun inTheMainWindow() {
        graph.store.enableDemo(tr.demoNames())
        graph.store.monitoringEnabled = true
        LiveState.setConnection(ConnState.LIVE, "", synced = true)
        for (dark in listOf(false, true)) {
            val suffix = if (dark) "dark" else "light"
            graph.appState.select(AppState.Tab.DEVICES)
            SettledSnapshots.render("window-devices-$suffix", 1100, 760, dark) { AppNav(graph, tr) }
            graph.appState.openDevice.value = Demo.PUMPS
            SettledSnapshots.render("window-detail-$suffix", 1100, 760, dark) { AppNav(graph, tr) }
            graph.appState.select(AppState.Tab.HISTORY)
            SettledSnapshots.render("window-history-$suffix", 1100, 760, dark) { AppNav(graph, tr) }
        }
    }
}
