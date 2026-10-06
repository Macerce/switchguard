package com.macerce.switchguard.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrayMenuTest {
    @Test
    fun pendingAlarmCanBeDismissedFromTheTray() {
        // Alarm penceresi bozulsa (yeniden kurma hakkı bitse) bile alarmı susturmanın ikinci yolu: ilk öğe.
        assertEquals(TrayAction.DISMISS_ALARM, trayActions(alarmPending = true).first())
    }

    @Test
    fun withoutAnAlarmThereIsNothingToDismiss() {
        assertFalse(TrayAction.DISMISS_ALARM in trayActions(alarmPending = false))
    }

    @Test
    fun theUsualItemsStayInBothCases() {
        val usual = listOf(TrayAction.OPEN, TrayAction.MONITORING, TrayAction.TEST_ALARM, TrayAction.EXIT)
        assertEquals(usual, trayActions(alarmPending = false))
        assertEquals(usual, trayActions(alarmPending = true).drop(1))
    }
}
