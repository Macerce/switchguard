package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLayoutTest {
    private fun d(id: String, name: String = id) = DeviceSnapshot(id, name, true, mapOf(0 to false))
    private val devices = listOf(d("a", "Priz"), d("b", "Rögar"), d("c", "Aydınlatma"), d("d", "Depo"))
    private fun ids(s: DeviceSection) = s.devices.map { it.id }

    @Test fun `duzen yokken ada gore tek basliksiz bolum`() {
        val s = DeviceLayout().sections(devices)
        assertEquals(1, s.size)
        assertEquals(DeviceSection.Kind.ALL, s[0].kind)
        assertEquals(listOf("c", "d", "a", "b"), ids(s[0]))
    }

    @Test fun `oncelikliler en ustte ve diger bolumde tekrar etmez`() {
        val s = DeviceLayout(order = listOf("a", "b", "c", "d"), priority = setOf("c")).sections(devices)
        assertEquals(DeviceSection.Kind.PRIORITY, s[0].kind)
        assertEquals(listOf("c"), ids(s[0]))
        assertEquals(DeviceSection.Kind.OTHER, s[1].kind)
        assertEquals(listOf("a", "b", "d"), ids(s[1]))
    }

    @Test fun `kategoriler sirasiyla, kategorisizler Diger'de, bos kategori baslikla kalir`() {
        val l = DeviceLayout(
            order = listOf("a", "b", "c", "d"),
            categories = listOf(Category("k1", "Rögarlar"), Category("k2", "Işıklar"), Category("k3", "Boş")),
            categoryOf = mapOf("b" to "k1", "c" to "k2", "x" to "k1"),
        )
        val s = l.sections(devices)
        assertEquals(listOf("k1", "k2", "k3", "OTHER"), s.map { it.key })
        assertEquals(listOf("b"), ids(s[0]))
        assertEquals(listOf("c"), ids(s[1]))
        assertTrue(s[2].devices.isEmpty())
        assertEquals(listOf("a", "d"), ids(s[3]))
    }

    @Test fun `tasima bolum icinde komsuyla yer degistirir, siniri asmaz`() {
        var l = DeviceLayout(order = listOf("a", "b", "c", "d"), categories = listOf(Category("k", "K")), categoryOf = mapOf("a" to "k", "c" to "k"))
        // "k" bölümü: a, c — c yukarı → c, a; aradaki b'ye dokunulmaz.
        l = l.move("c", -1, devices)
        assertEquals(listOf("c", "a"), ids(l.sections(devices)[0]))
        assertEquals(listOf("b", "d"), ids(l.sections(devices)[1]))
        assertEquals(l, l.move("c", -1, devices))
        assertEquals(l, l.move("a", +1, devices))
    }

    @Test fun `yeni cihaz sona eklenir, kategori silinince cihazlar kategorisiz kalir`() {
        val l = DeviceLayout(order = listOf("b", "a"), categories = listOf(Category("k", "K")), categoryOf = mapOf("a" to "k"))
        assertEquals(listOf("b", "a", "c", "d"), l.sorted(devices).map { it.id })
        val del = l.deleteCategory("k")
        assertTrue(del.categories.isEmpty())
        assertTrue(del.categoryOf.isEmpty())
    }

    @Test fun `kategori sirasi degisir`() {
        val l = DeviceLayout(categories = listOf(Category("1", "A"), Category("2", "B")))
        assertEquals(listOf("2", "1"), l.moveCategory("2", -1).categories.map { it.id })
        assertEquals(l, l.moveCategory("1", -1))
    }

    @Test fun `json gidis donus`() {
        val l = DeviceLayout(
            order = listOf("a", "b"), priority = setOf("a"), compact = setOf("b"),
            categories = listOf(Category("k", "Rögarlar")), categoryOf = mapOf("b" to "k"), collapsed = setOf("k"),
        )
        assertEquals(l, DeviceLayout.fromJson(l.toJson()))
        assertEquals(DeviceLayout(), DeviceLayout.fromJson(null))
        assertEquals(DeviceLayout(), DeviceLayout.fromJson("{bozuk"))
    }
}
