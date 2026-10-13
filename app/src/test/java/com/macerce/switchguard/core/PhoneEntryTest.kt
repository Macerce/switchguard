package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneEntryTest {
    private val key = ByteArray(32) { it.toByte() }
    private val nonce = ByteArray(16) { (100 + it).toByte() }
    private val secret = "ŞğÜ-secret-" + "x".repeat(70)

    /** assets/phone_entry.html içindeki JavaScript'in node ile ürettiği çıktı (key 0..31, nonce 100..115). */
    private val fromBrowser = """{"n":"ZGVmZ2hpamtsbW5vcHFycw","c":"NNlnYChxQUFX0hnX9ZIXHwqHhwvHj79wyLgwRrNd9lcmxtvNnQITiJvle4Af6ROqkgAtj6qaHffg1yq_48KksC-alsyxfbZCuZF8bZ6B3XlBObgg9H8A5UlXu8f-y2sc5djYtKZ3r80fqShIpKAieU0qobat","t":"j2BXRLA4CGTEqPZGkJSeSBz6P5MnUJ-IQuCnV8Teb5c"}"""

    @Test
    fun `tarayicinin sifreledigi TV'de acilir`() =
        assertEquals(mapOf("appId" to "abc123", "appSecret" to secret), PhoneEntry.open(key, fromBrowser))

    @Test
    fun `gidip gelir`() {
        val values = mapOf("token" to "123456:ABC", "name" to "Çağrı")
        assertEquals(values, PhoneEntry.open(key, PhoneEntry.seal(key, nonce, values)))
    }

    @Test
    fun `degistirilmis veri ya da yanlis anahtar reddedilir`() {
        val msg = JSONObject(fromBrowser)
        val c = msg.getString("c")
        val flipped = msg.put("c", (if (c[0] == 'A') 'B' else 'A') + c.substring(1)).toString()
        assertNull(PhoneEntry.open(key, flipped))
        assertNull(PhoneEntry.open(ByteArray(32), fromBrowser))
        assertNull(PhoneEntry.open(key, "{}"))
        assertNull(PhoneEntry.open(key, "not json"))
    }

    @Test
    fun `adres anahtari yalnizca diyez sonrasinda tasir`() {
        val url = PhoneEntry.url("192.168.1.20", 41234, "tok", key)
        assertTrue(url.startsWith("http://192.168.1.20:41234/e/tok#"))
        assertFalse(url.substringBefore('#').contains(url.substringAfter('#')))
    }

    @Test
    fun `sayfaya gomulen ayar script blogunu kapatamaz ve gizli alan doldurulmaz`() {
        val config = PhoneEntry.config(
            "</script><b>",
            listOf(PhoneEntry.Field("id", "App ID", value = "abc"), PhoneEntry.Field("secret", "Secret", secret = true, value = "hidden")),
            mapOf("send" to "Gönder"),
        )
        val page = PhoneEntry.page("<script>var CONFIG = /*CONFIG*/null;</script>", config)
        assertEquals(1, Regex("</script>").findAll(page).count())
        assertFalse(page.contains("hidden"))
        assertTrue(page.contains("abc"))
    }
}
