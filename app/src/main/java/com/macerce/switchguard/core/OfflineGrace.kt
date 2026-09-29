package com.macerce.switchguard.core

/**
 * Kısa kopmaları yutmak için "offline" olaylarını bir süre bekletir.
 * Cihaz süre dolmadan geri gelirse ne offline ne de online olayı üretilir.
 * Zaman dışarıdan verilir; böylece saf ve test edilebilir kalır.
 */
class OfflineGrace {
    private data class Held(val change: Change.Connection, val dueAt: Long)

    private val held = mutableMapOf<String, Held>()

    fun hold(change: Change.Connection, now: Long, graceMs: Long = 0) {
        held[change.deviceId] = Held(change, now + graceMs)
    }

    /** Cihaz beklemedeyken geri geldiyse true döner ve beklemeyi iptal eder. */
    fun cameBack(deviceId: String): Boolean = held.remove(deviceId) != null

    /** Süresi dolan offline olaylarını döndürür ve listeden çıkarır. */
    fun due(now: Long): List<Change.Connection> {
        val ready = held.values.filter { it.dueAt <= now }
        ready.forEach { held.remove(it.change.deviceId) }
        return ready.map { it.change }
    }

    fun nextDueAt(): Long? = held.values.minOfOrNull { it.dueAt }
}
