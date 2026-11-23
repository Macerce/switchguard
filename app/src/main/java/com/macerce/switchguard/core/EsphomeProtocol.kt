package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * ESPHome `web_server` REST API'si (cihaz yapılandırmasında `web_server:` olmalı).
 * - `GET /events` (SSE): bağlanınca `ping` olayı (`title` = cihaz adı) ve her varlık için `state` olayı
 *   (`id` = "switch-relay_1", yeni sürümlerde ayrıca `name_id` = "switch/Relay 1", `value`, `state`).
 * - Durum `GET /switch/<ad>` → `{"value":true,"state":"ON"}`; komut `POST /switch/<ad>/turn_on|turn_off`.
 * Anahtar olarak `switch` ve `light`, güç olarak birimi W olan ilk `sensor` alınır.
 * [Entities] cihaza [LocalDevice.extra] olarak JSON'la kaydedilir. Birim testli.
 */
object EsphomeProtocol {
    /** [switches]: URL yolları ("switch/relay_1"); [power]: güç sensörünün yolu. */
    data class Entities(val title: String?, val switches: List<String>, val labels: List<String>, val power: String?) {
        fun toJson(): String = JSONObject().put("sw", JSONArray(switches)).put("lb", JSONArray(labels)).put("pw", power ?: JSONObject.NULL).toString()
    }

    fun entitiesFromJson(json: String?): Entities = runCatching {
        val o = JSONObject(json ?: "")
        fun list(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        Entities(null, list("sw"), list("lb"), o.optString("pw").takeIf { !o.isNull("pw") && it.isNotEmpty() })
    }.getOrDefault(Entities(null, emptyList(), emptyList(), null))

    fun idFor(title: String?, host: String): String =
        "esphome-" + (title?.takeIf { it.isNotBlank() } ?: host).lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-')

    /** Bir varlığın URL yolu: yeni sürümlerde `name_id` ("switch/Relay 1" → ad kodlanır), eskilerde `id` ("switch-relay_1"). */
    fun pathOf(o: JSONObject): String? {
        o.optString("name_id").takeIf { it.contains('/') }?.let { n ->
            val (domain, name) = n.split('/', limit = 2)
            return domain + "/" + java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")
        }
        val id = o.optString("id").takeIf { it.contains('-') } ?: return null
        val (domain, oid) = id.split('-', limit = 2)
        return "$domain/$oid"
    }

    /** SSE akışının metni (ilk birkaç saniyesi) → varlıklar. ESPHome değilse (hiç ping/state yok) null. */
    fun parseEvents(text: String): Entities? {
        var title: String? = null
        val switches = ArrayList<String>()
        val labels = ArrayList<String>()
        var power: String? = null
        var seen = false
        var event = ""
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r')
            when {
                line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                line.startsWith("data:") -> {
                    val o = runCatching { JSONObject(line.removePrefix("data:").trim()) }.getOrNull() ?: continue
                    if (event == "ping") { seen = true; o.optString("title").takeIf { it.isNotBlank() }?.let { title = it } }
                    if (event != "state") continue
                    seen = true
                    val path = pathOf(o) ?: continue
                    val domain = path.substringBefore('/')
                    if ((domain == "switch" || domain == "light") && path !in switches && switches.size < 4) {
                        switches += path
                        labels += o.optString("name").ifEmpty { java.net.URLDecoder.decode(path.substringAfter('/'), "UTF-8") }
                    }
                    if (domain == "sensor" && power == null && o.optString("state").trim().endsWith(" W")) power = path
                }
            }
        }
        return if (seen) Entities(title, switches, labels, power) else null
    }

    /** `GET /switch/x` ya da `/light/x` yanıtı → açık mı (bilinmiyorsa null). */
    fun parseOn(json: String): Boolean? = runCatching {
        val o = JSONObject(json)
        when {
            o.has("value") && o.get("value") is Boolean -> o.getBoolean("value")
            o.has("state") -> o.getString("state").equals("ON", true)
            else -> null
        }
    }.getOrNull()

    fun parseValue(json: String): Double? = runCatching { JSONObject(json).getDouble("value") }.getOrNull()?.takeIf { !it.isNaN() }

    fun switchPath(path: String, on: Boolean) = "/$path/${if (on) "turn_on" else "turn_off"}"
}
