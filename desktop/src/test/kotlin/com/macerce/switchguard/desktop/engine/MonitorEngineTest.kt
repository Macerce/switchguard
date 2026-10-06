package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.core.WsEvent
import com.macerce.switchguard.core.WsMessages
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MonitorEngineTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Her testin kendi cihazı: [LiveState.expected] süreç genelinde paylaşılır, kayıtlar testler arasında karışmasın. */
    private val id = "dev-" + UUID.randomUUID()

    private val strings = Strings.load("en")
    private val host = FakeHost()
    private val ewelink = FakeEwelink()
    private val ewelinkLive = FakeEwelinkLive()
    private val tuya = FakeTuya()
    private val tuyaLive = FakeTuyaLive()
    private val clock = FakeClock(wallAt(12, 0))
    private val scheduler = ManualScheduler(clock)

    private lateinit var store: Store
    private lateinit var log: EventLog
    private lateinit var metrics: MetricsLog
    private lateinit var engine: MonitorEngine

    @Before
    fun setUp() {
        val dir = tmp.root.toPath()
        store = Store(dir.resolve("settings.json"), PlainCodec)
        log = EventLog(dir.resolve("events.db"))
        metrics = MetricsLog(dir.resolve("metrics.db"))
        // eWeLink'e girilmiş, WebSocket için apikey hazır.
        store.accessToken = "token"
        store.userApiKey = "apikey"
        val sources = Sources(ewelink, { ewelinkLive.attach(it) }, tuya, { tuyaLive.attach(it) })
        engine = MonitorEngine(store, log, metrics, { strings }, host, sources, clock, scheduler)
    }

    /** Windows açık veritabanı dosyasını silemez; geçici klasör temizlenmeden önce kapat. */
    @After
    fun tearDown() {
        engine.shutdown()
        scheduler.advance(0)
        log.close()
        metrics.close()
    }

    @Test
    fun externalSwitchOffRaisesAlarm() {
        startWith(pump(on = true), rules = mapOf(EventType.TURNED_OFF to AlertAction.ALARM))

        cloudUpdate(switches = mapOf(0 to false))

        assertEquals(1, host.alarms.size)
        assertTrue(host.alarms[0].lines.last().contains("Pump"))
        assertEquals("ALARM", log.recent(id).first().action)
    }

    @Test
    fun appControlDoesNotAlarm() {
        startWith(pump(on = true), rules = mapOf(EventType.TURNED_OFF to AlertAction.ALARM))
        var result: Throwable? = IllegalStateException("not called")

        engine.control(id, 0, false) { result = it }
        scheduler.advance(10_000) // doğrulama senkronu (5 sn) dahil

        assertNull(result)
        assertEquals(listOf(Triple(id, 0, false)), ewelink.switched)
        assertTrue(host.alarms.isEmpty())
        assertEquals(listOf(EventKind.USER), log.recent(id).map { it.kind })
    }

    @Test
    fun quietHoursTurnAlarmIntoNotification() {
        clock.wallMs = wallAt(3, 0)
        store.quietHours = QuietHours(enabled = true, startMinute = 23 * 60, endMinute = 7 * 60)
        startWith(pump(on = true), rules = mapOf(EventType.TURNED_OFF to AlertAction.ALARM))

        cloudUpdate(switches = mapOf(0 to false))

        assertEquals(1, host.notifications.size)
        assertEquals(0, host.alarms.size)
        assertEquals("NOTIFY", log.recent(id).first().action)
    }

    @Test
    fun shortDropWithinGraceIsLoggedNotAlarmed() {
        store.offlineGraceSec = 60
        startWith(
            pump(),
            rules = mapOf(EventType.WENT_OFFLINE to AlertAction.ALARM, EventType.CAME_ONLINE to AlertAction.NOTIFY),
        )

        cloudUpdate(online = false)
        scheduler.advance(30_000)
        cloudUpdate(online = true)
        scheduler.advance(60_000) // bekleme süresinin sonu geçer

        assertTrue(host.alarms.isEmpty())
        assertTrue(host.notifications.isEmpty())
        assertEquals(listOf(EventKind.SHORT_DROP), log.recent(id).map { it.kind })
    }

    @Test
    fun offlineBeyondGraceAlarms() {
        store.offlineGraceSec = 60
        startWith(pump(), rules = mapOf(EventType.WENT_OFFLINE to AlertAction.ALARM))

        cloudUpdate(online = false)
        assertTrue(host.alarms.isEmpty()) // bekletiliyor
        scheduler.advance(61_000)

        assertEquals(1, host.alarms.size)
    }

    @Test
    fun stayedOnAutomationFiresAfterDuration() {
        store.automations = listOf(
            Automation(
                id = "auto-$id", name = "Pump left on", enabled = true,
                trigger = Trigger(TriggerKind.STAYED_ON, id, channel = 0, minutes = 10),
                action = AutoAction(ActionKind.ALARM),
            )
        )
        startWith(pump(on = true), rules = emptyMap())

        scheduler.advance(9 * 60_000L)
        assertTrue(host.alarms.isEmpty())
        scheduler.advance(60_000L + 100)

        assertEquals(1, host.alarms.size)
    }

    @Test
    fun sleepGapResyncsAndLogs() {
        startWith(pump(), rules = emptyMap())
        scheduler.advance(15_000) // olağan bir tick
        val connects = ewelinkLive.connects
        val syncs = ewelink.getDevicesCalls

        clock.wallMs += 10 * 60_000L // bilgisayar uyudu: yalnızca duvar saati ilerledi
        scheduler.advance(15_000) // uyanınca ilk tick

        assertEquals(connects + 1, ewelinkLive.connects)
        assertEquals(syncs + 1, ewelink.getDevicesCalls)
        val slept = log.recent().filter { it.kind == EventKind.SYSTEM }
        assertEquals(1, slept.size)
        assertTrue(slept[0].text, slept[0].text.contains("10"))
    }

    @Test
    fun smallOrBackwardClockJumpIsIgnored() {
        startWith(pump(), rules = emptyMap())
        val connects = ewelinkLive.connects
        assertEquals(1, connects) // izleme başladı, WebSocket bir kez kuruldu

        clock.wallMs += 30_000 // NTP düzeltmesi gibi küçük ileri atlama
        scheduler.advance(15_000)
        clock.wallMs -= 5 * 60_000L // saat geri alındı
        scheduler.advance(15_000)
        scheduler.advance(15_000)

        assertTrue(log.recent().none { it.kind == EventKind.SYSTEM })
        assertEquals(connects, ewelinkLive.connects)
    }

    @Test
    fun pendingAlarmRestoredOnStart() {
        store.pendingAlarm = listOf("12:00  x")

        engine.start()
        scheduler.advance(0)

        assertEquals(listOf("12:00  x"), host.alarms.first().lines)
    }

    @Test
    fun fatalSocketCloseFallsBackToPolling() {
        startWith(pump(), rules = emptyMap())
        ewelinkLive.goLive()
        scheduler.advance(0)
        assertEquals(ConnState.LIVE, host.statuses.last().first)
        val syncs = ewelink.getDevicesCalls

        ewelinkLive.ready = false
        ewelinkLive.listener.onClosed("x", fatal = true)
        scheduler.advance(0)

        assertEquals(ConnState.POLLING, host.statuses.last().first)
        assertEquals(ConnState.POLLING, LiveState.connection.value.state)
        scheduler.advance(store.pollIntervalSec * 1000L)
        assertTrue(ewelink.getDevicesCalls > syncs)
    }

    @Test
    fun tuyaLiveOffNeverConnectsSocket() {
        store.logout()
        store.tuyaAccessId = "tuya-id"
        store.tuyaSecret = "tuya-secret"
        assertFalse(store.tuyaLive)
        tuya.deviceList = listOf(
            DeviceSnapshot(id, "Plug", online = true, switches = mapOf(0 to true), cloud = Cloud.TUYA, switchCodes = mapOf(0 to "switch_1"))
        )

        engine.start()
        scheduler.advance(0)
        assertEquals(1, tuya.getDevicesCalls)
        scheduler.advance(5 * 60_000L - 15_000)
        assertEquals(1, tuya.getDevicesCalls)
        scheduler.advance(15_000)
        assertEquals(2, tuya.getDevicesCalls)
        scheduler.advance(5 * 60_000L)
        assertEquals(3, tuya.getDevicesCalls)

        assertEquals(0, tuyaLive.connects)
    }

    @Test
    fun stopClearsAlarmAndKeepAwake() {
        startWith(pump(), rules = emptyMap())
        assertEquals(listOf(true), host.keepAwake)
        engine.testAlarm()
        scheduler.advance(0)
        assertTrue(store.pendingAlarm.isNotEmpty())

        engine.stop()
        scheduler.advance(0)

        assertEquals(false, host.keepAwake.last())
        assertTrue(host.stopAlarmCalls > 0)
        assertTrue(store.pendingAlarm.isEmpty())
        assertEquals(ConnState.STOPPED, host.statuses.last().first)
        assertEquals(ConnState.STOPPED, LiveState.connection.value.state)
        assertFalse(store.monitoringEnabled)
        assertFalse(engine.running)
    }

    @Test
    fun shutdownKeepsMonitoringFlag() {
        startWith(pump(), rules = emptyMap())

        engine.shutdown()
        scheduler.advance(0)

        assertTrue(store.monitoringEnabled)
        assertFalse(engine.running)
        assertEquals(false, host.keepAwake.last())
    }

    // ---------------------------------------------------------------- yardımcılar

    private fun pump(on: Boolean = true, online: Boolean = true) =
        DeviceSnapshot(id, "Pump", online = online, switches = mapOf(0 to on))

    /** Kurallar açıkça verilir; izleme başlar ve ilk senkron (temel durum) yapılır. */
    private fun startWith(vararg devices: DeviceSnapshot, rules: Map<EventType, AlertAction>) {
        ewelink.deviceList = devices.toList()
        store.setRules(id, DeviceRules(actions = rules))
        engine.start()
        scheduler.advance(0)
    }

    /** Buluttaki değişiklik: sahte bulutun listesi de güncellenir (sonraki senkron geri almasın), WebSocket ile gelir. */
    private fun cloudUpdate(online: Boolean? = null, switches: Map<Int, Boolean> = emptyMap()) {
        val update = WsEvent.Update(id, online, switches)
        ewelink.deviceList = ewelink.deviceList.map { if (it.id == id) WsMessages.apply(it, update) else it }
        ewelinkLive.listener.onEvent(update)
        scheduler.advance(0)
    }

    private companion object {
        /** Bugünün değil, sabit bir günün yerel saati (sistem saat dilimi): sessiz saat testleri her makinede aynı. */
        fun wallAt(hour: Int, minute: Int): Long =
            LocalDate.of(2026, 10, 6).atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
