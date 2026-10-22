package com.macerce.switchguard.core

import org.json.JSONObject

/** Wi-Fi sinyal gücü (dBm) ve kopmalarla birlikte yorumlanması. */
object Signal {
    enum class Quality(val bars: Int) { EXCELLENT(4), GOOD(3), FAIR(2), WEAK(1) }

    /** eWeLink params içindeki "rssi"; anlamsız değerler (0, pozitif, -120 altı) yok sayılır. */
    fun rssiOf(params: JSONObject?): Int? {
        if (params == null || !params.has("rssi")) return null
        val v = params.optInt("rssi", 0)
        return v.takeIf { it in -120..-1 }
    }

    fun quality(rssi: Int): Quality = when {
        rssi >= -55 -> Quality.EXCELLENT
        rssi >= -67 -> Quality.GOOD
        rssi >= -75 -> Quality.FAIR
        else -> Quality.WEAK
    }

    /** Bu kadar kopma "sık" sayılır (haftada). */
    const val FREQUENT_DROPS = 3

    /**
     * Sık kopan cihaz için olası neden: sinyal zayıfsa ya da sınırdaysa Wi-Fi, iyiyse güç/modem tarafı.
     * Sinyal bilinmiyorsa null (öneri genel kalır).
     */
    fun dropCause(drops: Int, rssi: Int?): Cause? {
        if (drops < FREQUENT_DROPS) return null
        rssi ?: return Cause.UNKNOWN
        return when (quality(rssi)) {
            Quality.WEAK, Quality.FAIR -> Cause.WEAK_SIGNAL
            else -> Cause.POWER_OR_ROUTER
        }
    }

    enum class Cause { WEAK_SIGNAL, POWER_OR_ROUTER, UNKNOWN }
}
