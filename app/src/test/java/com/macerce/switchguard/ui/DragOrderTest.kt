package com.macerce.switchguard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragOrderTest {
    private val order = DragOrder()

    @Test
    fun moveReordersById() {
        order.begin(listOf("a", "b", "c"))

        order.move("a", "c")

        assertEquals(listOf("b", "c", "a"), order.ids)
    }

    @Test
    fun endReturnsTheNewOrderOnce() {
        order.begin(listOf("a", "b", "c"))
        order.move("c", "a")

        assertEquals(listOf("c", "a", "b"), order.end())
        assertNull(order.end())
    }

    @Test
    fun noMoveEndsWithNull() {
        order.begin(listOf("a", "b"))
        order.move("a", "b")
        order.move("a", "b") // geri aldı

        assertNull(order.end())
    }

    @Test
    fun listChangesWhileDraggingKeepTheLocalOrder() {
        order.begin(listOf("a", "b", "c"))
        order.move("c", "a") // c, a, b

        // Motor bu arada listeyi değiştirdi: b gitti, d geldi (sıra ayarlardaki eski sıra).
        order.show(listOf("a", "c", "d"))

        assertEquals(listOf("c", "a", "d"), order.ids)
    }

    @Test
    fun displayedFollowsTheStoreWhenIdleAndTheDragWhileDragging() {
        assertEquals(listOf("x", "y"), order.displayed(listOf("x", "y")))
        order.show(listOf("y", "x")) // sürükleme yokken yok sayılır

        order.begin(listOf("x", "y"))
        order.move("x", "y")

        assertEquals(listOf("y", "x"), order.displayed(listOf("x", "y")))
        order.end()
        assertEquals(listOf("x", "y"), order.displayed(listOf("x", "y")))
    }
}
