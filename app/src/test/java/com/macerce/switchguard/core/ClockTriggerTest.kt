package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ClockTriggerTest {
    // Motor cihazın saat dilimini kullanır: test de varsayılan dilimle (CI UTC, telefon İstanbul) aynı sonucu vermeli.
    private val zone = TimeZone.getDefault()

    /** 2026-11-23 Pazartesi [h]:[m]. */
    private fun mon(h: Int, m: Int = 0, plusDays: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear(); set(2026, Calendar.NOVEMBER, 23 + plusDays, h, m, 0)
    }.timeInMillis

    private val monday = 1 shl 0
    private val weekdays = 0b0011111

    private fun rule(minute: Int, days: Int = ActiveWindow.ALL_DAYS, extra: List<AutoAction> = emptyList()) = Automation(
        id = "r1", name = "Sabah", enabled = true,
        trigger = Trigger(TriggerKind.TIME, "", minutes = minute, days = days),
        action = AutoAction(ActionKind.TURN_ON, "pump"),
        extraActions = extra,
    )

    @Test
    fun todayAndNextOccurrence() {
        assertEquals(mon(7), ClockTrigger.todayAt(7 * 60, ActiveWindow.ALL_DAYS, mon(5), zone))
        assertNull(ClockTrigger.todayAt(7 * 60, 1 shl 6, mon(5), zone)) // yalnız pazar
        assertEquals(mon(7), ClockTrigger.nextAt(7 * 60, weekdays, mon(5), zone))
        // Pazartesi 08:00'den sonra, yalnız pazartesi: gelecek hafta.
        assertEquals(mon(7, plusDays = 7), ClockTrigger.nextAt(7 * 60, monday, mon(8), zone))
        assertNull(ClockTrigger.nextAt(7 * 60, 0, mon(8), zone))
    }

    @Test
    fun firesOnceAtTheTime() {
        val e = AutomationEngine()
        val list = listOf(rule(7 * 60))
        // Kural vakitten önce görüldü: 06:59'da yok, 07:00'de var, aynı gün tekrar yok.
        assertTrue(e.onTick(emptyMap(), list, mon(6, 59)).isEmpty())
        assertEquals(listOf("r1"), e.onTick(emptyMap(), list, mon(7)).map { it.id })
        assertTrue(e.onTick(emptyMap(), list, mon(7, 1)).isEmpty())
        assertTrue(e.onTick(emptyMap(), list, mon(12)).isEmpty())
        // Ertesi gün yine.
        assertEquals(1, e.onTick(emptyMap(), list, mon(7, plusDays = 1)).size)
    }

    @Test
    fun newRuleDoesNotFireForATimeAlreadyPassedToday() {
        val e = AutomationEngine()
        // 07:00 kuralı 07:30'da kuruldu: bugün çalışmaz, yarın çalışır.
        assertTrue(e.onTick(emptyMap(), listOf(rule(7 * 60)), mon(7, 30)).isEmpty())
        assertEquals(1, e.onTick(emptyMap(), listOf(rule(7 * 60)), mon(7, plusDays = 1)).size)
    }

    @Test
    fun missedByMoreThanTwoHoursIsSkipped() {
        val e = AutomationEngine()
        e.onTick(emptyMap(), listOf(rule(7 * 60)), mon(6)) // görüldü, vakit gelmedi
        // Telefon kapalıydı, 09:30'da açıldı: 2 saatten geç, çalışmaz.
        assertTrue(e.onTick(emptyMap(), listOf(rule(7 * 60)), mon(9, 30)).isEmpty())
    }

    @Test
    fun onlyOnChosenDays() {
        val e = AutomationEngine()
        val sundayOnly = listOf(rule(7 * 60, days = 1 shl 6))
        e.onTick(emptyMap(), sundayOnly, mon(6))
        assertTrue(e.onTick(emptyMap(), sundayOnly, mon(7)).isEmpty())
        e.onTick(emptyMap(), sundayOnly, mon(6, plusDays = 6))
        assertEquals(1, e.onTick(emptyMap(), sundayOnly, mon(7, plusDays = 6)).size)
    }

    @Test
    fun nextDueIncludesClock() {
        val e = AutomationEngine()
        val due = e.nextDueAt(emptyMap(), listOf(rule(7 * 60)), now = mon(5))
        assertEquals(mon(7), due)
    }

    @Test
    fun stateSurvivesRestart() {
        val e = AutomationEngine()
        val list = listOf(rule(7 * 60))
        e.onTick(emptyMap(), list, mon(6))
        e.onTick(emptyMap(), list, mon(7))
        val restored = AutomationEngine().apply { restore(e.saveState(), list) }
        // Yeniden başlayan servis aynı gün ikinci kez çalıştırmaz.
        assertTrue(restored.onTick(emptyMap(), list, mon(7, 5)).isEmpty())
    }

    @Test
    fun clockRuleAndExtraActionsSurviveJson() {
        val a = rule(7 * 60 + 30, days = weekdays, extra = listOf(
            AutoAction(ActionKind.TURN_OFF, "valve", 1, waitMinutes = 5),
            AutoAction(ActionKind.RUN_SCENE, "scene-1"),
        ))
        val back = Automation.listFromJson(Automation.listToJson(listOf(a))).single()

        assertEquals(a, back)
        assertTrue(back.trigger.kind.isClock)
        assertTrue(!back.trigger.kind.usesDevice)
    }

    @Test
    fun oldSingleActionJsonStillReads() {
        val old = """[{"id":"x","name":"Eski","enabled":true,"delay":0,"trigger":{"kind":"TURNED_ON","device":"d","channel":0,"minutes":0,"watts":0,"grace":0},"action":{"kind":"TURN_OFF","device":"e","channel":0}}]"""
        val a = Automation.listFromJson(old).single()

        assertEquals(AutoAction(ActionKind.TURN_OFF, "e"), a.action)
        assertTrue(a.extraActions.isEmpty())
        assertEquals(ActiveWindow.ALL_DAYS, a.trigger.days)
    }

    @Test
    fun extraActionsAreScheduledByWaits() {
        val a = rule(7 * 60, extra = listOf(
            AutoAction(ActionKind.TURN_OFF, "a"),
            AutoAction(ActionKind.TURN_OFF, "b", waitMinutes = 5),
            AutoAction(ActionKind.TURN_OFF, "c", waitMinutes = 10),
        ))
        val (now, later) = ActionSteps.plan(a, 1_000_000)

        assertEquals(listOf(0), now)
        assertEquals(listOf(ActionSteps.Due("r1", 1, 1_000_000 + 300_000), ActionSteps.Due("r1", 2, 1_000_000 + 900_000)), later)
        assertEquals(later, ActionSteps.fromJson(ActionSteps.toJson(later)))
    }
}
