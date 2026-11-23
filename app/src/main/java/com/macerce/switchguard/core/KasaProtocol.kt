package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * TP-Link Kasa eski yerel protokolü (HS100/HS103/HS110/KP105/KP115/HS300/KP303, KL ampuller ...):
 * TCP 9999, gövde = 4 bayt uzunluk + "XOR otomatik anahtar" (ilk anahtar 171, her şifreli bayt bir sonrakinin
 * anahtarı). Keşif aynı şifreyle (uzunluksuz) UDP 9999 yayınıdır. Yeni donanımlı bazı Kasa'lar KLAP kullanır
 * (bkz. [TapoProtocol]); onlar Tapo gibi hesapla eklenir.
 * - `{"system":{"get_sysinfo":{}}}` → `alias`, `model`, `mac`/`mic_mac`, `relay_state`, çoklu prizde `children[]`
 *   (`id`, `state`, `alias`), ampulde `light_state.on_off`.
 * - Röle: `{"system":{"set_relay_state":{"state":1}}}`, çoklu prizde `context.child_ids`.
 * - Güç: `{"emeter":{"get_realtime":{}}}` → `power_mw` (yeni) ya da `power` (eski, W).
 */
object KasaProtocol {
    data class Info(val id: String, val model: String, val alias: String?, val children: List<String>, val bulb: Boolean)

    fun encrypt(json: String, withLength: Boolean = true): ByteArray {
        val plain = json.toByteArray(Charsets.UTF_8)
        val out = ByteArray(plain.size + if (withLength) 4 else 0)
        var o = 0
        if (withLength) {
            val n = plain.size
            out[0] = (n ushr 24).toByte(); out[1] = (n ushr 16).toByte(); out[2] = (n ushr 8).toByte(); out[3] = n.toByte()
            o = 4
        }
        var key = 171
        for (i in plain.indices) {
            val c = (plain[i].toInt() and 0xff) xor key
            key = c
            out[o + i] = c.toByte()
        }
        return out
    }

    /** Uzunluk başlığı olmadan şifreli bayt → metin. */
    fun decrypt(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): String {
        val out = ByteArray(length)
        var key = 171
        for (i in 0 until length) {
            val c = data[offset + i].toInt() and 0xff
            out[i] = (c xor key).toByte()
            key = c
        }
        return String(out, Charsets.UTF_8)
    }

    const val QUERY = """{"system":{"get_sysinfo":{}},"emeter":{"get_realtime":{}}}"""
    const val DISCOVER = """{"system":{"get_sysinfo":{}}}"""

    private fun sysinfo(json: String): JSONObject? = runCatching {
        JSONObject(json).getJSONObject("system").getJSONObject("get_sysinfo")
    }.getOrNull()

    fun idFromMac(mac: String) = "kasa-" + mac.filter { it.isLetterOrDigit() }.lowercase()

    fun parseInfo(json: String): Info? {
        val s = sysinfo(json) ?: return null
        val mac = (s.optString("mac").ifEmpty { s.optString("mic_mac") }).takeIf { it.isNotBlank() } ?: return null
        val kids = s.optJSONArray("children")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optString("id") } }.orEmpty()
        val bulb = s.has("light_state")
        if (!bulb && !s.has("relay_state") && kids.isEmpty()) return null
        return Info(idFromMac(mac), s.optString("model").substringBefore('('), s.optString("alias").takeIf { it.isNotBlank() }, kids.take(4), bulb)
    }

    fun extraFor(i: Info): String = JSONObject().put("c", JSONArray(i.children)).put("b", i.bulb).toString()
    fun children(extra: String): List<String> = runCatching {
        val a = JSONObject(extra).optJSONArray("c") ?: return emptyList()
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())
    fun isBulb(extra: String): Boolean = runCatching { JSONObject(extra).optBoolean("b") }.getOrDefault(false)

    fun parseStatus(json: String, channels: Int): LocalReading? {
        val s = sysinfo(json) ?: return null
        val sw = LinkedHashMap<Int, Boolean>()
        val kids = s.optJSONArray("children")
        when {
            kids != null && kids.length() > 0 -> for (i in 0 until minOf(kids.length(), channels.coerceIn(1, 4))) sw[i] = kids.getJSONObject(i).optInt("state") == 1
            s.has("light_state") -> sw[0] = s.getJSONObject("light_state").optInt("on_off") == 1
            s.has("relay_state") -> sw[0] = s.optInt("relay_state") == 1
        }
        if (sw.isEmpty()) return null
        val rt = runCatching { JSONObject(json).getJSONObject("emeter").getJSONObject("get_realtime") }.getOrNull()
            ?.takeIf { it.optInt("err_code", 0) == 0 }
        fun v(milli: String, unit: String): Double? = when {
            rt == null -> null
            rt.has(milli) -> rt.getDouble(milli) / 1000.0
            rt.has(unit) -> rt.getDouble(unit)
            else -> null
        }
        return LocalReading(sw, power = v("power_mw", "power"), voltage = v("voltage_mv", "voltage"), current = v("current_ma", "current"))
    }

    fun switchCommand(on: Boolean, childId: String?, bulb: Boolean): String {
        val v = if (on) 1 else 0
        if (bulb) return """{"smartlife.iot.smartbulb.lightingservice":{"transition_light_state":{"on_off":$v,"transition_period":0}}}"""
        val ctx = childId?.let { """"context":{"child_ids":["$it"]},""" }.orEmpty()
        return """{$ctx"system":{"set_relay_state":{"state":$v}}}"""
    }

    /** Komut yanıtında hata kodu 0 mı. */
    fun isOk(json: String): Boolean = runCatching {
        val o = JSONObject(json)
        val inner = o.optJSONObject("system")?.optJSONObject("set_relay_state")
            ?: o.optJSONObject("smartlife.iot.smartbulb.lightingservice")?.optJSONObject("transition_light_state")
            ?: return false
        inner.optInt("err_code", 0) == 0
    }.getOrDefault(false)
}
