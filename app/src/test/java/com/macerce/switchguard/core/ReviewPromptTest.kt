package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {
    private val day = 86_400_000L
    private val now = 100 * day

    private fun due(
        installedAt: Long = now - 10 * day,
        opens: Int = 6,
        asked: Boolean = false,
        monitoring: Boolean = true,
        devices: Int = 2,
        alarm: Boolean = false,
    ) = ReviewPrompt.due(installedAt, now, opens, asked, monitoring, devices, alarm)

    @Test fun asksWhenSettledIn() = assertTrue(due())
    @Test fun notInFirstWeek() = assertFalse(due(installedAt = now - 6 * day))
    @Test fun notBeforeEnoughOpens() = assertFalse(due(opens = ReviewPrompt.MIN_OPENS - 1))
    @Test fun onlyOnce() = assertFalse(due(asked = true))
    @Test fun notWhileAlarmRings() = assertFalse(due(alarm = true))
    @Test fun notWhenNotMonitoring() = assertFalse(due(monitoring = false))
    @Test fun notWithoutDevices() = assertFalse(due(devices = 0))
}
