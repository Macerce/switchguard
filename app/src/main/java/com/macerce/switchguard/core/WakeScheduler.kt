package com.macerce.switchguard.core

/**
 * Tek uyandırma zamanlayıcısı (saf mantık). İzleme çekirdeğinde birçok yer "şu vakitte uyan" ister (süre dolumu,
 * gecikmeli eylem, saatli şablon, bağlantı kopması payı, haber verme…); her isteğe ayrı gönderi kurulursa kuyruk
 * süreç yaşadıkça büyür ve vakit gelince yüzlerce tick() arka arkaya çalışıp alarmları geciktirir.
 *
 * Burada yalnızca en erken vakit tutulur: daha geç (ya da aynı) istek yeni gönderi doğurmaz; daha erken istek eskisinin
 * iptal edilip yenisinin kurulmasını söyler; gönderi çalışınca sıfırlanır. Android tarafı tek bir Runnable kullanır.
 */
class WakeScheduler {
    /** Kurulu gönderinin vakti (monoton ms); yoksa null. */
    @get:Synchronized
    var scheduledAt: Long? = null
        private set

    /** [at] vaktinde uyanma isteği. true: eski gönderi (varsa) iptal edilip [at] için yenisi kurulmalı. */
    @Synchronized
    fun request(at: Long): Boolean {
        val current = scheduledAt
        if (current != null && current <= at) return false
        scheduledAt = at
        return true
    }

    /** Gönderi çalıştı: sonraki istek yeniden kurulur. */
    @Synchronized
    fun fired() {
        scheduledAt = null
    }

    /** Bütün gönderiler iptal edildi (izleme durdu). */
    @Synchronized
    fun reset() {
        scheduledAt = null
    }
}
