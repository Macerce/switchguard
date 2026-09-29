package com.macerce.ewelinkalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macerce.ewelinkalarm.data.Store

/** Telefon açıldığında (veya uygulama güncellendiğinde) izleme açıksa servisi yeniden başlatır. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        if (Store(context).monitoringEnabled) {
            MonitorService.send(context, MonitorService.ACTION_START)
        }
    }
}
