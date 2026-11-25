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

    /**
     * Durum yanıtı → anahtarlar, güç ve sensörler. Ne röle ne ölçüm ne sensör varsa null. [channels]: kayıtlı kanal
     * sayısı (fazlası atılır; 0 = yalnız ölçer/sensör: Shelly EM, Pro 3EM, PM Mini, Plus Add-on, H&T, Door/Window).
     */
    fun parseStatus(gen: Int, json: String, channels: Int): LocalReading? = runCatching {
        val o = JSONObject(json)
        val n = channels.coerceIn(0, 4)
        val switches = LinkedHashMap<Int, Boolean>()
        val power = LinkedHashMap<Int, Double>()
        val sensors = LinkedHashMap<SensorKind, Double>()
        var meter: Double? = null
        var voltage: Double? = null
        var current: Double? = null
        if (gen <= 1) {
            val relays = o.optJSONArray("relays")
            if (relays != null) for (i in 0 until minOf(relays.length(), n)) switches[i] = relays.getJSONObject(i).optBoolean("ison")
            // Plug/1PM/2.5: meters[].power; EM ve 3EM: emeters[] (gerilim/akım da var; 3EM'de fazlar toplanır).
            val meters = o.optJSONArray("meters")
            if (meters != null) for (i in 0 until minOf(meters.length(), n)) {
                val m = meters.getJSONObject(i)
                if (m.has("power") && m.optBoolean("is_valid", true)) power[i] = m.getDouble("power")
            }
            val em = o.optJSONArray("emeters")
            if (em != null && em.length() > 0) {
                val m = em.getJSONObject(0)
                if (m.has("voltage")) voltage = m.getDouble("voltage")
                if (m.has("current")) current = m.getDouble("current")
                val sum = (0 until em.length()).mapNotNull { em.getJSONObject(it).takeIf { e -> e.has("power") }?.getDouble("power") }
                if (power.isEmpty() && sum.isNotEmpty()) meter = sum.sum()
            }
            // H&T, Door/Window, Flood (pilli; uyanıkken okunur).
            o.optJSONObject("tmp")?.takeIf { it.optBoolean("is_valid", true) && it.has("tC") }?.let { sensors[SensorKind.TEMPERATURE] = it.getDouble("tC") }
            o.optJSONObject("hum")?.takeIf { it.optBoolean("is_valid", true) && it.has("value") }?.let { sensors[SensorKind.HUMIDITY] = it.getDouble("value") }
            o.optJSONObject("lux")?.takeIf { it.optBoolean("is_valid", true) && it.has("value") }?.let { sensors[SensorKind.ILLUMINANCE] = it.getDouble("value") }
            o.optJSONObject("sensor")?.optString("state")?.takeIf { it == "open" || it == "close" }?.let { sensors[SensorKind.DOOR] = if (it == "open") 1.0 else 0.0 }
            if (o.has("flood")) sensors[SensorKind.WATER] = if (o.optBoolean("flood")) 1.0 else 0.0
            if (sensors.isNotEmpty()) o.optJSONObject("bat")?.takeIf { it.has("value") }?.let { sensors[SensorKind.BATTERY] = it.getDouble("value") }
        } else {
            for (i in 0 until n) {
                val s = o.optJSONObject("switch:$i") ?: continue
                switches[i] = s.optBoolean("output")
                if (s.has("apower")) power[i] = s.getDouble("apower")
                if (voltage == null && s.has("voltage")) voltage = s.getDouble("voltage")
                if (s.has("current")) current = (current ?: 0.0) + s.getDouble("current")
            }
            // Ölçerler: Pro 3EM (em:0), Pro EM / EM Gen3 (em1:N), PM Mini (pm1:0).
            o.optJSONObject("em:0")?.let { e ->
                if (e.has("total_act_power")) meter = e.getDouble("total_act_power")
                if (e.has("a_voltage")) voltage = e.getDouble("a_voltage")
                if (e.has("total_current")) current = e.getDouble("total_current")
            }
            if (meter == null) {
                val parts = (0 until 3).mapNotNull { o.optJSONObject("em1:$it") }
                if (parts.isNotEmpty()) {
                    meter = parts.sumOf { it.optDouble("act_power", 0.0) }
                    parts.first().takeIf { it.has("voltage") }?.let { voltage = it.getDouble("voltage") }
                    current = parts.sumOf { it.optDouble("current", 0.0) }
                }
            }
            if (meter == null) o.optJSONObject("pm1:0")?.let { p ->
                if (p.has("apower")) meter = p.getDouble("apower")
                if (p.has("voltage")) voltage = p.getDouble("voltage")
                if (p.has("current")) current = p.getDouble("current")
            }
            // Sensörler: Plus Add-on / H&T / Gen3 (temperature:N, humidity:N, illuminance:N, devicepower:0).
            o.optJSONObject("temperature:0")?.takeIf { it.has("tC") && !it.isNull("tC") }?.let { sensors[SensorKind.TEMPERATURE] = it.getDouble("tC") }
            o.optJSONObject("humidity:0")?.takeIf { it.has("rh") && !it.isNull("rh") }?.let { sensors[SensorKind.HUMIDITY] = it.getDouble("rh") }
            o.optJSONObject("illuminance:0")?.takeIf { it.has("lux") && !it.isNull("lux") }?.let { sensors[SensorKind.ILLUMINANCE] = it.getDouble("lux") }
            if (sensors.isNotEmpty()) o.optJSONObject("devicepower:0")?.optJSONObject("battery")?.takeIf { it.has("percent") }
                ?.let { sensors[SensorKind.BATTERY] = it.getDouble("percent") }
        }
        if (switches.isEmpty() && meter == null && sensors.isEmpty()) return null
        val total = meter ?: if (power.isEmpty()) null else power.values.sum()
        LocalReading(
            switches, power = total, voltage = voltage, current = current,
            channelPower = if (switches.size > 1) power else emptyMap(),
            sensors = sensors,
        )
    }.getOrNull()

    /** Fabrika ayarlı Shelly'nin kendi Wi-Fi erişim noktasındaki adresi. */
    const val SETUP_HOST = "192.168.33.1"

    /**
     * Fabrika ayarlı Shelly'yi ev Wi-Fi'sine tanıtan adres (cihazın kendi ağına bağlıyken çağrılır).
     * Gen1: `/settings/sta?enabled=1&ssid=..&key=..` (kendiliğinden yeniden başlar).
     * Gen2+: `/rpc/WiFi.SetConfig?config={"sta":{"ssid":..,"pass":..,"enable":true}}` (ardından [rebootPath]).
     */
    fun staConfigPath(gen: Int, ssid: String, password: String): String {
        fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")
        return if (gen <= 1) "/settings/sta?enabled=1&ssid=${enc(ssid)}&key=${enc(password)}"
        else {
            val sta = JSONObject().put("ssid", ssid).put("pass", password).put("enable", true)
            "/rpc/WiFi.SetConfig?config=" + enc(JSONObject().put("sta", sta).toString())
        }
    }

    /**
     * Gen2+ kendi erişim noktasını ev ağına bağlandıktan sonra da açık (ve şifresiz) tutar; kurulum bitince ev ağı
     * üzerinden bununla kapatılır. Gen1'de gerekmez (istemci kipine geçince erişim noktası kapanır).
     */
    fun apOffPath(): String =
        "/rpc/WiFi.SetConfig?config=" + java.net.URLEncoder.encode(JSONObject().put("ap", JSONObject().put("enable", false)).toString(), "UTF-8")

    /** `WiFi.SetConfig` yanıtı yeniden başlatma istiyor mu. */
    fun restartRequired(json: String): Boolean = runCatching { JSONObject(json).optBoolean("restart_required") }.getOrDefault(false)

    fun rebootPath(gen: Int): String = if (gen <= 1) "/reboot" else "/rpc/Shelly.Reboot"

    /** Wi-Fi ayarı kabul edildi mi: Gen1 yanıtında `wifi_sta.ssid` (ya da `ssid`), Gen2'de hata kodu yok. */
    fun isStaConfigOk(gen: Int, json: String, ssid: String): Boolean = runCatching {
        val o = JSONObject(json)
        if (gen <= 1) (o.optJSONObject("wifi_sta")?.optString("ssid") ?: o.optString("ssid")) == ssid
        else !o.has("code") && !o.has("error")
    }.getOrDefault(false)

    /** Shelly'nin kendi erişim noktası ("shelly1-A1B2C3", "ShellyPlus1PM-A8032AB12345"). */
    fun isSetupSsid(ssid: String?): Boolean = ssid != null && ssid.startsWith("shelly", ignoreCase = true)

    /** mDNS servis adı Shelly'ye mi ait ("shelly1pm-A1B2C3", "ShellyPlus1PM-a8032ab12345"). */
    fun isShellyServiceName(name: String): Boolean = name.startsWith("shelly", ignoreCase = true)
}
