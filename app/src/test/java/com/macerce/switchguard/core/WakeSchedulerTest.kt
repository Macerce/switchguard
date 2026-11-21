package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeSchedulerTest {
    @Test
    fun manyRequestsForSameTimeMakeOnePost() {
        val w = WakeScheduler()
        var posts = 0
        repeat(10_000) { if (w.request(60_000)) posts++ }

        assertEquals(1, posts)
        assertEquals(60_000L, w.scheduledAt)
    }

    @Test
    fun laterRequestIsIgnored() {
        val w = WakeScheduler()
        assertTrue(w.request(1_000))

        assertFalse(w.request(5_000))
        assertEquals(1_000L, w.scheduledAt)
    }

    @Test
    fun earlierRequestReplacesTheOldOne() {
        val w = WakeScheduler()
        assertTrue(w.request(5_000))

        assertTrue(w.request(1_000))
        assertEquals(1_000L, w.scheduledAt)
    }

    @Test
    fun afterFiringNextRequestIsScheduledAgain() {
        val w = WakeScheduler()
        w.request(1_000)
        w.fired()

        assertNull(w.scheduledAt)
        // Ertesi güne kurulan saatli şablon: yeniden tek gönderi.
        assertTrue(w.request(86_400_000))
        assertFalse(w.request(86_400_000))
    }

    @Test
    fun resetClearsPendingWake() {
        val w = WakeScheduler()
        w.request(1_000)
        w.reset()

        assertNull(w.scheduledAt)
        assertTrue(w.request(9_000))
    }
}
