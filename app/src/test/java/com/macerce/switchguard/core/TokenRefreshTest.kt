package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class TokenRefreshTest {

    @Test
    fun `token degismediyse yeniler`() {
        var token = "A"
        var refreshed = 0
        TokenRefresh().refreshIfStale("A", { token }) { refreshed++; token = "B" }
        assertEquals(1, refreshed)
    }

    @Test
    fun `token baska istek tarafindan yenilendiyse tekrar yenilemez`() {
        var token = "A"
        var refreshed = 0
        val r = TokenRefresh()
        r.refreshIfStale("A", { token }) { refreshed++; token = "B" }
        r.refreshIfStale("A", { token }) { refreshed++; token = "C" }
        assertEquals(1, refreshed)
        assertEquals("B", token)
    }

    @Test
    fun `ayni anda hata alan iki istek tek yenileme yapar`() {
        val token = AtomicReference("A")
        val refreshed = AtomicInteger()
        val r = TokenRefresh()
        val start = CountDownLatch(1)
        val workers = (1..2).map {
            thread {
                start.await()
                r.refreshIfStale("A", { token.get() }) {
                    refreshed.incrementAndGet()
                    Thread.sleep(100) // eski refresh token sunucuda iptal olurken diğer istek beklesin
                    token.set("B")
                }
            }
        }
        start.countDown()
        workers.forEach { it.join() }
        assertEquals(1, refreshed.get())
    }

    @Test
    fun `token bossa yenilemeye calisir`() {
        var refreshed = 0
        TokenRefresh().refreshIfStale("A", { "" }) { refreshed++ }
        assertEquals(1, refreshed)
    }

    @Test
    fun `sunucu hatalari gecici, yetki hatalari degil`() {
        assertTrue(TokenRefresh.isTransient(500))
        assertTrue(TokenRefresh.isTransient(503))
        assertTrue(TokenRefresh.isTransient(429))
        assertTrue(TokenRefresh.isTransient(-1))
        assertFalse(TokenRefresh.isTransient(401))
        assertFalse(TokenRefresh.isTransient(402))
        assertFalse(TokenRefresh.isTransient(406))
    }
}
