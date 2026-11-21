package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerClockTest {
    @Test
    fun correctClockNeedsNoOffset() {
        val c = ServerClock()
        c.observe(serverTime = 1_000_150, sentAt = 1_000_000, receivedAt = 1_000_300)

        assertEquals(0, c.offsetMs)
        assertEquals(5_000L, c.now(5_000))
    }

    @Test
    fun boxClockOneHourBehindIsCorrected() {
        val c = ServerClock()
        val hour = 3_600_000L
        // Cihaz saati 1 saat geride: sunucu "şimdi"si cihazınkinden 1 saat ileri.
        c.observe(serverTime = 10 * hour + hour, sentAt = 10 * hour - 200, receivedAt = 10 * hour + 200)

        assertEquals(hour, c.offsetMs)
        assertEquals(11 * hour, c.now(10 * hour))
    }

    @Test
    fun networkJitterDoesNotMoveTheOffset() {
        val c = ServerClock()
        c.observe(serverTime = 100_000 + 60_000, sentAt = 100_000, receivedAt = 100_000)
        c.observe(serverTime = 200_000 + 61_000, sentAt = 200_000, receivedAt = 200_000)

        assertEquals(60_000, c.offsetMs)
    }

    @Test
    fun invalidResponsesAreIgnored() {
        val c = ServerClock()
        c.observe(serverTime = 0, sentAt = 1, receivedAt = 2)
        c.observe(serverTime = 9_999_999, sentAt = 10, receivedAt = 5)

        assertEquals(0, c.offsetMs)
    }
}
