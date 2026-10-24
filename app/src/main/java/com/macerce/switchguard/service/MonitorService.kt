package com.macerce.switchguard.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.macerce.switchguard.R
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.api.AuthException
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.Environment
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.api.TelegramException
import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.api.TuyaSocket
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlarmReminders
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationEngine
import com.macerce.switchguard.core.BlindWatch
import com.macerce.switchguard.core.Change
import com.macerce.switchguard.core.ChangeDetector
import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.MonitorGap
import com.macerce.switchguard.core.OfflineGrace
import com.macerce.switchguard.core.OutageWatch
import com.macerce.switchguard.core.TelegramBot
import com.macerce.switchguard.core.Watchdog
import com.macerce.switchguard.core.RuleEngine
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.TokenRefresh
import com.macerce.switchguard.core.TuyaEvent
import com.macerce.switchguard.core.TuyaParser
import com.macerce.switchguard.core.WsEvent
import com.macerce.switchguard.core.WsMessages
import com.macerce.switchguard.core.toArgs
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import com.macerce.switchguard.ui.AlarmActivity
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Cihazları izleyen foreground service.
 *
 * Kaynaklar: WebSocket (anlık), periyodik HTTP senkronu (kaçan olaylar için), WebSocket
 * kullanılamazsa HTTP sorgulama. Tüm mantık tek bir worker thread'de çalışır.
 * eWeLink ve Tuya birbirinden bağımsız iki kaynaktır; cihaz listeleri [Cloud]'a göre birleştirilir.
 *
 * Zamanlama: telefon uyanıkken [handler] döngüsü, uyurken AlarmManager "tick"i aynı [tick]'i çağırır.
 */
class MonitorService : Service() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))


    private lateinit var store: Store
    private lateinit var log: EventLog
    private lateinit var client: EwelinkClient
    private lateinit var notifier: Notifier
    private lateinit var socket: EwelinkSocket
    private lateinit var worker: HandlerThread
    private lateinit var handler: Handler
    private lateinit var strings: EventStrings
    private val labels: ChangeLabels get() = strings.labels
    private lateinit var metrics: MetricsLog
    private lateinit var runtime: RuntimeState
    private var automations = AutomationEngine()

    private val blindWatch = BlindWatch()
    /** Kapatılan ama sürmekte olan alarmların hatırlatmaları (bkz. [checkReminders]). */
    private var reminders = AlarmReminders()

    /** Başlarken diskteki kayıttan yüklenir (bkz. [restoreRuntime]). */
    private var grace = OfflineGrace()
    private var outage = OutageWatch()
    private var running = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var authFailed = false
    private var wsUnsupported = false
    /** Anlık bağlantının neden kurulamadığı; periyodik senkron bu açıklamayı silmesin diye saklanır. */
    private var wsIssue = ""
    private var reconnectAttempt = 0
    private var nextReconnectAt = 0L
    private var lastSyncAt = 0L
    /** Anlık bağlantı canlıyken üst üste başarısız senkron sayısı (bkz. [retrySyncSoon]). */
    private var syncRetryAttempt = 0
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    /** Kanal gücü izlenen cihazlar: son canlı enerji isteği, son gelen kanal gücü, "veri yok" uyarısı verildi mi. */
    private val liveEnergyRequestedAt = mutableMapOf<String, Long>()
    private val channelPowerSeenAt = mutableMapOf<String, Long>()
    private val powerStaleWarned = mutableSetOf<String>()

    private lateinit var tuya: TuyaClient
    private lateinit var tuyaSocket: TuyaSocket
    private var tuyaIssue = ""
    private var tuyaReconnectAttempt = 0
    private var tuyaNextConnectAt = 0L
    private var lastTuyaSyncAt = 0L
    private var tuyaSyncRetryAttempt = 0
    /** Bilgiler hatalı ya da deneme süresi bitmiş: kota harcamamak için seyrek denenir. */
    private var tuyaBlocked = false
    private var tuyaWarned = false

    /** Demo: gücü düşürülmüş kanallar (cihaz → kanallar) ve son dalgalanma zamanı. */
    private val demoStalled = mutableMapOf<String, MutableSet<Int>>()
    private var lastDemoJitterAt = 0L

    /** Kaynakların ayrı ayrı durumu; ekrana ikisinin kötüsü yansır (bkz. [publishState]). */
    private var ewState: Pair<ConnState, String>? = null
    private var tuyaState: Pair<ConnState, String>? = null

    private val loop = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, LOOP_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = Store.get(this)
        log = EventLog.get(this)
        metrics = MetricsLog.get(this)
        runtime = RuntimeState.get(this)
        client = EwelinkClient(store)
        notifier = Notifier(this, store)
        strings = EventLog.strings(this)
        worker = HandlerThread("monitor").also { it.start() }
        handler = Handler(worker.looper)
        socket = EwelinkSocket(store, socketListener)
        tuya = TuyaClient(store)
        tuyaSocket = TuyaSocket(store, tuyaListener)
        instance = this
        // İşçi thread'inin ilk işi: sonraki hiçbir senkron/kayıt, diskteki bekleyen işlerin üstüne yazamasın.
        handler.post { restoreRuntime() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                handler.post { dismissAlarm() }
                // Süreç ölmüş ve servis yalnızca bu kapatma için yeniden doğmuş olabilir: izleme açıksa kapanmak
                // yerine devam etsin (bildirim düğmesinden gelen başlatmaya Android izin verir).
                if (!running) {
                    if (store.monitoringEnabled) {
                        runCatching { startMonitoring() }.onFailure {
                            Log.w(TAG, "could not resume monitoring after dismiss", it)
                            stopSelf()
                        }
                    } else stopSelf()
                }
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_TEST -> {
                val silent = intent.getBooleanExtra(EXTRA_SILENT, false)
                // Gecikme, telefonu kilitleyip alarm ekranının kilit ekranında açıldığını görmek için.
                val delay = intent.getLongExtra(EXTRA_DELAY_MS, 0).coerceIn(0, MAX_TEST_DELAY_MS)
                handler.postDelayed({
                    log.add("", getString(R.string.app_name), EventKind.TEST, strings.test, AlertAction.ALARM.name, EventArgs.Test)
                    addAlarm(getString(if (silent) R.string.test_alarm_line_silent else R.string.test_alarm_line), silent = silent, test = true)
                }, delay)
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_DEMO -> {
                val op = intent.getStringExtra(EXTRA_DEMO_OP).orEmpty()
                val id = intent.getStringExtra(EXTRA_DEVICE).orEmpty()
                val channel = intent.getIntExtra(EXTRA_CHANNEL, 0)
                // Simülasyon gerçek akıştan geçsin diye izleme kapalıysa önce açılır.
                startMonitoring()
                handler.post { demoSimulate(op, id, channel) }
                return START_STICKY
            }
            ACTION_SYNC -> {
                if (running) handler.post { sync() }
                return START_STICKY
            }
            ACTION_RESTART -> handler.post {
                authFailed = false
                wsUnsupported = false
                notifier.clearWarning()
                reconnectNow()
                resetTuya()
            }
        }
        startMonitoring()
        return START_STICKY
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    private fun startMonitoring() {
        store.monitoringEnabled = true
        val status = notifier.statusNotification(getString(R.string.app_name), getString(R.string.state_connecting))
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.ID_STATUS, status, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.ID_STATUS, status)
        }
        if (running) {
            handler.post { applyWakeMode() }
            return
        }
        running = true
        notifier.clearMonitoringStopped()
        registerNetworkCallback()
        registerPowerReceiver()
        handler.post {
            applyWakeMode()
            // Bu çalışma başladı: bundan sonra beklenmedik bir kapanma "kesinti" sayılır.
            runtime.cleanStop = false
            heartbeat(System.currentTimeMillis(), force = true)
            // Kapatılmamış alarm varsa (ör. telefon yeniden başladı) geri getir; yoksa önceki çalışmada
            // yükseltilip geri alınamamış alarm sesini eski seviyesine döndür.
            store.pendingAlarm.takeIf { it.isNotEmpty() }?.let { notifier.showAlarm(it, silent = runtime.silentAlarm) }
                ?: notifier.restoreAlarmVolume()
            setState(ConnState.CONNECTING)
            runCatching { metrics.prune() }
            SummaryScheduler.schedule(this)
            WeeklyScheduler.schedule(this)
            sync()
            reconnectNow()
            tuyaTick(SystemClock.elapsedRealtime())
            handler.post(loop)
        }
        scheduleAlarmTick()
    }

    private fun stopMonitoring() {
        // Kullanıcının kendi kapatması: sonraki açılışta "kesinti" sayılmasın.
        runtime.cleanStop = true
        store.monitoringEnabled = false
        running = false
        LiveState.setConnection(ConnState.STOPPED)
        handler.removeCallbacksAndMessages(null)
        handler.post {
            socket.close()
            tuyaSocket.close()
            store.pendingAlarm = emptyList()
            runtime.alarmStartedAt = 0
            runtime.silentAlarm = false
            endEscalation()
            runtime.clearEngine()
            grace = OfflineGrace()
            outage = OutageWatch()
            // Bekçi: diğer cihaz "izleme kapatıldı"yı görsün, sessizlik alarmı vermesin.
            if (store.watchdogMode == Store.WATCHDOG_SEND) sendBeat(System.currentTimeMillis(), stopped = true)
            automations = AutomationEngine()
            reminders = AlarmReminders()
            notifier.clearBlind()
            notifier.stopAlarm()
            // Kaydırılamayan oturum uyarısı izleme kapatılınca asılı kalmasın.
            notifier.clearWarning()
            StatusSurfaces.refresh(this)
        }
        cancelAlarmTick()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        instance = null
        running = false
        networkCallback?.let { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) }
        powerReceiver?.let { runCatching { unregisterReceiver(it) } }
        powerReceiver = null
        // İzleme hâlâ açıksa bekçi tick'i kalır: servisi yeniden başlatacak olan odur.
        if (!store.monitoringEnabled) cancelAlarmTick()
        // Kuyruğu boşaltma: stopMonitoring'in temizlik işleri henüz çalışmamış olabilir.
        // quitSafely zamanı gelmiş işleri çalıştırır, ileri tarihli (tick vb.) olanları atar.
        handler.removeCallbacks(loop)
        handler.post {
            socket.close()
            tuyaSocket.close()
            notifier.stopAlarm()
        }
        worker.quitSafely()
        wakeLock?.release()
        wakeLock = null
        if (LiveState.connection.value.state != ConnState.STOPPED) LiveState.setConnection(ConnState.STOPPED)
        // İzleme kullanıcı kapatmadan bittiyse widget "canlı" görünmeye devam etmesin.
        StatusSurfaces.refresh(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** "Her zaman uyanık" modu: açıksa işlemci uyanık tutulur, yerel döngü hiç durmaz. */
    @SuppressLint("WakelockTimeout") // Bilinçli ve kullanıcı seçimi: izleme süresince tutulur.
    private fun applyWakeMode() {
        if (store.alwaysAwake && wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "switchguard:monitor")
                .apply { setReferenceCounted(false); acquire() }
        } else if (!store.alwaysAwake) {
            wakeLock?.release()
            wakeLock = null
        }
    }

    // ---------------------------------------------------------------- zamanlama

    private fun tick() {
        // TV: alarm sürüyor ama ekranı kapanmış/arkada kalmışsa yeniden öne getir.
        if (store.pendingAlarm.isNotEmpty()) AlarmActivity.ensureShown(this)
        if (!running) return
        val now = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()

        grace.due(wall).forEach { dispatch(it, store.rulesFor(it.deviceId).actionFor(it.eventType)) }
        checkOutage(wall)
        runAutomations(automations.onTick(store.snapshotsWithGroups.associateBy { it.id }, store.automations, wall, env()))
        weatherTick(wall)
        tuyaTick(now)
        demoTick(wall)
        checkReminders(wall)
        checkEscalation(wall)
        checkVacation(wall)
        pollTelegram(wall)
        watchdogTick(wall)
        energyTick(wall)
        checkSecrets()
        persistRuntime()
        heartbeat(wall)
        checkBlind(wall)

        if (authFailed || !store.isLoggedIn) return
        keepChannelPowerLive(wall)
        socket.sendPingIfDue(wall)
        if (socket.isStale(wall)) {
            socket.close()
            scheduleReconnect()
        }
        if (!socket.ready && !wsUnsupported && now >= nextReconnectAt && nextReconnectAt != 0L) {
            reconnectNow()
        }
        val syncEvery = if (socket.ready) RESYNC_MS else store.pollIntervalSec * 1000L
        if (now - lastSyncAt >= syncEvery) sync()

        grace.nextDueAt()?.let { due -> handler.postDelayed({ tick() }, (due - wall).coerceAtLeast(0) + 50) }
    }

    /** İzleme fiilen çalışıyor mu (örnek var ama durdurulmuş olabilir). */
    val isMonitoring: Boolean get() = running

    /** AlarmManager tick'i: telefon uyurken de heartbeat ve senkron devam etsin. */
    fun onAlarmTick() {
        if (!running) return
        // tick bitene kadar işlemci uyumasın (kısa ve zaman aşımlı).
        val wl = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "switchguard:tick")
        wl.acquire(20_000)
        handler.post {
            try { tick() } finally { if (wl.isHeld) wl.release() }
        }
        scheduleAlarmTick()
    }

    private fun scheduleAlarmTick() = scheduleTick(this)

    private fun cancelAlarmTick() = getSystemService(AlarmManager::class.java).cancel(tickPendingIntent(this))

    private fun registerNetworkCallback() {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.post {
                    if (running && !socket.ready) reconnectNow()
                    if (running && store.hasTuya && !tuyaSocket.ready) {
                        tuyaNextConnectAt = 0
                        tuyaTick(SystemClock.elapsedRealtime())
                    }
                }
            }
        }
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(cb)
        networkCallback = cb
    }

    // ---------------------------------------------------------------- WebSocket

    private val socketListener = object : EwelinkSocket.Listener {
        override fun onReady() = handler.post {
            reconnectAttempt = 0
            nextReconnectAt = 0
            wsIssue = ""
            // Bağlantı yokken kaçan değişiklikleri yakala.
            sync()
        }.let { }

        override fun onEvent(event: WsEvent) = handler.post {
            if (event is WsEvent.Update) applyUpdate(event)
        }.let { }

        override fun onClosed(reason: String, fatal: Boolean) = handler.post {
            if (!running) return@post
            Log.w(TAG, "WebSocket closed: $reason (fatal=$fatal)")
            if (fatal) {
                wsUnsupported = true
                wsIssue = "${getString(R.string.detail_ws_unsupported)} ($reason)"
                setState(ConnState.POLLING, wsIssue)
            } else {
                scheduleReconnect()
                wsIssue = "${getString(R.string.detail_reconnecting)} ($reason)"
                if (!authFailed) setState(ConnState.POLLING, wsIssue)
            }
        }.let { }

        override fun onAuthRejected() = handler.post {
            if (!running) return@post
            // 5 dk'lık senkronu bekleme: HTTP isteği token'ı yeniler ya da oturumun düştüğünü hemen ortaya çıkarır.
            sync()
            if (!authFailed) scheduleReconnect()
        }.let { }
    }

    private fun reconnectNow() {
        if (!running || authFailed || wsUnsupported || !store.isLoggedIn) return
        nextReconnectAt = 0
        try {
            if (store.userApiKey.isEmpty()) {
                // Profil yolu bazı App ID'lere kapalı (407); o zaman apikey cihaz listesinden, o da yoksa (yalnız
                // paylaşılan cihazlar) ev listesinden alınır.
                try { client.loadProfile() } catch (e: ApiException) { Log.w(TAG, "profile unavailable: ${e.message}") }
                if (store.userApiKey.isEmpty()) client.getDevices()
                if (store.userApiKey.isEmpty()) {
                    try { client.loadFamilyApiKey() } catch (e: ApiException) { Log.w(TAG, "family unavailable: ${e.message}") }
                }
            }
            if (store.userApiKey.isEmpty()) {
                wsUnsupported = true
                wsIssue = "${getString(R.string.detail_ws_unsupported)} (no apikey)"
                Log.w(TAG, "WebSocket unavailable: no apikey")
                setState(ConnState.POLLING, wsIssue)
                return
            }
            socket.connect()
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket connect failed", e)
            wsIssue = "${getString(R.string.detail_reconnecting)} (${e.javaClass.simpleName}: ${e.message})"
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        val delays = longArrayOf(5_000, 15_000, 30_000, 60_000, 120_000)
        nextReconnectAt = SystemClock.elapsedRealtime() + delays[reconnectAttempt.coerceAtMost(delays.size - 1)]
        reconnectAttempt++
        handler.postDelayed({ tick() }, nextReconnectAt - SystemClock.elapsedRealtime() + 50)
    }

    // ---------------------------------------------------------------- durum işleme

    /** HTTP ile tam liste çekip farkları işler. */
    private fun sync() {
        lastSyncAt = SystemClock.elapsedRealtime()
        if (!store.isLoggedIn) {
            // Yalnızca Tuya kullanılıyorsa eWeLink'in girişsiz olması hata değildir.
            if (store.hasTuya || store.demoMode) {
                ewState = null
                if (store.snapshots.any { it.cloud == Cloud.EWELINK }) replaceCloud(Cloud.EWELINK, emptyList())
            } else onAuthFailed()
            return
        }
        try {
            replaceCloud(Cloud.EWELINK, client.getDevices())
            syncRetryAttempt = 0
            if (authFailed) {
                authFailed = false
                notifier.clearWarning()
            }
            if (socket.ready) setState(ConnState.LIVE, synced = true)
            else setState(ConnState.POLLING, wsIssue, synced = true)
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: IOException) {
            // Anlık bağlantı canlıyken tek bir HTTP hatası izlemeyi durdurmaz (bkz. retrySyncSoon).
            if (socket.ready) retrySyncSoon(e)
            // Telefonun interneti yok: cihaz "offline" sayılmaz, sadece durum güncellenir.
            else setState(ConnState.NO_INTERNET)
        } catch (e: ApiException) {
            if (socket.ready && TokenRefresh.isTransient(e.code)) retrySyncSoon(e)
            else setState(ConnState.ERROR, getString(R.string.detail_api_error, e.code))
        } catch (e: Exception) {
            setState(ConnState.ERROR, e.message.orEmpty())
        }
    }

    /**
     * Anlık bağlantı (WebSocket) canlıyken HTTP senkronu düştü (ör. ağ değişiminde tek bir zaman aşımı). İzleme sürüyor,
     * olaylar WebSocket'ten geliyor: durum "internet yok"a çevrilmez (kullanıcı izlemenin koptuğunu sanıyordu; bir sonraki
     * olağan senkron 5 dk sonradır), senkron [SYNC_RETRY_MS] aralıklarıyla yeniden denenir. Bağlantı gerçekten koptuysa
     * WebSocket de düşer (ping/bayatlık denetimi) ve durum oradan güncellenir.
     */
    private fun retrySyncSoon(cause: Exception) {
        val delay = SYNC_RETRY_MS[syncRetryAttempt.coerceAtMost(SYNC_RETRY_MS.size - 1)]
        syncRetryAttempt++
        Log.w(TAG, "eWeLink sync failed while live (${cause.javaClass.simpleName}); retrying in ${delay / 1000}s")
        handler.postDelayed({ if (running && socket.ready) sync() }, delay)
    }

    /**
     * Uygulamadan gönderilen aç/kapa komutu başarılı olunca çağrılır. Bulut, cihazın yeni durumunu
     * ancak cihaz geri bildirdikten sonra (≈1-2 sn) döndürdüğünden hemen senkron yapmak eski durumu
     * geri yazar. Bu yüzden yeni durum hemen uygulanır, bulutla karşılaştırma gecikmeli yapılır.
     */
    fun onControlled(deviceId: String, channel: Int, on: Boolean) {
        if (Demo.isDemo(deviceId)) {
            handler.post { demoUpdate(deviceId) { it.copy(switches = it.switches + (channel to on)) } }
            return
        }
        handler.post { applyUpdate(WsEvent.Update(deviceId, online = null, switches = mapOf(channel to on))) }
        val isTuya = store.snapshots.any { it.id == deviceId && it.cloud == Cloud.TUYA }
        handler.postDelayed({
            if (!running) return@postDelayed
            // Tuya'da sonuç mesaj servisinden gelir; kota harcamamak için yalnızca o yoksa sorgulanır.
            if (!isTuya) sync() else if (!tuyaSocket.ready) syncTuya()
        }, CONFIRM_DELAY_MS)
    }

    /** Cihazın bağlı olduğu buluta aç/kapa komutu gönderir. */
    private fun controlSwitch(device: DeviceSnapshot, channel: Int, on: Boolean) {
        when (device.cloud) {
            Cloud.TUYA -> tuya.setSwitch(device, channel, on)
            Cloud.EWELINK -> client.setSwitch(device, channel, on)
            Cloud.DEMO -> handler.post { demoUpdate(device.id) { it.copy(switches = it.switches + (channel to on)) } }
        }
    }

    // ---------------------------------------------------------------- Demo

    /** Demo cihazını değiştirir; güç değerleri anahtarlara göre yeniden hesaplanır ve değişiklik gerçek akıştan geçer. */
    private fun demoUpdate(deviceId: String, change: (DeviceSnapshot) -> DeviceSnapshot) {
        val list = store.snapshots
        val before = list.firstOrNull { it.id == deviceId && it.cloud == Cloud.DEMO } ?: return
        val changed = change(before)
        // Kapanan kanalın "güç düştü" durumu sıfırlanır; tekrar açılınca normal çalışır.
        demoStalled[deviceId]?.retainAll { changed.switches[it] == true }
        val after = Demo.recompute(changed, demoStalled[deviceId].orEmpty())
        if (after == before) return
        process(list.map { if (it.id == after.id) after else it })
        publishState(synced = true)
    }

    /**
     * Arayüzdeki simülasyon düğmeleri. Bunlar "dışarıdan" olmuş sayılır (biri cihazı kapattı, elektrik
     * kesildi...), bu yüzden kurallara göre alarm çalar; uygulamadan yapılan aç/kapa ise alarm çaldırmaz.
     */
    private fun demoSimulate(op: String, deviceId: String, channel: Int) {
        if (!store.demoMode) return
        when (op) {
            DEMO_TOGGLE -> demoUpdate(deviceId) { it.copy(switches = it.switches + (channel to (it.switches[channel] != true))) }
            DEMO_ONLINE -> demoUpdate(deviceId) { it.copy(online = !it.online) }
            DEMO_POWER -> {
                val set = demoStalled.getOrPut(deviceId) { mutableSetOf() }
                if (!set.add(channel)) set.remove(channel)
                demoUpdate(deviceId) { it }
                // Kanal gücü otomasyonu süre dolunca tick'te tetiklenir.
                runAutomations(emptyList())
            }
        }
    }

    /** Ölçülen değerler gerçek cihazdaki gibi dakikada bir hafifçe oynasın (grafikler de dolsun). */
    private fun demoTick(wall: Long) {
        if (!store.demoMode || wall - lastDemoJitterAt < DEMO_JITTER_MS) return
        lastDemoJitterAt = wall
        val random = kotlin.random.Random(wall)
        val list = store.snapshots
        if (list.none { it.cloud == Cloud.DEMO && it.hasEnergy }) return
        process(list.map {
            if (it.cloud == Cloud.DEMO && it.online) Demo.recompute(it, demoStalled[it.id].orEmpty(), random) else it
        })
    }

    /** Bir bulutun cihazlarını yenileriyle değiştirip farkları işler; diğer bulutun cihazlarına dokunmaz. */
    private fun replaceCloud(cloud: Cloud, devices: List<DeviceSnapshot>) {
        process(store.snapshots.filter { it.cloud != cloud } + devices)
    }

    private fun applyUpdate(update: WsEvent.Update) {
        // Kanal gücü (SPM/DualR3: actPow_N) ya da tek kanallı güç ölçerde toplam güç geldi.
        if (update.params?.keys()?.asSequence()?.any { it.startsWith("actPow_") || it == "power" } == true) {
            channelPowerSeenAt[update.deviceId] = System.currentTimeMillis()
            powerStaleWarned.remove(update.deviceId)
        }
        val list = store.snapshots
        val before = list.firstOrNull { it.id == update.deviceId } ?: return
        val after = WsMessages.apply(before, update)
        if (after == before) return
        process(list.map { if (it.id == after.id) after else it })
        setState(ConnState.LIVE, synced = true)
    }

    // ---------------------------------------------------------------- Tuya

    private val tuyaListener = object : TuyaSocket.Listener {
        override fun onReady() = handler.post {
            if (!running) return@post
            tuyaReconnectAttempt = 0
            tuyaNextConnectAt = 0
            tuyaIssue = ""
            // Bağlantı yokken olanları yakala; biriken eski mesajlar bu okumadan eski sayılıp atlanır.
            syncTuya()
        }.let { }

        override fun onEvent(event: TuyaEvent) = handler.post { if (running) applyTuya(event) }.let { }

        override fun onClosed(reason: String, rejected: Boolean) = handler.post {
            if (!running || !store.hasTuya) return@post
            Log.w(TAG, "Tuya message service closed: $reason (rejected=$rejected)")
            tuyaIssue = getString(if (rejected) R.string.tuya_ws_rejected else R.string.detail_reconnecting) + " ($reason)"
            scheduleTuyaReconnect(rejected)
            if (!tuyaBlocked) setTuyaState(ConnState.POLLING, tuyaIssue)
        }.let { }
    }

    private fun tuyaTick(now: Long) {
        if (!store.hasTuya) {
            if (tuyaState != null || tuyaSocket.ready) {
                tuyaSocket.close()
                tuyaState = null
                publishState()
            }
            return
        }
        if (!tuyaBlocked && !tuyaSocket.ready && now >= tuyaNextConnectAt) {
            // Bağlanma sürerken tekrar denenmesin; sonuç onReady/onClosed ile gelir.
            tuyaNextConnectAt = now + TUYA_CONNECT_TIMEOUT_MS
            tuyaSocket.connect()
        }
        val every = when {
            tuyaBlocked -> TUYA_BLOCKED_MS
            tuyaSocket.ready -> TUYA_RESYNC_MS
            else -> TUYA_POLL_MS
        }
        if (lastTuyaSyncAt == 0L || now - lastTuyaSyncAt >= every) syncTuya()
    }

    private fun scheduleTuyaReconnect(rejected: Boolean) {
        val delays = longArrayOf(5_000, 15_000, 30_000, 60_000, 120_000, 300_000)
        val delay = if (rejected) TUYA_BLOCKED_MS else delays[tuyaReconnectAttempt.coerceAtMost(delays.size - 1)]
        tuyaReconnectAttempt++
        tuyaNextConnectAt = SystemClock.elapsedRealtime() + delay
        handler.postDelayed({ tick() }, delay + 50)
    }

    /** Bilgiler değişti ya da kullanıcı yeniden denedi: baştan bağlan. */
    private fun resetTuya() {
        tuyaSocket.close()
        tuyaBlocked = false
        tuyaWarned = false
        tuyaReconnectAttempt = 0
        tuyaNextConnectAt = 0
        lastTuyaSyncAt = 0
        tuyaTick(SystemClock.elapsedRealtime())
    }

    private fun syncTuya() {
        lastTuyaSyncAt = SystemClock.elapsedRealtime()
        if (!store.hasTuya) return
        try {
            replaceCloud(Cloud.TUYA, tuya.getDevices())
            tuyaSyncRetryAttempt = 0
            tuyaBlocked = false
            tuyaWarned = false
            if (tuyaSocket.ready) setTuyaState(ConnState.LIVE, synced = true)
            else setTuyaState(ConnState.POLLING, tuyaIssue, synced = true)
        } catch (e: TuyaExpiredException) {
            onTuyaBlocked(getString(R.string.tuya_expired))
        } catch (e: TuyaAuthException) {
            onTuyaBlocked(getString(R.string.tuya_auth_error))
        } catch (e: IOException) {
            // Mesaj servisi canlıyken tek bir HTTP hatası izlemeyi durdurmaz (bkz. retrySyncSoon).
            if (tuyaSocket.ready) retryTuyaSyncSoon(e) else setTuyaState(ConnState.NO_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Tuya sync failed", e)
            setTuyaState(ConnState.ERROR, e.message.orEmpty())
        }
    }

    /** [retrySyncSoon]'un Tuya karşılığı: mesaj servisi canlıyken başarısız senkron kısa aralıklarla yeniden denenir. */
    private fun retryTuyaSyncSoon(cause: Exception) {
        val delay = SYNC_RETRY_MS[tuyaSyncRetryAttempt.coerceAtMost(SYNC_RETRY_MS.size - 1)]
        tuyaSyncRetryAttempt++
        Log.w(TAG, "Tuya sync failed while live (${cause.javaClass.simpleName}); retrying in ${delay / 1000}s")
        handler.postDelayed({ if (running && tuyaSocket.ready) syncTuya() }, delay)
    }

    /** İzleme fiilen durdu: durumu göster, bir kez bildirim ver. */
    private fun onTuyaBlocked(text: String) {
        tuyaBlocked = true
        tuyaSocket.close()
        setTuyaState(ConnState.ERROR, text)
        if (!tuyaWarned) {
            tuyaWarned = true
            notifier.notifyEvent(text)
        }
    }

    private fun applyTuya(event: TuyaEvent) {
        // Bağlantı kopukken biriken ve son okumadan eski olaylar zaten okunan duruma dahildir.
        if (event.time != 0L && event.time < tuya.syncServerTime) return
        val list = store.snapshots
        val before = list.firstOrNull { it.id == event.deviceId && it.cloud == Cloud.TUYA } ?: return
        val after = when (event) {
            is TuyaEvent.Properties -> TuyaParser.apply(before, event.values, tuya.scalesFor(event.deviceId))
            is TuyaEvent.Online -> before.copy(online = event.online)
        }
        if (after == before) return
        process(list.map { if (it.id == after.id) after else it })
        setTuyaState(ConnState.LIVE, synced = true)
    }

    private fun process(current: List<DeviceSnapshot>) {
        val wall = System.currentTimeMillis()
        // Bekleme süresi dolmuş kopmalar önce verilir: cihaz bu arada geri geldiyse (servis kapalıydı,
        // tick gecikti) sıra "koptu → geldi" olur, uzun kopma "kısa kopma" sayılıp yutulmaz.
        grace.due(wall).forEach { dispatch(it, store.rulesFor(it.deviceId).actionFor(it.eventType)) }
        val previous = store.snapshots.associateBy { it.id }
        // Otomasyonlar grupları da cihaz sayar: grubun durumu üyelerinden türetilir.
        val groups = store.groups
        val previousAuto = if (groups.isEmpty()) previous else DeviceGroup.withGroups(store.snapshots, groups).associateBy { it.id }
        store.snapshots = current
        runCatching { metrics.record(previous, current, wall) }
        // Cihaz düğmesi widget'ları açık/kapalı ve bağlantı değişince tazelenir.
        if (current.any { d -> previous[d.id]?.let { it.online != d.online || it.switches != d.switches } != false }) {
            DeviceWidget.updateAll(this)
            WearBridge.publish(this)
        }
        val changes = ChangeDetector.detect(previous, current.associateBy { it.id }, store.monitoredIds())
        changes.forEach(::handle)
        runAutomations(automations.onStates(previousAuto, DeviceGroup.withGroups(current, groups).associateBy { it.id }, store.automations, wall, env()))
        persistRuntime()
        heartbeat(wall)
    }

    /** Önceki süreçten kalan bekleyen işleri yükler; servis oluşurken işçi thread'inin ilk işi olarak çalışır. */
    private fun restoreRuntime() {
        grace = OfflineGrace.fromJson(runtime.grace)
        outage = OutageWatch.fromJson(runtime.outage)
        automations.restore(runtime.automationState, store.automations)
        reminders = AlarmReminders.fromJson(runtime.reminders)
        // Önceki süreçten kalan "izleyemiyor" uyarısı: yeni süreç o kesintiyi bilmez, düzelince kaldıramazdı.
        notifier.clearBlind()
        if (store.monitoringEnabled) reportGap(System.currentTimeMillis())
    }

    /**
     * Önceki çalışma beklenmedik şekilde bittiyse (telefon servisi kapattı, telefon kapalıydı) aradaki
     * süreyi geçmişe yazar ve bildirir: o sürede olan değişiklikler kaçırılmış olabilir.
     */
    private fun reportGap(wall: Long) {
        MonitorGap.detect(runtime.lastAliveAt, runtime.cleanStop, wall)?.let { (from, to) ->
            val args = EventArgs.MonitorGap(from, to)
            val text = args.render(getString(R.string.app_name), strings)
            Log.w(TAG, "monitoring gap ${(to - from) / 1000}s")
            log.add("", getString(R.string.app_name), EventKind.MONITOR_GAP, text, AlertAction.NOTIFY.name, args)
            notifier.notifyEvent(text, title = getString(R.string.notif_monitor_title))
        }
    }

    private var secretsWarned = false

    /** Kayıtlı anahtarlar bu cihazda çözülemedi (ör. veriler başka telefondan kopyalandı): bir kez söylenir. */
    private fun checkSecrets() {
        if (secretsWarned || !store.secretsUnreadable) return
        secretsWarned = true
        Log.w(TAG, "stored secrets can't be decrypted")
        notifier.notifyEvent(getString(R.string.secrets_unreadable), title = getString(R.string.notif_monitor_title))
    }

    /** İzleme uzun süre kör kalınca (internet/bulut/oturum) bir kez uyarır, düzelince bildirir. */
    private fun checkBlind(wall: Long) {
        val connection = LiveState.connection.value
        val blind = connection.state in BLIND_STATES
        when (val signal = blindWatch.update(blind, wall, store.blindAlertMin * 60_000L)) {
            is BlindWatch.Signal.Blind -> {
                val args = EventArgs.MonitorBlind(signal.since)
                val text = args.render(getString(R.string.app_name), strings)
                log.add("", getString(R.string.app_name), EventKind.MONITOR_BLIND, text, AlertAction.NOTIFY.name, args)
                // Oturum düşmesinin kendi kalıcı uyarısı zaten var; ikinci bildirim gürültü olur.
                if (connection.state != ConnState.AUTH_ERROR) {
                    val lines = listOf(text, connection.detail, getString(R.string.blind_hint)).filter { it.isNotEmpty() }
                    notifier.monitorBlind(lines.joinToString("\n"))
                }
            }
            is BlindWatch.Signal.Recovered -> {
                val args = EventArgs.MonitorRestored(signal.from, signal.to)
                val text = args.render(getString(R.string.app_name), strings)
                log.add("", getString(R.string.app_name), EventKind.MONITOR_BLIND, text, AlertAction.NOTIFY.name, args)
                notifier.clearBlind()
                notifier.notifyEvent(text, title = getString(R.string.notif_monitor_title))
            }
            null -> Unit
        }
    }

    private var lastHeartbeatAt = 0L

    /** "Canlıyım" işareti; diske dakikada en çok bir kez yazılır. */
    private fun heartbeat(wall: Long, force: Boolean = false) {
        if (!force && wall - lastHeartbeatAt < HEARTBEAT_MS) return
        lastHeartbeatAt = wall
        runtime.lastAliveAt = wall
    }

    /** Servis beklenmedik şekilde kapanırsa kaldığı yerden devam edebilsin. */
    private fun persistRuntime() {
        runtime.grace = grace.toJson()
        runtime.outage = outage.toJson()
        runtime.automationState = automations.saveState()
        runtime.reminders = reminders.toJson()
    }

    // ---------------------------------------------------------------- otomasyon

    /**
     * SPM gibi cihazlar kanal gücünü yalnızca istenince (uiActive) ve kısa süre gönderir. Kanal gücü
     * otomasyonunun kanalı açıkken istek yenilenir. Veri uzun süre gelmezse bir kez uyarılır; aksi halde
     * son değer ekranda donar ve güç düşmesi hiç fark edilmez.
     */
    private fun keepChannelPowerLive(wall: Long) {
        val byId = store.snapshots.associateBy { it.id }
        val watched = store.automations
            .filter { it.enabled && it.trigger.kind.usesChannelPower }
            // Tuya prizleri gücü kendiliğinden bildirir; istek yalnızca eWeLink (SPM/DualR3) için.
            .filter { byId[it.trigger.deviceId]?.cloud == Cloud.EWELINK }
            .filter { byId[it.trigger.deviceId]?.switches?.get(it.trigger.channel) == true }
            .mapNotNull { byId[it.trigger.deviceId] }
            .distinctBy { it.id }
        val ids = watched.map { it.id }.toSet()
        // Kanal kapanınca sıfırlanır; tekrar açılınca veri beklemesi baştan başlar.
        liveEnergyRequestedAt.keys.retainAll(ids)
        powerStaleWarned.retainAll(ids)
        for (d in watched) {
            val requested = liveEnergyRequestedAt[d.id]
            if (requested == null || wall - requested >= LIVE_ENERGY_REFRESH_MS) {
                if (requested == null) channelPowerSeenAt[d.id] = wall
                liveEnergyRequestedAt[d.id] = wall
                try {
                    client.requestLiveEnergy(d, LIVE_ENERGY_SECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "uiActive failed for ${d.id}", e)
                }
            }
            val seen = channelPowerSeenAt[d.id] ?: wall
            if (wall - seen >= POWER_STALE_MS && powerStaleWarned.add(d.id)) {
                val text = EventArgs.PowerStale.render(d.name, strings)
                Log.w(TAG, "no channel power from ${d.id} for ${(wall - seen) / 1000}s")
                log.add(d.id, d.name, EventKind.AUTOMATION, text, AlertAction.NOTIFY.name, EventArgs.PowerStale)
                notifier.notifyEvent(text)
            }
        }
    }

    private fun runAutomations(fired: List<Automation>) {
        fired.forEach(::execute)
        // Süre dolumu ya da gecikmeli eylem varsa tam vaktinde uyan.
        val due = automations.nextDueAt(store.snapshotsWithGroups.associateBy { it.id }, store.automations, env()) ?: return
        handler.postDelayed({ tick() }, (due - System.currentTimeMillis()).coerceAtLeast(0) + 50)
    }

    private fun execute(a: Automation) {
        when (a.action.kind) {
            ActionKind.TURN_ON, ActionKind.TURN_OFF -> {
                val on = a.action.kind == ActionKind.TURN_ON
                if (DeviceGroup.isGroup(a.action.deviceId)) return executeGroup(a, on)
                val target = store.snapshots.firstOrNull { it.id == a.action.deviceId }
                if (target == null) {
                    val args = EventArgs.AutoMissing(a.name)
                    log.add(a.action.deviceId, a.name, EventKind.AUTOMATION, args.render(a.name, strings), "", args)
                    return
                }
                val args = EventArgs.AutoSwitch(a.name, a.action.channel, target.isMultiChannel, on)
                val text = args.render(target.name, strings)
                try {
                    // Otomasyonun yaptığı değişiklik alarm çaldırmasın.
                    LiveState.expected.expect(target.id, a.action.channel, on, System.currentTimeMillis())
                    controlSwitch(target, a.action.channel, on)
                    log.add(target.id, target.name, EventKind.AUTOMATION, text, "", args)
                } catch (e: Exception) {
                    val failedArgs = args.copy(error = e.message.orEmpty())
                    val failed = failedArgs.render(target.name, strings)
                    log.add(target.id, target.name, EventKind.AUTOMATION, failed, AlertAction.NOTIFY.name, failedArgs)
                    notifier.notifyEvent(failed)
                }
            }
            ActionKind.RUN_SCENE -> {
                val scene = store.scenes.firstOrNull { it.id == a.action.deviceId }
                if (scene == null) {
                    val args = EventArgs.AutoMissing(a.name)
                    log.add("", a.name, EventKind.AUTOMATION, args.render(a.name, strings), "", args)
                    return
                }
                // Ağ işi: izleme iş parçacığı beklemesin.
                telegram.execute { runBlocking { Actions.runScene(this@MonitorService, scene, a.name) } }
            }
            ActionKind.ALARM, ActionKind.NOTIFY -> {
                val source = store.snapshotsWithGroups.firstOrNull { it.id == a.trigger.deviceId }
                val args = EventArgs.AutoAlert(a.name, source?.name)
                val text = args.render(source?.name ?: a.name, strings)
                val action = if (a.action.kind == ActionKind.ALARM) AlertAction.ALARM else AlertAction.NOTIFY
                // Ücretsiz sürümde izlenmeyen cihazın otomasyonu uyarı vermez (sınır otomasyonla aşılmasın); yalnızca kaydedilir.
                val allowed = store.isPro || !a.trigger.kind.usesDevice || store.monitoredIds().let { ids ->
                    a.trigger.deviceId in ids ||
                        store.groups.firstOrNull { it.deviceId == a.trigger.deviceId }?.members?.all { it.deviceId in ids } == true
                }
                val rules = store.rulesFor(a.trigger.deviceId)
                val effective = if (!allowed) AlertAction.IGNORE
                    else RuleEngine.effective(action, quiet = quietNow(), critical = rules.critical)
                log.add(a.trigger.deviceId, source?.name ?: a.name, EventKind.AUTOMATION, text, effective.name, args)
                val sound = rules.soundUri
                when (effective) {
                    AlertAction.ALARM -> addAlarm(text, sound)
                    AlertAction.NOTIFY -> notifier.notifyEvent(text, sound)
                    AlertAction.IGNORE -> Unit
                }
            }
        }
    }

    /** Otomasyonun grup eylemi: değişmesi gereken her üyeye komut gider; sonuç grup adıyla tek satır yazılır. */
    private fun executeGroup(a: Automation, on: Boolean) {
        val group = store.groups.firstOrNull { it.deviceId == a.action.deviceId }
        if (group == null) {
            val args = EventArgs.AutoMissing(a.name)
            log.add(a.action.deviceId, a.name, EventKind.AUTOMATION, args.render(a.name, strings), "", args)
            return
        }
        val args = EventArgs.AutoSwitch(a.name, 0, false, on)
        var error: Exception? = null
        for ((device, channel) in group.targets(store.snapshots.associateBy { it.id }, on)) {
            try {
                // Otomasyonun yaptığı değişiklik alarm çaldırmasın.
                LiveState.expected.expect(device.id, channel, on, System.currentTimeMillis())
                controlSwitch(device, channel, on)
            } catch (e: Exception) {
                error = e
            }
        }
        if (error == null) {
            log.add(group.deviceId, group.name, EventKind.AUTOMATION, args.render(group.name, strings), "", args)
        } else {
            val failedArgs = args.copy(error = error.message.orEmpty())
            val failed = failedArgs.render(group.name, strings)
            log.add(group.deviceId, group.name, EventKind.AUTOMATION, failed, AlertAction.NOTIFY.name, failedArgs)
            notifier.notifyEvent(failed)
        }
    }

    private fun handle(change: Change) {
        val wall = System.currentTimeMillis()
        if (LiveState.expected.consume(change, wall)) {
            log.add(change.deviceId, change.name, EventKind.USER, change.describe(labels), "", change.toArgs())
            return
        }
        val action = store.rulesFor(change.deviceId).actionFor(change.eventType)

        if (change is Change.Connection) {
            if (!change.to && store.offlineGraceSec > 0 && action != AlertAction.IGNORE) {
                grace.hold(change, wall, store.offlineGraceSec * 1000L)
                handler.postDelayed({ tick() }, store.offlineGraceSec * 1000L + 50)
                return
            }
            if (change.to && grace.cameBack(change.deviceId, wall)) {
                log.add(change.deviceId, change.name, EventKind.SHORT_DROP, strings.shortDrop(change.name), AlertAction.IGNORE.name, EventArgs.ShortDrop)
                return
            }
            if (change.to) when (val o = outage.onOnline(change.deviceId, wall)) {
                // Kesinti kararı beklenirken geri geldi: kopma ve dönüş olağan yoldan.
                is OutageWatch.Online.Release -> deliver(o.held.change, o.held.action)
                is OutageWatch.Online.Restored -> {
                    onPowerRestored(o.from, o.to)
                    deliverQuietly(change)
                    return
                }
                OutageWatch.Online.Absorbed -> {
                    deliverQuietly(change)
                    return
                }
                OutageWatch.Online.Pass -> Unit
            }
        }
        dispatch(change, action)
    }

    /** Kopmalar önce elektrik kesintisi algılamasından geçer (bkz. [OutageWatch]). */
    private fun dispatch(change: Change, action: AlertAction) {
        if (change is Change.Connection && !change.to) {
            val wall = System.currentTimeMillis()
            when (val o = outage.offer(change, action, wall, store.monitoredIds().size, store.outageDetect)) {
                is OutageWatch.Offer.Hold -> {
                    handler.postDelayed({ tick() }, (o.dueAt - wall).coerceAtLeast(0) + 50)
                    return
                }
                OutageWatch.Offer.Absorbed -> {
                    deliverQuietly(change)
                    return
                }
                OutageWatch.Offer.Pass -> Unit
            }
        }
        deliver(change, action)
    }

    private fun deliver(change: Change, action: AlertAction) {
        val rules = store.rulesFor(change.deviceId)
        val effective = RuleEngine.effective(action, quiet = quietNow(), critical = rules.critical)
        val text = change.describe(labels)
        log.add(change.deviceId, change.name, change.eventType.name, text, effective.name, change.toArgs())
        val sound = rules.soundUri
        when (effective) {
            AlertAction.ALARM -> {
                addAlarm(text, sound)
                // Hatırlatma kapalıyken kayıt tutulmaz: sonradan açılınca eski olaylar birden çalmasın.
                if (store.reminderMin > 0) reminders.onAlarm(change, System.currentTimeMillis())
            }
            AlertAction.NOTIFY -> notifier.notifyEvent(text, sound)
            AlertAction.IGNORE -> Unit
        }
    }

    /**
     * Alarmı kapatılmış ama düzelmemiş durumlar için hatırlatma (Ayarlar → Alarm). Sessiz saatlerde kritik
     * olmayan cihazın hatırlatması bildirime düşer; o durumda sayaç hemen yeniden kurulur.
     */
    private fun checkReminders(wall: Long) {
        val interval = store.reminderMin * 60_000L
        val monitored = store.monitoredIds()
        // Cihaz izlenmiyorsa ya da o olayın kuralı artık alarm değilse hatırlatma düşer.
        val due = reminders.due(
            store.snapshots.associateBy { it.id }, wall, interval, alarmActive = store.pendingAlarm.isNotEmpty(),
            stillAlarmed = { c -> c.deviceId in monitored && store.rulesFor(c.deviceId).actionFor(c.eventType) == AlertAction.ALARM },
        )
        if (due.isEmpty()) return
        val quiet = quietNow()
        for (r in due) {
            val args = EventArgs.Reminder.of(r)
            val text = args.render(r.change.name, strings)
            val rules = store.rulesFor(r.change.deviceId)
            val effective = RuleEngine.effective(AlertAction.ALARM, quiet, rules.critical)
            log.add(r.change.deviceId, r.change.name, EventKind.REMINDER, text, effective.name, args)
            if (effective == AlertAction.ALARM) addAlarm(text, rules.soundUri) else notifier.notifyEvent(text, rules.soundUri)
        }
        // Hiç alarm çalmadıysa (hepsi bildirime düştü) kapatılacak bir şey yok: hatırlatmalar yeniden kurulur.
        if (store.pendingAlarm.isEmpty()) reminders.onDismiss(wall)
    }

    /**
     * [sound]: olayın cihazına özel ses (yoksa genel ses). [silent]: sessiz test alarmı. Alarm ancak yalnızca sessiz
     * testlerden oluşuyorsa sessiz kalır: gerçek bir olay gelince ses başlar, çalan alarma eklenen sessiz test onu susturmaz.
     */
    private fun addAlarm(text: String, sound: String? = null, silent: Boolean = false, test: Boolean = false) {
        val now = System.currentTimeMillis()
        if (store.pendingAlarm.isEmpty()) {
            runtime.alarmStartedAt = now
            runtime.silentAlarm = silent
        } else if (!silent) runtime.silentAlarm = false
        // İkinci kişiye haber verme ilk gerçek olaydan sayılır; test alarmı kimseye mesaj göndermez.
        if (!test && runtime.realAlarmAt == 0L) {
            runtime.realAlarmAt = now
            if (store.escalationReady) handler.postDelayed({ tick() }, store.escalateMin * 60_000L + 50)
        }
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        store.pendingAlarm = store.pendingAlarm + "$time  $text"
        notifier.showAlarm(store.pendingAlarm, sound, silent = runtime.silentAlarm)
        StatusSurfaces.refresh(this)
    }

    /** Gönderilemeyen haber vermenin geçmişe yazıldığı alarm: her denemede tekrar yazılmasın. */
    private var escalationFailureLoggedFor = 0L
    private var escalationRetryAt = 0L
    /** Gönderim sürüyor: bir sonraki tick aynı mesajı ikinci kez göndermesin. */
    private var escalationInFlight = false

    /**
     * Kademeli haber verme: alarm kişinin bekleme süresi ([Store.stageFor]) içinde kapatılmazsa o kişiye Telegram mesajı
     * gider (alarm başına kişi başı bir kez). "Gördüm" ya da kapatma sonraki kademeleri iptal eder. Gönderilemezse
     * (internet yok) dakikada bir yeniden denenir; kalıcı hata alan kişi bu alarm için atlanır.
     * Ağ işi [telegram] iş parçacığında: yavaş bağlantı izlemeyi (cihaz olaylarını) bekletmesin.
     */
    private fun checkEscalation(wall: Long) {
        if (escalationInFlight || store.pendingAlarm.isEmpty() || !store.escalationReady || wall < escalationRetryAt) return
        val at = runtime.realAlarmAt
        val all = store.telegramRecipients
        val done = runtime.escalatedChats
        val due = Escalation.dueRecipients(all, store.telegramStages, store.escalateMin, at, done, wall)
        if (due.isEmpty()) return
        val minutes = ((wall - at) / 60_000L).toInt().coerceAtLeast(1)
        // Önceki kademede haber verilenler mesajda yazılır: kişi tek başına olmadığını bilsin.
        val earlier = all.filter { it.id in done }.map { it.name }
        val header = getString(R.string.tg_alarm_header, minutes) +
            (if (earlier.isNotEmpty()) "\n" + getString(R.string.tg_alarm_earlier, earlier.joinToString(", ")) else "")
        val text = Escalation.message(header = header, lines = store.pendingAlarm, footer = getString(R.string.tg_footer, deviceName()))
        val token = store.telegramToken
        // "Gördüm" düğmesi: kişi alarmı üstlenir, telefondaki alarm susar.
        val button = TelegramBot.button(getString(R.string.tg_ack_button), TelegramBot.ackData(runtime.installTag, at))
        escalationInFlight = true
        telegram.execute {
            val sent = mutableListOf<Long>()
            val gaveUp = mutableListOf<Long>()
            var error: Exception? = null
            for (r in due) {
                try {
                    TelegramClient.send(token, r.id, text, markup = button)
                    sent += r.id
                } catch (e: Exception) {
                    Log.w(TAG, "telegram send failed", e)
                    error = e
                    if (e is TelegramException && e.code in PERMANENT_TELEGRAM_ERRORS) gaveUp += r.id
                }
            }
            handler.post { onEscalationSent(at, minutes, sent, gaveUp, due.size, error, wall) }
        }
    }

    private fun onEscalationSent(at: Long, minutes: Int, sent: List<Long>, gaveUp: List<Long>, tried: Int, error: Exception?, wall: Long) {
        escalationInFlight = false
        val name = getString(R.string.app_name)
        if (runtime.realAlarmAt == at) runtime.escalatedChats = runtime.escalatedChats + sent + gaveUp
        if (sent.isNotEmpty()) {
            val args = EventArgs.Escalation(sent.size, minutes)
            log.add("", name, EventKind.ESCALATION, args.render(name, strings), "", args)
            // Gönderim sürerken alarm kapatıldıysa "kapatıldı" mesajı hemen ardından gider.
            if (runtime.realAlarmAt == at) runtime.escalatedFor = at else sendDismissed(at, sent.toSet())
        }
        if (sent.size + gaveUp.size < tried) escalationRetryAt = wall + ESCALATION_RETRY_MS
        if (sent.isEmpty() && escalationFailureLoggedFor != at) {
            escalationFailureLoggedFor = at
            val args = EventArgs.Escalation(0, minutes, error?.let(::telegramError))
            log.add("", name, EventKind.ESCALATION, args.render(name, strings), "", args)
        }
    }

    /** Alarm bitti: haber verilenlere kapatıldığı da bildirilir. */
    private fun endEscalation() {
        val at = runtime.realAlarmAt
        if (at != 0L && runtime.escalatedFor == at) sendDismissed(at, runtime.escalatedChats)
        runtime.realAlarmAt = 0
        runtime.escalatedFor = 0
        runtime.escalatedChats = emptySet()
    }

    /** "Alarm kapatıldı" mesajı (yalnızca haber verilenlere); yanıt beklenmez, gönderilemezse önemsizdir. */
    private fun sendDismissed(at: Long, chats: Set<Long>) {
        if (!store.escalationReady || chats.isEmpty()) return
        val now = System.currentTimeMillis()
        val text = getString(
            R.string.tg_dismissed,
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
            DurationText.format(now - at, EventLog.durations(this)),
        )
        val token = store.telegramToken
        telegram.execute { chats.forEach { id -> runCatching { TelegramClient.send(token, id, text) } } }
    }

    private fun telegramError(e: Exception): String = when {
        e is TelegramException && e.code == 401 -> getString(R.string.tg_err_token)
        e is TelegramException && e.code == 403 -> getString(R.string.tg_err_blocked)
        e is TelegramException && e.code == 400 -> getString(R.string.tg_err_chat)
        e is TelegramException -> e.message.orEmpty()
        else -> getString(R.string.state_no_internet)
    }

    /** Telefonun kullanıcıya görünen adı ("Xiaomi 11T"); yoksa model. */
    private fun deviceName(): String =
        runCatching { android.provider.Settings.Global.getString(contentResolver, "device_name") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: Build.MODEL

    /**
     * Alarmı kapatır (bildirimden, alarm ekranından ya da Telegram'dan). [byTelegram]: Telegram'dan üstlenen kişi; o zaman
     * kişilere "kapatıldı" yerine üstlenme mesajı gider ve kapatma ayrıca kaydedilmez (üstlenme kaydı yeter).
     */
    private fun dismissAlarm(byTelegram: String? = null) {
        val lines = store.pendingAlarm
        if (lines.isNotEmpty()) {
            if (byTelegram == null) logDismissal(lines.size) else runtime.alarmStartedAt = 0
            reminders.onDismiss(System.currentTimeMillis())
            persistRuntime()
        }
        store.pendingAlarm = emptyList()
        runtime.silentAlarm = false
        if (byTelegram != null) runtime.escalatedFor = 0
        endEscalation()
        notifier.stopAlarm()
        StatusSurfaces.refresh(this)
    }

    // ---------------------------------------------------------------- elektrik kesintisi

    private fun checkOutage(wall: Long) {
        val monitored = store.monitoredIds()
        val offline = store.snapshots.filter { it.id in monitored && !it.online }.map { it.id }.toSet()
        when (val v = outage.due(wall, offline, monitored)) {
            is OutageWatch.Verdict.Release -> v.held.forEach { deliver(it.change, it.action) }
            is OutageWatch.Verdict.Outage -> onOutage(v.held, monitored.size)
            null -> Unit
        }
    }

    /** İzlenen cihazların hepsi koptu: tek olay, en güçlü kuralın eylemiyle (kritik cihaz varsa sessiz saati deler). */
    private fun onOutage(held: List<OutageWatch.Held>, devices: Int) {
        val wall = System.currentTimeMillis()
        held.forEach { deliverQuietly(it.change) }
        val critical = held.any { store.rulesFor(it.change.deviceId).critical }
        val effective = RuleEngine.effective(OutageWatch.strongest(held), quiet = quietNow(), critical = critical)
        val app = getString(R.string.app_name)
        val args = EventArgs.Outage(devices)
        val text = args.render(app, strings)
        Log.w(TAG, "power outage: ${held.size} devices dropped together")
        log.add("", app, EventKind.OUTAGE, text, effective.name, args)
        val sound = held.firstNotNullOfOrNull { store.rulesFor(it.change.deviceId).soundUri }
        when (effective) {
            AlertAction.ALARM -> {
                addAlarm(text, sound)
                if (store.reminderMin > 0) held.forEach { reminders.onAlarm(it.change, wall) }
            }
            AlertAction.NOTIFY -> notifier.notifyEvent(text, sound)
            AlertAction.IGNORE -> Unit
        }
        if (effective != AlertAction.IGNORE) forward("⚡ $text")
    }

    private fun onPowerRestored(from: Long, to: Long) {
        val app = getString(R.string.app_name)
        val args = EventArgs.OutageEnded(from, to)
        val text = args.render(app, strings)
        log.add("", app, EventKind.OUTAGE, text, AlertAction.NOTIFY.name, args)
        notifier.notifyEvent(text)
        forward("🔌 $text")
    }

    /** Kesintinin parçası olan değişiklik: alarm/bildirim olmadan yalnızca geçmişe. */
    private fun deliverQuietly(change: Change) =
        log.add(change.deviceId, change.name, change.eventType.name, change.describe(labels), AlertAction.IGNORE.name, change.toArgs())

    // ---------------------------------------------------------------- Telegram: bildirim, komutlar, "Gördüm"

    /** Akıllı uyarıları (kesinti, tüketim, bekçi) Telegram'daki kişilere de yazar; yanıt beklenmez. */
    private fun forward(text: String, always: Boolean = false) {
        if (!store.escalationReady || !(always || store.telegramForward)) return
        val token = store.telegramToken
        val recipients = store.telegramRecipients
        val footer = getString(R.string.tg_footer, deviceName())
        telegram.execute { recipients.forEach { r -> runCatching { TelegramClient.send(token, r.id, "$text\n\n$footer") } } }
    }

    private fun reply(chatId: Long, text: String) {
        val token = store.telegramToken
        val footer = "— ${deviceName()}"
        telegram.execute { runCatching { TelegramClient.send(token, chatId, "$text\n\n$footer") } }
    }

    private var tgPollAt = 0L
    private var tgPolling = false

    /**
     * Telegram'a yazılan komutlar ve "Gördüm" düğmesi kısa aralıklarla yoklanır (sürekli açık bağlantı yok). Komutlar
     * kapalıysa yalnızca haber verilmiş bir alarm sürerken (düğme için) yoklanır.
     */
    private fun pollTelegram(wall: Long) {
        if (tgPolling || wall < tgPollAt || !store.escalationReady) return
        val waitingAck = runtime.realAlarmAt != 0L && runtime.escalatedFor == runtime.realAlarmAt
        if (!store.telegramCommands && !waitingAck) return
        tgPollAt = wall + TG_POLL_MS
        tgPolling = true
        val token = store.telegramToken
        val offset = runtime.tgOffset
        telegram.execute {
            val result = runCatching { TelegramClient.updates(token, offset) }
            handler.post {
                tgPolling = false
                result.onSuccess { onTelegramUpdates(it) }.onFailure { e ->
                    // Anahtar geçersiz: her 15 sn'de denemenin anlamı yok.
                    if (e is TelegramException && e.code == 401) tgPollAt = System.currentTimeMillis() + TG_BLOCKED_MS
                    else Log.w(TAG, "telegram poll failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun onTelegramUpdates(json: String) {
        val now = System.currentTimeMillis()
        val updates = runCatching { TelegramBot.parseUpdates(json) }.getOrDefault(emptyList())
        runtime.tgOffset = TelegramBot.confirmOffset(updates, now, runtime.tgOffset)
        runCatching { Escalation.chatsFromUpdates(json) }.getOrNull()?.let { seen ->
            TelegramClient.seenChats = (TelegramClient.seenChats + seen).associateBy { it.id }.values.toList()
        }
        val allowed = store.telegramRecipients.map { it.id }.toSet()
        var handled = runtime.tgHandled
        for (u in updates.sortedBy { it.id }) {
            if (u.id <= handled) continue
            handled = u.id
            // Yalnızca haber verilecek kişiler (ve gruplar) komut verebilir.
            if (u.chatId !in allowed) continue
            if (u.callbackData != null) onTelegramButton(u)
            else if (store.telegramCommands) u.text?.let(TelegramBot::parseCommand)?.let { onTelegramCommand(u, it, now) }
        }
        runtime.tgHandled = handled
    }

    /** "Gördüm" düğmesi: alarm bu cihazınsa ve hâlâ sürüyorsa üstlenilir, alarm susar. */
    private fun onTelegramButton(u: TelegramBot.Update) {
        val (tag, at) = TelegramBot.parseAck(u.callbackData) ?: return
        // Aynı botu kullanan başka cihazın alarmı: yanıtı o verir.
        if (tag != runtime.installTag) return
        val ok = store.pendingAlarm.isNotEmpty() && runtime.realAlarmAt == at
        if (ok) ackAlarm(u.from)
        val answer = getString(if (ok) R.string.tg_ack_done else R.string.tg_ack_stale)
        val token = store.telegramToken
        telegram.execute {
            runCatching { TelegramClient.answerCallback(token, u.callbackId.orEmpty(), answer) }
            runCatching { TelegramClient.removeButtons(token, u.chatId, u.messageId) }
        }
    }

    /** Alarmı Telegram'dan üstlenen kişi: alarm kapanır, herkese kimin ilgilendiği yazılır. */
    private fun ackAlarm(who: String) {
        val now = System.currentTimeMillis()
        val app = getString(R.string.app_name)
        val name = who.ifEmpty { getString(R.string.tg_someone) }
        val args = EventArgs.TelegramAck(name, now - runtime.realAlarmAt)
        val text = args.render(app, strings)
        log.add("", app, EventKind.TELEGRAM, text, "", args)
        dismissAlarm(byTelegram = name)
        notifier.notifyEvent(text)
        forward("✅ $text", always = true)
    }

    private fun onTelegramCommand(u: TelegramBot.Update, cmd: TelegramBot.Command, now: Long) {
        val age = now - u.date
        when (cmd) {
            TelegramBot.Command.Status -> if (age <= TelegramBot.STATUS_MAX_AGE_MS) reply(u.chatId, statusText())
            TelegramBot.Command.Help -> if (age <= TelegramBot.STATUS_MAX_AGE_MS) reply(u.chatId, getString(R.string.tg_help))
            is TelegramBot.Command.SetPresence -> {
                if (age > TelegramBot.STATUS_MAX_AGE_MS) return
                Actions.setAway(this, cmd.away, by = u.from.ifEmpty { getString(R.string.tg_someone) })
                reply(u.chatId, getString(if (cmd.away) R.string.tg_presence_away else R.string.tg_presence_home))
            }
            is TelegramBot.Command.RunScene -> {
                if (age > TelegramBot.SWITCH_MAX_AGE_MS) return reply(u.chatId, getString(R.string.tg_cmd_too_old))
                if (!store.telegramControl) return reply(u.chatId, getString(R.string.tg_control_off))
                val scenes = store.scenes
                val scene = Scene.find(cmd.query, scenes)
                    ?: return reply(u.chatId, getString(R.string.tg_scene_not_found, cmd.query, scenes.joinToString(", ") { it.name }.ifEmpty { "—" }))
                val who = u.from.ifEmpty { getString(R.string.tg_someone) }
                telegram.execute {
                    val (ok, failed) = runBlocking { Actions.runScene(this@MonitorService, scene, "Telegram ($who)") }
                    handler.post { reply(u.chatId, getString(R.string.tg_scene_done, scene.name, ok, failed)) }
                }
            }
            TelegramBot.Command.Ack -> {
                if (age > TelegramBot.STATUS_MAX_AGE_MS) return
                if (store.pendingAlarm.isNotEmpty() && runtime.realAlarmAt != 0L) ackAlarm(u.from)
                else reply(u.chatId, getString(R.string.tg_no_alarm))
            }
            is TelegramBot.Command.Switch -> {
                if (age > TelegramBot.SWITCH_MAX_AGE_MS) return reply(u.chatId, getString(R.string.tg_cmd_too_old))
                if (!store.telegramControl) return reply(u.chatId, getString(R.string.tg_control_off))
                if (cmd.query.isBlank()) return reply(u.chatId, getString(if (cmd.on) R.string.tg_usage_on else R.string.tg_usage_off))
                // Gruplar da adıyla bulunur ("/ac bahçe").
                when (val m = TelegramBot.match(cmd.query, store.snapshotsWithGroups)) {
                    is TelegramBot.Match.Found ->
                        if (DeviceGroup.isGroup(m.device.id)) telegramGroupSwitch(u, m.device, cmd.on)
                        else telegramSwitch(u, m.device, m.channel, cmd.on)
                    is TelegramBot.Match.NeedChannel -> reply(u.chatId, getString(R.string.tg_need_channel, m.device.name, m.device.switches.size))
                    is TelegramBot.Match.Ambiguous -> reply(u.chatId, getString(R.string.tg_ambiguous, m.names.joinToString(", ")))
                    TelegramBot.Match.NotFound -> reply(u.chatId, getString(R.string.tg_not_found, cmd.query))
                }
            }
        }
    }

    private fun telegramSwitch(u: TelegramBot.Update, device: DeviceSnapshot, channel: Int, on: Boolean) {
        val who = u.from.ifEmpty { getString(R.string.tg_someone) }
        val args = EventArgs.TelegramSwitch(who, channel, device.isMultiChannel, on)
        try {
            if (!device.online) throw IOException(labels.offline)
            // Uzaktan yapılan değişiklik alarm çaldırmasın (uygulamadan aç/kapa gibi).
            LiveState.expected.expect(device.id, channel, on, System.currentTimeMillis())
            controlSwitch(device, channel, on)
            log.add(device.id, device.name, EventKind.TELEGRAM, args.render(device.name, strings), "", args)
            val label = if (device.isMultiChannel) labels.channel(device.name, channel + 1) else device.name
            reply(u.chatId, "✅ $label: ${if (on) labels.on else labels.off}")
        } catch (e: Exception) {
            val failed = args.copy(error = e.message.orEmpty())
            log.add(device.id, device.name, EventKind.TELEGRAM, failed.render(device.name, strings), AlertAction.NOTIFY.name, failed)
            reply(u.chatId, "⚠️ " + failed.render(device.name, strings))
        }
    }

    /** Telegram'dan grup aç/kapa: değişmesi gereken her üyeye komut gider; geçmişe grup adıyla tek satır yazılır. */
    private fun telegramGroupSwitch(u: TelegramBot.Update, groupDevice: DeviceSnapshot, on: Boolean) {
        val who = u.from.ifEmpty { getString(R.string.tg_someone) }
        val args = EventArgs.TelegramSwitch(who, 0, false, on)
        val group = store.groups.firstOrNull { it.deviceId == groupDevice.id } ?: return reply(u.chatId, getString(R.string.group_missing))
        try {
            if (!groupDevice.online) throw IOException(labels.offline)
            val targets = group.targets(store.snapshots.associateBy { it.id }, on)
            for ((device, channel) in targets) {
                // Uzaktan yapılan değişiklik alarm çaldırmasın (uygulamadan aç/kapa gibi).
                LiveState.expected.expect(device.id, channel, on, System.currentTimeMillis())
                controlSwitch(device, channel, on)
            }
            log.add(group.deviceId, group.name, EventKind.TELEGRAM, args.render(group.name, strings), "", args)
            reply(u.chatId, getString(R.string.tg_group_done, group.name, if (on) labels.on else labels.off, targets.size))
        } catch (e: Exception) {
            val failed = args.copy(error = e.message.orEmpty())
            log.add(group.deviceId, group.name, EventKind.TELEGRAM, failed.render(group.name, strings), AlertAction.NOTIFY.name, failed)
            reply(u.chatId, "⚠️ " + failed.render(group.name, strings))
        }
    }

    /** /durum yanıtı: izleme ve alarm durumu, cihazlar (açık/kapalı, güç, çevrimdışı). */
    private fun statusText(): String {
        val connection = LiveState.connection.value.state
        val alarm = store.pendingAlarm
        return buildString {
            append("📊 ").append(getString(R.string.tg_status_title)).append('\n')
            append(getString(R.string.tg_status_monitoring, getString(stateTitle(connection)))).append('\n')
            append(
                if (alarm.isEmpty()) getString(R.string.tg_status_no_alarm)
                else resources.getQuantityString(R.plurals.tg_status_alarm, alarm.size, alarm.size)
            ).append('\n')
            if (outage.active) {
                val since = DurationText.format(System.currentTimeMillis() - outage.since, EventLog.durations(this@MonitorService))
                append("⚡ ").append(getString(R.string.tg_status_outage, since)).append('\n')
            }
            append('\n')
            for (d in store.snapshots.take(MAX_STATUS_DEVICES)) {
                if (!d.online) {
                    append("🔴 ").append(d.name).append(": ").append(labels.offline).append('\n')
                    continue
                }
                val switches = d.switches.toSortedMap().entries.joinToString(" · ") { (ch, on) ->
                    (if (d.isMultiChannel) "${ch + 1}: " else "") + if (on) labels.on else labels.off
                }
                append(if (d.switches.values.any { it }) "🟢 " else "⚪ ").append(d.name).append(": ").append(switches)
                d.power?.let { append(" · ").append(String.format(java.util.Locale.getDefault(), "%.0f W", it)) }
                append('\n')
            }
            if (store.snapshots.isEmpty()) append(getString(R.string.no_devices)).append('\n')
            val byId = store.snapshots.associateBy { it.id }
            val groups = store.groups
            if (groups.isNotEmpty()) append('\n').append(getString(R.string.groups_title)).append('\n')
            for (g in groups) {
                val (onCount, total) = g.counts(byId)
                append(if (onCount > 0) "🟢 " else "⚪ ").append(g.name).append(": ")
                    .append(getString(R.string.channels_on_short, onCount, total)).append('\n')
            }
            if (store.telegramControl) append('\n').append(getString(R.string.tg_status_hint))
        }.trimEnd().take(MAX_TG_CHARS)
    }

    private fun stateTitle(state: ConnState): Int = when (state) {
        ConnState.LIVE -> R.string.tg_state_live
        ConnState.POLLING -> R.string.tg_state_polling
        ConnState.CONNECTING -> R.string.state_connecting
        ConnState.NO_INTERNET -> R.string.state_no_internet
        ConnState.AUTH_ERROR -> R.string.state_auth_error
        ConnState.ERROR -> R.string.state_error
        ConnState.STOPPED -> R.string.state_stopped
    }

    // ---------------------------------------------------------------- bekçi

    private var beatAt = 0L
    private var watchCheckAt = 0L
    private var watchdogBusy = false

    /** Sinyalin yazıldığı sohbet: haber verilecek ilk kişi (grup değil, kişi tercih edilir). */
    private fun beatChat(): Long? = store.telegramRecipients.let { list -> (list.firstOrNull { it.id > 0 } ?: list.firstOrNull())?.id }

    private fun watchdogTick(wall: Long) {
        if (watchdogBusy || !store.escalationReady) return
        when (store.watchdogMode) {
            Store.WATCHDOG_SEND -> if (wall - beatAt >= Watchdog.BEAT_MS) sendBeat(wall, stopped = false)
            Store.WATCHDOG_WATCH -> if (wall - watchCheckAt >= Watchdog.CHECK_MS) checkWatched(wall)
        }
    }

    /**
     * Sabit mesajı günceller ("🟢 çalışıyor · son sinyal 14:05"); mesaj yoksa ya da silinmişse yenisini gönderip sabitler.
     * Gönderim [telegram] iş parçacığında; durum doğrudan [runtime]'a yazılır (izleme durdurulurken de çalışsın).
     */
    private fun sendBeat(wall: Long, stopped: Boolean) {
        if (!store.escalationReady) return
        val chat = beatChat() ?: return
        beatAt = wall
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(wall))
        val head = if (stopped) "${Watchdog.STOPPED} ${getString(R.string.wd_beat_stopped)}" else "${Watchdog.RUNNING} ${getString(R.string.wd_beat_running)}"
        val text = "$head\n${deviceName()}\n${getString(R.string.wd_beat_at, time)}"
        val token = store.telegramToken
        val known = runtime.beatMessage.takeIf { runtime.beatChat == chat } ?: 0L
        watchdogBusy = !stopped
        telegram.execute {
            var id = known
            if (id != 0L) {
                try {
                    TelegramClient.edit(token, chat, id, text)
                } catch (e: TelegramException) {
                    // Mesaj silinmiş: yenisi gönderilir. "Değişmedi" hatası önemsiz.
                    if (e.code == 400 && e.message?.contains("not modified") != true) id = 0
                } catch (e: Exception) {
                    Log.w(TAG, "heartbeat edit failed: ${e.javaClass.simpleName}")
                }
            }
            if (id == 0L && !stopped) {
                runCatching {
                    id = TelegramClient.send(token, chat, text, silent = true)
                    TelegramClient.pin(token, chat, id)
                }.onFailure { Log.w(TAG, "heartbeat send failed: ${it.javaClass.simpleName}") }
            }
            if (id != 0L) {
                runtime.beatChat = chat
                runtime.beatMessage = id
            }
            if (!stopped) handler.post { watchdogBusy = false }
        }
    }

    /** Diğer cihazın sabit mesajına bakar; [Store.watchdogMin] dakikadır güncellenmediyse alarm. */
    private fun checkWatched(wall: Long) {
        val chat = beatChat() ?: return
        watchCheckAt = wall
        watchdogBusy = true
        val token = store.telegramToken
        telegram.execute {
            val seen = runCatching { Watchdog.read(TelegramClient.chat(token, chat)) }
            handler.post {
                watchdogBusy = false
                // Okunamadıysa (internet yok) bir şey söylenemez; kör kalma uyarısı zaten bunu kapsar.
                seen.onSuccess { onWatchedSeen(it, System.currentTimeMillis()) }
            }
        }
    }

    private fun onWatchedSeen(seen: Watchdog.Seen, now: Long) {
        val app = getString(R.string.app_name)
        when (seen) {
            is Watchdog.Seen.Beat -> {
                if (Watchdog.silent(seen.at, now, store.watchdogMin * 60_000L)) {
                    if (!runtime.watchAlarmed) {
                        runtime.watchAlarmed = true
                        val args = EventArgs.Watchdog(seen.name, lost = true, silentMs = now - seen.at)
                        val text = args.render(app, strings)
                        val effective = RuleEngine.effective(AlertAction.ALARM, quiet = quietNow(), critical = false)
                        log.add("", app, EventKind.WATCHDOG, text, effective.name, args)
                        if (effective == AlertAction.ALARM) addAlarm(text) else notifier.notifyEvent(text)
                        forward("🚨 $text", always = true)
                    }
                } else if (runtime.watchAlarmed) {
                    runtime.watchAlarmed = false
                    val args = EventArgs.Watchdog(seen.name, lost = false, silentMs = (seen.at - runtime.watchLastBeat).coerceAtLeast(0))
                    val text = args.render(app, strings)
                    log.add("", app, EventKind.WATCHDOG, text, AlertAction.NOTIFY.name, args)
                    notifier.notifyEvent(text)
                    forward("✅ $text", always = true)
                }
                if (!runtime.watchAlarmed) runtime.watchLastBeat = seen.at
            }
            // Kullanıcı diğer cihazda izlemeyi kapattı: sessizlik beklenen durum.
            is Watchdog.Seen.Stopped -> runtime.watchAlarmed = false
            Watchdog.Seen.None -> Unit
        }
    }

    // ---------------------------------------------------------------- tatil

    /** Tatil bitince bir kez: bildirim, geçmiş ve Telegram. */
    private fun checkVacation(wall: Long) {
        val until = store.vacationUntil
        if (until == 0L || wall < until || runtime.vacationEndNotified == until) return
        runtime.vacationEndNotified = until
        store.vacationUntil = 0
        val app = getString(R.string.app_name)
        val args = EventArgs.Presence(away = store.awayMode, by = null, vacationEnded = true)
        val text = args.render(app, strings)
        log.add("", app, EventKind.PRESENCE, text, AlertAction.NOTIFY.name, args)
        notifier.notifyEvent(text)
        forward("🏠 $text")
        WeeklyScheduler.schedule(this)
    }

    // ---------------------------------------------------------------- hava ve konum

    private var weatherAt = 0L
    private var weatherBusy = false

    private fun env() = Environment(store.latitude, store.longitude, runtime.outdoorTemp, away = store.isAway())

    /**
     * Alarmın bildirime düştüğü an: sessiz saatler ya da (seçildiyse) evdeyken. Tatilde ikisi de geçersiz: ev boş,
     * her alarm çalsın. Kritik cihazlar her durumda çalar ([RuleEngine.effective]).
     */
    private fun quietNow(): Boolean {
        if (store.onVacation()) return false
        return store.quietHours.contains(minuteOfDay()) || (store.softenAtHome && !store.isAway())
    }

    /** Sıcaklık kuralı varsa yarım saatte bir dış sıcaklığı okur (Open-Meteo); okunamazsa son değer kalır. */
    private fun weatherTick(wall: Long) {
        if (weatherBusy || wall < weatherAt) return
        val lat = store.latitude ?: return
        val lon = store.longitude ?: return
        if (store.automations.none { it.enabled && it.trigger.kind.usesTemp }) return
        weatherAt = wall + WEATHER_MS
        weatherBusy = true
        telegram.execute {
            val t = runCatching { WeatherClient.temperature(lat, lon) }
            handler.post {
                weatherBusy = false
                t.onSuccess {
                    runtime.outdoorTemp = it
                    runAutomations(automations.onTick(store.snapshotsWithGroups.associateBy { d -> d.id }, store.automations, System.currentTimeMillis(), env()))
                }.onFailure {
                    Log.w(TAG, "weather failed: ${it.javaClass.simpleName}")
                    weatherAt = System.currentTimeMillis() + WEATHER_RETRY_MS
                }
            }
        }
    }

    // ---------------------------------------------------------------- şarjdan kesinti

    private var powerReceiver: BroadcastReceiver? = null

    /** Şarj takılıp çıkarılması: telefon/tablet prizdeyse şarjın kesilmesi elektriğin kesildiğini gösterir (internet de gitse). */
    private fun registerPowerReceiver() {
        if (powerReceiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val connected = intent.action == Intent.ACTION_POWER_CONNECTED
                handler.post { onPowerChanged(connected) }
            }
        }
        val filter = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(r, filter)
        powerReceiver = r
    }

    private fun isPlugged(): Boolean =
        (registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0

    private fun onPowerChanged(connected: Boolean) {
        val app = getString(R.string.app_name)
        if (connected) {
            val lost = runtime.chargerLostAt
            if (lost == 0L) return
            runtime.chargerLostAt = 0
            val args = EventArgs.Charger(lost = false, ms = System.currentTimeMillis() - lost)
            val text = args.render(app, strings)
            log.add("", app, EventKind.OUTAGE, text, AlertAction.NOTIFY.name, args)
            notifier.notifyEvent(text)
            forward("🔌 $text", always = true)
            return
        }
        if (!store.chargerOutage) return
        val unpluggedAt = System.currentTimeMillis()
        // Kullanıcı şarjı kendisi çıkarmış olabilir, kısa dalgalanma da olabilir: bir dakika sonra hâlâ şarjda değilse.
        handler.postDelayed({
            if (!running || !store.chargerOutage || runtime.chargerLostAt != 0L || isPlugged()) return@postDelayed
            runtime.chargerLostAt = unpluggedAt
            val args = EventArgs.Charger(lost = true, ms = 0)
            val text = args.render(app, strings)
            Log.w(TAG, "charger lost: possible power outage")
            log.add("", app, EventKind.OUTAGE, text, AlertAction.NOTIFY.name, args)
            notifier.notifyEvent(text)
            forward("⚡ $text", always = true)
        }, CHARGER_CONFIRM_MS)
    }

    // ---------------------------------------------------------------- tüketim

    private var energyCheckAt = 0L

    /** Yarım saatte bir: aylık bütçe; günde bir (gece yarısından sonra): dünkü anormal tüketim. */
    private fun energyTick(wall: Long) {
        if (wall < energyCheckAt) return
        energyCheckAt = wall + ENERGY_CHECK_MS
        val monitored = store.monitoredIds()
        val devices = store.snapshots.filter { it.hasEnergy && it.id in monitored }
        if (devices.isEmpty()) return
        runCatching { checkBudget(devices, wall) }.onFailure { Log.w(TAG, "budget check failed", it) }
        runCatching { checkAnomalies(devices, wall) }.onFailure { Log.w(TAG, "energy check failed", it) }
    }

    private fun checkBudget(devices: List<DeviceSnapshot>, wall: Long) {
        val budget = store.monthlyBudget
        val month = EnergyWatch.monthKey(wall)
        if (budget <= 0 || runtime.budgetWarnedMonth == month) return
        val from = EnergyWatch.startOfMonth(wall)
        // Her cihaz kendi fiyatıyla (yoksa varsayılanla); fiyatı olmayan cihaz maliyete girmez.
        val cost = devices.sumOf { d -> metrics.cost(d.id, from, wall, store.tariffFor(d.id)) }
        if (!EnergyWatch.budgetExceeded(cost, budget)) return
        runtime.budgetWarnedMonth = month
        val app = getString(R.string.app_name)
        val args = EventArgs.Budget(cost, budget)
        val text = args.render(app, strings)
        log.add("", app, EventKind.ENERGY, text, AlertAction.NOTIFY.name, args)
        notifier.notifyEvent(text)
        forward("💸 $text")
    }

    private fun checkAnomalies(devices: List<DeviceSnapshot>, wall: Long) {
        val today = EnergyWatch.dayStart(wall)
        if (!store.energyAnomaly || runtime.energyCheckedDay == today || wall - today < ENERGY_DAY_DELAY_MS) return
        runtime.energyCheckedDay = today
        val yesterday = EnergyWatch.dayStart(wall, -1)
        for (d in devices) {
            // Yalnızca izlemenin günün çoğunda açık olduğu günler karşılaştırılır.
            val days = (1..EnergyWatch.HISTORY_DAYS + 1).map { i -> EnergyWatch.dayStart(wall, -i) to EnergyWatch.dayStart(wall, -i + 1) }
                .filter { (from, to) -> metrics.sampleCount(d.id, from, to) >= EnergyWatch.MIN_SAMPLES }
            val last = days.firstOrNull()?.takeIf { it.first == yesterday } ?: continue
            val history = days.drop(1).map { (from, to) -> metrics.kwh(d.id, from, to) }
            val a = EnergyWatch.anomaly(metrics.kwh(d.id, last.first, last.second), history) ?: continue
            val args = EventArgs.EnergyAnomaly(a.high, a.kwh, a.average)
            val text = args.render(d.name, strings)
            log.add(d.id, d.name, EventKind.ENERGY, text, AlertAction.NOTIFY.name, args)
            notifier.notifyEvent(text)
            forward("📈 $text")
        }
    }

    /** Alarmın kapatılması geçmişe yazılır: kaç olay vardı ve ne kadar sürede kapatıldı (tepki süresi). */
    private fun logDismissal(count: Int) {
        val now = System.currentTimeMillis()
        val started = runtime.alarmStartedAt.takeIf { it in 1..now } ?: now
        val args = EventArgs.AlarmDismissed(count, now - started)
        log.add("", getString(R.string.app_name), EventKind.DISMISSED, args.render(getString(R.string.app_name), strings), "", args)
        runtime.alarmStartedAt = 0
    }

    private fun onAuthFailed() {
        socket.close()
        setState(ConnState.AUTH_ERROR)
        if (!authFailed) {
            authFailed = true
            if (store.sessionLost) {
                notifier.sessionLost(getString(R.string.session_lost_body), getString(R.string.session_lost_tip))
            } else {
                notifier.sessionLost(getString(R.string.warn_session_expired), "")
            }
        }
    }

    /** Uygulama dili değişince (Android 13+ sistem bildirir) durum bildirimi yeni dille yeniden yazılsın. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        strings = EventLog.strings(this)
        if (!running) return
        publishState()
    }

    /** eWeLink kaynağının durumu. */
    private fun setState(state: ConnState, detail: String = "", synced: Boolean = false) {
        ewState = state to detail
        publishState(synced)
    }

    private fun setTuyaState(state: ConnState, detail: String = "", synced: Boolean = false) {
        tuyaState = state to detail
        publishState(synced)
    }

    /** Kullanılan kaynakların en kötü durumunu gösterir; Tuya'dan geliyorsa açıklamaya "Tuya" eklenir. */
    private fun publishState(synced: Boolean = false) {
        // Durdurulduktan sonra biten bir istek bildirimi geri getirmesin.
        if (!running) return
        val ew = ewState.takeIf { store.isLoggedIn || (!store.hasTuya && !store.demoMode) }
        val tu = tuyaState.takeIf { store.hasTuya }
        // Demo her zaman "canlı"; gerçek bir kaynak da varsa onun durumu baskın gelir.
        val demo = (ConnState.LIVE to "").takeIf { store.demoMode }
        val worst = listOfNotNull(ew, tu, demo).maxByOrNull { SEVERITY.indexOf(it.first) }
        val state = worst?.first ?: ConnState.CONNECTING
        val detail = when {
            worst == null -> ""
            worst === tu && ew != null -> listOf("Tuya", worst.second).filter { it.isNotEmpty() }.joinToString(": ")
            else -> worst.second
        }
        LiveState.setConnection(state, detail, synced)
        val count = store.monitoredIds().size
        val text = when (state) {
            ConnState.LIVE -> resources.getQuantityString(R.plurals.status_live, count, count)
            ConnState.POLLING -> resources.getQuantityString(R.plurals.status_polling, count, count, store.pollIntervalSec)
            ConnState.CONNECTING -> getString(R.string.state_connecting)
            ConnState.NO_INTERNET -> getString(R.string.state_no_internet)
            ConnState.AUTH_ERROR -> getString(R.string.state_auth_error)
            ConnState.ERROR -> getString(R.string.state_error) + " " + detail
            ConnState.STOPPED -> getString(R.string.state_stopped)
        }
        notifier.updateStatus(getString(R.string.app_name), text)
        StatusSurfaces.refresh(this)
    }

    private fun minuteOfDay(): Int = Calendar.getInstance().run { get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE) }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_TEST = "test"
        /** [ACTION_TEST]: ses ve titreşim olmadan (yalnızca ekran ve bildirim). */
        const val EXTRA_SILENT = "silent"
        /** [ACTION_TEST]: bu kadar ms sonra çal. */
        const val EXTRA_DELAY_MS = "delayMs"
        private const val MAX_TEST_DELAY_MS = 60_000L
        const val ACTION_SYNC = "sync"
        /** Giriş/ayar değişince: hata durumlarını sıfırla ve yeniden bağlan. */
        const val ACTION_RESTART = "restart"
        /** Demo simülasyonu: [EXTRA_DEMO_OP] = [DEMO_TOGGLE] / [DEMO_ONLINE] / [DEMO_POWER]. */
        const val ACTION_DEMO = "demo"
        const val EXTRA_DEMO_OP = "op"
        const val EXTRA_DEVICE = "device"
        const val EXTRA_CHANNEL = "channel"
        const val DEMO_TOGGLE = "toggle"
        const val DEMO_ONLINE = "online"
        const val DEMO_POWER = "power"
        private const val DEMO_JITTER_MS = 60_000L

        private const val TAG = "SwitchGuard"
        private const val LIVE_ENERGY_REFRESH_MS = 90_000L
        private const val LIVE_ENERGY_SECONDS = 120
        private const val POWER_STALE_MS = 5 * 60_000L
        private const val LOOP_MS = 15_000L
        private const val ALARM_TICK_MS = 60_000L
        private const val HEARTBEAT_MS = 60_000L
        /** Cihazların durumunun alınamadığı durumlar. */
        private val BLIND_STATES = setOf(ConnState.CONNECTING, ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR)
        private const val RESYNC_MS = 5 * 60_000L
        /** Anlık bağlantı canlıyken başarısız senkronun yeniden deneme aralıkları (sonuncusu tekrarlanır). */
        private val SYNC_RETRY_MS = longArrayOf(15_000, 30_000, 60_000, 120_000)
        private const val ESCALATION_RETRY_MS = 60_000L
        private const val TG_POLL_MS = 15_000L
        private const val CHARGER_CONFIRM_MS = 60_000L
        private const val WEATHER_MS = 30 * 60_000L
        private const val WEATHER_RETRY_MS = 5 * 60_000L
        private const val TG_BLOCKED_MS = 30 * 60_000L
        private const val MAX_STATUS_DEVICES = 40
        private const val MAX_TG_CHARS = 3900
        private const val ENERGY_CHECK_MS = 30 * 60_000L
        /** Dünkü tüketim gece yarısından bu kadar sonra değerlendirilir (son ölçümler yazılsın). */
        private const val ENERGY_DAY_DELAY_MS = 30 * 60_000L
        /** Yeniden denemenin işe yaramayacağı Telegram hataları: 400 sohbet yok, 401 anahtar geçersiz, 403 bot engellendi. */
        private val PERMANENT_TELEGRAM_ERRORS = setOf(400, 401, 403)
        private const val CONFIRM_DELAY_MS = 5_000L
        /**
         * Tuya ücretsiz planı ayda ≈26 bin API çağrısı verir; her senkron 2 çağrı.
         * Mesaj servisi açıkken 15 dk'da bir (≈6 bin/ay), değilse 5 dk'da bir (≈17 bin/ay).
         */
        private const val TUYA_RESYNC_MS = 15 * 60_000L
        private const val TUYA_POLL_MS = 5 * 60_000L
        private const val TUYA_BLOCKED_MS = 30 * 60_000L
        private const val TUYA_CONNECT_TIMEOUT_MS = 60_000L
        private val SEVERITY = listOf(
            ConnState.LIVE, ConnState.POLLING, ConnState.CONNECTING,
            ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR,
        )

        @Volatile var instance: MonitorService? = null
            private set

        /**
         * Telegram gönderimleri (bkz. checkEscalation). Süreç boyunca tek ve hiç kapatılmaz: servis durdurulurken
         * kuyruğa giren "alarm kapatıldı" mesajı da gidebilsin.
         */
        private val telegram = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "telegram").apply { isDaemon = true } }

        private fun tickPendingIntent(context: Context) = PendingIntent.getBroadcast(
            context, 0, Intent(context, TickReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        /**
         * Bir sonraki tick'i (ve bekçi denetimini) kurar. Kesin olmayan "allow while idle" alarmı özel izin
         * gerektirmez; sistem Doze'da da çalıştırır.
         */
        fun scheduleTick(context: Context) {
            context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_TICK_MS,
                tickPendingIntent(context)
            )
        }

        fun send(context: Context, action: String) {
            val intent = Intent(context, MonitorService::class.java).setAction(action)
            if (action == ACTION_START || action == ACTION_RESTART) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun test(context: Context, silent: Boolean, delayMs: Long) {
            context.startService(
                Intent(context, MonitorService::class.java).setAction(ACTION_TEST)
                    .putExtra(EXTRA_SILENT, silent).putExtra(EXTRA_DELAY_MS, delayMs)
            )
        }

        fun demo(context: Context, op: String, deviceId: String, channel: Int = 0) {
            val intent = Intent(context, MonitorService::class.java).setAction(ACTION_DEMO)
                .putExtra(EXTRA_DEMO_OP, op).putExtra(EXTRA_DEVICE, deviceId).putExtra(EXTRA_CHANNEL, channel)
            // Servis izlemeyi başlatıp ön plana geçeceği için ön plan servisi olarak başlatılır.
            context.startForegroundService(intent)
        }
    }
}
