package com.macerce.switchguard.core

import kotlin.math.abs

/**
 * Sunucu saati ile cihaz saati arasındaki fark (saf mantık). Tuya imzadaki `t` zamanını sunucu saatine göre
 * denetler; saati yanlış cihazlarda (RTC'si olmayan TV kutusu, elle ayarlanmış saat) istekler "1013 request time is
 * invalid" ile reddedilir ve Tuya hiç izlenemezdi. Her yanıttaki sunucu zamanından fark çıkarılır, imzada
 * düzeltilmiş zaman kullanılır.
 */
class ServerClock {
    /** Sunucu − cihaz (ms). */
    @Volatile
    var offsetMs = 0L
        private set

    /** İmzada kullanılacak zaman. */
    fun now(local: Long): Long = local + offsetMs

    /**
     * Yanıttaki sunucu zamanı ([serverTime]); istek [sentAt]'te gönderildi, yanıt [receivedAt]'te geldi (cihaz saati).
     * Sunucu zamanı yolculuğun ortasına denk sayılır. Ağ gecikmesinden doğan küçük oynamalar ([JITTER_MS]) yok sayılır.
     */
    fun observe(serverTime: Long, sentAt: Long, receivedAt: Long) {
        if (serverTime <= 0 || receivedAt < sentAt) return
        val diff = serverTime - (sentAt + receivedAt) / 2
        if (abs(diff - offsetMs) > JITTER_MS) offsetMs = diff
    }

    companion object {
        const val JITTER_MS = 2_000L
    }
}
