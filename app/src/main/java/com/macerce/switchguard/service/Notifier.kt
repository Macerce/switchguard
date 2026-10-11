package com.macerce.switchguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AlarmVolume
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.AlarmActivity
import com.macerce.switchguard.ui.FormFactor
import com.macerce.switchguard.ui.LoginActivity
import com.macerce.switchguard.ui.MainActivity

/**
 * Bildirim kanalları ve alarm sesi.
 * Alarm sesi bildirim kanalına bırakılmaz: FLAG_INSISTENT bildirim paneli açılınca susar.
 * Bunun yerine ses [MediaPlayer] ile döngüde çalar ve sadece [stopAlarm] ile durur.
 * Tüm metotlar servisin worker thread'inden çağrılır.
 */
class Notifier(private val context: Context, private val store: Store) {

    private val nm = context.getSystemService(NotificationManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val runtime = RuntimeState.get(context)
    private var player: MediaPlayer? = null
    /** Alarm çalarken müzik/video duraklasın diye alınan ses odağı. */
    private var focus: AudioFocusRequest? = null
    /** Şu an çalan alarm sesinin kaynağı (cihaza özel ya da genel). */
    private var playing: Uri? = null
    private val main = Handler(Looper.getMainLooper())

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

    /**
     * Alarm yerine seçilen sessiz/normal bildirim. Her olay ayrı bildirim olarak gruplanır.
     * Cihaza özel ses varsa bildirim sessiz kanala gider ve ses bir kez elle çalınır:
     * Android'de kanal sesi sonradan değiştirilemediği için cihaz başına ses ancak böyle olur.
     */
    fun notifyEvent(text: String, deviceSound: String? = null, title: String? = null) {
        val custom = deviceSound?.let(Uri::parse)
        if (custom != null) playOnce(custom)
        nm.notify(
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            Notification.Builder(context, if (custom != null) CH_EVENTS_CUSTOM else CH_EVENTS)
                .setSmallIcon(R.drawable.ic_stat_guard)
                .setContentTitle(title ?: context.getString(R.string.notif_event_title))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setGroup(GROUP_EVENTS)
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build()
        )
    }

    /**
     * Oturum düştü: izleme fiilen durdu. Kaydırılarak kapatılamaz; ancak giriş yapılınca
     * ([clearWarning]) kalkar. Aksi halde alarm uygulaması sessizce çalışmıyor olurdu.
     */
    fun sessionLost(text: String, detail: String) {
        val login = PendingIntent.getActivity(
            context, 2,
            Intent(context, LoginActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        nm.notify(
            ID_WARN,
            Notification.Builder(context, CH_SESSION)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setColor(0xFFD32F2F.toInt())
                .setContentTitle(context.getString(R.string.session_lost_title))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(if (detail.isEmpty()) text else "$text\n\n$detail"))
                .setCategory(Notification.CATEGORY_ERROR)
                .setOngoing(true)
                .setContentIntent(login)
                .addAction(Notification.Action.Builder(null, context.getString(R.string.action_relogin), login).build())
                .build()
        )
    }

    fun clearWarning() = nm.cancel(ID_WARN)

    /** İzleme kör kaldı: düzelene ya da kullanıcı kaldırana kadar durur. */
    fun monitorBlind(text: String) = nm.notify(
        ID_BLIND,
        Notification.Builder(context, CH_HEALTH)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setColor(0xFFD32F2F.toInt())
            .setContentTitle(context.getString(R.string.notif_blind_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ERROR)
            .setContentIntent(openApp())
            .build()
    )

    fun clearBlind() = nm.cancel(ID_BLIND)

    fun clearMonitoringStopped() {
        nm.cancel(ID_STOPPED)
        runtime.stoppedWarnedAt = 0
    }

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

    /**
     * Alarm bildirimini [lines] ile gösterir/günceller ve ses + titreşimi başlatır.
     * [deviceSound] son olayın cihazına özel ses; farklıysa çalan ses ona geçer (en yeni olay duyulsun).
     */
    fun showAlarm(lines: List<String>, deviceSound: String? = null) {
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
            // Ekran kapalı/kilitliyken tam ekran alarm ekranı açılır; telefon kullanılıyorsa üstte bildirim olarak görünür.
            .setFullScreenIntent(alarmScreen(), true)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.action_dismiss_alarm), dismiss).build())
            .build()
        nm.notify(ID_ALARM, notification)
        // Android TV bildirimleri ekranda göstermez: alarm, açık olan her şeyin üstüne tam ekran çıkar.
        if (FormFactor.isTv(context)) AlarmActivity.show(context)
        startSound(deviceSound?.let(Uri::parse))
    }

    fun stopAlarm() {
        nm.cancel(ID_ALARM)
        releasePlayer()
        vibrator().cancel()
        focus?.let { audio.abandonAudioFocusRequest(it) }
        focus = null
        restoreAlarmVolume()
    }

    /**
     * Alarm sesi telefonun "alarm" ses seviyesiyle çalar; seviye kısıksa alarm duyulmaz.
     * Açıksa alarm süresince en yükseğe çıkarılır, eski değer diske yazılır (servis bu arada kapanırsa
     * da sonradan geri alınabilsin).
     */
    private fun boostAlarmVolume() {
        val stream = AudioManager.STREAM_ALARM
        val target = AlarmVolume.target(audio.getStreamVolume(stream), audio.getStreamMaxVolume(stream), store.alarmBoost) ?: return
        if (runtime.savedAlarmVolume < 0) runtime.savedAlarmVolume = audio.getStreamVolume(stream)
        runCatching { audio.setStreamVolume(stream, target, 0) }
    }

    /** Yükseltilmiş alarm sesini eski seviyesine döndürür (yükseltilmediyse bir şey yapmaz). */
    fun restoreAlarmVolume() {
        val saved = runtime.savedAlarmVolume
        if (saved < 0) return
        runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0) }
        runtime.savedAlarmVolume = -1
    }

    private fun requestFocus() {
        if (focus != null) return
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAttributes)
            .setOnAudioFocusChangeListener { } // Alarm odağı kaybetse de susmaz; yalnızca kapat düğmesiyle durur.
            .build()
            .also { audio.requestAudioFocus(it) }
    }

    private fun releasePlayer() {
        player?.run { runCatching { stop() }; release() }
        player = null
        playing = null
    }

    private fun startSound(deviceSound: Uri?) {
        boostAlarmVolume()
        requestFocus()
        val wanted = deviceSound ?: globalSound()
        if (player == null || playing != wanted) {
            releasePlayer()
            // Cihaz sesi silinmiş/erişilemezse genel sese düş.
            player = createPlayer(wanted) ?: deviceSound?.let { createPlayer(globalSound()) }
        }
        if (store.vibrate) vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    /** Bildirim için cihaz sesini bir kez, en fazla [ONE_SHOT_MS] boyunca çalar (alarm sesleri uzun olabilir). */
    private fun playOnce(uri: Uri) {
        val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull() ?: return
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        runCatching { ringtone.play() }
        main.postDelayed({ runCatching { ringtone.stop() } }, ONE_SHOT_MS)
    }

    private fun globalSound(): Uri =
        store.alarmSoundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun createPlayer(uri: Uri): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(alarmAttributes)
            setDataSource(context, uri)
            isLooping = true
            setVolume(store.alarmVolume, store.alarmVolume)
            prepare()
            start()
        }
    }.getOrNull()
        ?.also { playing = uri }
        // Seçilen genel ses silinmiş/erişilemez olabilir: sistem varsayılanına düş.
        ?: if (uri.toString() == store.alarmSoundUri) {
            store.alarmSoundUri = null
            createPlayer(globalSound())
        } else null

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    private fun alarmScreen() = PendingIntent.getActivity(
        context, 3,
        Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun openApp() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val CH_ALARM = "alarm"
        const val CH_EVENTS = "events"
        const val CH_EVENTS_CUSTOM = "events_custom"
        const val CH_STATUS = "status"
        const val CH_SESSION = "session"
        const val CH_HEALTH = "health"
        const val GROUP_EVENTS = "events"
        const val ID_STATUS = 1
        const val ID_ALARM = 2
        const val ID_WARN = 3
        const val ID_SUMMARY = 4
        const val ID_BLIND = 5
        const val ID_STOPPED = 6
        private const val ONE_SHOT_MS = 6_000L
        private const val STOPPED_REPEAT_MS = 30 * 60_000L
        private val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /**
         * Bekçi, telefonun kapattığı izlemeyi yeniden başlatamadı (ör. Android 12+ pil kısıtlaması):
         * dokununca uygulama açılır ve izleme oradan yeniden başlar.
         */
        fun monitoringStopped(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val runtime = RuntimeState.get(context)
            val now = System.currentTimeMillis()
            // Bekçi dakikada bir dener: uyarı zaten görünüyorsa ya da yakın zamanda gösterildiyse tekrar çalmasın.
            if (nm.activeNotifications.any { it.id == ID_STOPPED } || now - runtime.stoppedWarnedAt < STOPPED_REPEAT_MS) return
            runtime.stoppedWarnedAt = now
            createChannels(context)
            val open = PendingIntent.getActivity(
                context, 4,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val text = context.getString(R.string.monitoring_stopped_body)
            nm.notify(
                ID_STOPPED,
                Notification.Builder(context, CH_HEALTH)
                    .setSmallIcon(R.drawable.ic_stat_alarm)
                    .setColor(0xFFD32F2F.toInt())
                    .setContentTitle(context.getString(R.string.monitoring_stopped_title))
                    .setContentText(text)
                    .setStyle(Notification.BigTextStyle().bigText(text))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
            )
        }

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
                NotificationChannel(CH_EVENTS_CUSTOM, context.getString(R.string.channel_events_custom), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_events_custom_desc)
                    setSound(null, null) // Cihaza özel ses elle çalınır.
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_SESSION, context.getString(R.string.channel_session), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_session_desc)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_HEALTH, context.getString(R.string.channel_health), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_health_desc)
                }
            )
        }
    }
}
