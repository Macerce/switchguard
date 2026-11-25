package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Döngü koruması: "durdur, sonra geri getir" gibi kurallar sonsuz aç-kapa döngüsüne girmesin. */
class LoopGuardTest {
    private fun plug(power: Double) = DeviceSnapshot("p", "Akıllı priz", true, mapOf(0 to true), power = power)

    private val rule = AutomationTemplate.PAUSE_AND_RESTORE.build(plug(0.0), "Durdur").copy(id = "a")

    /** Güç eşiği aşılır (tetiklenir), sonra düşer (yeniden kurulur): bir "döngü turu". */
    private fun cycle(e: AutomationEngine, at: Long): List<Automation> {
        val low = mapOf("p" to plug(0.0))
        val high = mapOf("p" to plug(2500.0))
        val fired = e.onStates(low, high, listOf(rule), at)
        e.onStates(high, low, listOf(rule), at + 30_000)
        return fired
    }

    @Test fun `template enables loop guard with 3`() {
        assertEquals(3, rule.loopGuard)
    }

    @Test fun `fourth trigger within an hour locks out instead of running`() {
        val locked = mutableListOf<Automation>()
        val traces = mutableListOf<TraceEntry>()
        val e = AutomationEngine().apply { onLockout = { locked += it }; onTrace = { traces += it } }
        val start = 10_000_000L
        repeat(3) { i -> assertEquals(1, cycle(e, start + i * 120_000L).size) }
        assertTrue(locked.isEmpty())
        assertTrue(cycle(e, start + 3 * 120_000L).isEmpty())
        assertEquals(listOf("a"), locked.map { it.id })
        assertEquals(TraceOutcome.LOCKED_OUT, traces.last().outcome)
    }

    @Test fun `runs older than an hour do not count`() {
        val locked = mutableListOf<Automation>()
        val e = AutomationEngine().apply { onLockout = { locked += it } }
        val start = 10_000_000L
        repeat(3) { i -> cycle(e, start + i * 120_000L) }
        assertEquals(1, cycle(e, start + AutomationEngine.LOOP_WINDOW_MS + 300_000L).size)
        assertTrue(locked.isEmpty())
    }

    @Test fun `guard off never locks`() {
        val locked = mutableListOf<Automation>()
        val e = AutomationEngine().apply { onLockout = { locked += it } }
        val open = rule.copy(loopGuard = 0)
        val low = mapOf("p" to plug(0.0)); val high = mapOf("p" to plug(2500.0))
        repeat(6) { i ->
            assertEquals(1, e.onStates(low, high, listOf(open), 10_000_000L + i * 120_000L).size)
            e.onStates(high, low, listOf(open), 10_000_000L + i * 120_000L + 30_000)
        }
        assertTrue(locked.isEmpty())
    }

    @Test fun `run counts survive a service restart`() {
        val e = AutomationEngine()
        repeat(3) { i -> cycle(e, 10_000_000L + i * 120_000L) }
        val locked = mutableListOf<Automation>()
        val again = AutomationEngine().apply { onLockout = { locked += it }; restore(e.saveState(), listOf(rule)) }
        assertTrue(cycle(again, 10_000_000L + 3 * 120_000L).isEmpty())
        assertEquals(1, locked.size)
    }

    @Test fun `loop guard is saved in json`() {
        val back = Automation.listFromJson(Automation.listToJson(listOf(rule))).single()
        assertEquals(3, back.loopGuard)
        assertEquals(0, Automation.listFromJson(Automation.listToJson(listOf(rule.copy(loopGuard = 0)))).single().loopGuard)
    }
}
