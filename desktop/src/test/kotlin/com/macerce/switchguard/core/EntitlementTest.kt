package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EntitlementTest {
    private val ids = listOf("a", "b", "c")
    private val all: (String) -> Boolean = { true }

    @Test
    fun `pro kurali acik tum cihazlari izler`() {
        assertEquals(setOf("a", "c"), Entitlement.monitored(ids, { it != "b" }, pro = true, freeId = null))
    }

    @Test
    fun `ucretsizde secim yoksa ilk cihaz izlenir`() {
        assertEquals(setOf("a"), Entitlement.monitored(ids, all, pro = false, freeId = null))
    }

    @Test
    fun `ucretsizde secilen cihaz izlenir`() {
        assertEquals(setOf("c"), Entitlement.monitored(ids, all, pro = false, freeId = "c"))
    }

    @Test
    fun `secilen cihaz izlenmek istenmiyorsa siradakine gecer`() {
        assertEquals(setOf("b"), Entitlement.monitored(ids, { it != "a" }, pro = false, freeId = "a"))
    }

    @Test
    fun `secilen cihaz hesaptan kalkmissa ilk cihaz izlenir`() {
        assertEquals(setOf("a"), Entitlement.monitored(ids, all, pro = false, freeId = "silinmis"))
    }

    @Test
    fun `hicbiri izlenmek istenmiyorsa bos`() {
        assertEquals(emptySet<String>(), Entitlement.monitored(ids, { false }, pro = false, freeId = "a"))
    }
}
