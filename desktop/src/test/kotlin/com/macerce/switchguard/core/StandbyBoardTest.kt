package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StandbyBoardTest {
    private val zone = TimeZone.getTimeZone("Europe/Istanbul")
    private val lat = 41.0
    private val lon = 29.0

    /** 2026-11-02 Pazartesi [h]:[m] (İstanbul). */
    private fun at(h: Int, m: Int = 0, dayOffset: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.NOVEMBER, 2 + dayOffset, h, m)
    }.timeInMillis

    private fun sun(id: String, kind: TriggerKind, offsetMin: Int = 0, window: ActiveWindow? = null, enabled: Boolean = true) =
        Automation(id, id, enabled, Trigger(kind, "", minutes = offsetMin), AutoAction(ActionKind.TURN_ON, "d1"), window = window)

    private fun upcoming(
        automations: List<Automation> = emptyList(), cycles: List<Cycle> = emptyList(), runs: Map<String, CycleRun> = emptyMap(),
        templates: List<ChatTemplate> = emptyList(), now: Long, latitude: Double? = lat,
    ) = StandbyBoard.upcoming(automations, cycles, runs, templates, emptyMap(), latitude, lon, now, zone = zone)

    @Test fun sunsetLaterTodayIsListed() {
        val list = upcoming(listOf(sun("a", TriggerKind.SUNSET)), now = at(12))
        assertEquals(1, list.size)
        val expected = Sun.time(at(12), lat, lon, sunrise = false, zone = zone)!!
        assertEquals(expected, list[0].time)
        assertEquals(StandbyBoard.UpcomingKind.SUNSET, list[0].kind)
    }

    @Test fun passedSunriseMovesToTomorrow() {
        val list = upcoming(listOf(sun("a", TriggerKind.SUNRISE, offsetMin = 30)), now = at(12))
        val expected = Sun.time(at(12, dayOffset = 1), lat, lon, sunrise = true, zone = zone)!! + 30 * 60_000L
        assertEquals(listOf(expected), list.map { it.time })
    }

    @Test fun noLocationOrDisabledOrOutsideWindowSkipsSun() {
        assertTrue(upcoming(listOf(sun("a", TriggerKind.SUNSET)), now = at(12), latitude = null).isEmpty())
        assertTrue(upcoming(listOf(sun("a", TriggerKind.SUNSET, enabled = false)), now = at(12)).isEmpty())
        // Yalnızca pazar günleri: pazartesi öğlen bakılınca önümüzdeki 24 saatte yok.
        val sundayOnly = ActiveWindow(0, 0, 1 shl 6)
        assertTrue(upcoming(listOf(sun("a", TriggerKind.SUNSET, window = sundayOnly)), now = at(12)).isEmpty())
    }

    @Test fun cyclesAndTemplatesAreSorted() {
        val target = CycleTarget("d1", 0)
        val timed = Cycle("c1", "Bahçe", true, listOf(target), listOf(CycleStep(600, mapOf(target.key to true))), start = CycleStart.AT_TIME, startMinute = 20 * 60)
        val manual = Cycle("c2", "Elle", true, listOf(target), listOf(CycleStep(600)))
        val tpl = ChatTemplate("tpl:1", "Akşam", "ışıkları aç", atMinute = 18 * 60)
        val list = upcoming(cycles = listOf(timed, manual), templates = listOf(tpl), now = at(12))
        assertEquals(listOf("Akşam" to at(18), "Bahçe" to at(20)), list.map { it.name to it.time })
        assertEquals(listOf(StandbyBoard.UpcomingKind.TEMPLATE, StandbyBoard.UpcomingKind.CYCLE_START), list.map { it.kind })
    }

    @Test fun runningCycleShowsItsNextStepOrEnd() {
        val target = CycleTarget("d1", 0)
        val c = Cycle("c", "Geri sayım", true, listOf(target), listOf(CycleStep(1800)), repeat = 1)
        val run = CycleRun("c", at(12), manual = true, step = 0, round = 0, stepEndsAt = at(12, 30))
        val list = upcoming(cycles = listOf(c), runs = mapOf("c" to run), now = at(12, 10))
        assertEquals(listOf(StandbyBoard.Upcoming(at(12, 30), "Geri sayım", StandbyBoard.UpcomingKind.CYCLE_END)), list)
    }

    @Test fun totalPowerSumsDevicesAndDropsStaleSamples() {
        val a = listOf(PowerSample(0, 100.0), PowerSample(10 * 60_000L, 100.0), PowerSample(30 * 60_000L, 200.0))
        val b = listOf(PowerSample(0, 50.0))
        // 0, 20, 40, 60 dk; b'nin tek ölçümü 15 dk'dan eskiyince sayılmaz.
        val series = StandbyBoard.totalPower(listOf(a, b), 0, 60 * 60_000L, 4)
        assertEquals(listOf(150.0, 100.0, 200.0, 0.0), series)
    }

    @Test fun gaugeMaxRoundsUp() {
        assertEquals(500.0, StandbyBoard.gaugeMax(100.0), 0.0)
        assertEquals(2000.0, StandbyBoard.gaugeMax(1600.0), 0.0)
        assertEquals(30_000.0, StandbyBoard.gaugeMax(25_000.0), 0.0)
    }
}
