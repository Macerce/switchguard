package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * eWeLink yerel ağ (LAN) protokolü: cihaz kendini mDNS'te `_ewelink._tcp` olarak duyurur, durumu TXT kaydındadır
 * (`data1`…`data4`, `iv`, `seq`, `encrypt`). Komut, cihazın `http://ip:8081/zeroconf/switch(es)` adresine gider.
 * Şifreli kipte anahtar = MD5(devicekey), AES-128-CBC + PKCS7, iv rastgele 16 bayt (Base64).
 * devicekey cihaz listesinden (bulut) gelir; DIY kipindeki cihazlar şifresizdir.
 */
object LanProtocol {
    const val SERVICE_TYPE = "_ewelink._tcp"
    const val DEFAULT_PORT = 8081

    private val random = SecureRandom()

    private fun key(deviceKey: String): SecretKeySpec =
        SecretKeySpec(MessageDigest.getInstance("MD5").digest(deviceKey.toByteArray(Charsets.UTF_8)), "AES")

    fun encrypt(plain: String, deviceKey: String, iv: ByteArray): String {
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.ENCRYPT_MODE, key(deviceKey), IvParameterSpec(iv))
        return Base64.getEncoder().encodeToString(c.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(data: String, deviceKey: String, ivBase64: String): String {
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        c.init(Cipher.DECRYPT_MODE, key(deviceKey), IvParameterSpec(Base64.getDecoder().decode(ivBase64)))
        return String(c.doFinal(Base64.getDecoder().decode(data)), Charsets.UTF_8)
    }

    /** TXT kaydından okunan duyuru. [params] çözülemediyse (anahtar yok / yanlış) null. */
    data class Announce(val deviceId: String, val seq: Long, val params: JSONObject?)

    /**
     * TXT kaydını çözer. [txt]: anahtar → değer (UTF-8). [deviceKey]: şifreli cihazlar için; yoksa params null kalır.
     * Kimlik yoksa null döner.
     */
    fun parseTxt(txt: Map<String, String>, deviceKey: String?): Announce? {
        val id = txt["id"]?.takeIf { it.isNotBlank() } ?: return null
        val seq = txt["seq"]?.toLongOrNull() ?: 0L
        val data = (1..4).mapNotNull { txt["data$it"] }.joinToString("")
        if (data.isEmpty()) return Announce(id, seq, null)
        val encrypted = txt["encrypt"].equals("true", ignoreCase = true)
        val plain = when {
            !encrypted -> data
            deviceKey.isNullOrEmpty() -> return Announce(id, seq, null)
            else -> runCatching { decrypt(data, deviceKey, txt["iv"].orEmpty()) }.getOrNull() ?: return Announce(id, seq, null)
        }
        return Announce(id, seq, runCatching { JSONObject(plain) }.getOrNull())
    }

    /** Komut: yol (`/zeroconf/switch` ya da `/zeroconf/switches`) ve gövde. */
    data class Command(val path: String, val body: String)

    fun switchCommand(deviceId: String, deviceKey: String?, channel: Int, on: Boolean, multi: Boolean, now: Long = System.currentTimeMillis()): Command {
        val value = if (on) "on" else "off"
        val (path, data) = if (multi) {
            "/zeroconf/switches" to JSONObject().put("switches", JSONArray().put(JSONObject().put("switch", value).put("outlet", channel)))
        } else {
            "/zeroconf/switch" to JSONObject().put("switch", value)
        }
        val body = JSONObject().put("sequence", now.toString()).put("deviceid", deviceId).put("selfApikey", "123")
        if (deviceKey.isNullOrEmpty()) {
            body.put("data", data)
        } else {
            val iv = ByteArray(16).also { random.nextBytes(it) }
            body.put("iv", Base64.getEncoder().encodeToString(iv)).put("encrypt", true).put("data", encrypt(data.toString(), deviceKey, iv))
        }
        return Command(path, body.toString())
    }

    /** Cihazın yanıtı başarılı mı (`{"error":0}`). */
    fun isOk(response: String): Boolean = runCatching { JSONObject(response).optInt("error", -1) == 0 }.getOrDefault(false)
}
