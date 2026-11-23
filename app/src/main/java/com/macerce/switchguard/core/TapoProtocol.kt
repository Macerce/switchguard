package com.macerce.switchguard.core

import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * TP-Link KLAP yerel protokolü (Tapo P100/P105/P110/P115, L5xx ampuller; yeni donanımlı Kasa'lar).
 * Kimlik: TP-Link hesabının e-postası ve şifresi (cihaza yerel ağdan gönderilmez; yalnızca karması kullanılır).
 * El sıkışma: `POST /app/handshake1` (16 bayt istemci tohumu) → 16 bayt sunucu tohumu + 32 bayt karma (TP_SESSIONID
 * çerezi); `POST /app/handshake2` (istemci karması). Sonra her istek `POST /app/request?seq=N`, gövde =
 * imza (32) + AES-128-CBC şifreli JSON. v2 karması SHA256(SHA1(e-posta)+SHA1(şifre)), v1 MD5(MD5+MD5) (eski Kasa).
 * Saf kriptografi; ağ işi [com.macerce.switchguard.api.TapoLan]. Birim testli.
 */
object TapoProtocol {
    private fun sha256(vararg parts: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").run { parts.forEach(::update); digest() }
    private fun sha1(b: ByteArray) = MessageDigest.getInstance("SHA-1").digest(b)
    private fun md5(b: ByteArray) = MessageDigest.getInstance("MD5").digest(b)

    fun authHashV2(user: String, password: String): ByteArray =
        sha256(sha1(user.toByteArray(Charsets.UTF_8)) + sha1(password.toByteArray(Charsets.UTF_8)))

    fun authHashV1(user: String, password: String): ByteArray =
        md5(md5(user.toByteArray(Charsets.UTF_8)) + md5(password.toByteArray(Charsets.UTF_8)))

    /** Sunucu karmasını doğrular; uyan sürüm (2 ya da 1), uymazsa 0 (şifre yanlış). */
    fun verifyServer(local: ByteArray, remote: ByteArray, serverHash: ByteArray, user: String, password: String): Int = when {
        sha256(local, remote, authHashV2(user, password)).contentEquals(serverHash) -> 2
        sha256(local, authHashV1(user, password)).contentEquals(serverHash) -> 1
        // Hesapsız kurulmuş cihazlar boş kimlikle de el sıkışabilir.
        sha256(local, remote, authHashV2("", "")).contentEquals(serverHash) -> 3
        else -> 0
    }

    fun authFor(version: Int, user: String, password: String): ByteArray = when (version) {
        1 -> authHashV1(user, password)
        3 -> authHashV2("", "")
        else -> authHashV2(user, password)
    }

    fun handshake2Payload(version: Int, local: ByteArray, remote: ByteArray, auth: ByteArray): ByteArray =
        if (version == 1) sha256(remote, auth) else sha256(remote, local, auth)

    /** Oturum anahtarları ve sıra numarası (her istekte bir artar). */
    class Session(local: ByteArray, remote: ByteArray, auth: ByteArray, val cookie: String?) {
        private val key = sha256("lsk".toByteArray(), local, remote, auth).copyOf(16)
        private val ivFull = sha256("iv".toByteArray(), local, remote, auth)
        private val iv = ivFull.copyOf(12)
        private val sig = sha256("ldk".toByteArray(), local, remote, auth).copyOf(28)
        @Volatile var seq: Int = ByteBuffer.wrap(ivFull, 28, 4).int
            private set

        private fun ivFor(s: Int) = iv + ByteBuffer.allocate(4).putInt(s).array()

        /** Yeni sıra numarası ve gönderilecek gövde. */
        @Synchronized
        fun encrypt(json: String): Pair<Int, ByteArray> {
            seq += 1
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ivFor(seq)))
            val ct = c.doFinal(json.toByteArray(Charsets.UTF_8))
            val signature = sha256(sig, ByteBuffer.allocate(4).putInt(seq).array(), ct)
            return seq to (signature + ct)
        }

        fun decrypt(seq: Int, body: ByteArray): String {
            val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ivFor(seq)))
            return String(c.doFinal(body, 32, body.size - 32), Charsets.UTF_8)
        }
    }

    const val INFO = """{"method":"get_device_info"}"""
    const val POWER = """{"method":"get_current_power"}"""
    fun setOn(on: Boolean) = """{"method":"set_device_info","params":{"device_on":$on}}"""

    data class Info(val id: String, val model: String, val nickname: String?, val on: Boolean)

    fun idFromMac(mac: String) = "tapo-" + mac.filter { it.isLetterOrDigit() }.lowercase()

    fun parseInfo(json: String): Info? = runCatching {
        val o = JSONObject(json)
        if (o.optInt("error_code", -1) != 0) return null
        val r = o.getJSONObject("result")
        if (!r.has("device_on")) return null
        val nick = r.optString("nickname").takeIf { it.isNotBlank() }?.let { n ->
            runCatching { String(java.util.Base64.getDecoder().decode(n), Charsets.UTF_8) }.getOrDefault(n)
        }
        Info(idFromMac(r.optString("mac").ifEmpty { r.optString("device_id") }), r.optString("model"), nick, r.getBoolean("device_on"))
    }.getOrNull()

    /** `get_current_power` → W (desteklemeyen modelde null). */
    fun parsePower(json: String): Double? = runCatching {
        val o = JSONObject(json)
        if (o.optInt("error_code", -1) != 0) return null
        o.getJSONObject("result").getDouble("current_power")
    }.getOrNull()

    fun isOk(json: String): Boolean = runCatching { JSONObject(json).optInt("error_code", -1) == 0 }.getOrDefault(false)
}
