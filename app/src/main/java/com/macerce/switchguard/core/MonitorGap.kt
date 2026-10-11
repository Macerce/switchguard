package com.macerce.switchguard.core

/**
 * İzlemenin fiilen çalışmadığı aralığı bulur (telefon servisi kapattı, telefon kapalıydı...).
 * Servis çalışırken düzenli "canlıyım" işareti yazar; kullanıcı izlemeyi kapatınca temiz kapanış işaretlenir.
 */
object MonitorGap {
    /** Bundan kısa yeniden başlatmalar (ör. uygulama güncellemesi) kesinti sayılmaz. */
    const val MIN_GAP_MS = 5 * 60_000L

    /**
     * Servis açılırken çağrılır: önceki çalışma temiz kapanmadıysa ve son işaretten bu yana
     * [minGapMs]'den uzun süre geçtiyse (son işaret, şimdi) aralığını döndürür.
     */
    fun detect(lastAliveAt: Long, cleanStop: Boolean, now: Long, minGapMs: Long = MIN_GAP_MS): Pair<Long, Long>? {
        if (lastAliveAt <= 0 || cleanStop) return null
        if (now - lastAliveAt < minGapMs) return null
        return lastAliveAt to now
    }
}
