package com.macerce.ewelinkalarm.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import com.macerce.ewelinkalarm.api.ApiException
import com.macerce.ewelinkalarm.api.AuthException
import com.macerce.ewelinkalarm.api.EwelinkClient
import com.macerce.ewelinkalarm.core.ChangeDetector
import com.macerce.ewelinkalarm.data.Store
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cihazları periyodik olarak sorgulayan foreground service.
 * Partial wakelock tutar: aksi halde ekran kapalıyken CPU uyur ve Handler zamanlayıcısı durur.
 */
class MonitorService : Service() {

    private lateinit var store: Store
    private lateinit var client: EwelinkClient
    private lateinit var notifier: AlarmNotifier
    private lateinit var worker: HandlerThread
    private lateinit var handler: Handler
    private var wakeLock: PowerManager.WakeLock? = null
    private var authWarned = false

    private val pollTask = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, store.intervalSec * 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        client = EwelinkClient(store)
        notifier = AlarmNotifier(this)
        worker = HandlerThread("monitor").also { it.start() }
        handler = Handler(worker.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Alarm listesi ve MediaPlayer'a sadece worker thread'den dokunulur (poll ile yarış olmasın).
        when (intent?.action) {
            ACTION_STOP -> {
                store.monitoringEnabled = false
                store.statusText = "İzleme kapalı"
                handler.post { store.pendingAlarm = emptyList(); notifier.stopAlarm() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                handler.post { store.pendingAlarm = emptyList(); notifier.stopAlarm() }
                if (!store.monitoringEnabled) stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST -> {
                handler.post { addAlarmLines(listOf("${now()}  Test alarmı")) }
                return START_STICKY
            }
        }
        startMonitoring()
        return START_STICKY
    }

    @SuppressLint("WakelockTimeout") // Bilinçli: izleme süresince tutulur, onDestroy'da bırakılır.
    private fun startMonitoring() {
        store.monitoringEnabled = true
        val notification = notifier.statusNotification(store.statusText)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(AlarmNotifier.ID_STATUS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(AlarmNotifier.ID_STATUS, notification)
        }
        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ewelinkalarm:monitor")
                .apply { setReferenceCounted(false); acquire() }
            // Servis yeniden başladıysa (ör. telefon açıldı) kapatılmamış alarmı geri getir.
            handler.post { store.pendingAlarm.takeIf { it.isNotEmpty() }?.let { notifier.showAlarm(it) } }
        }
        handler.removeCallbacks(pollTask)
        handler.post(pollTask)
    }

    private fun poll() {
        if (!store.isLoggedIn) {
            setStatus("Giriş yapılmamış — uygulamayı açıp giriş yapın")
            return
        }
        try {
            val current = client.getDevices()
            val previous = store.snapshots.associateBy { it.id }
            val changes = ChangeDetector.detect(previous, current.associateBy { it.id }, store.effectiveMonitored())
            store.snapshots = current
            if (changes.isNotEmpty()) {
                val time = now()
                addAlarmLines(changes.map { "$time  ${it.describe()}" })
            }
            if (authWarned) {
                notifier.clearWarning()
                authWarned = false
            }
            val watched = store.effectiveMonitored().size
            setStatus("Son kontrol ${now()} · $watched cihaz izleniyor")
        } catch (e: AuthException) {
            setStatus("Oturum geçersiz — yeniden giriş yapın")
            if (!authWarned) {
                notifier.warn("eWeLink oturumu sona erdi. İzleme için uygulamadan yeniden giriş yapın.")
                authWarned = true
            }
        } catch (e: IOException) {
            // Telefonun interneti yok: cihaz "offline" sayılmaz, sadece durum güncellenir.
            setStatus("eWeLink'e ulaşılamıyor (${now()}) — internet bağlantısını kontrol edin")
        } catch (e: ApiException) {
            setStatus("eWeLink hatası ${e.code} (${now()})")
        } catch (e: Exception) {
            setStatus("Beklenmeyen hata: ${e.message}")
        }
    }

    private fun addAlarmLines(lines: List<String>) {
        store.pendingAlarm = store.pendingAlarm + lines
        notifier.showAlarm(store.pendingAlarm)
    }

    private fun setStatus(text: String) {
        store.statusText = text
        notifier.updateStatus(text)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollTask)
        handler.post { notifier.stopAlarm() }
        worker.quitSafely()
        wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_TEST = "test"

        private fun now() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        fun send(context: Context, action: String) {
            val intent = Intent(context, MonitorService::class.java).setAction(action)
            if (action == ACTION_START) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
