package com.macerce.switchguard.core

import com.macerce.switchguard.core.StandbyGrid.Item
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StandbyGridTest {
    @Test
    fun parseFormatRoundTrip() {
        val s = StandbyGrid.State(listOf(Item("clock", 4, 2, 3, 1), Item("dev:a:b:1", 2, 1)), setOf("rules"))
        assertEquals(s, StandbyGrid.parse(StandbyGrid.format(s)))
        // Konumsuz eski kayıt da okunur.
        assertEquals(Item("clock", 4, 2), StandbyGrid.parse("clock=4x2|").items.single())
        assertEquals(StandbyGrid.State(emptyList(), emptySet()), StandbyGrid.parse("CLOCK:0:100"))
        assertEquals("a:b" to 1, StandbyGrid.parseDevice("dev:a:b:1"))
        assertNull(StandbyGrid.parseSensor("dev:x:0"))
    }

    @Test
    fun visibleAddsNewAndSkipsRemovedAndMissing() {
        val state = StandbyGrid.State(listOf(Item("dev:gone:0", 2, 1), Item("weather", 2, 1)), setOf("rules"))
        val v = StandbyGrid.visible(state, listOf("clock", "weather", "rules", "dev:x:0"))
        assertEquals(listOf("weather", "clock", "dev:x:0"), v.map { it.id })
        val c = StandbyGrid.commit(state, v)
        assertEquals(listOf("weather", "clock", "dev:x:0", "dev:gone:0"), c.items.map { it.id })
    }

    @Test
    fun packKeepsPositionsAndFillsNewOnes() {
        // Konumlu öğe yerinde kalır, aradaki boşluk korunur; konumsuz öğe ilk boş yere gelir.
        val p = StandbyGrid.pack(listOf(Item("a", 2, 1, 4, 3), Item("b", 2, 1), Item("c", 2, 1)), 8)
        assertEquals(listOf(4 to 3, 0 to 0, 2 to 0), p.map { it.x to it.y })
        // Dar ekranda sığmayan öğe yer arar, sütundan geniş olan kırpılır.
        val q = StandbyGrid.pack(listOf(Item("a", 2, 1, 7, 0), Item("w", 10, 1)), 8)
        assertEquals(listOf(0 to 0, 0 to 1), q.map { it.x to it.y })
        assertEquals(8, q[1].w)
        // Çakışan iki kayıt: ikincisi yer arar.
        val r = StandbyGrid.pack(listOf(Item("a", 2, 2, 0, 0), Item("b", 2, 2, 1, 1)), 8)
        assertEquals(listOf(0 to 0, 2 to 0), r.map { it.x to it.y })
    }

    @Test
    fun placeOnlyOnFreeSpotAndOthersStay() {
        val placed = StandbyGrid.pack(listOf(Item("a", 2, 1), Item("b", 2, 1), Item("c", 2, 1)), 8)
        // b'yi a'nın üstüne koymak olmaz.
        assertNull(StandbyGrid.place(placed, "b", 1, 0, 2, 1, 8))
        assertNull(StandbyGrid.place(placed, "b", 7, 0, 2, 1, 8))
        // Boş yere: diğerleri yerinde kalır, hepsi sabitlenir.
        val moved = StandbyGrid.place(placed, "b", 0, 3, 2, 1, 8)!!
        assertEquals(listOf(0 to 0, 0 to 3, 4 to 0), moved.map { it.x to it.y })
        // Kendi alanına dokunarak büyüme serbest.
        val grown = StandbyGrid.place(placed, "a", 0, 0, 2, 3, 8)!!
        assertEquals(3, grown.first().h)
        assertEquals(2 to 2, StandbyGrid.clampSize("dev:x:0", 1, 1, 8))
        assertEquals(8 to StandbyGrid.MAX_H, StandbyGrid.clampSize("dev:x:0", 30, 99, 8))
    }

    @Test
    fun removingDoesNotShiftOthers() {
        val placed = StandbyGrid.pack(listOf(Item("a", 2, 1), Item("b", 2, 1), Item("c", 2, 1)), 8)
        val after = StandbyGrid.freeze(placed).filter { it.id != "a" }
        assertEquals(listOf(2 to 0, 4 to 0), StandbyGrid.pack(after, 8).map { it.x to it.y })
    }

    @Test
    fun moveSwapsLikeTheApp() {
        // a b c yan yana (her biri 4x2, 8 sütun: a 0,0  b 4,0  c 0,2).
        val placed = StandbyGrid.pack(listOf(Item("a", 4, 2), Item("b", 4, 2), Item("c", 4, 2)), 8)
        // a'yı b'nin üstüne bırak: takas.
        val swapped = StandbyGrid.pack(StandbyGrid.move(placed, "a", 4, 0, 8)!!, 8).associate { it.item.id to (it.x to it.y) }
        assertEquals(4 to 0, swapped["a"]); assertEquals(0 to 0, swapped["b"]); assertEquals(0 to 2, swapped["c"])
        // Boş yere bırakmak düz taşımadır; diğerleri kımıldamaz.
        val free = StandbyGrid.pack(StandbyGrid.move(placed, "c", 4, 2, 8)!!, 8).associate { it.item.id to (it.x to it.y) }
        assertEquals(4 to 2, free["c"]); assertEquals(0 to 0, free["a"])
        // Ekran dışı / aynı yer: değişiklik yok.
        assertNull(StandbyGrid.move(placed, "a", 6, 0, 8))
        assertNull(StandbyGrid.move(placed, "a", 0, 0, 8))
    }

    @Test
    fun moveBigOntoSmallPushesToFreeSpot() {
        // Saat 8x4 üstte, iki cihaz 4x2 altta. Cihazı saatin üstüne bırak: saat cihazın eski yerine sığmaz, ilk boş yere iner.
        val placed = StandbyGrid.pack(listOf(Item("clock", 8, 4), Item("d1", 4, 2), Item("d2", 4, 2)), 8)
        val r = StandbyGrid.pack(StandbyGrid.move(placed, "d1", 0, 0, 8)!!, 8)
        val m = r.associate { it.item.id to (it.x to it.y) }
        assertEquals(0 to 0, m["d1"])
        // Çakışma yok.
        for (a in r) for (b in r) if (a !== b) assert(!(a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h)) { "${a.item.id}/${b.item.id}" }
    }
}
