package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboundHooksTest {
    private val url = "https://hc-ping.com/1234-abcd"

    @Test
    fun heartbeatOnlyWhileHealthyAndAfterTheInterval() {
        assertTrue(Heartbeat.due(0, 1_000, 5, healthy = true, url = url))
        assertFalse(Heartbeat.due(0, 1_000, 5, healthy = false, url = url))
        assertFalse(Heartbeat.due(0, 1_000, 5, healthy = true, url = ""))
        assertFalse(Heartbeat.due(1_000, 1_000 + 4 * 60_000, 5, healthy = true, url = url))
        assertTrue(Heartbeat.due(1_000, 1_000 + 5 * 60_000, 5, healthy = true, url = url))
    }

    @Test
    fun onlyHttpAddressesAreAccepted() {
        assertTrue(Heartbeat.isValidUrl(url))
        assertTrue(Heartbeat.isValidUrl("http://192.168.1.10:3001/api/push/abc"))
        assertTrue(Heartbeat.isValidUrl("https://ntfy.sh/ev-alarmi-x7"))
        assertFalse(Heartbeat.isValidUrl("ftp://x.com"))
        assertFalse(Heartbeat.isValidUrl("hc-ping.com/1234"))
        assertFalse(Heartbeat.isValidUrl("https://"))
        assertFalse(Heartbeat.isValidUrl("https://a b.com"))
    }

    @Test
    fun ntfyHeadersUseHighestPriorityForAlarms() {
        val h = AlertHooks.ntfyHeaders(AlertHooks.Event.ALARM, "Mi Box")
        assertEquals("5", h["Priority"])
        assertEquals("SwitchGuard - Mi Box", h["Title"])
        assertEquals("3", AlertHooks.ntfyHeaders(AlertHooks.Event.DISMISSED, "")["Priority"])
        // Başlıklar ASCII olmalı: Türkçe harfler gövdede kalır.
        assertEquals("SwitchGuard - Salon TV'si ?", AlertHooks.headerSafe("SwitchGuard - Salon TV'si ✓"))
        assertEquals("Cocuk odasi Isik", AlertHooks.headerSafe("Çocuk odası Işık"))
    }

    @Test
    fun webhookBodyIsPlainJson() {
        val o = JSONObject(AlertHooks.webhookJson(AlertHooks.Event.ALARM, "Telefon", "Dondurucu: çevrimdışı", 0))
        assertEquals("alarm", o.getString("event"))
        assertEquals("SwitchGuard", o.getString("app"))
        assertEquals("Dondurucu: çevrimdışı", o.getString("text"))
        assertEquals("1970-01-01T00:00:00Z", o.getString("time"))
    }
}
