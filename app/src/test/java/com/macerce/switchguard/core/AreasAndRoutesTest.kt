package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AreasAndRoutesTest {
    private fun dev(id: String, vararg sw: Boolean, online: Boolean = true, power: Double? = null, sensors: Map<SensorKind, Double> = emptyMap()) =
        DeviceSnapshot(id, id, online, sw.withIndex().associate { it.index to it.value }, power = power, sensors = sensors)

    private val layout = DeviceLayout(
        categories = listOf(Category("k", "Mutfak"), Category("s", "Salon")),
        categoryOf = mapOf("a" to "k", "b" to "k", "c" to "s", "t" to "k"),
    )

    @Test fun areaGroupsCoverEveryChannelOfAreaDevices() {
        val devices = listOf(dev("a", true), dev("b", false, true), dev("c", false), dev("x", true))
        val groups = DeviceGroup.areaGroups(layout, devices)
        assertEquals(2, groups.size)
        val k = groups.first { it.name == "Mutfak" }
        assertEquals(listOf(GroupMember("a", 0), GroupMember("b", 0), GroupMember("b", 1)), k.members)
        assertTrue(DeviceGroup.isArea(k.deviceId))
        assertTrue(DeviceGroup.isGroup(k.deviceId))
        assertEquals(DeviceGroup.areaDeviceId("k"), k.deviceId)
        assertFalse(DeviceGroup.isArea(DeviceGroup.PREFIX + "user1"))
        // Kapatmak için yalnız açık olanlar hedeflenir.
        assertEquals(listOf("a" to 0, "b" to 1), k.targets(devices.associateBy { it.id }, false).map { it.first.id to it.second })
    }

    @Test fun noAreasNoGroups() {
        assertTrue(DeviceGroup.areaGroups(DeviceLayout(), listOf(dev("a", true))).isEmpty())
    }

    @Test fun summaryCountsSwitchesPowerAndSensors() {
        val devices = listOf(
            dev("a", true, power = 100.0), dev("b", false, true, power = 40.0), dev("c", true),
            dev("t", sensors = mapOf(SensorKind.TEMPERATURE to 22.0, SensorKind.DOOR to 1.0)),
        )
        val s = AreaSummary.of("k", layout, devices)
        assertEquals(3, s.devices)
        assertEquals(2, s.switchesOn)
        assertEquals(3, s.switchesTotal)
        assertEquals(140.0, s.watts!!, 0.001)
        assertEquals(22.0, s.temperature!!, 0.001)
        assertEquals(1, s.doorsOpen)
        assertNull(s.humidity)
        val off = AreaSummary.of(listOf(dev("a", true, online = false)))
        assertEquals(0, off.switchesOn)
        assertEquals(1, off.offline)
    }

    @Test fun iconByName() {
        assertEquals(AreaIcon.KITCHEN, AreaSummary.iconKind("Mutfak"))
        assertEquals(AreaIcon.BED, AreaSummary.iconKind("Yatak odası"))
        assertEquals(AreaIcon.ROOM, AreaSummary.iconKind("Oda 12"))
    }

    @Test fun routeDecidesSingleStateSource() {
        // Otomatik: bulut canlıyken bulut, kopukken yerel ağ.
        assertFalse(RouteMerge.lanAuthoritative(ControlRoute.AUTO, lanSeen = true, cloudLive = true))
        assertTrue(RouteMerge.lanAuthoritative(ControlRoute.AUTO, lanSeen = true, cloudLive = false))
        // Yalnız yerel ağ: görüldüğü sürece her zaman yerel ağ.
        assertTrue(RouteMerge.lanAuthoritative(ControlRoute.LAN, lanSeen = true, cloudLive = true))
        assertFalse(RouteMerge.lanAuthoritative(ControlRoute.LAN, lanSeen = false, cloudLive = true))
        // Yalnız bulut: yerel ağ asla.
        assertFalse(RouteMerge.lanAuthoritative(ControlRoute.CLOUD, lanSeen = true, cloudLive = false))
        assertTrue(RouteMerge.acceptCloudSwitches(ControlRoute.CLOUD, true, false))
        assertFalse(RouteMerge.acceptCloudSwitches(ControlRoute.LAN, true, true))
    }

    @Test fun mergeKeepsLanSwitchesAndCloudPower() {
        val cloud = dev("a", false, true, online = false, power = 55.0)
        val merged = RouteMerge.apply(cloud, mapOf(0 to true, 1 to false, 7 to true), ControlRoute.LAN, cloudLive = true)
        assertEquals(mapOf(0 to true, 1 to false), merged.switches)
        assertTrue(merged.online)
        assertEquals(55.0, merged.power!!, 0.001)
        // Bulut canlı ve Otomatik: bulut olduğu gibi kalır.
        assertEquals(cloud, RouteMerge.apply(cloud, mapOf(0 to true), ControlRoute.AUTO, cloudLive = true))
        assertEquals(cloud, RouteMerge.apply(cloud, null, ControlRoute.LAN, cloudLive = false))
    }

    @Test fun routeNames() {
        assertEquals(ControlRoute.LAN, ControlRoute.from("LAN"))
        assertNull(ControlRoute.from("x"))
    }
}
