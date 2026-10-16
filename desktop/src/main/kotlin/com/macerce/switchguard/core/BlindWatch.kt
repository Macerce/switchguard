package com.macerce.switchguard.core

/**
 * İzlemenin "kör" kaldığı (internet yok, buluta ulaşılamıyor, oturum düştü) süreyi izler.
 * Kesinti [thresholdMs]'yi geçince bir kez [Signal.Blind] verir; uyarıdan sonra düzelince
 * [Signal.Recovered] ile kesinti aralığını bildirir. Eşikten kısa kopmalar hiçbir şey üretmez.
 */
class BlindWatch {
    sealed class Signal {
        data class Blind(val since: Long) : Signal()
        data class Recovered(val from: Long, val to: Long) : Signal()
    }

    private var blindSince: Long? = null
    private var warned = false

    /** [thresholdMs] 0 ise uyarı kapalıdır. */
    fun update(blind: Boolean, now: Long, thresholdMs: Long): Signal? {
        if (!blind) {
            val since = blindSince
            blindSince = null
            if (warned && since != null) {
                warned = false
                return Signal.Recovered(since, now)
            }
            warned = false
            return null
        }
        val since = blindSince ?: now.also { blindSince = it }
        if (!warned && thresholdMs > 0 && now - since >= thresholdMs) {
            warned = true
            return Signal.Blind(since)
        }
        return null
    }
}
