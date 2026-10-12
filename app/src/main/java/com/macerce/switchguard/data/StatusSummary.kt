package com.macerce.switchguard.data

/** Ana ekran widget'ı ve Hızlı Ayarlar kutucuğunda gösterilen tek kelimelik durum. */
enum class StatusKind { ALARM, STOPPED, LIVE, POLLING, CONNECTING, PROBLEM }

object StatusSummary {
    /**
     * Önce çalan alarm, sonra izlemenin açık olup olmadığı, en son bağlantı durumu. [serviceAlive] false iken
     * izleme açık görünse de çalışmıyordur (telefon servisi kapattı): sorun sayılır.
     */
    fun kind(monitoring: Boolean, alarms: Int, state: ConnState, serviceAlive: Boolean = true): StatusKind = when {
        alarms > 0 -> StatusKind.ALARM
        !monitoring -> StatusKind.STOPPED
        !serviceAlive -> StatusKind.PROBLEM
        else -> when (state) {
            ConnState.LIVE -> StatusKind.LIVE
            ConnState.POLLING -> StatusKind.POLLING
            // STOPPED: izleme açık ama servis henüz durum bildirmedi.
            ConnState.CONNECTING, ConnState.STOPPED -> StatusKind.CONNECTING
            ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR -> StatusKind.PROBLEM
        }
    }
}
