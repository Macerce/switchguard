package com.macerce.switchguard.desktop

import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.desktop.ui.StatusColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test
    fun alarmLinesSetAndClear() {
        val s = AppState()
        assertTrue(s.alarmLines.value.isEmpty())
        s.setAlarmLines(listOf("05:00  a", "05:01  b"))
        assertEquals(2, s.alarmLines.value.size)
        s.clearAlarm()
        assertTrue(s.alarmLines.value.isEmpty())
    }

    @Test
    fun showMainWindowRevealsAndRaises() {
        val s = AppState()
        s.windowVisible.value = false
        val before = s.mainRaise.value
        s.showMainWindow()
        assertTrue(s.windowVisible.value)
        assertEquals(before + 1, s.mainRaise.value)
    }

    @Test
    fun raiseAlarmWindowBumpsTheCounterEveryTime() {
        val s = AppState()
        s.raiseAlarmWindow()
        s.raiseAlarmWindow()
        assertEquals(2, s.alarmRaise.value)
    }

    @Test
    fun tabsFollowThePhoneOrder() {
        // Sohbet masaüstünde ayrı sekme (telefonda Cihazlar'ın üst çubuğundan açılır).
        assertEquals(
            listOf(AppState.Tab.DEVICES, AppState.Tab.ALERTS, AppState.Tab.AUTOMATIONS, AppState.Tab.HISTORY, AppState.Tab.CHAT, AppState.Tab.SETTINGS),
            AppState.Tab.entries,
        )
    }

    @Test
    fun clickingTheSelectedAlertsTabAgainAsksForItsStart() {
        val s = AppState()
        s.select(AppState.Tab.ALERTS)
        assertEquals(0, s.alertsHome.value) // ilk seçim başa dönme isteği değil
        s.select(AppState.Tab.ALERTS)
        assertEquals(1, s.alertsHome.value)
        assertEquals(0, s.settingsHome.value)
    }

    @Test
    fun warningCanBeSetAndCleared() {
        val s = AppState()
        assertNull(s.warning.value)
        s.setWarning("session expired")
        assertEquals("session expired", s.warning.value)
        s.setWarning(null)
        assertNull(s.warning.value)
    }

    @Test
    fun statusIsStoredWithItsText() {
        val s = AppState()
        s.setStatus(ConnState.LIVE, "Live")
        assertEquals(ConnState.LIVE to "Live", s.status.value)
    }

    @Test
    fun trayColoursFollowTheSpec() {
        // yeşil LIVE, sarı CONNECTING/POLLING, kırmızı NO_INTERNET/ERROR/AUTH_ERROR, gri STOPPED
        val live = StatusColors.of(ConnState.LIVE)
        val yellow = StatusColors.of(ConnState.CONNECTING)
        val red = StatusColors.of(ConnState.ERROR)
        val gray = StatusColors.of(ConnState.STOPPED)

        assertEquals(yellow, StatusColors.of(ConnState.POLLING))
        assertEquals(red, StatusColors.of(ConnState.NO_INTERNET))
        assertEquals(red, StatusColors.of(ConnState.AUTH_ERROR))
        assertEquals(4, setOf(live, yellow, red, gray).size)
        assertTrue("live is green", live.green > live.red && live.green > live.blue)
        assertTrue("yellow has red and green", yellow.red > 0.9f && yellow.green > 0.6f && yellow.blue < 0.3f)
        assertTrue("error is red", red.red > red.green && red.red > red.blue)
        assertTrue("stopped is neutral gray", gray.red == gray.green && gray.green == gray.blue)
        assertNotEquals(gray, live)
    }
}
