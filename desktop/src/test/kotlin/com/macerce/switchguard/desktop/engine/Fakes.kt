package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.api.TuyaSocket
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.ConnState
import java.util.PriorityQueue

/** Duvar saati ve geçen süre ayrı tutulur: uykuda yalnızca duvar saati atlar ([wallMs]'i elle değiştir). */
class FakeClock(var wallMs: Long, var elapsedMs: Long = 10_000_000L) : Clock {
    override fun wall() = wallMs
    override fun elapsed() = elapsedMs

    /** Uyanık geçen süre: testlerde uyku yalnızca [wallMs]'i atlattığı için [elapsedMs] ile aynı. */
    override fun awake() = elapsedMs

    /** İki saati birlikte ilerletir (normal geçen zaman). */
    fun advance(ms: Long) {
        if (ms <= 0) return
        wallMs += ms
        elapsedMs += ms
    }
}

/**
 * Elle ilerletilen zamanlayıcı. İşler [FakeClock.elapsedMs]'e göre sıralanır; [advance] zamanı gelen işleri
 * sırayla ve her birini kendi zamanında çalıştırır (saat o ana ilerletilir). Aynı andaki işler eklenme sırasıyla.
 */
class ManualScheduler(private val clock: FakeClock) : Scheduler {
    private class Task(val at: Long, val seq: Long, val run: () -> Unit)

    private val queue = PriorityQueue(compareBy<Task>({ it.at }, { it.seq }))
    private var seq = 0L
    var isShutdown = false
        private set

    override fun post(task: () -> Unit) = postDelayed(0, task)

    override fun postDelayed(delayMs: Long, task: () -> Unit) {
        if (isShutdown) return
        queue += Task(clock.elapsedMs + delayMs.coerceAtLeast(0), seq++, task)
    }

    override fun cancelAll() = queue.clear()

    /** quitSafely gibi: zamanı gelmiş işler kalır ([advance] ile çalışır), ileri tarihliler atılır. */
    override fun shutdown() {
        isShutdown = true
        queue.removeAll { it.at > clock.elapsedMs }
    }

    fun advance(ms: Long) {
        val end = clock.elapsedMs + ms
        while (true) {
            val next = queue.peek() ?: break
            if (next.at > end) break
            queue.poll()
            clock.advance(next.at - clock.elapsedMs)
            next.run()
        }
        clock.advance(end - clock.elapsedMs)
    }
}

/** Sahte eWeLink bulutu: cihaz listesi değiştirilebilir; aç/kapa komutu listeyi de günceller (gerçek bulut gibi). */
class FakeEwelink(var deviceList: List<DeviceSnapshot> = emptyList()) : EwelinkApi {
    var getDevicesCalls = 0
        private set
    val switched = mutableListOf<Triple<String, Int, Boolean>>()
    val liveEnergyRequests = mutableListOf<String>()

    /** Her [getDevices] çağrısında çalışır (ör. tıkanan bir bulut çağrısını saati ilerleterek taklit etmek). */
    var onGetDevices: (() -> Unit)? = null

    override fun getDevices(): List<DeviceSnapshot> {
        getDevicesCalls++
        onGetDevices?.invoke()
        return deviceList
    }

    override fun loadProfile() = Unit

    override fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean) {
        switched += Triple(d.id, channel, on)
        deviceList = deviceList.map { if (it.id == d.id) it.copy(switches = it.switches + (channel to on)) else it }
    }

    override fun requestLiveEnergy(d: DeviceSnapshot, seconds: Int) {
        liveEnergyRequests += d.id
    }
}

/** Sahte eWeLink WebSocket'i. [connect] gerçeği gibi önce kapatır; hazır olmayı test [goLive] ile bildirir. */
class FakeEwelinkLive : EwelinkLive {
    lateinit var listener: EwelinkSocket.Listener
        private set
    override var ready = false
    var connects = 0
        private set
    var closes = 0
        private set
    var stale = false

    fun attach(listener: EwelinkSocket.Listener): FakeEwelinkLive = apply { this.listener = listener }

    /** El sıkışma tamamlandı: anlık güncellemeler gelmeye başlar. */
    fun goLive() {
        ready = true
        listener.onReady()
    }

    override fun connect() {
        connects++
        ready = false
    }

    override fun close() {
        closes++
        ready = false
    }

    override fun sendPingIfDue(now: Long) = Unit

    override fun isStale(now: Long) = stale
}

class FakeTuya(var deviceList: List<DeviceSnapshot> = emptyList()) : TuyaApi {
    override var syncServerTime = 0L
    var getDevicesCalls = 0
        private set
    val switched = mutableListOf<Triple<String, Int, Boolean>>()

    override fun getDevices(): List<DeviceSnapshot> {
        getDevicesCalls++
        return deviceList
    }

    override fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean) {
        switched += Triple(d.id, channel, on)
        deviceList = deviceList.map { if (it.id == d.id) it.copy(switches = it.switches + (channel to on)) else it }
    }

    override fun scalesFor(deviceId: String): Map<String, Int> = emptyMap()
}

class FakeTuyaLive : TuyaLive {
    lateinit var listener: TuyaSocket.Listener
        private set
    override var ready = false
    var connects = 0
        private set

    fun attach(listener: TuyaSocket.Listener): FakeTuyaLive = apply { this.listener = listener }

    override fun connect() {
        connects++
        ready = false
    }

    override fun close() {
        ready = false
    }
}

/** Motorun platforma yaptığı her çağrıyı kaydeder. */
class FakeHost : MonitorHost {
    data class Alarm(val lines: List<String>, val sound: String?, val silent: Boolean = false)

    val alarms = mutableListOf<Alarm>()
    var ensureVisibleCalls = 0
        private set
    var stopAlarmCalls = 0
        private set
    val notifications = mutableListOf<Pair<String, String?>>()
    val sessionLost = mutableListOf<Pair<String, String>>()
    var clearWarningCalls = 0
        private set
    val statuses = mutableListOf<Pair<ConnState, String>>()
    val keepAwake = mutableListOf<Boolean>()
    /** Bildirim başlıkları ([notifications] ile aynı sırada; null = uygulama adı). */
    val notificationTitles = mutableListOf<String?>()
    val blindWarnings = mutableListOf<String>()
    var clearBlindCalls = 0
        private set

    override fun showAlarm(lines: List<String>, sound: String?, silent: Boolean) {
        alarms += Alarm(lines, sound, silent)
    }

    override fun ensureAlarmVisible() {
        ensureVisibleCalls++
    }

    override fun stopAlarm() {
        stopAlarmCalls++
    }

    override fun notify(text: String, sound: String?, title: String?) {
        notifications += text to sound
        notificationTitles += title
    }

    override fun monitorBlind(text: String) {
        blindWarnings += text
    }

    override fun clearBlind() {
        clearBlindCalls++
    }

    override fun sessionLost(text: String, detail: String) {
        sessionLost += text to detail
    }

    override fun clearWarning() {
        clearWarningCalls++
    }

    override fun statusChanged(state: ConnState, text: String) {
        statuses += state to text
    }

    override fun keepAwake(on: Boolean) {
        keepAwake += on
    }
}
