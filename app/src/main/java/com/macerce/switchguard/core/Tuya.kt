package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Tuya Cloud (OpenAPI) imzası: HMAC-SHA256, büyük harf onaltılık. */
object TuyaSign {
    fun sha256Hex(body: String): String =
        MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }

    /**
     * [token] token alma isteğinde boştur. [path] sorgu dizesi dahil, anahtarları alfabetik sıralı olmalı.
     */
    fun sign(clientId: String, secret: String, token: String, t: String, nonce: String, method: String, body: String, path: String): String {
        val stringToSign = "$method\n${sha256Hex(body)}\n\n$path"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal((clientId + token + t + nonce + stringToSign).toByteArray())
            .joinToString("") { "%02X".format(it) }
    }

    /** Mesaj servisi (Pulsar) WebSocket parolası. */
    fun pulsarPassword(clientId: String, secret: String): String = md5(clientId + md5(secret)).substring(8, 24)

    private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * Tuya veri noktalarını (DP) [DeviceSnapshot]'a çevirir. Ölçek (scale) ürün tanımından gelir:
 * gerçek değer = ham / 10^scale. Tanım yoksa yaygın priz değerleri varsayılır.
 */
object TuyaParser {
    private val SWITCH_CODE = Regex("^switch_(\\d)$")
    private val DEFAULT_SCALES = mapOf("cur_power" to 1, "cur_voltage" to 1, "cur_current" to 0)

    /** Kanal no → DP kodu. "switch_1..8" → 0..7, yoksa "switch", o da yoksa lamba "switch_led". */
    fun switchCodes(codes: Collection<String>): Map<Int, String> {
        val numbered = codes.mapNotNull { c -> SWITCH_CODE.find(c)?.let { it.groupValues[1].toInt() - 1 to c } }
            .filter { it.first in 0..7 }.toMap()
        return when {
            numbered.isNotEmpty() -> numbered.toSortedMap()
            "switch" in codes -> mapOf(0 to "switch")
            "switch_led" in codes -> mapOf(0 to "switch_led")
            else -> emptyMap()
        }
    }

    /** Ürün tanımındaki ("specification") status listesinden DP kodu → scale. */
    fun scales(specification: JSONObject): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        val status = specification.optJSONArray("status") ?: return out
        for (i in 0 until status.length()) {
            val s = status.getJSONObject(i)
            val values = runCatching { JSONObject(s.optString("values", "{}")) }.getOrNull() ?: continue
            if (values.has("scale")) out[s.getString("code")] = values.getInt("scale")
        }
        return out
    }

    /**
     * [device]: /v2.0/cloud/thing/device listesindeki bir öğe; [status]: o cihazın {code, value} listesi.
     */
    fun snapshot(device: JSONObject, status: JSONArray, scales: Map<String, Int>): DeviceSnapshot {
        val values = (0 until status.length()).associate { i ->
            val s = status.getJSONObject(i)
            s.getString("code") to s.opt("value")
        }
        val codes = switchCodes(values.keys)
        val name = device.optString("customName").ifBlank { device.optString("name") }.ifBlank { device.getString("id") }
        val base = DeviceSnapshot(
            id = device.getString("id"),
            name = name,
            online = device.optBoolean("isOnline", false),
            switches = emptyMap(),
            cloud = Cloud.TUYA,
            switchCodes = codes,
        )
        return apply(base, values, scales)
    }

    /** Gelen DP değerlerini (tam liste ya da mesaj servisindeki değişenler) uygular. */
    fun apply(before: DeviceSnapshot, values: Map<String, Any?>, scales: Map<String, Int>): DeviceSnapshot {
        val switches = before.switches.toMutableMap()
        for ((channel, code) in before.switchCodes) {
            (values[code] as? Boolean)?.let { switches[channel] = it }
        }
        fun scaled(code: String): Double? {
            val raw = (values[code] as? Number)?.toDouble() ?: return null
            return raw / Math.pow(10.0, (scales[code] ?: DEFAULT_SCALES[code] ?: 0).toDouble())
        }
        return before.copy(
            switches = switches.toSortedMap(),
            power = scaled("cur_power") ?: before.power,
            voltage = scaled("cur_voltage") ?: before.voltage,
            // cur_current mA cinsinden gelir.
            current = scaled("cur_current")?.let { it / 1000.0 } ?: before.current,
        )
    }
}

/** Mesaj servisinden (Pulsar) gelen ve çözülmüş bir olay. */
sealed class TuyaEvent {
    abstract val deviceId: String
    /** Olayın Tuya'daki zamanı (ms); bağlantı kopukken biriken eski mesajları ayıklamak için. */
    abstract val time: Long
    data class Properties(override val deviceId: String, val values: Map<String, Any?>, override val time: Long = 0) : TuyaEvent()
    data class Online(override val deviceId: String, val online: Boolean, override val time: Long = 0) : TuyaEvent()
}

object TuyaMessages {
    /** Pulsar çerçevesinin kimliği (onaylamak için) ve çözülmüş olay (tanınmıyorsa null). */
    data class Frame(val messageId: String, val event: TuyaEvent?)

    fun decode(frame: String, secret: String): Frame {
        val msg = JSONObject(frame)
        val messageId = msg.getString("messageId")
        val outer = JSONObject(String(Base64.getDecoder().decode(msg.getString("payload"))))
        val mode = outer.optString("encryptMode").ifEmpty { msg.optJSONObject("properties")?.optString("em").orEmpty() }
        val inner = JSONObject(decrypt(outer.getString("data"), secret, mode))
        val data = inner.optJSONObject("bizData") ?: return Frame(messageId, null)
        val devId = data.optString("devId").ifEmpty { return Frame(messageId, null) }
        val time = inner.optLong("ts", data.optLong("time", 0))
        val event = when (inner.optString("bizCode")) {
            "devicePropertyMessage" -> {
                val props = data.optJSONArray("properties") ?: JSONArray()
                TuyaEvent.Properties(devId, (0 until props.length()).associate { i ->
                    val p = props.getJSONObject(i)
                    p.getString("code") to p.opt("value")
                }, time)
            }
            "deviceOnline" -> TuyaEvent.Online(devId, true, time)
            "deviceOffline" -> TuyaEvent.Online(devId, false, time)
            else -> null
        }
        return Frame(messageId, event)
    }

    /** Anahtar: Access Secret'ın 8..24 arası. AES-GCM'de ilk 12 bayt IV, sonda 16 baytlık etiket. */
    fun decrypt(data: String, secret: String, mode: String): String {
        val raw = Base64.getDecoder().decode(data)
        val key = SecretKeySpec(secret.substring(8, 24).toByteArray(), "AES")
        return if (mode.contains("gcm", ignoreCase = true)) {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw, 0, 12))
            String(c.doFinal(raw, 12, raw.size - 12))
        } else {
            val c = Cipher.getInstance("AES/ECB/PKCS5Padding")
            c.init(Cipher.DECRYPT_MODE, key)
            String(c.doFinal(raw))
        }
    }
}
