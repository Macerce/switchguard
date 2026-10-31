package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRetryTest {
    private val r = AutoRetry(listOf(30_000L, 60_000L))

    @Test fun retriesWithGrowingDelaysThenGivesUp() {
        assertTrue(r.failed("a", "d1", 0, true, failedAttempt = 1, now = 0))
        assertEquals(30_000L, r.nextDueAt())
        assertEquals(emptyList<AutoRetry.Pending>(), r.due(29_999))
        val first = r.due(30_000).single()
        assertEquals(2, first.attempt)
        assertNull(r.nextDueAt())
        assertTrue(r.failed("a", "d1", 0, true, failedAttempt = 2, now = 30_000))
        assertEquals(90_000L, r.nextDueAt())
        assertEquals(3, r.due(90_000).single().attempt)
        assertFalse(r.failed("a", "d1", 0, true, failedAttempt = 3, now = 90_000))
        assertNull(r.nextDueAt())
    }

    @Test fun comingOnlineMakesDueNow() {
        r.failed("a", "d1", 0, true, 1, now = 0)
        r.failed("a", "d2", 0, true, 1, now = 0)
        assertTrue(r.cameOnline("d1", 5_000))
        assertFalse(r.cameOnline("d3", 5_000))
        assertEquals(listOf("d1"), r.due(5_000).map { it.deviceId })
        assertEquals(30_000L, r.nextDueAt())
    }

    @Test fun newCommandReplacesOrCancels() {
        r.failed("a", "d1", 1, true, 1, now = 0)
        r.failed("b", "d1", 1, false, 1, now = 10_000)
        val p = r.due(40_000).single()
        assertEquals("b", p.automation); assertFalse(p.on)
        r.failed("a", "d1", 0, true, 1, now = 0)
        r.cancel("d1", 0)
        assertNull(r.nextDueAt())
    }
}
