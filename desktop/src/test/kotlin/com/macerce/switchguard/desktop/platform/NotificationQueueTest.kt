package com.macerce.switchguard.desktop.platform

import androidx.compose.ui.window.Notification
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class NotificationQueueTest {
    private fun note(n: Int) = Notification("SwitchGuard", "event $n", Notification.Type.Info)

    /** [count] bildirim teslim edilene dek pompayı çalıştırır, sonra iptal eder; teslim edilenleri sırayla döndürür. */
    private fun runUntilDelivered(
        queue: NotificationQueue,
        count: Int,
        spacingMs: Long = 1,
        sink: (Notification) -> Boolean = { true },
    ): List<String> {
        val delivered = CopyOnWriteArrayList<String>()
        val done = CompletableDeferred<Unit>()
        runBlocking {
            withTimeout(10_000) {
                val pump: Job = launch {
                    queue.pump(initialDelayMs = 0, spacingMs = spacingMs) {
                        delivered += it.message
                        if (delivered.size == count) done.complete(Unit)
                        sink(it)
                    }
                }
                done.await()
                pump.cancel()
            }
        }
        return delivered
    }

    @Test
    fun everythingEnqueuedBeforeThePumpStartsIsDeliveredInOrder() {
        val queue = NotificationQueue()
        // Compose'un tamponsuz kanalı, alıcı yokken (ör. Tray henüz toplamıyorken) bunların hepsini düşürürdü.
        repeat(25) { queue.enqueue(note(it)) }

        val delivered = runUntilDelivered(queue, 25)

        assertEquals((0 until 25).map { "event $it" }, delivered)
    }

    @Test
    fun enqueueFromAnotherThreadWhilePumpRunsLosesNothing() {
        val queue = NotificationQueue()
        val producer = thread { repeat(40) { queue.enqueue(note(it)); Thread.sleep(1) } }

        val delivered = runUntilDelivered(queue, 40)
        producer.join()

        assertEquals((0 until 40).map { "event $it" }, delivered)
    }

    @Test
    fun deliveriesAreSpacedApart() {
        val queue = NotificationQueue()
        repeat(4) { queue.enqueue(note(it)) }
        val times = Collections.synchronizedList(mutableListOf<Long>())

        runUntilDelivered(queue, 4, spacingMs = 60) { times += System.nanoTime(); true }

        val gapsMs = times.zipWithNext { a, b -> (b - a) / 1_000_000 }
        assertTrue("gaps were $gapsMs", gapsMs.all { it >= 50 })
    }

    @Test
    fun theFirstDeliveryWaitsForTheInitialDelay() {
        val queue = NotificationQueue()
        queue.enqueue(note(0))
        val start = System.nanoTime()
        var firstAt = 0L

        runBlocking {
            withTimeout(10_000) {
                val done = CompletableDeferred<Unit>()
                val pump = launch {
                    queue.pump(initialDelayMs = 150, spacingMs = 1) {
                        firstAt = System.nanoTime()
                        done.complete(Unit)
                        true
                    }
                }
                done.await()
                pump.cancel()
            }
        }

        assertTrue("first delivery after ${(firstAt - start) / 1_000_000} ms", (firstAt - start) / 1_000_000 >= 140)
    }

    @Test
    fun onDeliveredRunsOnlyForNotificationsTheSinkAccepted() {
        val queue = NotificationQueue()
        val acknowledged = CopyOnWriteArrayList<Int>()
        repeat(3) { i -> queue.enqueue(note(i)) { acknowledged += i } }

        // 1 numaralı bildirim reddedilir (ör. tepsi yok): onDelivered çağrılmaz, ötekiler sürer.
        runUntilDelivered(queue, 3) { it.message != "event 1" }

        assertEquals(listOf(0, 2), acknowledged.toList())
    }

    @Test
    fun aFailingSinkDoesNotStopThePump() {
        val queue = NotificationQueue()
        repeat(3) { queue.enqueue(note(it)) }

        val delivered = runUntilDelivered(queue, 3) { if (it.message == "event 0") error("boom") else true }

        assertEquals(listOf("event 0", "event 1", "event 2"), delivered)
    }
}
