package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutageWatchTest {
    private fun off(id: String) = Change.Connection(id, id, from = true, to = false)

    @Test fun allDevicesDroppingTogetherIsOneOutage() {
        val w = OutageWatch()
        assertTrue(w.offer(off("a"), AlertAction.ALARM, 1_000, monitored = 2, enabled = true) is OutageWatch.Offer.Hold)
        assertTrue(w.offer(off("b"), AlertAction.NOTIFY, 30_000, monitored = 2, enabled = true) is OutageWatch.Offer.Hold)
        assertNull(w.due(60_000, setOf("a", "b"), setOf("a", "b")))
        val v = w.due(OutageWatch.WINDOW_MS + 1_000, setOf("a", "b"), setOf("a", "b")) as OutageWatch.Verdict.Outage
        assertEquals(2, v.held.size)
        assertEquals(AlertAction.ALARM, OutageWatch.strongest(v.held))
        assertTrue(w.active)
        // Elektrik geldi: ilk dönen kesintiyi bitirir, diğeri yalnızca kaydedilir.
        val r = w.onOnline("a", 600_000) as OutageWatch.Online.Restored
        assertEquals(1_000L, r.from)
        assertEquals(600_000L, r.to)
        assertEquals(OutageWatch.Online.Absorbed, w.onOnline("b", 610_000))
        assertEquals(OutageWatch.Online.Pass, w.onOnline("b", 620_000))
    }

    @Test fun notAllOfflineReleasesIndividually() {
        val w = OutageWatch()
        w.offer(off("a"), AlertAction.ALARM, 0, 3, true)
        w.offer(off("b"), AlertAction.ALARM, 1_000, 3, true)
        val v = w.due(OutageWatch.WINDOW_MS, setOf("a", "b"), setOf("a", "b", "c"))
        assertTrue(v is OutageWatch.Verdict.Release && v.held.size == 2)
        assertFalse(w.active)
    }

    @Test fun singleDropIsReleased() {
        val w = OutageWatch()
        w.offer(off("a"), AlertAction.ALARM, 0, 2, true)
        val v = w.due(OutageWatch.WINDOW_MS, setOf("a", "b"), setOf("a", "b"))
        assertTrue(v is OutageWatch.Verdict.Release)
    }

    @Test fun disabledOrSingleDevicePasses() {
        assertEquals(OutageWatch.Offer.Pass, OutageWatch().offer(off("a"), AlertAction.ALARM, 0, 2, enabled = false))
        assertEquals(OutageWatch.Offer.Pass, OutageWatch().offer(off("a"), AlertAction.ALARM, 0, 1, enabled = true))
    }

    @Test fun cameBackWhileHeldIsReleased() {
        val w = OutageWatch()
        w.offer(off("a"), AlertAction.ALARM, 0, 2, true)
        assertTrue(w.onOnline("a", 10_000) is OutageWatch.Online.Release)
        assertNull(w.nextDueAt())
    }

    @Test fun lateDropDuringOutageIsAbsorbedAndStateSurvivesRestart() {
        val w = OutageWatch()
        w.offer(off("a"), AlertAction.ALARM, 1_000, 2, true)
        w.offer(off("b"), AlertAction.ALARM, 1_000, 2, true)
        w.due(OutageWatch.WINDOW_MS + 1_000, setOf("a", "b"), setOf("a", "b"))
        val restored = OutageWatch.fromJson(w.toJson())
        assertTrue(restored.active)
        assertEquals(OutageWatch.Offer.Absorbed, restored.offer(off("c"), AlertAction.ALARM, 100_000, 3, true))
        assertTrue(restored.onOnline("c", 200_000) is OutageWatch.Online.Restored)
    }

    @Test fun heldDropsSurviveRestart() {
        val w = OutageWatch()
        w.offer(off("a"), AlertAction.NOTIFY, 5, 2, true)
        val r = OutageWatch.fromJson(w.toJson())
        assertEquals(OutageWatch.WINDOW_MS + 5, r.nextDueAt())
        val v = r.due(OutageWatch.WINDOW_MS + 5, setOf("a"), setOf("a", "b")) as OutageWatch.Verdict.Release
        assertEquals(AlertAction.NOTIFY, v.held.single().action)
    }
}

class TelegramBotTest {
    private val devices = listOf(
        DeviceSnapshot("1", "Salon Lambası", true, mapOf(0 to false)),
        DeviceSnapshot("2", "Pompalar", true, mapOf(0 to false, 1 to true, 2 to false)),
        DeviceSnapshot("3", "Salon Priz", true, mapOf(0 to true)),
        DeviceSnapshot("4", "Pompa 2", true, mapOf(0 to true)),
    )

    @Test fun parsesCommands() {
        assertEquals(TelegramBot.Command.Status, TelegramBot.parseCommand("/durum"))
        assertEquals(TelegramBot.Command.Status, TelegramBot.parseCommand("/status@MyBot"))
        assertEquals(TelegramBot.Command.Switch(true, "Salon 2"), TelegramBot.parseCommand("/aç Salon 2"))
        assertEquals(TelegramBot.Command.Switch(false, ""), TelegramBot.parseCommand("/off"))
        assertEquals(TelegramBot.Command.Ack, TelegramBot.parseCommand("/gördüm"))
        assertNull(TelegramBot.parseCommand("merhaba"))
        assertNull(TelegramBot.parseCommand("/bilinmeyen"))
    }

    @Test fun matchesDevices() {
        assertEquals("1", (TelegramBot.match("salon lambasi", devices) as TelegramBot.Match.Found).device.id)
        assertTrue(TelegramBot.match("salon", devices) is TelegramBot.Match.Ambiguous)
        val ch = TelegramBot.match("pompalar 2", devices) as TelegramBot.Match.Found
        assertEquals("2" to 1, ch.device.id to ch.channel)
        assertTrue(TelegramBot.match("pompalar", devices) is TelegramBot.Match.NeedChannel)
        // Adı sayıyla biten tek kanallı cihaz.
        assertEquals("4", (TelegramBot.match("Pompa 2", devices) as TelegramBot.Match.Found).device.id)
        assertEquals(TelegramBot.Match.NotFound, TelegramBot.match("kombi", devices))
    }

    @Test fun parsesUpdatesAndAck() {
        val json = """{"ok":true,"result":[
            {"update_id":10,"message":{"message_id":1,"date":100,"chat":{"id":42},"from":{"first_name":"Ali"},"text":"/durum"}},
            {"update_id":11,"callback_query":{"id":"cb","from":{"first_name":"Ayşe","last_name":"K"},"data":"ack:tag1:555",
              "message":{"message_id":7,"date":200,"chat":{"id":-5}}}}]}"""
        val u = TelegramBot.parseUpdates(json)
        assertEquals(2, u.size)
        assertEquals("/durum", u[0].text)
        assertEquals(100_000L, u[0].date)
        assertEquals("Ayşe K", u[1].from)
        assertEquals(-5L, u[1].chatId)
        assertEquals(7L, u[1].messageId)
        assertEquals("tag1" to 555L, TelegramBot.parseAck(u[1].callbackData))
        assertEquals("tag1" to 555L, TelegramBot.parseAck(TelegramBot.ackData("tag1", 555)))
        assertNull(TelegramBot.parseAck("x"))
    }

    @Test fun confirmsOnlyOldUpdates() {
        val now = 10 * TelegramBot.KEEP_MS
        val updates = listOf(
            TelegramBot.Update(5, 1, "", now - TelegramBot.KEEP_MS - 1),
            TelegramBot.Update(6, 1, "", now - 1000),
        )
        assertEquals(6L, TelegramBot.confirmOffset(updates, now, 0))
        assertEquals(9L, TelegramBot.confirmOffset(updates, now, 9))
        assertEquals(3L, TelegramBot.confirmOffset(updates.drop(1), now, 3))
    }
}

class EnergyWatchTest {
    @Test fun detectsHighAndLowDays() {
        val history = List(10) { 2.0 }
        assertTrue(EnergyWatch.anomaly(3.2, history)!!.high)
        assertFalse(EnergyWatch.anomaly(0.5, history)!!.high)
        assertNull(EnergyWatch.anomaly(2.5, history))
        assertEquals(60, EnergyWatch.percent(EnergyWatch.anomaly(3.2, history)!!))
    }

    @Test fun needsEnoughHistoryAndMeaningfulDifference() {
        assertNull(EnergyWatch.anomaly(9.0, List(EnergyWatch.MIN_DAYS - 1) { 2.0 }))
        // Küçük tüketimde oran büyük olsa da fark küçük.
        assertNull(EnergyWatch.anomaly(0.2, List(10) { 0.1 }))
    }

    @Test fun budget() {
        assertTrue(EnergyWatch.budgetExceeded(500.0, 500.0))
        assertFalse(EnergyWatch.budgetExceeded(499.0, 500.0))
        assertFalse(EnergyWatch.budgetExceeded(999.0, 0.0))
    }
}

class WatchdogTest {
    @Test fun readsPinnedBeat() {
        val json = """{"ok":true,"result":{"id":1,"pinned_message":{"message_id":3,"date":100,"edit_date":700,
            "text":"🟢 SwitchGuard çalışıyor\nXiaomi 11T\nSon sinyal: 14:05"}}}"""
        assertEquals(Watchdog.Seen.Beat("Xiaomi 11T", 700_000), Watchdog.read(json))
        val stopped = """{"ok":true,"result":{"pinned_message":{"date":100,"text":"⏸ kapatıldı\nTV"}}}"""
        assertEquals(Watchdog.Seen.Stopped("TV"), Watchdog.read(stopped))
        assertEquals(Watchdog.Seen.None, Watchdog.read("""{"ok":true,"result":{"id":1}}"""))
        assertEquals(Watchdog.Seen.None, Watchdog.read("""{"ok":true,"result":{"pinned_message":{"date":1,"text":"başka"}}}"""))
    }

    @Test fun silence() {
        assertTrue(Watchdog.silent(0 + 1, 3_600_001, 3_600_000))
        assertFalse(Watchdog.silent(1, 1000, 3_600_000))
        assertFalse(Watchdog.silent(0, 9_999_999, 1))
    }
}

class WeeklyStatsTest {
    @Test fun countsWeek() {
        val e = listOf(
            WeeklyStats.Entry("OUTAGE", "", EventArgs.Outage(2)),
            WeeklyStats.Entry("OUTAGE", "", EventArgs.OutageEnded(0, 600_000)),
            WeeklyStats.Entry("DISMISSED", "", EventArgs.AlarmDismissed(1, 60_000)),
            WeeklyStats.Entry("TELEGRAM", "", EventArgs.TelegramAck("Ali", 180_000)),
            WeeklyStats.Entry("ESCALATION", "", EventArgs.Escalation(1, 5)),
            WeeklyStats.Entry("ESCALATION", "", EventArgs.Escalation(0, 5, "x")),
            WeeklyStats.Entry("WENT_OFFLINE", "Dondurucu", EventArgs.Connection(true, false)),
            WeeklyStats.Entry("WENT_OFFLINE", "Dondurucu", EventArgs.Connection(true, false)),
            WeeklyStats.Entry("WENT_OFFLINE", "Kombi", EventArgs.Connection(true, false)),
            WeeklyStats.Entry("CAME_ONLINE", "Kombi", EventArgs.Connection(false, true)),
            WeeklyStats.Entry("MONITOR_GAP", "", EventArgs.MonitorGap(0, 120_000)),
        )
        val s = WeeklyStats.from(e)
        assertEquals(1, s.outages)
        assertEquals(600_000L, s.outageMs)
        assertEquals(2, s.alarms)
        assertEquals(120_000L, s.avgResponseMs)
        assertEquals(1, s.escalations)
        assertEquals(listOf("Dondurucu" to 2, "Kombi" to 1), s.drops)
        assertEquals(120_000L, s.gapMs)
        assertNull(WeeklyStats.from(emptyList()).avgResponseMs)
    }

    @Test fun newArgsRoundTrip() {
        listOf(
            EventArgs.Outage(3), EventArgs.OutageEnded(1, 2), EventArgs.EnergyAnomaly(true, 3.5, 2.0),
            EventArgs.Budget(510.5, 500.0), EventArgs.TelegramAck("Ali", 5), EventArgs.TelegramSwitch("Ali", 1, true, false, "err"),
            EventArgs.Watchdog("TV", true, 60_000),
        ).forEach { assertEquals(it, EventArgs.parse(it.toJson())) }
    }
}

class PowerHoldTest {
    private fun dev(w: Double, online: Boolean = true) = mapOf("d" to DeviceSnapshot("d", "Dondurucu", online, mapOf(0 to true), power = w))
    private val auto = Automation("a", "Durdu", true, Trigger(TriggerKind.POWER_BELOW, "d", minutes = 60, watts = 5.0), AutoAction(ActionKind.ALARM))

    @Test fun firesOnlyAfterSustainedCondition() {
        val e = AutomationEngine()
        val list = listOf(auto)
        assertTrue(e.onStates(emptyMap(), dev(2.0), list, 0).isEmpty())
        assertEquals(3_600_000L, e.nextDueAt(dev(2.0), list))
        assertTrue(e.onTick(dev(2.0), list, 3_599_999).isEmpty())
        assertEquals(listOf(auto), e.onTick(dev(2.0), list, 3_600_000))
        // Bir kez: koşul sürdükçe tekrar etmez.
        assertTrue(e.onTick(dev(2.0), list, 9_000_000).isEmpty())
        // Belirgin toparlanma yeniden kurar.
        e.onTick(dev(50.0), list, 9_100_000)
        e.onTick(dev(1.0), list, 9_200_000)
        assertEquals(listOf(auto), e.onTick(dev(1.0), list, 9_200_000 + 3_600_000))
    }

    @Test fun interruptionRestartsTimerAndStateSurvivesRestart() {
        val e = AutomationEngine()
        val list = listOf(auto)
        e.onStates(emptyMap(), dev(2.0), list, 0)
        e.onTick(dev(2.0, online = false), list, 1_000_000)
        e.onTick(dev(2.0), list, 2_000_000)
        assertTrue(e.onTick(dev(2.0), list, 3_600_000).isEmpty())
        val r = AutomationEngine().apply { restore(e.saveState(), list) }
        assertEquals(listOf(auto), r.onTick(dev(2.0), list, 2_000_000 + 3_600_000))
    }

    @Test fun zeroMinutesKeepsInstantBehaviour() {
        val instant = auto.copy(trigger = auto.trigger.copy(minutes = 0))
        val e = AutomationEngine()
        e.onStates(emptyMap(), dev(50.0), listOf(instant), 0)
        assertEquals(listOf(instant), e.onStates(dev(50.0), dev(2.0), listOf(instant), 20_000))
    }
}
