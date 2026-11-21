package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetGuardTest {
    private val group = DeviceGroup("g1", "Bahçe", listOf(GroupMember("a", 0), GroupMember("b", 2)))

    @Test
    fun onlyAddedWidgetIdsAreAccepted() {
        val own = intArrayOf(12, 40)

        assertTrue(WidgetGuard.isOwnWidget(12, own))
        assertFalse(WidgetGuard.isOwnWidget(13, own))
        assertFalse(WidgetGuard.isOwnWidget(WidgetGuard.INVALID_ID, own))
        assertFalse(WidgetGuard.isOwnWidget(5, intArrayOf()))
    }

    @Test
    fun groupWidgetOnlyTogglesItsOwnMembers() {
        assertTrue(WidgetGuard.isGroupMember(group, "a", 0))
        assertTrue(WidgetGuard.isGroupMember(group, "b", 2))
        // Başka cihaz, aynı cihazın başka kanalı ve bağlı grubu olmayan widget reddedilir.
        assertFalse(WidgetGuard.isGroupMember(group, "freezer", 0))
        assertFalse(WidgetGuard.isGroupMember(group, "b", 0))
        assertFalse(WidgetGuard.isGroupMember(null, "a", 0))
    }

    @Test
    fun configScreenOnlyForOwnProvider() {
        val own = "com.macerce.switchguard"
        val cls = "com.macerce.switchguard.service.DeviceWidget"

        assertTrue(WidgetGuard.isOwnProvider(cls, own, own, cls))
        assertFalse(WidgetGuard.isOwnProvider(cls, "com.evil", own, cls))
        assertFalse(WidgetGuard.isOwnProvider("com.macerce.switchguard.service.GroupWidget", own, own, cls))
        assertFalse(WidgetGuard.isOwnProvider(null, null, own, cls))
    }
}
