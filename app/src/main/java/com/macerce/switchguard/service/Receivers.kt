package com.macerce.switchguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.macerce.switchguard.data.Store

/** Telefon açıldığında (veya uygulama güncellendiğinde) izleme açıksa servisi yeniden başlatır. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        // Alarmlar yeniden başlatmada silinir.
        SummaryScheduler.schedule(context)
        WeeklyScheduler.schedule(context)
        // Bölge kayıtları da yeniden başlatmada silinir.
        ZoneWatcher.sync(context)
        if (Store.get(context).monitoringEnabled) {
            MonitorService.send(context, MonitorService.ACTION_START)
        }
    }
}

/**
 * AlarmManager "tick"i: servis çalışıyorsa bir tur heartbeat/senkron yaptırır. Bekçi görevi de görür:
 * izleme açık ama servis yoksa (telefon kapatmış) yeniden başlatır; aksi halde ancak uygulama açılınca
 * ya da telefon yeniden başlayınca geri gelirdi.
 */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = MonitorService.instance
        if (service != null && service.isMonitoring) {
            service.onAlarmTick()
            return
        }
        if (!Store.get(context).monitoringEnabled) return
        // Bekçi bir sonraki denemeyi kendisi kurar: servis başlamazsa da denetim sürsün.
        MonitorService.scheduleTick(context)
        // Android 12+ arka plandan ön plan servisi başlatmaya yalnızca pil optimizasyonu muafiyetiyle izin verir.
        // Başlatılamazsa kullanıcı uyarılır: sessizce izlenmiyor olmaktan iyidir.
        runCatching { MonitorService.send(context, MonitorService.ACTION_START) }
            .onFailure {
                Log.w("SwitchGuard", "watchdog restart failed", it)
                Notifier.monitoringStopped(context)
            }
        StatusSurfaces.refresh(context)
    }
}
