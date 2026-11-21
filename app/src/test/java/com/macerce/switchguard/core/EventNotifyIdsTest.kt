package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventNotifyIdsTest {
    @Test
    fun sameDeviceAndEventReuseTheSameId() {
        val a = EventNotifyIds.keyed("freezer/WENT_OFFLINE")

        assertEquals(a, EventNotifyIds.keyed("freezer/WENT_OFFLINE"))
        assertNotEquals(a, EventNotifyIds.keyed("freezer/CAME_ONLINE"))
        assertTrue(EventNotifyIds.isEventId(a))
    }

    @Test
    fun eventIdsNeverHitFixedIds() {
        // Durum, alarm, uyarı, özetler: 1–99.
        (1..99).forEach { assertFalse(EventNotifyIds.isEventId(it)) }
        listOf("", "x", "çok/uzun/anahtar/ğüşiöç", "a".repeat(500)).forEach { assertTrue(EventNotifyIds.isEventId(EventNotifyIds.keyed(it))) }
        listOf(0L, 1L, 99_999L, 1_790_000_000_123L, Long.MAX_VALUE).forEach { assertTrue(EventNotifyIds.isEventId(EventNotifyIds.rotating(it))) }
    }

    @Test
    fun sixtyFlapsLeaveAtMostTwentyEvents() {
        // 60 kez çevrimdışı/çevrimiçi; anahtarsız (dönen) kimliklerle en kötü durum.
        val active = mutableListOf<Pair<Int, Long>>()
        for (t in 1L..120L) {
            val id = EventNotifyIds.rotating(t * 1000)
            val cancel = EventNotifyIds.toCancel(active, id)
            active.removeAll { it.first in cancel }
            active.removeAll { it.first == id }
            active += id to t
            assertTrue(active.size <= EventNotifyIds.MAX_ACTIVE)
        }
        assertEquals(EventNotifyIds.MAX_ACTIVE, active.size)
        // Kalanlar en yenileri.
        assertEquals((101L..120L).toList(), active.map { it.second }.sorted())
    }

    @Test
    fun oldestAreCancelledFirstAndAlarmsAreNeverTouched() {
        val alarms = listOf(2 to 0L, 3 to 0L, 8 to 0L)
        val events = (1..20).map { EventNotifyIds.rotating(it.toLong()) to it.toLong() }

        val cancel = EventNotifyIds.toCancel(alarms + events, EventNotifyIds.rotating(999))

        assertEquals(listOf(events[0].first), cancel)
        assertTrue(cancel.none { it < 100 })
    }

    @Test
    fun replacingTheSameKeyDoesNotCancelAnything() {
        val key = EventNotifyIds.keyed("pump/TURNED_OFF")
        val events = (1..18).map { EventNotifyIds.rotating(it.toLong()) to it.toLong() } + (key to 50L)

        assertTrue(EventNotifyIds.toCancel(events, key).isEmpty())
    }
}
