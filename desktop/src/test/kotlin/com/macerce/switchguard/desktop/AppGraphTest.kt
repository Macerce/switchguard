package com.macerce.switchguard.desktop

import com.macerce.switchguard.data.ConnState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.file.Files

class AppGraphTest {
    @get:Rule val tmp = TemporaryFolder()

    private val graphs = mutableListOf<AppGraph>()

    @Before
    fun windowsOnly() {
        // AppGraph kurulumu DPAPI kullanır.
        Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"))
    }

    @After
    fun closeGraphs() {
        graphs.forEach { it.shutdown() }
    }

    private fun graph() = AppGraph(tmp.root.toPath()).also { graphs += it }

    @Test
    fun stringsAreOneInstanceUntilTheLanguageChanges() {
        val g = graph()
        g.store.language = "en"
        val en = g.strings()
        assertSame(en, g.strings())
        assertEquals("Dismiss", en["alarm_dismiss"])

        g.store.language = "tr"
        val tr = g.strings()
        assertNotSame(en, tr)
        assertEquals("Kapat", tr["alarm_dismiss"])
        assertSame(tr, g.strings())
    }

    @Test
    fun startsWithMonitoringStoppedStatus() {
        val g = graph()
        g.store.language = "en"
        assertEquals(ConnState.STOPPED, g.appState.status.value.first)
    }

    @Test
    fun unreadableSettingsStopStartupInsteadOfStartingEmpty() {
        // settings.json bir klasör: okunamaz (NoSuchFile değil), yeniden denenip IOException olarak dışarı atılır.
        Files.createDirectories(tmp.root.toPath().resolve("settings.json"))
        try {
            AppGraph(tmp.root.toPath()).also { graphs += it }
            fail("AppGraph must not start without readable settings")
        } catch (_: IOException) {
            // beklenen
        }
        assertTrue(Files.isDirectory(tmp.root.toPath().resolve("settings.json")))
    }

    /** Kuyruğa girenlerden [count] tanesini (tepsi yerine sahte alıcıya) teslim eder; teslim edilen metinleri döndürür. */
    private fun AppGraph.deliver(count: Int, accept: Boolean = true): List<String> {
        val delivered = mutableListOf<String>()
        val done = CompletableDeferred<Unit>()
        runBlocking {
            withTimeout(10_000) {
                val pump = launch {
                    notifications.pump(0, 1) {
                        delivered += it.message
                        if (delivered.size == count) done.complete(Unit)
                        accept
                    }
                }
                done.await()
                pump.cancel()
            }
        }
        return delivered
    }

    private val hintMarker get() = tmp.root.toPath().resolve("tray-hint-shown")

    @Test
    fun closingTheWindowQueuesTheTrayHintAndMarksItOnlyOnceItIsHandedToTheTray() {
        val g = graph()
        g.store.language = "en"

        g.hideMainWindow()
        assertFalse(g.appState.windowVisible.value)
        assertFalse("not shown yet, only queued", Files.exists(hintMarker))

        assertEquals(listOf(g.strings()["tray_minimized_hint"]), g.deliver(1))
        assertTrue("handed to the tray", Files.exists(hintMarker))
    }

    @Test
    fun theHintIsQueuedOnlyOncePerRun() {
        val g = graph()
        g.hideMainWindow()
        g.hideMainWindow()
        assertEquals(1, g.deliver(1).size)
        // ikinci bildirim kuyrukta olsaydı sahte alıcı onu da teslim alırdı: yeni bir bildirim yollayıp sıralarını sına
        g.notifications.enqueue(androidx.compose.ui.window.Notification("t", "marker", androidx.compose.ui.window.Notification.Type.Info))
        assertEquals(listOf("marker"), g.deliver(1))
    }

    @Test
    fun aHintThatWasNeverHandedOverIsNotBurnedAndComesBackNextRun() {
        val first = graph()
        first.hideMainWindow() // kuyruğa girdi, uygulama teslimden önce kapandı
        first.shutdown()
        graphs.clear()
        assertFalse(Files.exists(hintMarker))

        val second = graph()
        second.hideMainWindow()
        assertEquals(1, second.deliver(1).size)
        assertTrue(Files.exists(hintMarker))
    }

    @Test
    fun anAlreadyShownHintIsNotQueuedAgainOnTheNextRun() {
        val first = graph()
        first.hideMainWindow()
        first.deliver(1)
        first.shutdown()
        graphs.clear()

        val second = graph()
        assertFalse(second.trayHintDue())
    }

    @Test
    fun aRejectedHintDeliveryDoesNotMarkIt() {
        val g = graph()
        g.hideMainWindow()
        g.deliver(1, accept = false)
        assertFalse(Files.exists(hintMarker))
    }
}
