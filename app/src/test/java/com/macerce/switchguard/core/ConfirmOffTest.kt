package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmOffTest {
    private val freezer = DeviceSnapshot("f", "Freezer", online = true, switches = mapOf(0 to true))
    private val lamp = DeviceSnapshot("l", "Lamp", online = true, switches = mapOf(0 to true))
    private val byId = listOf(freezer, lamp).associateBy { it.id }
    private val group = DeviceGroup("g", "Kitchen", listOf(GroupMember("f", 0), GroupMember("l", 0)))
    private val ids = setOf("f")

    @Test
    fun onlyTurningOffAsks() {
        assertTrue(ConfirmOff.needed("f", false, ids, emptyList(), byId))
        assertFalse(ConfirmOff.needed("f", true, ids, emptyList(), byId))
        assertFalse(ConfirmOff.needed("l", false, ids, emptyList(), byId))
    }

    @Test
    fun groupAsksWhenAConfirmedMemberWouldTurnOff() {
        assertTrue(ConfirmOff.needed(group.deviceId, false, ids, listOf(group), byId))
        val freezerOff = byId + ("f" to freezer.copy(switches = mapOf(0 to false)))
        assertFalse(ConfirmOff.needed(group.deviceId, false, ids, listOf(group), freezerOff))
    }
}
