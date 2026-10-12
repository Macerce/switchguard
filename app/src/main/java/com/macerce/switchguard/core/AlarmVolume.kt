package com.macerce.switchguard.core

/**
 * Alarm çalarken telefonun "alarm" ses seviyesi. Seviye 0 ya da düşükse alarm duyulmaz; [boost] açıkken
 * alarm süresince en yükseğe çıkarılır, alarm kapanınca eski değerine döner.
 */
object AlarmVolume {
    /** Ayarlanması gereken seviye; değişiklik gerekmiyorsa null. */
    fun target(current: Int, max: Int, boost: Boolean): Int? =
        if (boost && max > 0 && current < max) max else null

    /** Alarm başlarken: saklanacak eski seviye ve ayarlanacak seviye (gerekmiyorsa null). */
    data class Start(val remember: Int?, val set: Int?)

    /**
     * [saved]: bu alarmın başında eski seviye zaten saklandı. Seviye zaten en yüksekte olsa da saklanır: alarm
     * çalarken kullanıcı sesi kısarsa kapatınca eski haline döner, telefonun saat alarmı kısık kalmaz.
     */
    fun start(current: Int, max: Int, boost: Boolean, saved: Boolean): Start =
        if (!boost) Start(null, null)
        else Start(remember = if (saved) null else current, set = target(current, max, boost = true))
}
