package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Yerel ağdan eklenmiş Sonoff (eWeLink hesabında olmayan, DIY/şifresiz LAN kipinde): yalnızca Wi-Fi'den kontrol
 * edilir. [ssid]/[ip]: eklenirken telefonun bağlı olduğu ağ ve cihazın adresi (detayda "Eklendiği ağ").
 */
data class LocalDevice(
    val id: String,
    val name: String,
    val channels: Int = 1,
    val ssid: String? = null,
    val ip: String? = null,
    val addedAt: Long = 0,
) {
    /** Cihazlar ekranındaki kartı: durum yerel ağdan gelir; görülmüyorsa çevrimdışı ve son bilinen anahtarlar. */
    fun snapshot(seen: Map<Int, Boolean>?, previous: DeviceSnapshot?): DeviceSnapshot {
        val base = (0 until channels.coerceAtLeast(1)).associateWith { false }
        val switches = base + (previous?.switches ?: emptyMap()).filterKeys { it in base } + (seen ?: emptyMap()).filterKeys { it in base }
        return DeviceSnapshot(id, name, online = seen != null, switches = switches, uiid = if (channels > 1) 0 else 1, cloud = Cloud.LAN)
    }

    companion object {
        fun listToJson(list: List<LocalDevice>): String = JSONArray().apply {
            list.forEach { d ->
                put(JSONObject().put("id", d.id).put("name", d.name).put("channels", d.channels)
                    .put("ssid", d.ssid ?: JSONObject.NULL).put("ip", d.ip ?: JSONObject.NULL).put("addedAt", d.addedAt))
            }
        }.toString()

        /** Bozuk kayıtlar atlanır. */
        fun listFromJson(json: String?): List<LocalDevice> {
            if (json.isNullOrEmpty()) return emptyList()
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    LocalDevice(
                        o.getString("id"), o.optString("name"), o.optInt("channels", 1).coerceIn(1, 4),
                        o.optString("ssid").takeIf { !o.isNull("ssid") && it.isNotEmpty() },
                        o.optString("ip").takeIf { !o.isNull("ip") && it.isNotEmpty() },
                        o.optLong("addedAt"),
                    )
                }.getOrNull()
            }
        }

        /**
         * Yerel cihazların kartlarını listeye işler: eklenen gelir, kaldırılanın kartı gider, diğer bulutlara dokunmaz.
         * [seen]: cihaz kimliği → yerel ağda okunan anahtarlar (görülmeyen yok).
         */
        fun merge(all: List<DeviceSnapshot>, locals: List<LocalDevice>, seen: (String) -> Map<Int, Boolean>?): List<DeviceSnapshot> {
            val previous = all.filter { it.cloud == Cloud.LAN }.associateBy { it.id }
            val others = all.filter { it.cloud != Cloud.LAN }
            val taken = others.map { it.id }.toSet()
            return others + locals.filter { it.id !in taken }.map { it.snapshot(seen(it.id), previous[it.id]) }
        }
    }
}
