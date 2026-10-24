package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceGroupTest {
    private val lamp = DeviceSnapshot("a", "Lamba", online = true, switches = mapOf(0 to false))
    private val relay = DeviceSnapshot("b", "Pano", online = true, switches = mapOf(0 to true, 1 to false, 2 to false, 3 to false))
    private val offline = DeviceSnapshot("c", "Garaj", online = false, switches = mapOf(0 to true))
    private val byId = listOf(lamp, relay, offline).associateBy { it.id }
    private val group = DeviceGroup(
        "g1", "Bahçe",
        listOf(GroupMember("a", 0), GroupMember("b", 0), GroupMember("b", 1), GroupMember("c", 0), GroupMember("gone", 0)),
    )

    @Test
    fun snapshotIsOnWhenAnyOnlineMemberIsOn() {
        val s = group.snapshot(byId)

        assertEquals("group:g1", s.id)
        assertEquals("Bahçe", s.name)
        assertTrue(s.online)
        assertEquals(mapOf(0 to true), s.switches)
        assertFalse(s.isMultiChannel)
    }

    @Test
    fun offlineMembersDoNotCountAsOn() {
        val s = DeviceGroup("g", "x", listOf(GroupMember("a", 0), GroupMember("c", 0))).snapshot(byId)

        assertTrue(s.online)
        assertEquals(mapOf(0 to false), s.switches)
    }

    @Test
    fun allMembersOfflineMeansOffline() {
        assertFalse(DeviceGroup("g", "x", listOf(GroupMember("c", 0))).snapshot(byId).online)
    }

    @Test
    fun countsSkipMissingMembers() {
        assertEquals(1 to 4, group.counts(byId))
    }

    @Test
    fun targetsAreOnlineMembersThatMustChange() {
        val on = group.targets(byId, on = true).map { it.first.id to it.second }
        val off = group.targets(byId, on = false).map { it.first.id to it.second }

        assertEquals(listOf("a" to 0, "b" to 1), on)
        assertEquals(listOf("b" to 0), off)
    }

    @Test
    fun withGroupsAppendsVirtualDevices() {
        val list = DeviceGroup.withGroups(listOf(lamp), listOf(DeviceGroup("g", "x", listOf(GroupMember("a", 0)))))

        assertEquals(listOf("a", "group:g"), list.map { it.id })
        assertTrue(DeviceGroup.isGroup(list[1].id))
        assertEquals(DeviceKind.GROUP, DeviceKinds.of(list[1]))
    }

    @Test
    fun jsonRoundTrip() {
        val back = DeviceGroup.listFromJson(DeviceGroup.listToJson(listOf(group)))

        assertEquals(listOf(group), back)
        assertEquals(emptyList<DeviceGroup>(), DeviceGroup.listFromJson("bozuk"))
    }
}
