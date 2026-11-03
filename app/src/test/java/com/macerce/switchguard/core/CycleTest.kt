package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CycleTest {
    private val zone = TimeZone.getTimeZone("Europe/Istanbul")
    private val a = CycleTarget("d1", 0)
    private val b = CycleTarget("d2", 1)
    private val half = { 0.5 }

    /** 2026-11-02 Pazartesi [h]:[m] (İstanbul). */
    private fun at(h: Int, m: Int = 0, dayOffset: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.NOVEMBER, 2 + dayOffset, h, m)
    }.timeInMillis

    private fun cycle(
        steps: List<CycleStep>, repeat: Int = 1, end: CycleEnd = CycleEnd.OFF, start: CycleStart = CycleStart.MANUAL,
        targets: List<CycleTarget> = listOf(a), days: Int = ActiveWindow.ALL_DAYS, from: Int = 7 * 60, to: Int = 22 * 60,
        presence: PresenceRule? = null, manualStop: Boolean = false,
    ) = Cycle("c", "Test", true, targets, steps, repeat, start, from, to, days, end, 0, presence, manualStop)

    private fun cmds(r: CycleResult) = r.commands.map { it.target.key to it.on }

    @Test fun countdownTurnsOffWhenTimeIsUp() {
        val (steps, repeat, end) = CycleEngine.Presets.countdown(listOf(a), 1800, on = false)
        val c = cycle(steps, repeat, end)
        val started = CycleEngine.start(c, emptyMap(), emptyMap(), mapOf(a.key to true), 0)
        assertEquals(emptyList<Pair<String, Boolean>>(), cmds(started))
        assertEquals(1_800_000L, started.runs.getValue("c").stepEndsAt)
        val mid = CycleEngine.tick(listOf(c), started.runs, started.done, emptyMap(), 1_000_000, away = false, zone = zone)
        assertTrue(mid.commands.isEmpty())
        val done = CycleEngine.tick(listOf(c), mid.runs, mid.done, emptyMap(), 1_800_000, away = false, zone = zone)
        assertEquals(listOf(a.key to false), cmds(done))
        assertTrue(done.runs.isEmpty())
        assertEquals(CycleEventKind.FINISHED, done.events.single().kind)
    }

    @Test fun blinkRepeatsGivenRoundsThenTurnsOff() {
        val (steps, _, end) = CycleEngine.Presets.blink(listOf(a), 10, 20, 2)
        val c = cycle(steps, 2, end)
        var r = CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0)
        assertEquals(listOf(a.key to true), cmds(r))
        val seen = mutableListOf<Pair<Long, List<Pair<String, Boolean>>>>()
        for (t in listOf(10_000L, 30_000L, 40_000L, 60_000L)) {
            r = CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), t, false, zone)
            seen += t to cmds(r)
        }
        assertEquals(listOf(
            10_000L to listOf(a.key to false),
            30_000L to listOf(a.key to true),
            40_000L to listOf(a.key to false),
            60_000L to listOf(a.key to false),
        ), seen)
        assertTrue(r.runs.isEmpty())
    }

    @Test fun chaseTurnsOnOneAtATime() {
        val (steps, _, _) = CycleEngine.Presets.chase(listOf(a, b), 60, 0)
        assertEquals(mapOf(a.key to true, b.key to false), steps[0].states)
        assertEquals(mapOf(a.key to false, b.key to true), steps[1].states)
    }

    @Test fun sleepingPhoneCatchesUpAndSendsOnlyLatestState() {
        val (steps, _, end) = CycleEngine.Presets.blink(listOf(a), 10, 10, 0)
        val c = cycle(steps, 0, end)
        val r = CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0)
        // 35 sn sonra: 10 kapalı, 20 açık, 30 kapalı → yalnızca "kapalı" gider, sıradaki adım 40'ta.
        val late = CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), 35_000, false, zone)
        assertEquals(listOf(a.key to false), cmds(late))
        assertEquals(40_000L, late.runs.getValue("c").stepEndsAt)
        assertEquals(1, late.runs.getValue("c").round)
    }

    @Test fun restoreReturnsToStatesBeforeStart() {
        val c = cycle(listOf(CycleStep(10, mapOf(a.key to true, b.key to true))), end = CycleEnd.RESTORE, targets = listOf(a, b))
        val r = CycleEngine.start(c, emptyMap(), emptyMap(), mapOf(a.key to false, b.key to true), 0)
        val done = CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), 10_000, false, zone)
        assertEquals(listOf(a.key to false, b.key to true), cmds(done))
    }

    @Test fun atTimeStartsOnSelectedDaysOnlyOncePerDay() {
        // Pazartesi (bit 0) ve Çarşamba (bit 2); 07:00'de 20 dk açık.
        val c = cycle(CycleEngine.Presets.onFor(listOf(a), 1200).first, start = CycleStart.AT_TIME, days = 0b101)
        val before = CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(6, 59), false, zone)
        assertTrue(before.runs.isEmpty())
        val r = CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(7, 2), false, zone)
        assertEquals(listOf(a.key to true), cmds(r))
        val off = CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), at(7, 22), false, zone)
        assertEquals(listOf(a.key to false), cmds(off))
        // Aynı gün yeniden başlamaz.
        val again = CycleEngine.tick(listOf(c), off.runs, off.done, emptyMap(), at(7, 23), false, zone)
        assertTrue(again.runs.isEmpty())
        // Salı yok, Çarşamba var.
        assertEquals(at(7, 0, dayOffset = 2), CycleEngine.nextStart(c, at(8), zone))
    }

    @Test fun atTimeSkipsWhenCaughtTooLate() {
        val c = cycle(listOf(CycleStep(60, mapOf(a.key to true))), start = CycleStart.AT_TIME)
        val r = CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(7, 20), false, zone)
        assertTrue(r.runs.isEmpty())
    }

    @Test fun windowRunsUntilEndAndCrossesMidnight() {
        val c = cycle(CycleEngine.Presets.blink(listOf(a), 600, 600, 0).first, repeat = 0, start = CycleStart.WINDOW, from = 22 * 60, to = 2 * 60)
        val r = CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(23), false, zone)
        assertEquals(at(2, dayOffset = 1), r.runs.getValue("c").windowEnd)
        val end = CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), at(2, 0, dayOffset = 1), false, zone)
        assertTrue(end.runs.isEmpty())
        assertEquals(listOf(a.key to false), cmds(end))
        // Gece yarısından sonra (dünün aralığı) da içeride sayılır.
        assertEquals(at(22) to at(2, dayOffset = 1), CycleEngine.opportunity(c, at(1, dayOffset = 1), zone))
    }

    @Test fun presenceRuleBlocksAndStopsScheduledButNotManual() {
        val c = cycle(listOf(CycleStep(60, mapOf(a.key to true))), repeat = 0, start = CycleStart.WINDOW, presence = PresenceRule.AWAY)
        assertTrue(CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(9), away = false, zone = zone).runs.isEmpty())
        val r = CycleEngine.tick(listOf(c), emptyMap(), emptyMap(), emptyMap(), at(9), away = true, zone = zone)
        assertFalse(r.runs.isEmpty())
        assertTrue(CycleEngine.tick(listOf(c), r.runs, r.done, emptyMap(), at(9, 0) + 1, away = false, zone = zone).runs.isEmpty())
        val manual = CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), at(9))
        assertFalse(CycleEngine.tick(listOf(c), manual.runs, manual.done, emptyMap(), at(9) + 1, away = false, zone = zone).runs.isEmpty())
    }

    @Test fun manualChangeStopsOnlyWhenOptedInAndStateDiffers() {
        val c = cycle(listOf(CycleStep(60, mapOf(a.key to true))), manualStop = true)
        val r = CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0)
        // Döngünün kendi komutu (açık) durdurmaz.
        assertEquals(r.runs, CycleEngine.manualChange(listOf(c), r.runs, r.done, a, emptySet(), to = true).runs)
        val stopped = CycleEngine.manualChange(listOf(c), r.runs, r.done, a, emptySet(), to = false)
        assertTrue(stopped.runs.isEmpty())
        assertTrue(stopped.commands.isEmpty())
        assertEquals(CycleEventKind.OVERRIDDEN, stopped.events.single().kind)
        val optedOut = c.copy(stopOnManual = false)
        assertEquals(r.runs, CycleEngine.manualChange(listOf(optedOut), r.runs, r.done, a, emptySet(), to = false).runs)
    }

    @Test fun jitterVariesDurationWithinBounds() {
        val c = cycle(listOf(CycleStep(100)), end = CycleEnd.KEEP).copy(jitterPct = 20)
        assertEquals(80_000L, CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0) { 0.0 }.runs.getValue("c").stepEndsAt)
        assertEquals(120_000L, CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0) { 1.0 }.runs.getValue("c").stepEndsAt)
        assertEquals(100_000L, CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0, half).runs.getValue("c").stepEndsAt)
    }

    @Test fun deletedOrDisabledCycleStops() {
        val c = cycle(listOf(CycleStep(60, mapOf(a.key to true))))
        val r = CycleEngine.start(c, emptyMap(), emptyMap(), emptyMap(), 0)
        assertTrue(CycleEngine.tick(emptyList(), r.runs, r.done, emptyMap(), 1, false, zone).runs.isEmpty())
        val off = CycleEngine.tick(listOf(c.copy(enabled = false)), r.runs, r.done, emptyMap(), 1, false, zone)
        assertTrue(off.runs.isEmpty())
        assertEquals(listOf(a.key to false), cmds(off))
    }

    @Test fun jsonRoundTrip() {
        val c = cycle(listOf(CycleStep(15, mapOf(a.key to true)), CycleStep(30)), repeat = 0, end = CycleEnd.RESTORE,
            start = CycleStart.WINDOW, targets = listOf(a, b), days = 0b11, presence = PresenceRule.HOME, manualStop = true)
            .copy(jitterPct = 30, preset = CyclePreset.CUSTOM)
        assertEquals(listOf(c), Cycle.listFromJson(Cycle.listToJson(listOf(c))))
        val run = CycleRun("c", 1, true, 1, 2, 3, mapOf(a.key to false), 4)
        assertEquals(mapOf("c" to run), CycleRun.mapFromJson(CycleRun.mapToJson(mapOf("c" to run))))
        assertEquals(emptyList<Cycle>(), Cycle.listFromJson("bozuk"))
        assertNull(CycleEngine.nextStart(c.copy(start = CycleStart.MANUAL), 0, zone))
    }
}
