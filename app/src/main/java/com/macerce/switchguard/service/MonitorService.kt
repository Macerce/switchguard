package com.macerce.switchguard.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.api.AuthException
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.api.TuyaSocket
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.TuyaEvent
import com.macerce.switchguard.core.TuyaParser
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationEngine
import com.macerce.switchguard.core.Change
import com.macerce.switchguard.core.ChangeDetector
import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.toArgs
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.OfflineGrace
import com.macerce.switchguard.core.RuleEngine
import com.macerce.switchguard.core.WsEvent
import com.macerce.switchguard.core.WsMessages
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
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
    private val automations = AutomationEngine()

    private val grace = OfflineGrace()
    private var running = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var authFailed = false
    private var wsUnsupported = false
    /** Anlık bağlantının neden kurulamadığı; periyodik senkron bu açıklamayı silmesin diye saklanır. */
    private var wsIssue = ""
    private var reconnectAttempt = 0
    private var nextReconnectAt = 0L
    private var lastSyncAt = 0L
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
    /** Bilgiler hatalı ya da deneme süresi bitmiş: kota harcamamak için seyrek denenir. */
    private var tuyaBlocked = false
    private var tuyaWarned = false

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
        client = EwelinkClient(store)
        notifier = Notifier(this, store)
        strings = EventLog.strings(this)
        worker = HandlerThread("monitor").also { it.start() }
        handler = Handler(worker.looper)
        socket = EwelinkSocket(store, socketListener)
        tuya = TuyaClient(store)
        tuyaSocket = TuyaSocket(store, tuyaListener)
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                handler.post {
                    store.pendingAlarm = emptyList()
                    notifier.stopAlarm()
                }
                if (!running) stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TEST -> {
                handler.post {
                    log.add("", getString(R.string.app_name), EventKind.TEST, strings.test, AlertAction.ALARM.name, EventArgs.Test)
                    addAlarm(getString(R.string.test_alarm_line))
                }
                return if (running) START_STICKY else START_NOT_STICKY
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
        registerNetworkCallback()
        handler.post {
            applyWakeMode()
            // Kapatılmamış alarm varsa (ör. telefon yeniden başladı) geri getir.
            store.pendingAlarm.takeIf { it.isNotEmpty() }?.let { notifier.showAlarm(it) }
            setState(ConnState.CONNECTING)
            runCatching { metrics.prune() }
            SummaryScheduler.schedule(this)
            sync()
            reconnectNow()
            tuyaTick(SystemClock.elapsedRealtime())
            handler.post(loop)
        }
        scheduleAlarmTick()
    }

    private fun stopMonitoring() {
        store.monitoringEnabled = false
        running = false
        LiveState.setConnection(ConnState.STOPPED)
        handler.removeCallbacksAndMessages(null)
        handler.post {
            socket.close()
            tuyaSocket.close()
            store.pendingAlarm = emptyList()
            notifier.stopAlarm()
            // Kaydırılamayan oturum uyarısı izleme kapatılınca asılı kalmasın.
            notifier.clearWarning()
        }
        cancelAlarmTick()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        instance = null
        running = false
        networkCallback?.let { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it) }
        cancelAlarmTick()
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
        if (!running) return
        val now = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()

        grace.due(wall).forEach { dispatch(it, store.rulesFor(it.deviceId).actionFor(it.eventType)) }
        runAutomations(automations.onTick(store.snapshots.associateBy { it.id }, store.automations, wall))
        tuyaTick(now)

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

    private fun tickPendingIntent() = PendingIntent.getBroadcast(
        this, 0, Intent(this, TickReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun scheduleAlarmTick() {
        // Kesin olmayan "allow while idle" alarmı özel izin gerektirmez; sistem Doze'da da çalıştırır.
        getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + ALARM_TICK_MS,
            tickPendingIntent()
        )
    }

    private fun cancelAlarmTick() = getSystemService(AlarmManager::class.java).cancel(tickPendingIntent())

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
                // Profil yolu bazı App ID'lere kapalı (407); o zaman apikey cihaz listesinden alınır.
                try { client.loadProfile() } catch (e: ApiException) { Log.w(TAG, "profile unavailable: ${e.message}") }
                if (store.userApiKey.isEmpty()) client.getDevices()
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
            if (store.hasTuya) {
                ewState = null
                if (store.snapshots.any { it.cloud == Cloud.EWELINK }) replaceCloud(Cloud.EWELINK, emptyList())
            } else onAuthFailed()
            return
        }
        try {
            replaceCloud(Cloud.EWELINK, client.getDevices())
            if (authFailed) {
                authFailed = false
                notifier.clearWarning()
            }
            if (socket.ready) setState(ConnState.LIVE, synced = true)
            else setState(ConnState.POLLING, wsIssue, synced = true)
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: IOException) {
            // Telefonun interneti yok: cihaz "offline" sayılmaz, sadece durum güncellenir.
            setState(ConnState.NO_INTERNET)
        } catch (e: ApiException) {
            setState(ConnState.ERROR, getString(R.string.detail_api_error, e.code))
        } catch (e: Exception) {
            setState(ConnState.ERROR, e.message.orEmpty())
        }
    }

    /**
     * Uygulamadan gönderilen aç/kapa komutu başarılı olunca çağrılır. Bulut, cihazın yeni durumunu
     * ancak cihaz geri bildirdikten sonra (≈1-2 sn) döndürdüğünden hemen senkron yapmak eski durumu
     * geri yazar. Bu yüzden yeni durum hemen uygulanır, bulutla karşılaştırma gecikmeli yapılır.
     */
    fun onControlled(deviceId: String, channel: Int, on: Boolean) {
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
        if (device.cloud == Cloud.TUYA) tuya.setSwitch(device, channel, on) else client.setSwitch(device, channel, on)
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
            tuyaBlocked = false
            tuyaWarned = false
            if (tuyaSocket.ready) setTuyaState(ConnState.LIVE, synced = true)
            else setTuyaState(ConnState.POLLING, tuyaIssue, synced = true)
        } catch (e: TuyaExpiredException) {
            onTuyaBlocked(getString(R.string.tuya_expired))
        } catch (e: TuyaAuthException) {
            onTuyaBlocked(getString(R.string.tuya_auth_error))
        } catch (e: IOException) {
            setTuyaState(ConnState.NO_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Tuya sync failed", e)
            setTuyaState(ConnState.ERROR, e.message.orEmpty())
        }
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
        val previous = store.snapshots.associateBy { it.id }
        store.snapshots = current
        val wall = System.currentTimeMillis()
        runCatching { metrics.record(previous, current, wall) }
        val changes = ChangeDetector.detect(previous, current.associateBy { it.id }, store.monitoredIds())
        changes.forEach(::handle)
        runAutomations(automations.onStates(previous, current.associateBy { it.id }, store.automations, wall))
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
        val due = automations.nextDueAt(store.snapshots.associateBy { it.id }, store.automations) ?: return
        handler.postDelayed({ tick() }, (due - System.currentTimeMillis()).coerceAtLeast(0) + 50)
    }

    private fun execute(a: Automation) {
        when (a.action.kind) {
            ActionKind.TURN_ON, ActionKind.TURN_OFF -> {
                val on = a.action.kind == ActionKind.TURN_ON
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
            ActionKind.ALARM, ActionKind.NOTIFY -> {
                val source = store.snapshots.firstOrNull { it.id == a.trigger.deviceId }
                val args = EventArgs.AutoAlert(a.name, source?.name)
                val text = args.render(source?.name ?: a.name, strings)
                val action = if (a.action.kind == ActionKind.ALARM) AlertAction.ALARM else AlertAction.NOTIFY
                // Ücretsiz sürümde izlenmeyen cihazın otomasyonu uyarı vermez (sınır otomasyonla aşılmasın); yalnızca kaydedilir.
                val allowed = store.isPro || a.trigger.deviceId in store.monitoredIds()
                val effective = if (!allowed) AlertAction.IGNORE
                    else RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()))
                log.add(a.trigger.deviceId, source?.name ?: a.name, EventKind.AUTOMATION, text, effective.name, args)
                val sound = store.rulesFor(a.trigger.deviceId).soundUri
                when (effective) {
                    AlertAction.ALARM -> addAlarm(text, sound)
                    AlertAction.NOTIFY -> notifier.notifyEvent(text, sound)
                    AlertAction.IGNORE -> Unit
                }
            }
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
            if (change.to && grace.cameBack(change.deviceId)) {
                log.add(change.deviceId, change.name, EventKind.SHORT_DROP, strings.shortDrop(change.name), AlertAction.IGNORE.name, EventArgs.ShortDrop)
                return
            }
        }
        dispatch(change, action)
    }

    private fun dispatch(change: Change, action: AlertAction) {
        val effective = RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()))
        val text = change.describe(labels)
        log.add(change.deviceId, change.name, change.eventType.name, text, effective.name, change.toArgs())
        val sound = store.rulesFor(change.deviceId).soundUri
        when (effective) {
            AlertAction.ALARM -> addAlarm(text, sound)
            AlertAction.NOTIFY -> notifier.notifyEvent(text, sound)
            AlertAction.IGNORE -> Unit
        }
    }

    /** [sound]: olayın cihazına özel ses (yoksa genel ses). */
    private fun addAlarm(text: String, sound: String? = null) {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        store.pendingAlarm = store.pendingAlarm + "$time  $text"
        notifier.showAlarm(store.pendingAlarm, sound)
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
        val ew = ewState.takeIf { store.isLoggedIn || !store.hasTuya }
        val tu = tuyaState.takeIf { store.hasTuya }
        val worst = listOfNotNull(ew, tu).maxByOrNull { SEVERITY.indexOf(it.first) }
        val state = worst?.first ?: ConnState.CONNECTING
        val detail = when {
            worst == null -> ""
            worst === tu && ew != null -> listOf("Tuya", worst.second).filter { it.isNotEmpty() }.joinToString(": ")
            else -> worst.second
        }
        LiveState.setConnection(state, detail, synced)
        val count = store.monitoredIds().size
        val text = when (state) {
            ConnState.LIVE -> getString(R.string.status_live, count)
            ConnState.POLLING -> getString(R.string.status_polling, count, store.pollIntervalSec)
            ConnState.CONNECTING -> getString(R.string.state_connecting)
            ConnState.NO_INTERNET -> getString(R.string.state_no_internet)
            ConnState.AUTH_ERROR -> getString(R.string.state_auth_error)
            ConnState.ERROR -> getString(R.string.state_error) + " " + detail
            ConnState.STOPPED -> getString(R.string.state_stopped)
        }
        notifier.updateStatus(getString(R.string.app_name), text)
    }

    private fun minuteOfDay(): Int = Calendar.getInstance().run { get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE) }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_TEST = "test"
        const val ACTION_SYNC = "sync"
        /** Giriş/ayar değişince: hata durumlarını sıfırla ve yeniden bağlan. */
        const val ACTION_RESTART = "restart"

        private const val TAG = "SwitchGuard"
        private const val LIVE_ENERGY_REFRESH_MS = 90_000L
        private const val LIVE_ENERGY_SECONDS = 120
        private const val POWER_STALE_MS = 5 * 60_000L
        private const val LOOP_MS = 15_000L
        private const val ALARM_TICK_MS = 60_000L
        private const val RESYNC_MS = 5 * 60_000L
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

        fun send(context: Context, action: String) {
            val intent = Intent(context, MonitorService::class.java).setAction(action)
            if (action == ACTION_START || action == ACTION_RESTART) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
