package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.api.AuthException
import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.api.TelegramException
import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.api.TuyaSocket
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlarmReminders
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationEngine
import com.macerce.switchguard.core.BlindWatch
import com.macerce.switchguard.core.Change
import com.macerce.switchguard.core.ChangeDetector
import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.core.MonitorGap
import com.macerce.switchguard.core.OfflineGrace
import com.macerce.switchguard.core.RuleEngine
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
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.i18n.Strings
import java.io.IOException
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

/**
 * Cihazları izleyen motor: telefondaki `app/.../service/MonitorService.kt`'in masaüstü karşılığı. Yöntemler
 * telefondakiyle aynı adları ve sırayı taşır, yan yana karşılaştırılabilsin diye.
 *
 * Kaynaklar: WebSocket (anlık), periyodik HTTP senkronu (kaçan olaylar için), WebSocket
 * kullanılamazsa HTTP sorgulama. Tüm mantık tek bir iş parçacığında ([scheduler]) çalışır.
 * eWeLink ve Tuya birbirinden bağımsız iki kaynaktır; cihaz listeleri [Cloud]'a göre birleştirilir.
 *
 * Telefondan farklar: Android'e bağlı kısımlar (ön plan servisi, AlarmManager tick'i, ağ geri çağrısı, günlük
 * özet) yok; bildirim/alarm ekranı/uyanık tutma [host] üzerinden. Masaüstüne özgü: Tuya anlık bağlantısı
 * yalnızca `store.tuyaLive` açıkken kurulur; bilgisayar uyuyup uyandığında (duvar saati iki tick arasında
 * [SLEEP_GAP_MS]'den fazla ilerlediyse) bağlantılar baştan kurulur ve iki bulut hemen senkronlanır. Olay geçmişine
 * yazma hatası ölümcül değildir: alarm/bildirim önce verilir, geçmiş sonra ve yalnızca [record] ile yazılır.
 * Telegram gönderimleri [telegramRunner]'da (telefonda ayrı "telegram" iş parçacığı): yavaş bağlantı izlemeyi bekletmesin.
 */
class MonitorEngine(
    private val store: Store,
    private val log: EventLog,
    private val metrics: MetricsLog,
    /** Yeniden başlatmalarda korunan iç durum (`runtime.json`; telefondaki `RuntimeState`). */
    private val runtime: RuntimeState,
    /** Her çağrıda o anki dil; dil değişince yeni metinler hemen kullanılır (telefonda onConfigurationChanged). */
    private val strings: () -> Strings,
    private val host: MonitorHost,
    private val sources: Sources,
    private val clock: Clock = SystemClock,
    private val scheduler: Scheduler = ExecutorScheduler("monitor"),
    /** Telegram gönderimlerini çalıştırır (testlerde aynı iş parçacığında). */
    private val telegramRunner: (() -> Unit) -> Unit = { TELEGRAM.execute(it) },
) {
    private val client: EwelinkApi = sources.ewelink
    private val socket: EwelinkLive by lazy { sources.ewelinkLive(socketListener) }
    private val eventStrings: EventStrings get() = strings().eventStrings()
    private val labels: ChangeLabels get() = eventStrings.labels
    private var automations = AutomationEngine()

    private val blindWatch = BlindWatch()
    /** Kapatılan ama sürmekte olan alarmların hatırlatmaları (bkz. [checkReminders]). */
    private var reminders = AlarmReminders()

    /** Başlarken diskteki kayıttan yüklenir (bkz. [restoreRuntime]). */
    private var grace = OfflineGrace()
    /** İzleme açık mı. Arayüz iş parçacığından da okunur. */
    @Volatile var running = false
        private set
    private var authFailed = false
    private var wsUnsupported = false
    /** Anlık bağlantının neden kurulamadığı; periyodik senkron bu açıklamayı silmesin diye saklanır. */
    private var wsIssue = ""
    private var reconnectAttempt = 0
    private var nextReconnectAt = 0L
    private var lastSyncAt = 0L
    /** Kanal gücü izlenen cihazlar: son canlı enerji isteği, son gelen kanal gücü, "veri yok" uyarısı verildi mi. */
    private val liveEnergyRequestedAt = mutableMapOf<String, Long>()
    private val channelPowerSeenAt = mutableMapOf<String, Long>()
    private val powerStaleWarned = mutableSetOf<String>()

    private val tuya: TuyaApi = sources.tuya
    private val tuyaSocket: TuyaLive by lazy { sources.tuyaLive(tuyaListener) }
    private var tuyaIssue = ""
    private var tuyaReconnectAttempt = 0
    private var tuyaNextConnectAt = 0L
    private var lastTuyaSyncAt = 0L
    /** Bilgiler hatalı ya da deneme süresi bitmiş: kota harcamamak için seyrek denenir. */
    private var tuyaBlocked = false
    private var tuyaWarned = false

    /** Demo: gücü düşürülmüş kanallar (cihaz → kanallar) ve son dalgalanma zamanı. */
    private val demoStalled = mutableMapOf<String, MutableSet<Int>>()
    private var lastDemoJitterAt = 0L

    /** Kaynakların ayrı ayrı durumu; ekrana ikisinin kötüsü yansır (bkz. [publishState]). */
    private var ewState: Pair<ConnState, String>? = null
    private var tuyaState: Pair<ConnState, String>? = null

    /** Masaüstü: bir önceki tick'in duvar saati (uyku algılama, bkz. [sleptFor]); 0 = bu çalışmada henüz tick yok. */
    private var lastTickWall = 0L
    private var lastTickAwake = 0L

    /**
     * Masaüstü: döngünün kuşağı. Telefonda servis durunca `onDestroy` döngüyü kaldırır; burada motor yaşamaya devam
     * ettiğinden, durdurma anında çalışmakta olan döngü kendini yeniden kurabilir. Her başlatma yeni kuşak açar,
     * eski kuşağın döngüsü sırası gelince kendiliğinden biter (iki döngü birden çalışmaz).
     */
    private var loopGeneration = 0

    init {
        // İş parçacığının ilk işi: sonraki hiçbir senkron/kayıt, diskteki bekleyen işlerin üstüne yazamasın.
        scheduler.post { restoreRuntime() }
    }

    // ---------------------------------------------------------------- komutlar (telefonda onStartCommand eylemleri)

    /** `ACTION_START`. */
    fun start() = startMonitoring()

    /** `ACTION_STOP`: izlemeyi kapatır (`monitoringEnabled` = false); alarm susar, bilgisayar uyuyabilir. */
    fun stop() = stopMonitoring()

    /**
     * `ACTION_DISMISS`. Masaüstü: yalnızca kullanıcının gördüğü [lines] kapatılır (ses ve pencere arayüz tarafında
     * zaten kapandı, bkz. `AppGraph.dismissAlarm`). Tıklamayla bu iş arasında gelen yeni satır kaybolmaz: kalırsa
     * alarm onunla yeniden gösterilir.
     */
    fun dismissAlarm(lines: List<String>) {
        scheduler.post {
            val pending = store.pendingAlarm
            val rest = if (pending.take(lines.size) == lines) pending.drop(lines.size)
                else pending.toMutableList().apply { lines.forEach { remove(it) } }
            if (lines.isNotEmpty() && pending.isNotEmpty()) logDismissal(lines.size)
            store.pendingAlarm = rest
            if (rest.isEmpty()) {
                reminders.onDismiss(clock.wall())
                persistRuntime()
                runtime.silentAlarm = false
                endEscalation()
                host.stopAlarm()
            } else {
                // Masaüstü: kapatırken yeni satır geldi; alarm o satırla sürer, tepki süresi ondan sayılır.
                runtime.alarmStartedAt = clock.wall()
                host.showAlarm(rest, null, silent = runtime.silentAlarm)
            }
        }
    }

    /**
     * `ACTION_TEST`: izleme kapalıyken de çalışır, izlemeyi başlatmaz. [silent]: ses olmadan (yalnızca pencere);
     * [delayMs] sonra çalar (en çok [MAX_TEST_DELAY_MS]). Test alarmı kimseye Telegram mesajı göndermez.
     */
    fun testAlarm(silent: Boolean = false, delayMs: Long = 0) {
        scheduler.postDelayed(delayMs.coerceIn(0, MAX_TEST_DELAY_MS)) {
            addAlarm(strings()[if (silent) "test_alarm_line_silent" else "test_alarm_line"], silent = silent, test = true)
            record("", strings()["app_name"], EventKind.TEST, eventStrings.test, AlertAction.ALARM.name, EventArgs.Test)
        }
    }

    /** `ACTION_DEMO`: [op] = [DEMO_TOGGLE] / [DEMO_ONLINE] / [DEMO_POWER]. */
    fun demo(op: String, deviceId: String, channel: Int = 0) {
        // Simülasyon gerçek akıştan geçsin diye izleme kapalıysa önce açılır.
        startMonitoring()
        scheduler.post { demoSimulate(op, deviceId, channel) }
    }

    /**
     * Masaüstü: kartta "İzle" / "Kurallar" değişikliği. Motor iş parçacığında, **o anki** kurala uygulanır (`spec.md` §4:
     * arayüz yalnızca motora gönderir). Bütün kural değil değişikliğin kendisi gönderilir: motor meşgulken art arda
     * yapılan değişiklikler (ör. "İzleniyor"u kapatıp hemen kural penceresinde bir olay seçmek) birbirini ezmez.
     */
    fun updateRules(deviceId: String, change: (DeviceRules) -> DeviceRules) {
        scheduler.post { store.setRules(deviceId, change(store.rulesFor(deviceId))) }
    }

    /** Otomasyonlar ekranından ekleme/değiştirme: motor iş parçacığında yazılır (`spec.md` §4). */
    fun saveAutomation(a: Automation) {
        scheduler.post { store.saveAutomation(a) }
    }

    /** Otomasyonlar ekranından silme: motor iş parçacığında. */
    fun deleteAutomation(id: String) {
        scheduler.post { store.deleteAutomation(id) }
    }

    /**
     * Demo cihazlarını açar/kapatır (telefondaki `Actions.enableDemo` / `disableDemo`), motor iş parçacığında: anlık
     * görüntülerin okuma-yazmasıyla yarışmaz. Kapatınca izlenecek hesap kalmadıysa izleme de durur.
     */
    fun setDemo(on: Boolean, names: Demo.Names) {
        scheduler.post {
            if (on) {
                store.enableDemo(names)
            } else {
                store.disableDemo()
                if (store.monitoringEnabled && !store.hasAnyAccount) stopMonitoring()
            }
        }
    }

    /** `ACTION_SYNC`. */
    fun syncNow() {
        if (running) scheduler.post { sync() }
    }

    /** `ACTION_RESTART` (giriş/ayar değişince): hata durumlarını sıfırla ve yeniden bağlan; izlemeyi de başlatır. */
    fun restart() {
        scheduler.post {
            authFailed = false
            wsUnsupported = false
            host.clearWarning()
            reconnectNow()
            resetTuya()
        }
        startMonitoring()
    }

    /**
     * Cihazı uygulamadan açar/kapatır (telefondaki `Actions.setSwitch`). Bu değişiklik alarm çaldırmaz, geçmişe
     * "uygulamadan" diye yazılır. Sonuç [onResult]'a motor iş parçacığında verilir (hata yoksa null).
     */
    fun control(deviceId: String, channel: Int, on: Boolean, onResult: (Throwable?) -> Unit = {}) {
        scheduler.post {
            val result = runCatching {
                val device = store.snapshots.firstOrNull { it.id == deviceId }
                    ?: throw IllegalArgumentException("unknown device $deviceId")
                LiveState.expected.expect(device.id, channel, on, clock.wall())
                // Demo cihazına komut gitmez; yeni durumu onControlled uygular.
                if (device.cloud != Cloud.DEMO) controlSwitch(device, channel, on)
                if (running) {
                    onControlled(device.id, channel, on)
                } else {
                    // İzleme kapalı: durumu doğrudan güncelle.
                    store.snapshots = store.snapshots.map {
                        if (it.id == device.id) Demo.recompute(it.copy(switches = it.switches + (channel to on))) else it
                    }
                }
            }
            onResult(result.exceptionOrNull())
        }
    }

    /**
     * Uygulamadan çıkış (telefondaki `onDestroy`): `monitoringEnabled`'a ve kapatılmamış alarma dokunmaz, bir
     * sonraki açılışta izleme ve alarm geri gelir. Bağlantılar kapanır, alarm susar, uyanık tutma bırakılır.
     */
    fun shutdown() {
        running = false
        // Kuyruğu boşaltma: stopMonitoring'in temizlik işleri henüz çalışmamış olabilir.
        // shutdown zamanı gelmiş işleri çalıştırır, ileri tarihli (tick vb.) olanları atar.
        scheduler.post {
            socket.close()
            tuyaSocket.close()
            host.stopAlarm()
            applyWakeMode()
        }
        scheduler.shutdown()
        if (LiveState.connection.value.state != ConnState.STOPPED) LiveState.setConnection(ConnState.STOPPED)
    }

    // ---------------------------------------------------------------- yaşam döngüsü

    private fun startMonitoring() {
        store.monitoringEnabled = true
        if (running) {
            scheduler.post { applyWakeMode() }
            return
        }
        running = true
        scheduler.post {
            try {
                applyWakeMode()
                // Bu çalışma başladı: bundan sonra beklenmedik bir kapanma "kesinti" sayılır.
                runtime.cleanStop = false
                heartbeat(clock.wall(), force = true)
                // Kapatılmamış alarm varsa (ör. bilgisayar yeniden başladı) geri getir.
                store.pendingAlarm.takeIf { it.isNotEmpty() }?.let { host.showAlarm(it, null, silent = runtime.silentAlarm) }
                setState(ConnState.CONNECTING)
                runCatching { metrics.prune(clock.wall()) }.onFailure { Log.w(TAG, "metrics prune failed; continuing", it) }
                lastTickWall = 0
                sync()
                reconnectNow()
                tuyaTick(clock.elapsed())
            } finally {
                // Telefonda buradaki bir hata süreci çökertir ve servis yeniden başlar; burada döngü yine kurulsun.
                startLoop()
            }
        }
    }

    private fun stopMonitoring() {
        // Kullanıcının kendi kapatması: sonraki açılışta "kesinti" sayılmasın.
        runtime.cleanStop = true
        store.monitoringEnabled = false
        running = false
        LiveState.setConnection(ConnState.STOPPED)
        scheduler.cancelAll()
        scheduler.post {
            loopGeneration++
            socket.close()
            tuyaSocket.close()
            store.pendingAlarm = emptyList()
            runtime.alarmStartedAt = 0
            runtime.silentAlarm = false
            endEscalation()
            runtime.clearEngine()
            grace = OfflineGrace()
            automations = AutomationEngine()
            reminders = AlarmReminders()
            host.clearBlind()
            host.stopAlarm()
            // Kaydırılamayan oturum uyarısı izleme kapatılınca asılı kalmasın.
            host.clearWarning()
            applyWakeMode()
            // Telefonda durum bildirimi ön plan servisiyle birlikte kalkar; burada tepsi "durdu"yu gösterir.
            host.statusChanged(ConnState.STOPPED, strings()["state_stopped"])
        }
    }

    /** "Her zaman uyanık" modu: açıksa bilgisayar izleme süresince uyumaz, yerel döngü hiç durmaz. */
    private fun applyWakeMode() {
        host.keepAwake(running && store.alwaysAwake)
    }

    // ---------------------------------------------------------------- zamanlama

    /** Telefondaki `loop`: [LOOP_MS]'de bir [tick]. */
    private fun startLoop() {
        val generation = ++loopGeneration
        lateinit var loop: () -> Unit
        loop = {
            if (generation == loopGeneration) {
                // tick hata verse de döngü sürsün (telefonda süreç çöker ve servis yeniden başlar).
                try {
                    tick()
                } finally {
                    scheduler.postDelayed(LOOP_MS, loop)
                }
            }
        }
        scheduler.post(loop)
    }

    private fun tick() {
        // Alarm sürüyor ama pencere kapanmış/arkada kalmışsa yeniden öne getir.
        if (store.pendingAlarm.isNotEmpty()) host.ensureAlarmVisible()
        if (!running) return
        val now = clock.elapsed()
        val wall = clock.wall()
        val slept = sleptFor(wall)

        grace.due(wall).forEach { dispatch(it, store.rulesFor(it.deviceId).actionFor(it.eventType)) }
        runAutomations(automations.onTick(store.snapshots.associateBy { it.id }, store.automations, wall))
        // Uykuda süresi dolan bekletmeler ve süreli otomasyonlar yukarıda, uyumadan önceki son bilinen durumla
        // değerlendirildi (telefondaki sıra); taze durum ancak şimdi okunur. Önce okunsaydı uyku boyunca çevrimdışı
        // kalıp uyanınca dönmüş cihaz "kısa kopma" sayılır, alarm hiç çalmazdı.
        if (slept > 0) resyncAfterSleep(slept)
        tuyaTick(now)
        demoTick(wall)
        checkReminders(wall)
        checkEscalation(wall)
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

        grace.nextDueAt()?.let { due -> scheduler.postDelayed((due - wall).coerceAtLeast(0) + 50) { tick() } }
    }

    /**
     * Masaüstü: iki tick arasında bilgisayarın uyuduğu süre (ms); uyumadıysa 0. Uyku = duvar saati boşluğu − uyanık
     * geçen süre ([Clock.awake]); [SLEEP_GAP_MS]'den uzunsa sayılır. Böylece uzun süren bir bulut çağrısı (motor
     * tıkalı, bilgisayar uyanık) uyku sayılmaz; uyku bir çağrının ortasında olsa da ölçülür. Küçük ya da geriye
     * atlamalar (NTP düzeltmesi, saati geri alma) yok sayılır.
     */
    private fun sleptFor(wall: Long): Long {
        val last = lastTickWall
        val lastAwake = lastTickAwake
        val awake = clock.awake()
        lastTickWall = wall
        lastTickAwake = awake
        if (last == 0L) return 0
        val slept = (wall - last) - (awake - lastAwake)
        return if (slept > SLEEP_GAP_MS) slept else 0
    }

    /**
     * Masaüstü: uykudan sonra bağlantılar kopmuş, olaylar kaçmış olabilir. Bağlantılar baştan kurulur, iki bulut
     * hemen senkronlanır; [SLEEP_LOG_MS] ve üstü uykular geçmişe yazılır.
     */
    private fun resyncAfterSleep(gap: Long) {
        if (gap >= SLEEP_LOG_MS) {
            val s = strings()
            record("", s["app_name"], EventKind.SYSTEM, s.format("desktop_slept", gap / 60_000), "")
        }
        Log.i(TAG, "wall clock jumped ${gap / 1000}s between ticks (sleep?): reconnecting and syncing")
        reconnectNow()
        resetTuya()
        sync()
    }

    // ---------------------------------------------------------------- WebSocket

    private val socketListener = object : EwelinkSocket.Listener {
        override fun onReady() = scheduler.post {
            reconnectAttempt = 0
            nextReconnectAt = 0
            wsIssue = ""
            // Bağlantı yokken kaçan değişiklikleri yakala.
            sync()
        }

        override fun onEvent(event: WsEvent) = scheduler.post {
            if (event is WsEvent.Update) applyUpdate(event)
        }

        override fun onClosed(reason: String, fatal: Boolean) = scheduler.post {
            if (!running) return@post
            Log.w(TAG, "WebSocket closed: $reason (fatal=$fatal)")
            if (fatal) {
                wsUnsupported = true
                wsIssue = "${strings()["detail_ws_unsupported"]} ($reason)"
                setState(ConnState.POLLING, wsIssue)
            } else {
                scheduleReconnect()
                wsIssue = "${strings()["detail_reconnecting"]} ($reason)"
                if (!authFailed) setState(ConnState.POLLING, wsIssue)
            }
        }

        override fun onAuthRejected() = scheduler.post {
            if (!running) return@post
            // 5 dk'lık senkronu bekleme: HTTP isteği token'ı yeniler ya da oturumun düştüğünü hemen ortaya çıkarır.
            sync()
            if (!authFailed) scheduleReconnect()
        }
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
                wsIssue = "${strings()["detail_ws_unsupported"]} (no apikey)"
                Log.w(TAG, "WebSocket unavailable: no apikey")
                setState(ConnState.POLLING, wsIssue)
                return
            }
            socket.connect()
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket connect failed", e)
            wsIssue = "${strings()["detail_reconnecting"]} (${e.javaClass.simpleName}: ${e.message})"
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        val delays = longArrayOf(5_000, 15_000, 30_000, 60_000, 120_000)
        nextReconnectAt = clock.elapsed() + delays[reconnectAttempt.coerceAtMost(delays.size - 1)]
        reconnectAttempt++
        scheduler.postDelayed(nextReconnectAt - clock.elapsed() + 50) { tick() }
    }

    // ---------------------------------------------------------------- durum işleme

    /** HTTP ile tam liste çekip farkları işler. */
    private fun sync() {
        lastSyncAt = clock.elapsed()
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
            if (authFailed) {
                authFailed = false
                host.clearWarning()
            }
            if (socket.ready) setState(ConnState.LIVE, synced = true)
            else setState(ConnState.POLLING, wsIssue, synced = true)
        } catch (e: AuthException) {
            onAuthFailed()
        } catch (e: IOException) {
            // Bilgisayarın interneti yok: cihaz "offline" sayılmaz, sadece durum güncellenir.
            setState(ConnState.NO_INTERNET)
        } catch (e: ApiException) {
            setState(ConnState.ERROR, strings().format("detail_api_error", e.code))
        } catch (e: Exception) {
            setState(ConnState.ERROR, e.message.orEmpty())
        }
    }

    /**
     * Uygulamadan gönderilen aç/kapa komutu başarılı olunca çağrılır. Bulut, cihazın yeni durumunu
     * ancak cihaz geri bildirdikten sonra (≈1-2 sn) döndürdüğünden hemen senkron yapmak eski durumu
     * geri yazar. Bu yüzden yeni durum hemen uygulanır, bulutla karşılaştırma gecikmeli yapılır.
     */
    private fun onControlled(deviceId: String, channel: Int, on: Boolean) {
        if (Demo.isDemo(deviceId)) {
            scheduler.post { demoUpdate(deviceId) { it.copy(switches = it.switches + (channel to on)) } }
            return
        }
        scheduler.post { applyUpdate(WsEvent.Update(deviceId, online = null, switches = mapOf(channel to on))) }
        val isTuya = store.snapshots.any { it.id == deviceId && it.cloud == Cloud.TUYA }
        scheduler.postDelayed(CONFIRM_DELAY_MS) {
            if (!running) return@postDelayed
            // Tuya'da sonuç mesaj servisinden gelir; kota harcamamak için yalnızca o yoksa sorgulanır.
            if (!isTuya) sync() else if (!tuyaSocket.ready) syncTuya()
        }
    }

    /** Cihazın bağlı olduğu buluta aç/kapa komutu gönderir. */
    private fun controlSwitch(device: DeviceSnapshot, channel: Int, on: Boolean) {
        when (device.cloud) {
            Cloud.TUYA -> tuya.setSwitch(device, channel, on)
            Cloud.EWELINK -> client.setSwitch(device, channel, on)
            Cloud.DEMO -> scheduler.post { demoUpdate(device.id) { it.copy(switches = it.switches + (channel to on)) } }
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
            channelPowerSeenAt[update.deviceId] = clock.wall()
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
        override fun onReady() = scheduler.post {
            if (!running) return@post
            tuyaReconnectAttempt = 0
            tuyaNextConnectAt = 0
            tuyaIssue = ""
            // Bağlantı yokken olanları yakala; biriken eski mesajlar bu okumadan eski sayılıp atlanır.
            syncTuya()
        }

        override fun onEvent(event: TuyaEvent) = scheduler.post { if (running) applyTuya(event) }

        override fun onClosed(reason: String, rejected: Boolean) = scheduler.post {
            if (!running || !store.hasTuya) return@post
            Log.w(TAG, "Tuya message service closed: $reason (rejected=$rejected)")
            tuyaIssue = strings()[if (rejected) "tuya_ws_rejected" else "detail_reconnecting"] + " ($reason)"
            scheduleTuyaReconnect(rejected)
            if (!tuyaBlocked) setTuyaState(ConnState.POLLING, tuyaIssue)
        }
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
        // Masaüstü: Tuya anlık bağlantısı ayarla açılır (kapalıysa telefonun bağlantısını çalmasın; yalnızca sorgulama).
        if (store.tuyaLive && !tuyaBlocked && !tuyaSocket.ready && now >= tuyaNextConnectAt) {
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
        tuyaNextConnectAt = clock.elapsed() + delay
        scheduler.postDelayed(delay + 50) { tick() }
    }

    /** Bilgiler değişti ya da kullanıcı yeniden denedi: baştan bağlan. */
    private fun resetTuya() {
        tuyaSocket.close()
        tuyaBlocked = false
        tuyaWarned = false
        tuyaReconnectAttempt = 0
        tuyaNextConnectAt = 0
        lastTuyaSyncAt = 0
        tuyaTick(clock.elapsed())
    }

    private fun syncTuya() {
        lastTuyaSyncAt = clock.elapsed()
        if (!store.hasTuya) return
        try {
            replaceCloud(Cloud.TUYA, tuya.getDevices())
            tuyaBlocked = false
            tuyaWarned = false
            if (tuyaSocket.ready) setTuyaState(ConnState.LIVE, synced = true)
            else setTuyaState(ConnState.POLLING, tuyaIssue, synced = true)
        } catch (e: TuyaExpiredException) {
            onTuyaBlocked(strings()["tuya_expired"])
        } catch (e: TuyaAuthException) {
            onTuyaBlocked(strings()["tuya_auth_error"])
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
            host.notify(text, null)
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
        val wall = clock.wall()
        // Bekleme süresi dolmuş kopmalar önce verilir: cihaz bu arada geri geldiyse (uygulama kapalıydı, tick
        // gecikti) sıra "koptu → geldi" olur, uzun kopma "kısa kopma" sayılıp yutulmaz.
        grace.due(wall).forEach { dispatch(it, store.rulesFor(it.deviceId).actionFor(it.eventType)) }
        val previous = store.snapshots.associateBy { it.id }
        store.snapshots = current
        runCatching { metrics.record(previous, current, wall) }.onFailure { Log.w(TAG, "metrics write failed; continuing", it) }
        val changes = ChangeDetector.detect(previous, current.associateBy { it.id }, store.monitoredIds())
        changes.forEach(::handle)
        runAutomations(automations.onStates(previous, current.associateBy { it.id }, store.automations, wall))
        persistRuntime()
        heartbeat(wall)
    }

    /** Önceki çalışmadan kalan bekleyen işleri yükler; motor kurulurken iş parçacığının ilk işi olarak çalışır. */
    private fun restoreRuntime() {
        grace = OfflineGrace.fromJson(runtime.grace)
        automations.restore(runtime.automationState, store.automations)
        reminders = AlarmReminders.fromJson(runtime.reminders)
        if (store.monitoringEnabled) reportGap(clock.wall())
    }

    /**
     * Önceki çalışma beklenmedik şekilde bittiyse (bilgisayar kapandı, uygulama kapatıldı ya da çöktü) aradaki
     * süreyi geçmişe yazar ve bildirir: o sürede olan değişiklikler kaçırılmış olabilir.
     */
    private fun reportGap(wall: Long) {
        MonitorGap.detect(runtime.lastAliveAt, runtime.cleanStop, wall)?.let { (from, to) ->
            val args = EventArgs.MonitorGap(from, to)
            val s = strings()
            val text = args.render(s["app_name"], eventStrings)
            Log.w(TAG, "monitoring gap ${(to - from) / 1000}s")
            host.notify(text, null, title = s["notif_monitor_title"])
            record("", s["app_name"], EventKind.MONITOR_GAP, text, AlertAction.NOTIFY.name, args)
        }
    }

    private var secretsWarned = false

    /** Kayıtlı anahtarlar bu bilgisayarda çözülemedi (ör. ayarlar başka bilgisayardan kopyalandı): bir kez söylenir. */
    private fun checkSecrets() {
        if (secretsWarned || !store.secretsUnreadable) return
        secretsWarned = true
        Log.w(TAG, "stored secrets can't be decrypted")
        val s = strings()
        host.notify(s["desktop_secrets_unreadable"], null, title = s["notif_monitor_title"])
    }

    /** İzleme uzun süre kör kalınca (internet/bulut/oturum) bir kez uyarır, düzelince bildirir. */
    private fun checkBlind(wall: Long) {
        val connection = LiveState.connection.value
        val blind = connection.state in BLIND_STATES
        val s = strings()
        when (val signal = blindWatch.update(blind, wall, store.blindAlertMin * 60_000L)) {
            is BlindWatch.Signal.Blind -> {
                val args = EventArgs.MonitorBlind(signal.since)
                val text = args.render(s["app_name"], eventStrings)
                // Oturum düşmesinin kendi kalıcı uyarısı zaten var; ikinci uyarı gürültü olur.
                if (connection.state != ConnState.AUTH_ERROR) {
                    host.monitorBlind(listOf(text, connection.detail, s["desktop_blind_hint"]).filter { it.isNotEmpty() }.joinToString("\n"))
                }
                record("", s["app_name"], EventKind.MONITOR_BLIND, text, AlertAction.NOTIFY.name, args)
            }
            is BlindWatch.Signal.Recovered -> {
                val args = EventArgs.MonitorRestored(signal.from, signal.to)
                val text = args.render(s["app_name"], eventStrings)
                host.clearBlind()
                host.notify(text, null, title = s["notif_monitor_title"])
                record("", s["app_name"], EventKind.MONITOR_BLIND, text, AlertAction.NOTIFY.name, args)
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

    /** Uygulama beklenmedik şekilde kapanırsa kaldığı yerden devam edebilsin. */
    private fun persistRuntime() {
        runtime.grace = grace.toJson()
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
                val text = EventArgs.PowerStale.render(d.name, eventStrings)
                Log.w(TAG, "no channel power from ${d.id} for ${(wall - seen) / 1000}s")
                host.notify(text, null)
                record(d.id, d.name, EventKind.AUTOMATION, text, AlertAction.NOTIFY.name, EventArgs.PowerStale)
            }
        }
    }

    private fun runAutomations(fired: List<Automation>) {
        fired.forEach(::execute)
        // Süre dolumu ya da gecikmeli eylem varsa tam vaktinde uyan.
        val due = automations.nextDueAt(store.snapshots.associateBy { it.id }, store.automations) ?: return
        scheduler.postDelayed((due - clock.wall()).coerceAtLeast(0) + 50) { tick() }
    }

    private fun execute(a: Automation) {
        when (a.action.kind) {
            ActionKind.TURN_ON, ActionKind.TURN_OFF -> {
                val on = a.action.kind == ActionKind.TURN_ON
                val target = store.snapshots.firstOrNull { it.id == a.action.deviceId }
                if (target == null) {
                    val args = EventArgs.AutoMissing(a.name)
                    record(a.action.deviceId, a.name, EventKind.AUTOMATION, args.render(a.name, eventStrings), "", args)
                    return
                }
                val args = EventArgs.AutoSwitch(a.name, a.action.channel, target.isMultiChannel, on)
                val text = args.render(target.name, eventStrings)
                try {
                    // Otomasyonun yaptığı değişiklik alarm çaldırmasın.
                    LiveState.expected.expect(target.id, a.action.channel, on, clock.wall())
                    controlSwitch(target, a.action.channel, on)
                    record(target.id, target.name, EventKind.AUTOMATION, text, "", args)
                } catch (e: Exception) {
                    val failedArgs = args.copy(error = e.message.orEmpty())
                    val failed = failedArgs.render(target.name, eventStrings)
                    host.notify(failed, null)
                    record(target.id, target.name, EventKind.AUTOMATION, failed, AlertAction.NOTIFY.name, failedArgs)
                }
            }
            ActionKind.ALARM, ActionKind.NOTIFY -> {
                val source = store.snapshots.firstOrNull { it.id == a.trigger.deviceId }
                val args = EventArgs.AutoAlert(a.name, source?.name)
                val text = args.render(source?.name ?: a.name, eventStrings)
                val action = if (a.action.kind == ActionKind.ALARM) AlertAction.ALARM else AlertAction.NOTIFY
                // Ücretsiz sürümde izlenmeyen cihazın otomasyonu uyarı vermez (sınır otomasyonla aşılmasın); yalnızca kaydedilir.
                val allowed = store.isPro || a.trigger.deviceId in store.monitoredIds()
                val rules = store.rulesFor(a.trigger.deviceId)
                val effective = if (!allowed) AlertAction.IGNORE
                    else RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()), critical = rules.critical)
                val sound = rules.soundUri
                // Masaüstü: önce uyarı, sonra geçmiş (bkz. [record]).
                when (effective) {
                    AlertAction.ALARM -> addAlarm(text, sound)
                    AlertAction.NOTIFY -> host.notify(text, sound)
                    AlertAction.IGNORE -> Unit
                }
                record(a.trigger.deviceId, source?.name ?: a.name, EventKind.AUTOMATION, text, effective.name, args)
            }
        }
    }

    private fun handle(change: Change) {
        val wall = clock.wall()
        if (LiveState.expected.consume(change, wall)) {
            record(change.deviceId, change.name, EventKind.USER, change.describe(labels), "", change.toArgs())
            return
        }
        val action = store.rulesFor(change.deviceId).actionFor(change.eventType)

        if (change is Change.Connection) {
            if (!change.to && store.offlineGraceSec > 0 && action != AlertAction.IGNORE) {
                grace.hold(change, wall, store.offlineGraceSec * 1000L)
                scheduler.postDelayed(store.offlineGraceSec * 1000L + 50) { tick() }
                return
            }
            if (change.to && grace.cameBack(change.deviceId, wall)) {
                record(change.deviceId, change.name, EventKind.SHORT_DROP, eventStrings.shortDrop(change.name), AlertAction.IGNORE.name, EventArgs.ShortDrop)
                return
            }
        }
        dispatch(change, action)
    }

    private fun dispatch(change: Change, action: AlertAction) {
        val rules = store.rulesFor(change.deviceId)
        val effective = RuleEngine.effective(action, quiet = store.quietHours.contains(minuteOfDay()), critical = rules.critical)
        val text = change.describe(labels)
        val sound = rules.soundUri
        // Masaüstü: önce uyarı, sonra geçmiş (bkz. [record]).
        when (effective) {
            AlertAction.ALARM -> {
                addAlarm(text, sound)
                // Hatırlatma kapalıyken kayıt tutulmaz: sonradan açılınca eski olaylar birden çalmasın.
                if (store.reminderMin > 0) reminders.onAlarm(change, clock.wall())
            }
            AlertAction.NOTIFY -> host.notify(text, sound)
            AlertAction.IGNORE -> Unit
        }
        record(change.deviceId, change.name, change.eventType.name, text, effective.name, change.toArgs())
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
        val quiet = store.quietHours.contains(minuteOfDay())
        for (r in due) {
            val args = EventArgs.Reminder.of(r)
            val text = args.render(r.change.name, eventStrings)
            val rules = store.rulesFor(r.change.deviceId)
            val effective = RuleEngine.effective(AlertAction.ALARM, quiet, rules.critical)
            if (effective == AlertAction.ALARM) addAlarm(text, rules.soundUri) else host.notify(text, rules.soundUri)
            record(r.change.deviceId, r.change.name, EventKind.REMINDER, text, effective.name, args)
        }
        // Hiç alarm çalmadıysa (hepsi bildirime düştü) kapatılacak bir şey yok: hatırlatmalar yeniden kurulur.
        if (store.pendingAlarm.isEmpty()) reminders.onDismiss(wall)
    }

    /**
     * Masaüstü: olayı geçmişe yazar; yazılamazsa (disk dolu, dosya başka programda kilitli, bozuk veritabanı)
     * yalnızca günlüğe not düşülür. Telefonda `SQLiteDatabase.insert` hatayı yutar (-1 döner); burada JDBC fırlatır.
     * Geçmiş hatası alarmı/bildirimi, aynı partideki diğer değişiklikleri, otomasyonları ya da uyku sonrası
     * senkronu asla engellememeli: anlık görüntüler [process]'te önceden kaydedildiği için kaçan değişiklik bir daha
     * fark edilmez. Bu yüzden motor geçmişe yalnızca bu yöntemle yazar ve uyarıyı her zaman önce verir.
     */
    private fun record(deviceId: String, deviceName: String, kind: String, text: String, action: String, args: EventArgs? = null) {
        runCatching { log.add(deviceId, deviceName, kind, text, action, args) }
            .onFailure { Log.w(TAG, "event history write failed ($kind, device '$deviceId'); continuing", it) }
    }

    /**
     * [sound]: olayın cihazına özel ses (yoksa genel ses). [silent]: sessiz test alarmı. Alarm ancak yalnızca sessiz
     * testlerden oluşuyorsa sessiz kalır: gerçek bir olay gelince ses başlar, çalan alarma eklenen sessiz test onu susturmaz.
     */
    private fun addAlarm(text: String, sound: String? = null, silent: Boolean = false, test: Boolean = false) {
        val now = clock.wall()
        if (store.pendingAlarm.isEmpty()) {
            runtime.alarmStartedAt = now
            runtime.silentAlarm = silent
        } else if (!silent) runtime.silentAlarm = false
        // İkinci kişiye haber verme ilk gerçek olaydan sayılır; test alarmı kimseye mesaj göndermez.
        if (!test && runtime.realAlarmAt == 0L) {
            runtime.realAlarmAt = now
            if (store.escalationReady) scheduler.postDelayed(store.escalateMin * 60_000L + 50) { tick() }
        }
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
        store.pendingAlarm = store.pendingAlarm + "$time  $text"
        host.showAlarm(store.pendingAlarm, sound, silent = runtime.silentAlarm)
    }

    /** Gönderilemeyen haber vermenin geçmişe yazıldığı alarm: her denemede tekrar yazılmasın. */
    private var escalationFailureLoggedFor = 0L
    /** Kalıcı hata (anahtar geçersiz, bot engellenmiş, sohbet yok) alınan alarm: yeniden denenmez. */
    private var escalationGaveUpFor = 0L
    private var escalationRetryAt = 0L
    /** Gönderim sürüyor: bir sonraki tick aynı mesajı ikinci kez göndermesin. */
    private var escalationInFlight = false

    /**
     * Alarm [Store.escalateMin] dakika kapatılmazsa seçilen kişilere Telegram mesajı gider (alarm başına bir kez).
     * Gönderilemezse (internet yok) dakikada bir yeniden denenir; ilk başarısızlık geçmişe yazılır.
     * Ağ işi [telegramRunner]'da: yavaş bağlantı izlemeyi (cihaz olaylarını) bekletmesin.
     */
    private fun checkEscalation(wall: Long) {
        if (escalationInFlight || store.pendingAlarm.isEmpty() || !store.escalationReady) return
        val at = runtime.realAlarmAt
        if (!Escalation.due(at, runtime.escalatedFor, wall, store.escalateMin * 60_000L)) return
        if (at == escalationGaveUpFor || wall < escalationRetryAt) return
        val minutes = ((wall - at) / 60_000L).toInt().coerceAtLeast(store.escalateMin)
        val s = strings()
        val text = Escalation.message(
            header = s.format("tg_alarm_header", minutes),
            lines = store.pendingAlarm,
            footer = s.format("tg_footer", deviceName()),
        )
        val token = store.telegramToken
        val recipients = store.telegramRecipients
        escalationInFlight = true
        telegramRunner {
            var sent = 0
            var error: Exception? = null
            var permanent = true
            for (r in recipients) {
                try {
                    sources.telegram.send(token, r.id, text)
                    sent++
                } catch (e: Exception) {
                    Log.w(TAG, "telegram send failed", e)
                    error = e
                    if (!(e is TelegramException && e.code in PERMANENT_TELEGRAM_ERRORS)) permanent = false
                }
            }
            scheduler.post { onEscalationSent(at, minutes, sent, error, permanent, wall) }
        }
    }

    private fun onEscalationSent(at: Long, minutes: Int, sent: Int, error: Exception?, permanent: Boolean, wall: Long) {
        escalationInFlight = false
        val name = strings()["app_name"]
        if (sent > 0) {
            val args = EventArgs.Escalation(sent, minutes)
            record("", name, EventKind.ESCALATION, args.render(name, eventStrings), "", args)
            // Gönderim sürerken alarm kapatıldıysa "kapatıldı" mesajı hemen ardından gider.
            if (runtime.realAlarmAt == at) runtime.escalatedFor = at else sendDismissed(at)
            return
        }
        if (permanent) escalationGaveUpFor = at else escalationRetryAt = wall + ESCALATION_RETRY_MS
        if (escalationFailureLoggedFor != at) {
            escalationFailureLoggedFor = at
            val args = EventArgs.Escalation(0, minutes, error?.let(::telegramError))
            record("", name, EventKind.ESCALATION, args.render(name, eventStrings), "", args)
        }
    }

    /** Alarm bitti: haber verildiyse kişilere kapatıldığı da bildirilir. */
    private fun endEscalation() {
        val at = runtime.realAlarmAt
        if (at != 0L && runtime.escalatedFor == at) sendDismissed(at)
        runtime.realAlarmAt = 0
        runtime.escalatedFor = 0
    }

    /** "Alarm kapatıldı" mesajı; yanıt beklenmez, gönderilemezse önemsizdir. */
    private fun sendDismissed(at: Long) {
        if (!store.escalationReady) return
        val now = clock.wall()
        val s = strings()
        val text = s.format("tg_dismissed", s.time(now), DurationText.format(now - at, s.durations()))
        val token = store.telegramToken
        val recipients = store.telegramRecipients
        telegramRunner { recipients.forEach { r -> runCatching { sources.telegram.send(token, r.id, text) } } }
    }

    private fun telegramError(e: Exception): String {
        val s = strings()
        return when {
            e is TelegramException && e.code == 401 -> s["tg_err_token"]
            e is TelegramException && e.code == 403 -> s["tg_err_blocked"]
            e is TelegramException && e.code == 400 -> s["tg_err_chat"]
            e is TelegramException -> e.message.orEmpty()
            else -> s["state_no_internet"]
        }
    }

    /** Masaüstü: mesajın altındaki gönderen cihaz = bilgisayarın adı (telefonda cihaz adı / model). */
    private fun deviceName(): String =
        System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
            ?: runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "PC"

    /** Alarmın kapatılması geçmişe yazılır: kaç olay vardı ve ne kadar sürede kapatıldı (tepki süresi). */
    private fun logDismissal(count: Int) {
        val now = clock.wall()
        val started = runtime.alarmStartedAt.takeIf { it in 1..now } ?: now
        val args = EventArgs.AlarmDismissed(count, now - started)
        val name = strings()["app_name"]
        record("", name, EventKind.DISMISSED, args.render(name, eventStrings), "", args)
        runtime.alarmStartedAt = 0
    }

    private fun onAuthFailed() {
        socket.close()
        setState(ConnState.AUTH_ERROR)
        if (!authFailed) {
            authFailed = true
            if (store.sessionLost) {
                host.sessionLost(strings()["session_lost_body"], strings()["session_lost_tip"])
            } else {
                host.sessionLost(strings()["warn_session_expired"], "")
            }
        }
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
        // Durdurulduktan sonra biten bir istek durumu geri getirmesin.
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
        val s = strings()
        val text = when (state) {
            ConnState.LIVE -> s.plural("status_live", count, count)
            ConnState.POLLING -> s.plural("status_polling", count, count, store.pollIntervalSec)
            ConnState.CONNECTING -> s["state_connecting"]
            ConnState.NO_INTERNET -> s["state_no_internet"]
            ConnState.AUTH_ERROR -> s["state_auth_error"]
            ConnState.ERROR -> s["state_error"] + " " + detail
            ConnState.STOPPED -> s["state_stopped"]
        }
        host.statusChanged(state, text)
    }

    /** Sistem saat dilimine göre günün dakikası; duvar saati [clock]'tan (testler sabitleyebilsin). */
    private fun minuteOfDay(): Int =
        Instant.ofEpochMilli(clock.wall()).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

    companion object {
        /** Demo simülasyonu: [demo]'nun `op` değerleri (telefondakilerle aynı). */
        const val DEMO_TOGGLE = "toggle"
        const val DEMO_ONLINE = "online"
        const val DEMO_POWER = "power"
        private const val DEMO_JITTER_MS = 60_000L

        private const val TAG = "SwitchGuard"
        private const val LIVE_ENERGY_REFRESH_MS = 90_000L
        private const val LIVE_ENERGY_SECONDS = 120
        private const val POWER_STALE_MS = 5 * 60_000L
        private const val LOOP_MS = 15_000L
        private const val HEARTBEAT_MS = 60_000L
        /** Cihazların durumunun alınamadığı durumlar. */
        private val BLIND_STATES = setOf(ConnState.CONNECTING, ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR)
        private const val RESYNC_MS = 5 * 60_000L
        private const val ESCALATION_RETRY_MS = 60_000L
        /** Yeniden denemenin işe yaramayacağı Telegram hataları: 400 sohbet yok, 401 anahtar geçersiz, 403 bot engellendi. */
        private val PERMANENT_TELEGRAM_ERRORS = setOf(400, 401, 403)
        const val MAX_TEST_DELAY_MS = 60_000L

        /**
         * Telegram gönderimleri. Süreç boyunca tek ve hiç kapatılmaz: izleme durdurulurken kuyruğa giren
         * "alarm kapatıldı" mesajı da gidebilsin.
         */
        private val TELEGRAM = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "telegram").apply { isDaemon = true } }
        private const val CONFIRM_DELAY_MS = 5_000L
        /**
         * Tuya ücretsiz planı ayda ≈26 bin API çağrısı verir; her senkron 2 çağrı.
         * Mesaj servisi açıkken 15 dk'da bir (≈6 bin/ay), değilse 5 dk'da bir (≈17 bin/ay).
         */
        private const val TUYA_RESYNC_MS = 15 * 60_000L
        private const val TUYA_POLL_MS = 5 * 60_000L
        private const val TUYA_BLOCKED_MS = 30 * 60_000L
        private const val TUYA_CONNECT_TIMEOUT_MS = 60_000L
        /** Masaüstü: iki tick arasında duvar saati bundan fazla ilerlediyse bilgisayar uyumuştur. */
        private const val SLEEP_GAP_MS = 60_000L
        /** Masaüstü: bu ve daha uzun uykular geçmişe yazılır. */
        private const val SLEEP_LOG_MS = 2 * 60_000L
        private val SEVERITY = listOf(
            ConnState.LIVE, ConnState.POLLING, ConnState.CONNECTING,
            ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR,
        )
    }
}
