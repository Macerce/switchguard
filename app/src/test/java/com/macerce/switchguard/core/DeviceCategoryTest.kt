package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCategoryTest {

    private val silent = EventType.entries.fold(DeviceRules()) { r, t -> r.with(t, AlertAction.IGNORE) }

    @Test
    fun `alarm secilen kritik, yalniz bildirim onemli, digerleri normal`() {
        assertEquals(DeviceCategory.CRITICAL, DeviceCategory.auto(DeviceRules()))
        assertEquals(DeviceCategory.IMPORTANT, DeviceCategory.auto(silent.with(EventType.WENT_OFFLINE, AlertAction.NOTIFY)))
        assertEquals(DeviceCategory.NORMAL, DeviceCategory.auto(silent))
        assertEquals(DeviceCategory.NORMAL, DeviceCategory.auto(DeviceRules(monitored = false)))
    }

    @Test
    fun `elle secilen kategori otomatigi ezer`() {
        assertEquals(DeviceCategory.NORMAL, DeviceRules(category = DeviceCategory.NORMAL).effectiveCategory)
        assertEquals(DeviceCategory.CRITICAL, silent.copy(category = DeviceCategory.CRITICAL).effectiveCategory)
    }

    @Test
    fun `sessiz saat varsayilani eski davranisi korur`() {
        assertEquals(setOf(DeviceCategory.CRITICAL), DeviceCategory.quietDefault(quietEnabled = false, anyMarkedCritical = false))
        assertEquals(setOf(DeviceCategory.CRITICAL), DeviceCategory.quietDefault(quietEnabled = true, anyMarkedCritical = true))
        assertEquals(emptySet<DeviceCategory>(), DeviceCategory.quietDefault(quietEnabled = true, anyMarkedCritical = false))
        val set = setOf(DeviceCategory.NORMAL, DeviceCategory.CRITICAL)
        assertEquals(set, DeviceCategory.setFromString(DeviceCategory.setToString(set)))
        assertEquals(emptySet<DeviceCategory>(), DeviceCategory.setFromString(""))
    }

    @Test
    fun `kategori tetikleyicisi cihazlara ve kanallara acilir`() {
        val relay = DeviceSnapshot("r", "Röle", online = true, switches = mapOf(0 to true, 1 to false))
        val lamp = DeviceSnapshot("l", "Lamba", online = true, switches = mapOf(0 to true))
        val group = DeviceGroup("g", "Grup", listOf(GroupMember("r", 0))).snapshot(mapOf("r" to relay))
        val cats = mapOf("r" to DeviceCategory.CRITICAL, "l" to DeviceCategory.NORMAL)
        val off = Automation("a", "A", true, Trigger(TriggerKind.TURNED_OFF, DeviceCategory.CRITICAL.deviceId), AutoAction(ActionKind.ALARM))
        val offline = off.copy(id = "b", trigger = Trigger(TriggerKind.WENT_OFFLINE, DeviceCategory.CRITICAL.deviceId))
        val power = off.copy(id = "c", trigger = Trigger(TriggerKind.POWER_ABOVE, DeviceCategory.CRITICAL.deviceId, watts = 10.0))
        val plain = off.copy(id = "d", trigger = Trigger(TriggerKind.TURNED_OFF, "l"))

        val out = DeviceCategory.expand(listOf(off, offline, power, plain), listOf(relay, lamp, group)) { cats.getValue(it) }

        assertEquals(listOf("a@r/0", "a@r/1", "b@r", "d"), out.map { it.id })
        assertEquals(listOf("r" to 0, "r" to 1, "r" to 0, "l" to 0), out.map { it.trigger.deviceId to it.trigger.channel })
    }

    @Test
    fun `kategori tetikleyicisi motorda cihaz gibi calisir`() {
        val d = DeviceSnapshot("x", "Pompa", online = true, switches = mapOf(0 to true))
        val a = Automation("a", "A", true, Trigger(TriggerKind.TURNED_OFF, DeviceCategory.CRITICAL.deviceId), AutoAction(ActionKind.ALARM))
        val list = DeviceCategory.expand(listOf(a), listOf(d)) { DeviceCategory.CRITICAL }
        val engine = AutomationEngine()
        engine.onStates(emptyMap(), mapOf("x" to d), list, 0)
        val fired = engine.onStates(mapOf("x" to d), mapOf("x" to d.copy(switches = mapOf(0 to false))), list, 60_000)
        assertEquals(1, fired.size)
        assertTrue(fired.single().trigger.deviceId == "x")
    }

    @Test
    fun `kategori kimligi`() {
        assertEquals(DeviceCategory.IMPORTANT, DeviceCategory.fromId("category:IMPORTANT"))
        assertEquals(null, DeviceCategory.fromId("category:NOPE"))
        assertEquals(null, DeviceCategory.fromId("1000abc"))
        assertEquals(DeviceKind.CATEGORY, DeviceKinds.of(DeviceCategory.snapshots { it.name }.first()))
    }
}
