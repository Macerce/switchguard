package com.macerce.ewelinkalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import com.macerce.ewelinkalarm.ui.MainActivity

/**
 * Bildirimleri ve alarm sesini yönetir.
 * Alarm sesi bildirim kanalına bırakılmaz: FLAG_INSISTENT bildirim paneli açılınca susar.
 * Bunun yerine ses [MediaPlayer] ile döngüde çalar ve sadece [stopAlarm] ile durur.
 */
class AlarmNotifier(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private var player: MediaPlayer? = null

    init {
        nm.createNotificationChannel(
            NotificationChannel(CH_ALARM, "Cihaz alarmı", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cihaz durumu değişince, siz kapatana kadar çalar"
                setSound(null, null) // Ses MediaPlayer'dan geliyor.
                enableVibration(false) // Titreşim de elle, döngüde.
                setBypassDnd(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STATUS, "İzleme durumu", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_WARN, "Uyarılar", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun statusNotification(text: String): Notification =
        Notification.Builder(context, CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("eWeLink izleniyor")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .build()

    fun updateStatus(text: String) = nm.notify(ID_STATUS, statusNotification(text))

    fun warn(text: String) = nm.notify(
        ID_WARN,
        Notification.Builder(context, CH_WARN)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("eWeLink Alarm")
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
    )

    fun clearWarning() = nm.cancel(ID_WARN)

    /** Alarm bildirimini [lines] ile gösterir/günceller ve ses + titreşimi başlatır. */
    fun showAlarm(lines: List<String>) {
        val dismiss = PendingIntent.getService(
            context, 1,
            Intent(context, MonitorService::class.java).setAction(MonitorService.ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(context, CH_ALARM)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Cihaz değişikliği (${lines.size})")
            .setContentText(lines.last())
            .setStyle(Notification.BigTextStyle().bigText(lines.reversed().joinToString("\n")))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppIntent())
            .setDeleteIntent(dismiss) // Kaydırarak kapatmak da "Kapat" sayılır.
            .addAction(Notification.Action.Builder(null, "KAPAT", dismiss).build())
            .build()
        nm.notify(ID_ALARM, notification)
        startSound()
    }

    fun stopAlarm() {
        nm.cancel(ID_ALARM)
        player?.run { runCatching { stop() }; release() }
        player = null
        vibrator().cancel()
    }

    private fun startSound() {
        if (player == null) {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            player = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        }
        vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    private fun openAppIntent() = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val CH_ALARM = "alarm"
        const val CH_STATUS = "status"
        const val CH_WARN = "warn"
        const val ID_STATUS = 1
        const val ID_ALARM = 2
        const val ID_WARN = 3
    }
}
