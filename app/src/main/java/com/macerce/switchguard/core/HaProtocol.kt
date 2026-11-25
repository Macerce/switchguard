package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Home Assistant REST API'si (`/api/`, uzun ömürlü erişim anahtarı `Authorization: Bearer`).
 * - `GET /api/` → 200 `{"message":"API running."}`, anahtarsız 401.
 * - `GET /api/states` → tüm varlıklar; `GET /api/states/<varlık>` → `state` ("on"/"off"/"unavailable"),
 *   `attributes.friendly_name`.
 * - `POST /api/services/<alan>/turn_on|turn_off {"entity_id":".."}`.
 * Aç/kapa yapılabilen alanlar SwitchGuard cihazı olur (tek kanal). Sunucu adresleri ve anahtar [Server] olarak
 * gizli depoda tutulur; yerel adres önce denenir, ulaşılamazsa dış adres (Nabu Casa, Tailscale, tünel ...).
 */
object HaProtocol {
    val DOMAINS = setOf("switch", "light", "input_boolean", "fan", "siren", "humidifier", "climate")
    const val DEFAULT_PORT = 8123

    data class Server(val local: String, val remote: String, val token: String) {
        fun toJson(): String = JSONObject().put("l", local).put("r", remote).put("t", token).toString()
    }

    fun serverFromJson(json: String?): Server? = runCatching {
        val o = JSONObject(json ?: return null)
        Server(o.optString("l"), o.optString("r"), o.getString("t")).takeIf { it.token.isNotBlank() && (it.local.isNotBlank() || it.remote.isNotBlank()) }
    }.getOrNull()

    /** "192.168.1.5" → "http://192.168.1.5:8123"; "ev.duckdns.org" → "https://ev.duckdns.org"; sondaki / atılır. */
    fun normalize(address: String): String {
        val a = address.trim().trimEnd('/')
        if (a.isEmpty()) return ""
        if (a.contains("://")) return a
        val hostPart = a.substringBefore('/')
        val name = hostPart.substringBefore(':')
        val hasPort = hostPart.contains(':')
        // Ev ağı adresleri (IP, .local) şifresiz ve varsayılan 8123; diğer adlar dış adres sayılır (HTTPS).
        val home = name.all { it.isDigit() || it == '.' } || name.endsWith(".local") || !name.contains('.')
        return when {
            home && !hasPort -> "http://$a:$DEFAULT_PORT"
            home -> "http://$a"
            else -> "https://$a"
        }
    }

    fun idFor(entityId: String) = "ha-$entityId"

    data class Entity(
        val id: String,
        val name: String,
        val state: String,
        val deviceClass: String = "",
        val unit: String = "",
        /** climate: odanın sıcaklığı (°C) / nemi (`current_temperature`, `current_humidity`). */
        val currentTemp: Double? = null,
        val currentHumidity: Double? = null,
    ) {
        val domain: String get() = id.substringBefore('.')
        val on: Boolean get() =
            if (domain == "climate") available && state != "off" else state == "on" || state == "open" || state == "playing"
        val available: Boolean get() = state != "unavailable" && state != "unknown"
        val controllable: Boolean get() = domain in DOMAINS

        /** sensor / binary_sensor varlığının SwitchGuard sensör türü (eşlenemiyorsa null). */
        val sensorKind: SensorKind? get() = when (domain) {
            "sensor" -> when (deviceClass) {
                "temperature" -> SensorKind.TEMPERATURE
                "humidity" -> SensorKind.HUMIDITY
                "battery" -> SensorKind.BATTERY
                "illuminance" -> SensorKind.ILLUMINANCE
                else -> null
            }
            "binary_sensor" -> when (deviceClass) {
                "door", "window", "garage_door", "opening" -> SensorKind.DOOR
                "motion", "occupancy", "presence" -> SensorKind.MOTION
                "moisture" -> SensorKind.WATER
                else -> null
            }
            else -> null
        }

        /** Sensör değeri (°F → °C; ikili sensörde 1.0 = etkin). Okunamıyorsa null. */
        val sensorValue: Double? get() {
            val k = sensorKind ?: return null
            if (!available) return null
            if (k.binary) return if (state == "on") 1.0 else 0.0
            val v = state.toDoubleOrNull() ?: return null
            return if (k == SensorKind.TEMPERATURE && unit == "°F") (v - 32) * 5 / 9 else v
        }

        /** Listede gösterilip eklenebilir: aç/kapa yapılabilen ya da eşlenen sensör. */
        val listable: Boolean get() = controllable || sensorKind != null

        /** Yerel okuma: anahtar + (climate'te) oda sıcaklığı/nem; sensör varlığında yalnız sensör. Erişilemezse null. */
        fun reading(): LocalReading? {
            if (!available) return null
            sensorKind?.let { k -> return LocalReading(emptyMap(), sensors = sensorValue?.let { mapOf(k to it) }.orEmpty()) }
            val sensors = buildMap {
                currentTemp?.let { put(SensorKind.TEMPERATURE, it) }
                currentHumidity?.let { put(SensorKind.HUMIDITY, it) }
            }
            return LocalReading(mapOf(0 to on), sensors = sensors)
        }
    }

    private fun entity(o: JSONObject): Entity? {
        val id = o.optString("entity_id").takeIf { it.contains('.') } ?: return null
        val a = o.optJSONObject("attributes")
        val name = a?.optString("friendly_name")?.takeIf { it.isNotBlank() } ?: id
        fun num(k: String) = a?.opt(k)?.toString()?.toDoubleOrNull()
        val unit = a?.optString("unit_of_measurement").orEmpty()
        val fahrenheit = a?.optString("temperature_unit") == "°F"
        val temp = num("current_temperature")?.let { if (fahrenheit) (it - 32) * 5 / 9 else it }
        return Entity(id, name, o.optString("state"), a?.optString("device_class").orEmpty(), unit, temp, num("current_humidity"))
    }

    fun parseState(json: String): Entity? = runCatching { entity(JSONObject(json)) }.getOrNull()

    /** Eklenebilen varlıklar (aç/kapa yapılabilenler önce, sonra sensörler), ada göre sıralı. */
    fun parseStates(json: String): List<Entity> = runCatching {
        val a = JSONArray(json)
        (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::entity) }.filter { it.listable }
            .sortedWith(compareBy<Entity> { !it.controllable }.thenBy { it.name.lowercase() })
    }.getOrDefault(emptyList())

    fun servicePath(entityId: String, on: Boolean) = "/api/services/${entityId.substringBefore('.')}/${if (on) "turn_on" else "turn_off"}"
    fun serviceBody(entityId: String): String = JSONObject().put("entity_id", entityId).toString()

    // ---------------------------------------------------------------- WebSocket (/api/websocket)
    // Sunucu "auth_required" yollar → istemci {"type":"auth","access_token":..} → "auth_ok" / "auth_invalid";
    // sonra {"id":1,"type":"subscribe_events","event_type":"state_changed"} ile her durum değişikliği anında gelir.

    /** "http://x:8123" → "ws://x:8123/api/websocket"; https → wss. */
    fun wsUrl(base: String): String {
        val b = normalize(base)
        return when {
            b.startsWith("https://") -> "wss://" + b.removePrefix("https://")
            b.startsWith("http://") -> "ws://" + b.removePrefix("http://")
            else -> b
        } + "/api/websocket"
    }

    fun wsAuth(token: String): String = JSONObject().put("type", "auth").put("access_token", token).toString()

    fun wsSubscribe(id: Int): String = JSONObject().put("id", id).put("type", "subscribe_events").put("event_type", "state_changed").toString()

    sealed class WsMessage {
        object AuthRequired : WsMessage()
        object AuthOk : WsMessage()
        data class AuthInvalid(val message: String) : WsMessage()
        /** [state] null: varlık silindi. */
        data class StateChanged(val entityId: String, val state: Entity?) : WsMessage()
        /** Abonelik sonucu ("result"). */
        data class Result(val id: Int, val success: Boolean) : WsMessage()
        object Other : WsMessage()
    }

    /** Sunucudan gelen tek ileti (toplu "[...]" dizileri [parseWsBatch] ile). */
    fun parseWs(text: String): WsMessage = runCatching { ws(JSONObject(text)) }.getOrDefault(WsMessage.Other)

    /** Sunucu iletileri birleştirebilir (`[{..},{..}]`). */
    fun parseWsBatch(text: String): List<WsMessage> = runCatching {
        val t = text.trimStart()
        if (t.startsWith("[")) JSONArray(t).let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::ws) } }
        else listOf(ws(JSONObject(t)))
    }.getOrDefault(emptyList())

    private fun ws(o: JSONObject): WsMessage = when (o.optString("type")) {
        "auth_required" -> WsMessage.AuthRequired
        "auth_ok" -> WsMessage.AuthOk
        "auth_invalid" -> WsMessage.AuthInvalid(o.optString("message"))
        "result" -> WsMessage.Result(o.optInt("id"), o.optBoolean("success"))
        "event" -> {
            val data = o.optJSONObject("event")?.optJSONObject("data")
            val id = data?.optString("entity_id").orEmpty()
            if (id.isEmpty()) WsMessage.Other
            else WsMessage.StateChanged(id, data?.optJSONObject("new_state")?.let(::entity))
        }
        else -> WsMessage.Other
    }
}
