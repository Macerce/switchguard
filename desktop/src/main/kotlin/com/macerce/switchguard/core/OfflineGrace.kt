package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Kısa kopmaları yutmak için "offline" olaylarını bir süre bekletir.
 * Cihaz süre dolmadan geri gelirse ne offline ne de online olayı üretilir.
 * Zaman dışarıdan verilir; böylece saf ve test edilebilir kalır.
 * Bekleyenler [toJson] ile diske yazılır: servis bu arada yeniden başlarsa (telefon servisi kapattı,
 * uygulama güncellendi) cihazın kopuk durumu zaten kayıtlı olduğundan fark görülmez ve alarm hiç çalmazdı.
 */
class OfflineGrace {
    private data class Held(val change: Change.Connection, val dueAt: Long)

    private val held = mutableMapOf<String, Held>()

    fun hold(change: Change.Connection, now: Long, graceMs: Long = 0) {
        held[change.deviceId] = Held(change, now + graceMs)
    }

    /**
     * Cihaz bekleme süresi dolmadan geri geldiyse true döner ve beklemeyi iptal eder ("kısa kopma").
     * Süre dolmuşsa (ör. servis bu arada kapalıydı, tick gecikti) false: kopma [due] ile yine verilir.
     */
    fun cameBack(deviceId: String, now: Long): Boolean {
        val h = held[deviceId] ?: return false
        if (h.dueAt <= now) return false
        held.remove(deviceId)
        return true
    }

    /** Süresi dolan offline olaylarını döndürür ve listeden çıkarır. */
    fun due(now: Long): List<Change.Connection> {
        val ready = held.values.filter { it.dueAt <= now }
        ready.forEach { held.remove(it.change.deviceId) }
        return ready.map { it.change }
    }

    fun nextDueAt(): Long? = held.values.minOfOrNull { it.dueAt }

    fun toJson(): String = JSONArray().apply {
        held.values.forEach { h ->
            put(JSONObject().put("id", h.change.deviceId).put("name", h.change.name).put("due", h.dueAt))
        }
    }.toString()

    companion object {
        /** Bozuk ya da boş kayıt: bekleyen yok. */
        fun fromJson(json: String?): OfflineGrace = OfflineGrace().apply {
            val arr = runCatching { JSONArray(json ?: return@apply) }.getOrNull() ?: return@apply
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").ifEmpty { continue }
                held[id] = Held(Change.Connection(id, o.optString("name"), from = true, to = false), o.optLong("due"))
            }
        }
    }
}
