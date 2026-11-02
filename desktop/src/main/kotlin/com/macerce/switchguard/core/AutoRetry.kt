package com.macerce.switchguard.core

/**
 * Otomasyonun cihaza ulaşamayan aç/kapa komutları. Cihaz tam o anda buluttan kısa süre
 * koptuysa (eWeLink 4002 "控制失败") komut kaybolmasın diye artan aralıklarla yeniden
 * denenir; cihaz yeniden bağlanınca beklemeden denenir.
 * Aynı cihaz/kanala yeni bir otomasyon komutu gelirse eskisinin yerini alır.
 */
class AutoRetry(private val delaysMs: List<Long> = DEFAULT_DELAYS_MS) {
    /** [attempt]: sıradaki deneme kaçıncı (ilk gönderim 1). */
    data class Pending(
        val automation: String, val deviceId: String, val channel: Int, val on: Boolean,
        val attempt: Int, val dueAt: Long,
    )

    private val pending = LinkedHashMap<Pair<String, Int>, Pending>()

    /** [failedAttempt]. deneme başarısız oldu: yeniden denenecekse true, denemeler bittiyse false. */
    fun failed(automation: String, deviceId: String, channel: Int, on: Boolean, failedAttempt: Int, now: Long): Boolean {
        val key = deviceId to channel
        val delay = delaysMs.getOrNull(failedAttempt - 1)
        if (delay == null) {
            pending.remove(key)
            return false
        }
        pending[key] = Pending(automation, deviceId, channel, on, failedAttempt + 1, now + delay)
        return true
    }

    /** Bu cihaz/kanal için bekleyen denemeyi bırakır (komut gitti ya da yerine yenisi geliyor). */
    fun cancel(deviceId: String, channel: Int) {
        pending.remove(deviceId to channel)
    }

    /** Cihaz yeniden bağlandı: bekleyen denemeleri hemen yapılacak hale getirir. */
    fun cameOnline(deviceId: String, now: Long): Boolean {
        var any = false
        for (entry in pending.entries) if (entry.value.deviceId == deviceId && entry.value.dueAt > now) {
            entry.setValue(entry.value.copy(dueAt = now))
            any = true
        }
        return any
    }

    /** Vakti gelen denemeleri listeden çıkarıp döndürür. */
    fun due(now: Long): List<Pending> {
        val ready = pending.values.filter { it.dueAt <= now }
        ready.forEach { pending.remove(it.deviceId to it.channel) }
        return ready
    }

    fun nextDueAt(): Long? = pending.values.minOfOrNull { it.dueAt }

    companion object {
        /** İlk gönderimden sonra 4 deneme daha: toplam ~6,5 dakika. */
        val DEFAULT_DELAYS_MS = listOf(30_000L, 60_000L, 120_000L, 180_000L)
    }
}
