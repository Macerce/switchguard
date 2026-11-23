package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Yerel ağdan okunan anlık durum: anahtarlar ve (ölçen cihazlarda) güç değerleri. */
data class LocalReading(
    val switches: Map<Int, Boolean>,
    val power: Double? = null,
    val voltage: Double? = null,
    val current: Double? = null,
    val channelPower: Map<Int, Double> = emptyMap(),
)

/**
 * Yerel ağdan eklenmiş, hesaba bağlı olmayan cihaz: yalnızca Wi-Fi'den kontrol edilir. [brand]: [BRAND_SONOFF]
 * (DIY/şifresiz LAN kipi) ya da [BRAND_SHELLY] ([gen]: Shelly kuşağı, 1 = eski HTTP, 2+ = RPC).
 * [ssid]/[ip]: eklenirken telefonun bağlı olduğu ağ ve cihazın adresi (detayda "Eklendiği ağ").
 */
data class LocalDevice(
    val id: String,
    val name: String,
    val channels: Int = 1,
    val ssid: String? = null,
    val ip: String? = null,
    val addedAt: Long = 0,
    val brand: String = BRAND_SONOFF,
    val gen: Int = 0,
    /** Markaya özel kalıcı bilgi (JSON ya da düz metin; ör. ESPHome varlık yolları, Hue lamba kimliği). */
    val extra: String = "",
) {
    val isShelly: Boolean get() = brand == BRAND_SHELLY
    val isSonoff: Boolean get() = brand == BRAND_SONOFF

    /** Cihazlar ekranındaki kartı: durum yerel ağdan gelir; görülmüyorsa çevrimdışı ve son bilinen anahtarlar. */
    fun snapshot(seen: LocalReading?, previous: DeviceSnapshot?): DeviceSnapshot {
        val base = (0 until channels.coerceAtLeast(1)).associateWith { false }
        val switches = base + (previous?.switches ?: emptyMap()).filterKeys { it in base } + (seen?.switches ?: emptyMap()).filterKeys { it in base }
        return DeviceSnapshot(
            id, name, online = seen != null, switches = switches, uiid = if (channels > 1) 0 else 1, cloud = Cloud.LAN,
            power = seen?.power, voltage = seen?.voltage, current = seen?.current,
            channelPower = seen?.channelPower.orEmpty().filterKeys { it in base },
        )
    }

    companion object {
        const val BRAND_SONOFF = "sonoff"
        const val BRAND_SHELLY = "shelly"
        const val BRAND_TASMOTA = "tasmota"
        const val BRAND_ESPHOME = "esphome"
        const val BRAND_HUE = "hue"
        const val BRAND_KASA = "kasa"
        const val BRAND_TAPO = "tapo"
        const val BRAND_HA = "ha"

        fun listToJson(list: List<LocalDevice>): String = JSONArray().apply {
            list.forEach { d ->
                put(JSONObject().put("id", d.id).put("name", d.name).put("channels", d.channels)
                    .put("ssid", d.ssid ?: JSONObject.NULL).put("ip", d.ip ?: JSONObject.NULL).put("addedAt", d.addedAt)
                    .put("brand", d.brand).put("gen", d.gen).put("extra", d.extra))
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
                        o.optString("brand").ifEmpty { BRAND_SONOFF }, o.optInt("gen"), o.optString("extra"),
                    )
                }.getOrNull()
            }
        }

        /**
         * Yerel cihazların kartlarını listeye işler: eklenen gelir, kaldırılanın kartı gider, diğer bulutlara dokunmaz.
         * [seen]: cihaz → yerel ağda okunan durum (görülmeyen null).
         */
        fun merge(all: List<DeviceSnapshot>, locals: List<LocalDevice>, seen: (LocalDevice) -> LocalReading?): List<DeviceSnapshot> {
            val previous = all.filter { it.cloud == Cloud.LAN }.associateBy { it.id }
            val others = all.filter { it.cloud != Cloud.LAN }
            val taken = others.map { it.id }.toSet()
            return others + locals.filter { it.id !in taken }.map { it.snapshot(seen(it), previous[it.id]) }
        }
    }
}
