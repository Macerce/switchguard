package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Tasmota yerel HTTP API'si: `GET /cm?cmnd=<komut>` (web şifresi varsa `&user=admin&password=..`).
 * - `Status 0`: `Status` (DeviceName, FriendlyName[]), `StatusNET.Mac`, `StatusSTS.POWER` / `POWER1..8`,
 *   `StatusSNS.ENERGY` (Power, Voltage, Current; çok kanallıda dizi).
 * - Komut `Power<N> On|Off` → `{"POWER<N>":"ON"}` (tek kanallıda `POWER`).
 * - Şifre yanlış/eksik: HTTP 401 ya da `{"WARNING":"Need user=<username>&password=<password>"}`.
 * Yalnızca ayrıştırma ve adresler (ağ işi [com.macerce.switchguard.api.TasmotaLan]); birim testli.
 */
object TasmotaProtocol {
    data class Info(val id: String, val model: String, val name: String?, val channels: Int)

    fun idFromMac(mac: String): String = "tasmota-" + mac.filter { it.isLetterOrDigit() }.lowercase()

    fun path(cmnd: String, cred: ShellyAuth.Credentials?): String {
        fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")
        val auth = cred?.let { "user=${enc(it.user)}&password=${enc(it.password)}&" }.orEmpty()
        return "/cm?${auth}cmnd=${enc(cmnd)}"
    }

    fun statusPath(cred: ShellyAuth.Credentials?) = path("Status 0", cred)

    /** Tasmota numaraları 1'den başlar; SwitchGuard kanalları 0'dan. */
    fun switchPath(channel: Int, on: Boolean, cred: ShellyAuth.Credentials?) = path("Power${channel + 1} ${if (on) "On" else "Off"}", cred)

    fun needsPassword(json: String): Boolean = runCatching { JSONObject(json).optString("WARNING").contains("Need user") }.getOrDefault(false)

    /** `StatusSTS` (ya da doğrudan komut yanıtı) içindeki röleler: POWER → 0, POWER1..8 → 0..7. */
    fun powers(o: JSONObject): Map<Int, Boolean> {
        val out = LinkedHashMap<Int, Boolean>()
        for (i in 1..8) o.optString("POWER$i").takeIf { it.isNotEmpty() }?.let { out[i - 1] = it.equals("ON", true) }
        if (out.isEmpty()) o.optString("POWER").takeIf { it.isNotEmpty() }?.let { out[0] = it.equals("ON", true) }
        return out
    }

    fun parseInfo(json: String): Info? = runCatching {
        val o = JSONObject(json)
        val net = o.optJSONObject("StatusNET") ?: return null
        val mac = net.optString("Mac").takeIf { it.isNotBlank() } ?: return null
        val st = o.optJSONObject("Status")
        val friendly = st?.optJSONArray("FriendlyName")?.optString(0)?.takeIf { it.isNotBlank() }
        val model = st?.optString("DeviceName")?.takeIf { it.isNotBlank() } ?: friendly ?: "Tasmota"
        val channels = powers(o.optJSONObject("StatusSTS") ?: JSONObject()).size
        Info(idFromMac(mac), model, friendly, channels.coerceAtMost(4))
    }.getOrNull()

    fun parseStatus(json: String, channels: Int): LocalReading? = runCatching {
        val o = JSONObject(json)
        val n = channels.coerceIn(1, 4)
        val sw = powers(o.optJSONObject("StatusSTS") ?: return null).filterKeys { it < n }
        if (sw.isEmpty()) return null
        val e = o.optJSONObject("StatusSNS")?.optJSONObject("ENERGY")
        fun num(key: String): List<Double> = when (val v = e?.opt(key)) {
            is Number -> listOf(v.toDouble())
            is JSONArray -> (0 until v.length()).map { v.optDouble(it) }
            else -> emptyList()
        }
        val p = num("Power")
        val channelPower = if (p.size > 1) p.take(n).withIndex().associate { it.index to it.value } else emptyMap()
        LocalReading(
            sw, power = p.takeIf { it.isNotEmpty() }?.sum(), voltage = num("Voltage").firstOrNull(),
            current = num("Current").takeIf { it.isNotEmpty() }?.sum(), channelPower = if (sw.size > 1) channelPower else emptyMap(),
        )
    }.getOrNull()

    /** Komut yanıtı istenen durumu doğruluyor mu. */
    fun isSwitchOk(json: String, channel: Int, on: Boolean): Boolean = runCatching {
        val p = powers(JSONObject(json))
        (p[channel] ?: p[0].takeIf { channel == 0 }) == on
    }.getOrDefault(false)
}
