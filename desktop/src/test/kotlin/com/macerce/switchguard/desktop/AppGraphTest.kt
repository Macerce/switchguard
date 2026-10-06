package com.macerce.switchguard.desktop

import com.macerce.switchguard.data.ConnState
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

    @Test
    fun trayHintIsClaimedOncePerInstall() {
        val first = graph()
        assertTrue(first.claimTrayHint())
        assertFalse(first.claimTrayHint())
        first.shutdown()
        graphs.clear()

        // Yeniden açılış: işaret dosyası kalıcı olduğundan ipucu bir daha gösterilmez.
        assertFalse(graph().claimTrayHint())
    }
}
