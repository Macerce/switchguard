package com.macerce.switchguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.MainActivity

/**
 * Bildirim kanalları ve alarm sesi.
 * Alarm sesi bildirim kanalına bırakılmaz: FLAG_INSISTENT bildirim paneli açılınca susar.
 * Bunun yerine ses [MediaPlayer] ile döngüde çalar ve sadece [stopAlarm] ile durur.
 * Tüm metotlar servisin worker thread'inden çağrılır.
 */
class Notifier(private val context: Context, private val store: Store) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private var player: MediaPlayer? = null

    init {
        createChannels(context)
    }

    fun statusNotification(title: String, text: String): Notification =
        Notification.Builder(context, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(openApp())
            .build()

    fun updateStatus(title: String, text: String) = nm.notify(ID_STATUS, statusNotification(title, text))

    /** Alarm yerine seçilen sessiz/normal bildirim. Her olay ayrı bildirim olarak gruplanır. */
    fun notifyEvent(text: String) {
        nm.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            Notification.Builder(context, CH_EVENTS)
                .setSmallIcon(R.drawable.ic_stat_guard)
                .setContentTitle(context.getString(R.string.notif_event_title))
                .setContentText(text)
                .setGroup(GROUP_EVENTS)
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build()
        )
    }

    fun warn(text: String) = nm.notify(
        ID_WARN,
        Notification.Builder(context, CH_EVENTS)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
    )

    fun clearWarning() = nm.cancel(ID_WARN)

    fun notifySummary(title: String, lines: List<String>) = nm.notify(
        ID_SUMMARY,
        Notification.Builder(context, CH_EVENTS)
            .setSmallIcon(R.drawable.ic_stat_guard)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(Notification.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
    )

    /** Alarm bildirimini [lines] ile gösterir/günceller ve ses + titreşimi başlatır. */
    fun showAlarm(lines: List<String>) {
        val dismiss = PendingIntent.getService(
            context, 1,
            Intent(context, MonitorService::class.java).setAction(MonitorService.ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(context, CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setColor(0xFFD32F2F.toInt())
            .setContentTitle(context.getString(R.string.notif_alarm_title, lines.size))
            .setContentText(lines.last())
            .setStyle(Notification.BigTextStyle().bigText(lines.reversed().joinToString("\n")))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openApp())
            .setDeleteIntent(dismiss) // Kaydırarak kapatmak da "Kapat" sayılır.
            .addAction(Notification.Action.Builder(null, context.getString(R.string.action_dismiss_alarm), dismiss).build())
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
        if (player == null) player = createPlayer(soundUri())
        if (store.vibrate) vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    private fun soundUri(): Uri =
        store.alarmSoundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun createPlayer(uri: Uri): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            setVolume(store.alarmVolume, store.alarmVolume)
            prepare()
            start()
        }
    }.getOrNull()
        // Seçilen ses silinmiş/erişilemez olabilir: varsayılana düş.
        ?: if (store.alarmSoundUri != null) {
            store.alarmSoundUri = null
            createPlayer(soundUri())
        } else null

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    private fun openApp() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val CH_ALARM = "alarm"
        const val CH_EVENTS = "events"
        const val CH_STATUS = "status"
        const val GROUP_EVENTS = "events"
        const val ID_STATUS = 1
        const val ID_ALARM = 2
        const val ID_WARN = 3
        const val ID_SUMMARY = 4

        fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH_ALARM, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_alarm_desc)
                    setSound(null, null) // Ses MediaPlayer'dan geliyor.
                    enableVibration(false) // Titreşim de elle, döngüde.
                    setBypassDnd(true)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_EVENTS, context.getString(R.string.channel_events), NotificationManager.IMPORTANCE_DEFAULT)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }
}
