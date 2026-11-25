package com.macerce.switchguard.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import kotlinx.coroutines.runBlocking

/**
 * Bildirimdeki eylem düğmeleri ("Kapat"): uygulamayı açmadan cihazı kapatır. Başarılıysa bildirim kalkar,
 * gidemezse yerine "kapatılamadı" bildirimi gelir. Dışa kapalı: yalnız uygulamanın kendi PendingIntent'i ulaşır.
 */
class NotificationActions : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_OFF) return
        val deviceId = intent.getStringExtra(EXTRA_DEVICE) ?: return
        val channel = intent.getIntExtra(EXTRA_CHANNEL, 0)
        val notifId = intent.getIntExtra(EXTRA_NOTIF, 0)
        val result = goAsync()
        Thread {
            try {
                val device = Store.get(context).snapshotsWithGroups.firstOrNull { it.id == deviceId }
                val ok = device != null && device.online &&
                    runBlocking { Actions.setSwitch(context, device, channel, false) }.isSuccess
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.cancel(notifId)
                if (!ok) {
                    val name = device?.name ?: deviceId
                    Notifier(context, Store.get(context)).notifyEvent(context.getString(R.string.notif_action_off_failed, name))
                }
            } finally {
                result.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_OFF = "com.macerce.switchguard.NOTIF_TURN_OFF"
        private const val EXTRA_DEVICE = "device"
        private const val EXTRA_CHANNEL = "channel"
        private const val EXTRA_NOTIF = "notif"

        fun turnOffIntent(context: Context, deviceId: String, channel: Int, notifId: Int): PendingIntent = PendingIntent.getBroadcast(
            context, notifId,
            Intent(context, NotificationActions::class.java).setAction(ACTION_OFF)
                .putExtra(EXTRA_DEVICE, deviceId).putExtra(EXTRA_CHANNEL, channel).putExtra(EXTRA_NOTIF, notifId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
