package com.macerce.switchguard.core

import com.macerce.switchguard.core.StandbyGrid.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StandbyGridTest {
    @Test
    fun parseFormatRoundTrip() {
        val s = StandbyGrid.State(listOf(Item("clock", 4, 2), Item("dev:a:b:1", 2, 1)), setOf("rules"))
        assertEquals(s, StandbyGrid.parse(StandbyGrid.format(s)))
        assertEquals(StandbyGrid.State(emptyList(), emptySet()), StandbyGrid.parse("CLOCK:0:100"))
        assertEquals("a:b" to 1, StandbyGrid.parseDevice("dev:a:b:1"))
        assertNull(StandbyGrid.parseDevice("sen:x"))
    }

    @Test
    fun visibleAddsNewAndSkipsRemovedAndMissing() {
        val state = StandbyGrid.State(listOf(Item("dev:gone:0", 2, 1), Item("weather", 2, 1)), setOf("rules"))
        val v = StandbyGrid.visible(state, listOf("clock", "weather", "rules", "dev:x:0"))
        assertEquals(listOf("weather", "clock", "dev:x:0"), v.map { it.id })
        // Kaydederken görünmeyen kayıtlı öğe korunur.
        val c = StandbyGrid.commit(state, v)
        assertEquals(listOf("weather", "clock", "dev:x:0", "dev:gone:0"), c.items.map { it.id })
    }

    @Test
    fun packFirstFit() {
        val p = StandbyGrid.pack(listOf(Item("clock", 4, 2), Item("a", 2, 1), Item("b", 2, 1), Item("c", 1, 1)), 4)
        assertEquals(listOf(0 to 0, 0 to 2, 2 to 2, 0 to 3), p.map { it.x to it.y })
        // Geniş ekranda yan yana dizilir; sütundan geniş öğe kırpılır.
        val q = StandbyGrid.pack(listOf(Item("clock", 4, 2), Item("a", 2, 1), Item("b", 2, 1), Item("big", 6, 1)), 8)
        assertEquals(listOf(0 to 0, 4 to 0, 6 to 0, 0 to 2), q.map { it.x to it.y })
        assertEquals(4, StandbyGrid.pack(listOf(Item("w", 6, 1)), 4).single().w)
        assertEquals("a", StandbyGrid.at(q, 5, 0)?.item?.id)
    }

    @Test
    fun moveAndResize() {
        val l = listOf(Item("a", 1, 1), Item("b", 1, 1), Item("c", 1, 1))
        assertEquals(listOf("c", "a", "b"), StandbyGrid.move(l, "c", "a").map { it.id })
        assertEquals(listOf("b", "c", "a"), StandbyGrid.moveToEnd(l, "a").map { it.id })
        val d = StandbyGrid.item("dev:x:0")
        assertEquals(4 to 2, d.w to d.h)
        val r = StandbyGrid.resize(listOf(d), d.id, 1, 99, columns = 8).single()
        assertEquals(2 to StandbyGrid.MAX_H, r.w to r.h)
        assertEquals(8, StandbyGrid.resize(listOf(d), d.id, 30, 3, columns = 8).single().w)
    }
}
