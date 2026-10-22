package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Elektrik kesintisi algılama. İzlenen cihazların hepsi kısa aralıkla koparsa her biri için ayrı alarm yerine
 * tek "elektrik kesilmiş olabilir" olayı verilir; elektrik gelince (ilk cihaz dönünce) kesintinin süresi bildirilir.
 *
 * Kopmalar [WINDOW_MS] boyunca bekletilir: süre sonunda en az iki cihaz koptuysa ve izlenen cihazların hepsi
 * kopuksa kesinti, değilse kopmalar tek tek verilir. Zaman dışarıdan verilir; saf ve test edilebilir.
 * Durum [toJson] ile diske yazılır: servis yeniden başlasa da kesinti ve bekleyen kopmalar kaybolmaz.
 */
class OutageWatch {
    data class Held(val change: Change.Connection, val action: AlertAction, val at: Long)

    sealed class Offer {
        /** Bekletiliyor; karar [dueAt]'te. */
        data class Hold(val dueAt: Long) : Offer()
        /** Süren kesintinin parçası: ayrı alarm verilmez. */
        data object Absorbed : Offer()
        /** Olağan yoldan verilsin. */
        data object Pass : Offer()
    }

    sealed class Verdict {
        /** Kesinti başladı: [held] kopmaları tek olayda toplanır. */
        data class Outage(val since: Long, val held: List<Held>) : Verdict()
        /** Kesinti değil: kopmalar tek tek verilsin. */
        data class Release(val held: List<Held>) : Verdict()
    }

    sealed class Online {
        /** Bekletilen kopması vardı: önce o, sonra dönüş olağan yoldan verilsin. */
        data class Release(val held: Held) : Online()
        /** Elektrik geldi: kesinti [from]–[to] sürdü. */
        data class Restored(val from: Long, val to: Long, val devices: Int) : Online()
        /** Kesintiden dönen diğer cihazlar: yalnızca kaydedilir. */
        data object Absorbed : Online()
        data object Pass : Online()
    }

    private val held = mutableListOf<Held>()
    private var windowEnd = 0L

    /** Süren kesinti: başlangıcı (0 = yok), kapsadığı cihazlar ve elektriğin geldiği an (0 = gelmedi). */
    var since = 0L
        private set
    private val outageIds = mutableSetOf<String>()
    var restoredAt = 0L
        private set

    val active get() = since > 0 && restoredAt == 0L

    /** Kopma; [monitored] izlenen cihaz sayısı. Algılama kapalıysa ya da tek cihaz izleniyorsa hep [Offer.Pass]. */
    fun offer(change: Change.Connection, action: AlertAction, now: Long, monitored: Int, enabled: Boolean): Offer {
        if (change.to) return Offer.Pass
        // Elektrik gelmiş, sonra yine kesilmiş: önceki kesinti kapanır, yeni algılama başlar.
        if (since > 0 && restoredAt > 0) clear()
        if (active) {
            outageIds += change.deviceId
            return Offer.Absorbed
        }
        if (!enabled || monitored < 2) return Offer.Pass
        if (held.isEmpty()) windowEnd = now + WINDOW_MS
        held.removeAll { it.change.deviceId == change.deviceId }
        held += Held(change, action, now)
        return Offer.Hold(windowEnd)
    }

    /** Bekleme süresi dolduysa kararı verir. [offline]: şu an kopuk izlenen cihazlar, [monitored]: izlenenler. */
    fun due(now: Long, offline: Set<String>, monitored: Set<String>): Verdict? {
        if (held.isEmpty() || now < windowEnd) {
            // Elektrik geleli yeterince oldu: geç dönen cihazlar artık olağan yoldan bildirilir.
            if (restoredAt > 0 && now - restoredAt >= SETTLE_MS) clear()
            return null
        }
        val list = held.toList()
        held.clear()
        windowEnd = 0
        val all = monitored.isNotEmpty() && monitored.all { it in offline }
        if (list.size < 2 || !all) return Verdict.Release(list)
        since = list.minOf { it.at }
        restoredAt = 0
        outageIds.clear()
        outageIds += offline
        return Verdict.Outage(since, list)
    }

    fun nextDueAt(): Long? = windowEnd.takeIf { held.isNotEmpty() }

    /** Cihaz geri geldi. */
    fun onOnline(deviceId: String, now: Long): Online {
        held.firstOrNull { it.change.deviceId == deviceId }?.let { h ->
            held.remove(h)
            if (held.isEmpty()) windowEnd = 0
            return Online.Release(h)
        }
        if (since == 0L || deviceId !in outageIds) return Online.Pass
        outageIds -= deviceId
        if (restoredAt == 0L) {
            restoredAt = now
            return Online.Restored(since, now, outageIds.size + 1)
        }
        if (outageIds.isEmpty()) clear()
        return Online.Absorbed
    }

    private fun clear() {
        since = 0
        restoredAt = 0
        outageIds.clear()
    }

    fun toJson(): String = JSONObject()
        .put("since", since).put("restored", restoredAt).put("ids", JSONArray(outageIds.toList())).put("end", windowEnd)
        .put("held", JSONArray().apply {
            held.forEach { h ->
                put(JSONObject().put("id", h.change.deviceId).put("name", h.change.name).put("action", h.action.name).put("at", h.at))
            }
        })
        .toString()

    companion object {
        /** Kopmaların toplandığı süre: bulut cihazların kopmasını farklı anlarda bildirir. */
        const val WINDOW_MS = 90_000L
        /** Elektrik geldikten sonra dönen cihazların ayrıca bildirilmediği süre. */
        const val SETTLE_MS = 10 * 60_000L

        fun fromJson(json: String?): OutageWatch = OutageWatch().apply {
            val o = runCatching { JSONObject(json ?: return@apply) }.getOrNull() ?: return@apply
            since = o.optLong("since")
            restoredAt = o.optLong("restored")
            windowEnd = o.optLong("end")
            o.optJSONArray("ids")?.let { a -> (0 until a.length()).forEach { outageIds += a.getString(it) } }
            val arr = o.optJSONArray("held") ?: return@apply
            for (i in 0 until arr.length()) {
                val h = arr.optJSONObject(i) ?: continue
                val id = h.optString("id").ifEmpty { continue }
                val action = runCatching { AlertAction.valueOf(h.optString("action")) }.getOrDefault(AlertAction.ALARM)
                held += Held(Change.Connection(id, h.optString("name"), from = true, to = false), action, h.optLong("at"))
            }
        }

        /** Toplanan kopmaların en güçlü eylemi (alarm > bildirim > yok say). */
        fun strongest(held: List<Held>): AlertAction = when {
            held.any { it.action == AlertAction.ALARM } -> AlertAction.ALARM
            held.any { it.action == AlertAction.NOTIFY } -> AlertAction.NOTIFY
            else -> AlertAction.IGNORE
        }
    }
}
