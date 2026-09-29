package com.macerce.switchguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macerce.switchguard.data.Store

/** Telefon açıldığında (veya uygulama güncellendiğinde) izleme açıksa servisi yeniden başlatır. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        if (Store.get(context).monitoringEnabled) {
            MonitorService.send(context, MonitorService.ACTION_START)
        }
    }
}

/** AlarmManager "tick"i: servis çalışıyorsa bir tur heartbeat/senkron yaptırır. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MonitorService.instance?.onAlarmTick()
    }
}
