package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Haritada seçilen daire biçimli alan (ev, iş yeri, yazlık). Telefon alana girince ya da çıkınca
 * [TriggerKind.ZONE_ENTER] / [TriggerKind.ZONE_EXIT] kuralları çalışır; [Trigger.deviceId] bölgenin kimliğidir.
 */
data class Zone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** Yarıçap (m); Android'in bölge bildirimi 100 m altında güvenilir değil. */
    val radius: Int = DEFAULT_RADIUS,
) {
    /** Nokta bu alanın içinde mi (yarıçap kadar). */
    fun contains(latitude: Double, longitude: Double): Boolean = distance(this.latitude, this.longitude, latitude, longitude) <= radius

    companion object {
        const val PREFIX = "zone:"
        const val MIN_RADIUS = 100
        const val MAX_RADIUS = 5_000
        const val DEFAULT_RADIUS = 200
        /** Yarıçap seçenekleri (m). */
        val RADII = listOf(100, 150, 200, 300, 500, 750, 1_000, 2_000, 5_000)

        fun isZone(id: String) = id.startsWith(PREFIX)

        /** İki nokta arası uzaklık (m, haversine). */
        fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
            return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
        }

        fun listToJson(list: List<Zone>): String = JSONArray().apply {
            list.forEach { z ->
                put(JSONObject().put("id", z.id).put("name", z.name).put("lat", z.latitude).put("lon", z.longitude).put("r", z.radius))
            }
        }.toString()

        /** Bozuk kayıtlar atlanır. */
        fun listFromJson(json: String?): List<Zone> {
            val arr = runCatching { JSONArray(json ?: return emptyList()) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    Zone(
                        o.getString("id"), o.optString("name"), o.getDouble("lat"), o.getDouble("lon"),
                        o.optInt("r", DEFAULT_RADIUS).coerceIn(MIN_RADIUS, MAX_RADIUS),
                    )
                }.getOrNull()
            }
        }
    }
}
