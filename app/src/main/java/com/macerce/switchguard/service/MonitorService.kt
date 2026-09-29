package com.macerce.switchguard.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.macerce.switchguard.R
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.api.AuthException
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationEngine
import com.macerce.switchguard.core.Change
import com.macerce.switchguard.core.ChangeDetector
import com.macerce.switchguard.core.ChangeLabels
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
 *
 * Zamanlama: telefon uyanıkken [handler] döngüsü, uyurken AlarmManager "tick"i aynı [tick]'i çağırır.
 */
class MonitorService : Service() {

    private lateinit var store: Store
    private lateinit var log: EventLog
    private lateinit var client: EwelinkClient
    private lateinit var notifier: Notifier
    private lateinit var socket: EwelinkSocket
    private lateinit var worker: HandlerThread
    private lateinit var handler: Handler
    private lateinit var labels: ChangeLabels
    private lateinit var metrics: MetricsLog
    private val automations = AutomationEngine()

    private val grace = OfflineGrace()
    private var running = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var authFailed = false
    private var wsUnsupported = false
    private var reconnectAttempt = 0
    private var nextReconnectAt = 0L
    private var lastSyncAt = 0L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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
        labels = labels(this)
        worker = HandlerThread("monitor").also { it.start() }
        handler = Handler(worker.looper)
        socket = EwelinkSocket(store, socketListener)
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
                    log.add("", getString(R.string.app_name), EventKind.TEST, getString(R.string.test_alarm_line), AlertAction.ALARM.name)
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
            store.pendingAlarm = emptyList()
            notifier.stopAlarm()
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

        if (authFailed) return
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
                handler.post { if (running && !socket.ready) reconnectNow() }
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
            // Bağlantı yokken kaçan değişiklikleri yakala.
            sync()
        }.let { }

        override fun onEvent(event: WsEvent) = handler.post {
            if (event is WsEvent.Update) applyUpdate(event)
        }.let { }

        override fun onClosed(reason: String, fatal: Boolean) = handler.post {
            if (!running) return@post
            if (fatal) {
                wsUnsupported = true
                setState(ConnState.POLLING, getString(R.string.detail_ws_unsupported))
            } else {
                scheduleReconnect()
                if (!authFailed) setState(ConnState.POLLING, getString(R.string.detail_reconnecting))
            }
        }.let { }
    }

    private fun reconnectNow() {
        if (!running || authFailed || wsUnsupported) return
        nextReconnectAt = 0
        try {
            if (store.userApiKey.isEmpty()) client.loadProfile()
            if (store.userApiKey.isEmpty()) {
                wsUnsupported = true
                setState(ConnState.POLLING, getString(R.string.detail_ws_unsupported))
                return
            }
            socket.connect()
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: Exception) {
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
            onAuthFailed()
            return
        }
        try {
            process(client.getDevices())
            if (authFailed) {
                authFailed = false
                notifier.clearWarning()
            }
            setState(if (socket.ready) ConnState.LIVE else ConnState.POLLING, synced = true)
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
        handler.postDelayed({ if (running) sync() }, CONFIRM_DELAY_MS)
    }

    private fun applyUpdate(update: WsEvent.Update) {
        val list = store.snapshots
        val before = list.firstOrNull { it.id == update.deviceId } ?: return
        val after = WsMessages.apply(before, update)
        if (after == before) return
        process(list.map { if (it.id == after.id) after else it })
        setState(ConnState.LIVE, synced = true)
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

    private fun runAutomations(fired: List<Automation>) {
        fired.forEach(::execute)
        // Süre dolumu ya da gecikmeli eylem varsa tam vaktinde uyan.
        val due = automations.nextDueAt(store.snapshots.associateBy { it.id }, store.automations) ?: return
        handler.postDelayed({ tick() }, (due - System.currentTimeMillis()).coerceAtLeast(0) + 50)
    }

    private fun execute(a: Automation) {
        val title = getString(R.string.automation_fired, a.name)
        when (a.action.kind) {
            ActionKind.TURN_ON, ActionKind.TURN_OFF -> {
                val on = a.action.kind == ActionKind.TURN_ON
                val target = store.snapshots.firstOrNull { it.id == a.action.deviceId }
                if (target == null) {
                    log.add(a.action.deviceId, a.name, EventKind.AUTOMATION, "$title: ${getString(R.string.device_missing)}", "")
                    return
                }
                val label = if (target.isMultiChannel) labels.channel(target.name, a.action.channel + 1) else target.name
                val text = "$title: $label → ${if (on) labels.on else labels.off}"
                try {
                    // Otomasyonun yaptığı değişiklik alarm çaldırmasın.
                    LiveState.expected.expect(target.id, a.action.channel, on, System.currentTimeMillis())
                    client.setSwitch(target, a.action.channel, on)
                    log.add(target.id, target.name, EventKind.AUTOMATION, text, "")
                } catch (e: Exception) {
                    val failed = getString(R.string.automation_failed, text, e.message.orEmpty())
                    log.add(target.id, target.name, EventKind.AUTOMATION, failed, AlertAction.NOTIFY.name)
                    notifier.notifyEvent(failed)
                }
            }
            ActionKind.ALARM, ActionKind.NOTIFY -> {
                val source = store.snapshots.firstOrNull { it.id == a.trigger.deviceId }
                val text = if (source != null) "$title (${source.name})" else title
                val action = if (a.action.kind == ActionKind.ALARM) AlertAction.ALARM else AlertAction.NOTIFY
                val effective = RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()))
                log.add(a.trigger.deviceId, source?.name ?: a.name, EventKind.AUTOMATION, text, effective.name)
                if (effective == AlertAction.ALARM) addAlarm(text) else notifier.notifyEvent(text)
            }
        }
    }

    private fun handle(change: Change) {
        val wall = System.currentTimeMillis()
        if (LiveState.expected.consume(change, wall)) {
            log.add(change.deviceId, change.name, EventKind.USER, change.describe(labels), "")
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
                log.add(change.deviceId, change.name, EventKind.SHORT_DROP, getString(R.string.event_short_drop, change.name), AlertAction.IGNORE.name)
                return
            }
        }
        dispatch(change, action)
    }

    private fun dispatch(change: Change, action: AlertAction) {
        val effective = RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()))
        val text = change.describe(labels)
        log.add(change.deviceId, change.name, change.eventType.name, text, effective.name)
        when (effective) {
            AlertAction.ALARM -> addAlarm(text)
            AlertAction.NOTIFY -> notifier.notifyEvent(text)
            AlertAction.IGNORE -> Unit
        }
    }

    private fun addAlarm(text: String) {
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())
        store.pendingAlarm = store.pendingAlarm + "$time  $text"
        notifier.showAlarm(store.pendingAlarm)
    }

    private fun onAuthFailed() {
        socket.close()
        setState(ConnState.AUTH_ERROR)
        if (!authFailed) {
            authFailed = true
            notifier.warn(getString(R.string.warn_session_expired))
        }
    }

    private fun setState(state: ConnState, detail: String = "", synced: Boolean = false) {
        // Durdurulduktan sonra biten bir istek bildirimi geri getirmesin.
        if (!running) return
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

        private const val LOOP_MS = 15_000L
        private const val ALARM_TICK_MS = 60_000L
        private const val RESYNC_MS = 5 * 60_000L
        private const val CONFIRM_DELAY_MS = 5_000L

        @Volatile var instance: MonitorService? = null
            private set

        fun labels(context: Context) = ChangeLabels(
            on = context.getString(R.string.label_on),
            off = context.getString(R.string.label_off),
            online = context.getString(R.string.label_online),
            offline = context.getString(R.string.label_offline),
        ) { name, n -> context.getString(R.string.label_channel, name, n) }

        fun send(context: Context, action: String) {
            val intent = Intent(context, MonitorService::class.java).setAction(action)
            if (action == ACTION_START || action == ACTION_RESTART) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
