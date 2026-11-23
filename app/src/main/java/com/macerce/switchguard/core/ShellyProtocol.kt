package com.macerce.switchguard.core

import org.json.JSONObject

/**
 * Shelly yerel HTTP API'si (bulutsuz, aynı Wi-Fi). İki kuşak:
 * - Gen1 (Shelly 1, 1PM, Plug S, 2.5 ...): `GET /shelly` (tür, MAC, kanal sayısı), `GET /status`
 *   (`relays[].ison`, `meters[].power` ya da `emeters[]`), komut `GET /relay/N?turn=on|off`.
 * - Gen2 ve sonrası (Plus, Pro, Gen3, Gen4): `GET /shelly` (`gen`, `id`, `model`), `GET /rpc/Shelly.GetStatus`
 *   (`switch:N` → `output`, `apower`, `voltage`, `current`), komut `GET /rpc/Switch.Set?id=N&on=true|false`.
 * Burada yalnızca ayrıştırma ve adresler var (ağ işi [com.macerce.switchguard.api.ShellyLan]); birim testli.
 */
object ShellyProtocol {
    /** `/shelly` yanıtından cihaz bilgisi. [id]: SwitchGuard'daki kimlik ("shelly-" + küçük harf MAC). */
    data class Info(val id: String, val gen: Int, val model: String, val channels: Int, val auth: Boolean)

    /** Kimlik MAC'ten üretilir: IP değişse de, kuşak hangisi olursa olsun aynı kalır. */
    fun idFromMac(mac: String): String = "shelly-" + mac.filter { it.isLetterOrDigit() }.lowercase()

    /** `GET /shelly` yanıtı. Shelly değilse (MAC yok) null. Gen2'de kanal sayısı durumdan sayılır ([channelsOf]). */
    fun parseInfo(json: String): Info? = runCatching {
        val o = JSONObject(json)
        val mac = o.optString("mac").takeIf { it.isNotBlank() } ?: return null
        val gen = o.optInt("gen", 1).coerceAtLeast(1)
        if (gen == 1) {
            val type = o.optString("type")
            Info(idFromMac(mac), 1, type, o.optInt("num_outputs", 1).coerceIn(0, 4), o.optBoolean("auth"))
        } else {
            Info(idFromMac(mac), gen, o.optString("app").ifEmpty { o.optString("model") }, 0, o.optBoolean("auth_en"))
        }
    }.getOrNull()

    /** Gen2 durumundaki röle sayısı (`switch:0` ... `switch:3`). */
    fun channelsOf(statusJson: String): Int = runCatching {
        val o = JSONObject(statusJson)
        (0 until 4).count { o.has("switch:$it") }
    }.getOrDefault(0)

    /** Durum okuma adresi (sunucu kökünden sonra). */
    fun statusPath(gen: Int): String = if (gen <= 1) "/status" else "/rpc/Shelly.GetStatus"

    /** Aç/kapa adresi. */
    fun switchPath(gen: Int, channel: Int, on: Boolean): String =
        if (gen <= 1) "/relay/$channel?turn=${if (on) "on" else "off"}" else "/rpc/Switch.Set?id=$channel&on=$on"

    /** Komut yanıtı kabul mü: Gen1 `ison` istenenle aynı, Gen2 `was_on` alanı var (hata gövdesinde yok). */
    fun isSwitchOk(gen: Int, json: String, on: Boolean): Boolean = runCatching {
        val o = JSONObject(json)
        if (gen <= 1) o.has("ison") && o.getBoolean("ison") == on else o.has("was_on") && !o.has("code")
    }.getOrDefault(false)

    /** Durum yanıtı → anahtarlar ve güç. Röle bilgisi yoksa null. [channels]: kayıtlı kanal sayısı (fazlası atılır). */
    fun parseStatus(gen: Int, json: String, channels: Int): LocalReading? = runCatching {
        val o = JSONObject(json)
        val n = channels.coerceIn(1, 4)
        val switches = LinkedHashMap<Int, Boolean>()
        val power = LinkedHashMap<Int, Double>()
        var voltage: Double? = null
        var current: Double? = null
        if (gen <= 1) {
            val relays = o.optJSONArray("relays") ?: return null
            for (i in 0 until minOf(relays.length(), n)) switches[i] = relays.getJSONObject(i).optBoolean("ison")
            // Plug/1PM/2.5: meters[].power; EM ve 3EM: emeters[] (gerilim/akım da var).
            val meters = o.optJSONArray("meters")
            if (meters != null) for (i in 0 until minOf(meters.length(), n)) {
                val m = meters.getJSONObject(i)
                if (m.has("power") && m.optBoolean("is_valid", true)) power[i] = m.getDouble("power")
            }
            val em = o.optJSONArray("emeters")
            if (em != null && em.length() > 0) {
                val m = em.getJSONObject(0)
                if (power.isEmpty() && m.has("power")) power[0] = m.getDouble("power")
                if (m.has("voltage")) voltage = m.getDouble("voltage")
                if (m.has("current")) current = m.getDouble("current")
            }
        } else {
            for (i in 0 until n) {
                val s = o.optJSONObject("switch:$i") ?: continue
                switches[i] = s.optBoolean("output")
                if (s.has("apower")) power[i] = s.getDouble("apower")
                if (voltage == null && s.has("voltage")) voltage = s.getDouble("voltage")
                if (s.has("current")) current = (current ?: 0.0) + s.getDouble("current")
            }
        }
        if (switches.isEmpty()) return null
        val total = if (power.isEmpty()) null else power.values.sum()
        LocalReading(
            switches, power = total, voltage = voltage, current = current,
            channelPower = if (switches.size > 1) power else emptyMap(),
        )
    }.getOrNull()

    /** mDNS servis adı Shelly'ye mi ait ("shelly1pm-A1B2C3", "ShellyPlus1PM-a8032ab12345"). */
    fun isShellyServiceName(name: String): Boolean = name.startsWith("shelly", ignoreCase = true)
}
