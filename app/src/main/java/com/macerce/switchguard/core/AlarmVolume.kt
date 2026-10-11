package com.macerce.switchguard.core

/**
 * Alarm çalarken telefonun "alarm" ses seviyesi. Seviye 0 ya da düşükse alarm duyulmaz; [boost] açıkken
 * alarm süresince en yükseğe çıkarılır, alarm kapanınca eski değerine döner.
 */
object AlarmVolume {
    /** Ayarlanması gereken seviye; değişiklik gerekmiyorsa null. */
    fun target(current: Int, max: Int, boost: Boolean): Int? =
        if (boost && max > 0 && current < max) max else null
}
