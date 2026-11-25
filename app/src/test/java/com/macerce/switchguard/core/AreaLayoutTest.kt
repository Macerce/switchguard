package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaLayoutTest {
    private val a = Category("a", "Mutfak")
    private val b = Category("b", "Garaj")
    private val c = Category("c", "Ofis")
    private val layout = DeviceLayout(categories = listOf(a, b, c))

    @Test
    fun orderCategoriesFollowsDragOrderAndKeepsUnknownAtEnd() {
        assertEquals(listOf("c", "a", "b"), layout.orderCategories(listOf("c", "a")).categories.map { it.id })
    }

    @Test
    fun iconOverrideWinsOverNameGuessAndSurvivesJson() {
        val l = layout.setAreaIcon("a", AreaIcon.GARDEN).toggleAreaLarge("b").toggleHidden("c").toggleHidden("dev1")
        val back = DeviceLayout.fromJson(l.toJson())
        assertEquals(AreaIcon.GARDEN, back.categories.first { it.id == "a" }.iconKind)
        assertEquals(AreaSummary.iconKind("Garaj"), back.categories.first { it.id == "b" }.iconKind)
        assertEquals(setOf("b"), back.areaLarge)
        assertEquals(setOf("c", "dev1"), back.hidden)
        assertNull(back.setAreaIcon("a", null).categories.first().icon)
    }

    @Test
    fun oldJsonWithoutNewFieldsStillLoads() {
        val old = """{"categories":[{"id":"a","name":"Mutfak"}],"order":[]}"""
        val l = DeviceLayout.fromJson(old)
        assertEquals("Mutfak", l.categories.single().name)
        assertTrue(l.hidden.isEmpty() && l.areaLarge.isEmpty())
    }

    @Test
    fun deletingAreaClearsItsFlags() {
        val l = layout.toggleAreaLarge("a").toggleHidden("a").deleteCategory("a")
        assertTrue("a" !in l.areaLarge && "a" !in l.hidden)
    }
}
