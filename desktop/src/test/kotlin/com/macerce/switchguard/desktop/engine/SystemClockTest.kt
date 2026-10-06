package com.macerce.switchguard.desktop.engine

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class SystemClockTest {
    @Test
    fun awakeTimeAdvancesInMillisecondsOnWindows() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"))
        val start = SystemClock.awake()
        Thread.sleep(300)
        val delta = SystemClock.awake() - start

        assertTrue("awake() delta $delta ms after a 300 ms wait", start > 0 && delta in 250..1_000)
    }
}
