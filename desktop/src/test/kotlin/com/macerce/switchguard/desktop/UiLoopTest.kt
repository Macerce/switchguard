package com.macerce.switchguard.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.desktop.platform.TrayIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.GraphicsEnvironment
import java.awt.SystemTray

class UiLoopTest {
    // ---------------------------------------------------------------- yeniden başlatma hakkı

    @Test
    fun threeRestartsAreAllowedWithinTenMinutes() {
        val restarts = UiRestarts(max = 3, windowMs = 600_000)

        assertTrue(restarts.allow(0))
        assertTrue(restarts.allow(1_000))
        assertTrue(restarts.allow(2_000))
        assertFalse(restarts.allow(3_000))
    }

    @Test
    fun aRestartStopsCountingTenMinutesLater() {
        val restarts = UiRestarts(max = 3, windowMs = 600_000)
        restarts.allow(0)
        restarts.allow(1_000)
        restarts.allow(2_000)

        assertFalse(restarts.allow(599_999))
        assertTrue(restarts.allow(600_000)) // 0'daki düştü
        assertFalse(restarts.allow(600_001))
    }

    // ---------------------------------------------------------------- arayüz döngüsü

    @Test
    fun failedUiIsStartedAgainUntilItEndsNormally() {
        var runs = 0

        val ended = runUi(UiRestarts(max = 3, windowMs = 600_000), clock = { 0L }) {
            runs++
            if (runs < 3) error("boom $runs")
        }

        assertTrue(ended)
        assertEquals(3, runs)
    }

    @Test
    fun uiThatKeepsFailingIsGivenUpAfterItsRestarts() {
        var runs = 0

        val ended = runUi(UiRestarts(max = 3, windowMs = 600_000), clock = { 0L }) {
            runs++
            error("boom")
        }

        assertFalse(ended)
        assertEquals(4, runs) // ilk çalışma + 3 yeniden başlatma
    }

    // ---------------------------------------------------------------- Compose varsayımı

    /**
     * Yeniden başlatma tasarımının dayandığı varsayım: pencere dışında (uygulama düzeyinde) hata veren `application`
     * hatayı çağırana fırlatır, açtığı pencereyi ve tepsi simgesini kapatır, ve aynı süreçte yeniden çalıştırılabilir.
     */
    @Test(timeout = 60_000)
    fun composeApplicationCanRunAgainAfterAnAppLevelErrorAndLeavesNothingBehind() {
        assumeFalse(GraphicsEnvironment.isHeadless())
        val traysBefore = if (isTraySupported) SystemTray.getSystemTray().trayIcons.size else 0
        var boom by mutableStateOf(false)

        val failure = runCatching {
            application(exitProcessOnExit = false) {
                if (isTraySupported) Tray(icon = TrayIcons.painter(ConnState.STOPPED), tooltip = "ui-loop-test")
                Window(onCloseRequest = {}, state = rememberWindowState(size = DpSize(120.dp, 80.dp)), title = "ui-loop-test") {}
                if (boom) error("app-level boom")
                LaunchedEffect(Unit) { boom = true }
            }
        }.exceptionOrNull()

        assertEquals("app-level boom", failure?.message)
        assertTrue("a window survived the failed UI", java.awt.Window.getWindows().none { it.isDisplayable })
        if (isTraySupported) assertEquals(traysBefore, SystemTray.getSystemTray().trayIcons.size)

        var secondRan = false
        application(exitProcessOnExit = false) {
            LaunchedEffect(Unit) {
                secondRan = true
                exitApplication()
            }
        }
        assertTrue(secondRan)
    }
}
