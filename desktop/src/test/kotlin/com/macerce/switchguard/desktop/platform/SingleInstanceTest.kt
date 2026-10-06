package com.macerce.switchguard.desktop.platform

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetAddress
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SingleInstanceTest {
    @get:Rule val tmp = TemporaryFolder()

    private val opened = mutableListOf<SingleInstance>()

    private fun instance(): SingleInstance = SingleInstance(tmp.root.toPath()).also { opened += it }

    @After
    fun releaseAll() {
        opened.forEach { it.release() }
    }

    @Test
    fun secondAcquireSignalsFirst() {
        val shown = CountDownLatch(1)
        val a = instance()
        val b = instance()

        assertTrue(a.acquire { shown.countDown() })
        assertFalse(b.acquire { throw AssertionError("the second instance must not become the owner") })

        assertTrue("the first instance's callback must run within 2 s", shown.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun releaseAllowsReacquire() {
        val a = instance()
        assertTrue(a.acquire {})
        a.release()

        val b = instance()
        assertTrue(b.acquire {})
    }

    @Test
    fun releaseIsIdempotentAndSafeWithoutAcquire() {
        val never = instance()
        never.release()

        val a = instance()
        assertTrue(a.acquire {})
        a.release()
        a.release()
    }

    @Test
    fun failedAcquireKeepsTheOwnersLock() {
        val a = instance()
        assertTrue(a.acquire {})

        // Başarısız ikinci denemeler sahibin kilidini bırakmamalı: sonrakiler de reddedilir.
        assertFalse(instance().acquire {})
        assertFalse(instance().acquire {})
        assertFalse(instance().acquire {})
    }

    @Test
    fun createsMissingDirectory() {
        val dir = tmp.root.toPath().resolve("nested").resolve("SwitchGuard")
        val a = SingleInstance(dir).also { opened += it }
        assertTrue(a.acquire {})
        assertTrue(Files.exists(dir.resolve("instance.lock")))
    }

    @Test
    fun ownerPublishesLoopbackPortAndReleaseRemovesIt() {
        val a = instance()
        assertTrue(a.acquire {})
        val portFile = tmp.root.toPath().resolve("instance.port")
        val port = Files.readString(portFile).trim().toInt()
        assertTrue(port in 1..65535)

        a.release()
        assertFalse(Files.exists(portFile))
    }

    @Test
    fun onlyShowMessageRaisesTheWindow() {
        val shown = AtomicInteger()
        val latch = CountDownLatch(1)
        val a = instance()
        assertTrue(a.acquire { shown.incrementAndGet(); latch.countDown() })
        val port = Files.readString(tmp.root.toPath().resolve("instance.port")).trim().toInt()
        val loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))

        // Başka bir şey yazan (ya da hiçbir şey yazmadan kapatan) istemci pencereyi öne getirmez, sahibi de bozmaz.
        Socket(loopback, port).use { it.getOutputStream().write("hello\n".toByteArray()) }
        Socket(loopback, port).close()
        Thread.sleep(300)
        assertEquals(0, shown.get())

        assertFalse(instance().acquire {})
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(1, shown.get())
    }
}
