package com.macerce.switchguard.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

/** Telefondaki SwitchGuard ile konuşma (Wearable Data Layer). Yollar telefondaki WearBridge ile aynı. */
object PhoneLink {
    const val PATH_STATUS = "/switchguard/status"
    private const val PATH_DISMISS = "/switchguard/dismiss"
    private const val PATH_TOGGLE = "/switchguard/toggle"
    private const val PATH_SYNC = "/switchguard/sync"
    private const val PATH_SCENE = "/switchguard/scene"
    private const val PATH_PRESENCE = "/switchguard/presence"

    fun runScene(context: Context, id: String) = send(context, PATH_SCENE, id.toByteArray(Charsets.UTF_8))

    fun setAway(context: Context, away: Boolean) = send(context, PATH_PRESENCE, (if (away) "1" else "0").toByteArray(Charsets.UTF_8))

    fun dismiss(context: Context) = send(context, PATH_DISMISS, ByteArray(0))

    fun toggle(context: Context, deviceId: String, channel: Int, on: Boolean) =
        send(context, PATH_TOGGLE, "$deviceId|$channel|${if (on) 1 else 0}".toByteArray(Charsets.UTF_8))

    fun sync(context: Context) = send(context, PATH_SYNC, ByteArray(0))

    /** Telefonun son yayınladığı durumu okur (uygulama açılınca). */
    fun readCurrent(context: Context) {
        Wearable.getDataClient(context).dataItems.addOnSuccessListener { items ->
            items.forEach { item ->
                if (item.uri.path == PATH_STATUS) {
                    DataMapItem.fromDataItem(item).dataMap.getString("json")?.let { WearState.update(context, it) }
                }
            }
            items.release()
        }
    }

    private fun send(context: Context, path: String, data: ByteArray) {
        val messages = Wearable.getMessageClient(context)
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { messages.sendMessage(it.id, path, data) }
        }
    }
}

/** Telefondan durum gelince: yeni alarmsa bilekte alarm ekranı (titreşimli), alarm bitince kapanır. */
class StatusListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type != DataEvent.TYPE_CHANGED || e.dataItem.uri.path != PhoneLink.PATH_STATUS) continue
            val json = DataMapItem.fromDataItem(e.dataItem).dataMap.getString("json") ?: continue
            val before = WearState.status.value
            val now = WearState.update(this, json) ?: continue
            WearAlarm.onStatus(this, before, now)
        }
    }
}

object WearAlarm {
    private const val CHANNEL = "alarm"
    private const val ID = 1

    fun onStatus(context: Context, before: PhoneStatus, now: PhoneStatus) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!now.alarming) {
            nm.cancel(ID)
            return
        }
        // Aynı alarma eklenen satır: bildirimi güncelle, yeniden çaldırma.
        val fresh = !before.alarming || before.alarmId != now.alarmId
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = AlarmActivity.PATTERN
            }
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setContentText(now.alarm.last())
            .setStyle(NotificationCompat.BigTextStyle().bigText(now.alarm.reversed().joinToString("\n")))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setOnlyAlertOnce(!fresh)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .build()
        nm.notify(ID, n)
    }
}
