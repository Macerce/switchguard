package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class LanProtocolTest {
    private val key = "0a1b2c3d-1111-2222-3333-444455556666"

    @Test fun encryptDecryptRoundTrip() {
        val iv = ByteArray(16) { it.toByte() }
        val enc = LanProtocol.encrypt("""{"switch":"on"}""", key, iv)
        assertEquals("""{"switch":"on"}""", LanProtocol.decrypt(enc, key, Base64.getEncoder().encodeToString(iv)))
    }

    @Test fun parsesEncryptedTxtSplitAcrossDataFields() {
        val iv = ByteArray(16) { (it * 3).toByte() }
        val plain = """{"switches":[{"switch":"on","outlet":0},{"switch":"off","outlet":1}],"configure":[]}"""
        val enc = LanProtocol.encrypt(plain, key, iv)
        val txt = mapOf(
            "id" to "1000abcdef", "seq" to "17", "encrypt" to "true", "iv" to Base64.getEncoder().encodeToString(iv),
            "data1" to enc.take(20), "data2" to enc.drop(20),
        )
        val a = LanProtocol.parseTxt(txt, key)!!
        assertEquals("1000abcdef", a.deviceId)
        assertEquals(17L, a.seq)
        assertEquals(mapOf(0 to true, 1 to false), DeviceParser.parseSwitches(a.params))
    }

    @Test fun encryptedWithoutKeyHasNoParams() {
        val a = LanProtocol.parseTxt(mapOf("id" to "x", "encrypt" to "true", "iv" to "AAAAAAAAAAAAAAAAAAAAAA==", "data1" to "abc"), null)!!
        assertNull(a.params)
    }

    @Test fun wrongKeyHasNoParams() {
        val iv = ByteArray(16)
        val enc = LanProtocol.encrypt("""{"switch":"on"}""", key, iv)
        val txt = mapOf("id" to "x", "encrypt" to "true", "iv" to Base64.getEncoder().encodeToString(iv), "data1" to enc)
        val params = LanProtocol.parseTxt(txt, "another-key")!!.params
        // Yanlış anahtar ya dolgu hatası verir ya da JSON olmayan çöp üretir.
        assertTrue(params == null || DeviceParser.parseSwitches(params).isEmpty())
    }

    @Test fun plainDiyTxt() {
        val a = LanProtocol.parseTxt(mapOf("id" to "d1", "data1" to """{"switch":"off"}"""), null)!!
        assertEquals(mapOf(0 to false), DeviceParser.parseSwitches(a.params))
    }

    @Test fun missingIdIsNull() = assertNull(LanProtocol.parseTxt(mapOf("data1" to "{}"), null))

    @Test fun encryptedSwitchCommandDecryptsBack() {
        val cmd = LanProtocol.switchCommand("d1", key, 2, on = true, multi = true, now = 5)
        assertEquals("/zeroconf/switches", cmd.path)
        val body = JSONObject(cmd.body)
        assertEquals("d1", body.getString("deviceid"))
        assertEquals("5", body.getString("sequence"))
        assertTrue(body.getBoolean("encrypt"))
        val data = JSONObject(LanProtocol.decrypt(body.getString("data"), key, body.getString("iv")))
        assertEquals(mapOf(2 to true), DeviceParser.parseSwitches(data))
    }

    @Test fun plainSingleSwitchCommand() {
        val cmd = LanProtocol.switchCommand("d1", null, 0, on = false, multi = false)
        assertEquals("/zeroconf/switch", cmd.path)
        val body = JSONObject(cmd.body)
        assertFalse(body.has("iv"))
        assertEquals("off", body.getJSONObject("data").getString("switch"))
    }

    @Test fun responseOk() {
        assertTrue(LanProtocol.isOk("""{"seq":3,"error":0}"""))
        assertFalse(LanProtocol.isOk("""{"error":400}"""))
        assertFalse(LanProtocol.isOk("not json"))
        assertNotNull(LanProtocol.switchCommand("d", key, 0, true, false).body)
    }
}
