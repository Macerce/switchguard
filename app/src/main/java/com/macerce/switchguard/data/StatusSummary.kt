package com.macerce.switchguard.data

/** Ana ekran widget'ı ve Hızlı Ayarlar kutucuğunda gösterilen tek kelimelik durum. */
enum class StatusKind { ALARM, STOPPED, LIVE, POLLING, CONNECTING, PROBLEM }

object StatusSummary {
    /** Önce çalan alarm, sonra izlemenin açık olup olmadığı, en son bağlantı durumu. */
    fun kind(monitoring: Boolean, alarms: Int, state: ConnState): StatusKind = when {
        alarms > 0 -> StatusKind.ALARM
        !monitoring -> StatusKind.STOPPED
        else -> when (state) {
            ConnState.LIVE -> StatusKind.LIVE
            ConnState.POLLING -> StatusKind.POLLING
            // STOPPED: izleme açık ama servis henüz durum bildirmedi.
            ConnState.CONNECTING, ConnState.STOPPED -> StatusKind.CONNECTING
            ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR -> StatusKind.PROBLEM
        }
    }
}
