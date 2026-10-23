package com.macerce.switchguard.core

/**
 * Google Play değerlendirme penceresi ne zaman istenir: uygulama bir süredir kullanılıyor, izleme çalışıyor,
 * çalan alarm yok ve daha önce hiç istenmedi. Pencereyi Play gösterip göstermemeye kendisi karar verir (kota);
 * biz yalnızca bir kez, sakin bir anda isteriz.
 */
object ReviewPrompt {
    const val MIN_DAYS = 7
    const val MIN_OPENS = 5

    fun due(
        installedAt: Long,
        now: Long,
        opens: Int,
        asked: Boolean,
        monitoring: Boolean,
        monitoredDevices: Int,
        alarmPending: Boolean,
    ): Boolean =
        !asked && monitoring && monitoredDevices > 0 && !alarmPending &&
            opens >= MIN_OPENS && now - installedAt >= MIN_DAYS * 86_400_000L
}
