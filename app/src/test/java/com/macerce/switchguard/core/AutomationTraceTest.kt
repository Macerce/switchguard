package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationTraceTest {
    private fun plug(on: Boolean, power: Double? = null, online: Boolean = true) =
        DeviceSnapshot("p", "Akıllı priz", online, mapOf(0 to on), power = power)
    private fun lamp(on: Boolean) = DeviceSnapshot("l", "Lamba", true, mapOf(0 to on))

    private fun rule(conds: List<Condition> = emptyList(), delay: Int = 0) = Automation(
        "a", "Test", true, Trigger(TriggerKind.TURNED_ON, "p"), AutoAction(ActionKind.TURN_OFF, "l"),
        delayMinutes = delay, conditions = conds,
    )

    private fun run(engine: AutomationEngine, a: Automation, extra: Map<String, DeviceSnapshot> = emptyMap(), now: Long = 1_000_000L): List<Automation> {
        val before = mapOf("p" to plug(false)) + extra
        val after = mapOf("p" to plug(true)) + extra
        engine.onStates(before, before, listOf(a), now - 1)
        return engine.onStates(before, after, listOf(a), now)
    }

    @Test fun `ran is traced with each condition result`() {
        val traces = mutableListOf<TraceEntry>()
        val e = AutomationEngine().apply { onTrace = { traces += it } }
        val a = rule(listOf(Condition(ConditionKind.IS_ON, "l")))
        assertEquals(1, run(e, a, mapOf("l" to lamp(true))).size)
        assertEquals(TraceOutcome.RAN, traces.single().outcome)
        assertEquals(listOf(true), traces.single().conds)
    }

    @Test fun `blocked condition is traced and not run`() {
        val traces = mutableListOf<TraceEntry>()
        val e = AutomationEngine().apply { onTrace = { traces += it } }
        val a = rule(listOf(Condition(ConditionKind.IS_ON, "l"), Condition(ConditionKind.IS_ONLINE, "l")))
        assertTrue(run(e, a, mapOf("l" to lamp(false))).isEmpty())
        assertEquals(TraceOutcome.BLOCKED_CONDITION, traces.single().outcome)
        assertEquals(listOf(false, true), traces.single().conds)
    }

    @Test fun `delayed rule traces delayed then ran`() {
        val traces = mutableListOf<TraceEntry>()
        val e = AutomationEngine().apply { onTrace = { traces += it } }
        val a = rule(delay = 1)
        assertTrue(run(e, a).isEmpty())
        assertEquals(TraceOutcome.DELAYED, traces.last().outcome)
        val due = e.onTick(mapOf("p" to plug(true)), listOf(a), 1_000_000L + 60_000L)
        assertEquals(1, due.size)
        assertEquals(TraceOutcome.RAN, traces.last().outcome)
    }

    @Test fun `power value is recorded`() {
        val traces = mutableListOf<TraceEntry>()
        val e = AutomationEngine().apply { onTrace = { traces += it } }
        val a = Automation("a", "W", true, Trigger(TriggerKind.POWER_ABOVE, "p", watts = 100.0), AutoAction(ActionKind.NOTIFY))
        e.onStates(mapOf("p" to plug(true, 10.0)), mapOf("p" to plug(true, 150.0)), listOf(a), 5_000L)
        assertEquals(150.0, traces.single().value!!, 0.0)
    }

    @Test fun `repeated identical blocks collapse into one row`() {
        var list = emptyList<TraceEntry>()
        repeat(3) { i -> list = AutomationTraces.add(list, TraceEntry("a", 100L + i, TraceOutcome.BLOCKED_WINDOW)) }
        assertEquals(1, list.size)
        assertEquals(3, list[0].count)
        assertEquals(102L, list[0].lastAt)
        list = AutomationTraces.add(list, TraceEntry("a", 200L, TraceOutcome.RAN))
        list = AutomationTraces.add(list, TraceEntry("a", 201L, TraceOutcome.RAN))
        assertEquals(3, list.size) // çalışmalar hiç birleşmez
    }

    @Test fun `per automation cap keeps newest`() {
        var list = emptyList<TraceEntry>()
        repeat(AutomationTraces.PER_AUTOMATION + 5) { i -> list = AutomationTraces.add(list, TraceEntry("a", i.toLong(), TraceOutcome.RAN)) }
        list = AutomationTraces.add(list, TraceEntry("b", 1L, TraceOutcome.RAN))
        assertEquals(AutomationTraces.PER_AUTOMATION, list.count { it.automationId == "a" })
        assertEquals(5L, list.first().at)
    }

    @Test fun `steps attach to latest run and replace retries`() {
        var list = listOf(TraceEntry("a", 1L, TraceOutcome.RAN), TraceEntry("a", 2L, TraceOutcome.BLOCKED_WINDOW))
        list = AutomationTraces.step(list, "a", TraceStep(0, StepResult.FAILED, 3L, "timeout"))
        list = AutomationTraces.step(list, "a", TraceStep(0, StepResult.DONE, 4L))
        list = AutomationTraces.step(list, "a", TraceStep(1, StepResult.DONE, 5L))
        assertEquals(listOf(StepResult.DONE, StepResult.DONE), list[0].steps.map { it.result })
        assertTrue(list[1].steps.isEmpty())
        assertEquals(list, AutomationTraces.step(list, "zzz", TraceStep(0, StepResult.DONE, 6L)))
    }

    @Test fun `json round trip and corrupt input`() {
        val list = listOf(
            TraceEntry("a", 1L, TraceOutcome.BLOCKED_CONDITION, listOf(true, false), 12.5, true, count = 4, lastAt = 9L),
            TraceEntry("b", 2L, TraceOutcome.RAN, steps = listOf(TraceStep(0, StepResult.FAILED, 3L, "x"), TraceStep(1, StepResult.SILENCED, 4L))),
        )
        assertEquals(list, AutomationTraces.fromJson(AutomationTraces.toJson(list)))
        assertTrue(AutomationTraces.fromJson("{bozuk").isEmpty())
        assertTrue(AutomationTraces.fromJson(null).isEmpty())
        assertEquals(1, AutomationTraces.fromJson("""[{"a":"x","t":1,"o":"NOPE"},{"a":"y","t":2,"o":"RAN"}]""").size)
    }

    @Test fun `verdict reports window and presence separately`() {
        val a = rule().copy(presence = PresenceRule.AWAY)
        val v = a.verdict(emptyMap(), 0L, away = false)
        assertTrue(v.windowOk)
        assertFalse(v.presenceOk)
        assertEquals(TraceOutcome.BLOCKED_PRESENCE, v.outcome)
        assertNull(null)
    }
}

class StateSnapshotTest {
    private val devices = listOf(
        DeviceSnapshot("p", "Priz", true, mapOf(0 to true)),
        DeviceSnapshot("d", "İkili", true, mapOf(0 to false, 1 to true)),
        DeviceSnapshot("off", "Çevrimdışı", false, mapOf(0 to true)),
        DeviceSnapshot("s", "Sensör", true, emptyMap(), sensors = mapOf(SensorKind.TEMPERATURE to 20.0)),
        DeviceSnapshot("group:x", "Grup", true, mapOf(0 to true)),
    )

    private val all = setOf("p", "d", "off", "s", "group:x")

    @Test fun `capture skips offline sensors and virtual devices`() {
        val saved = StateSnapshot.capture(devices, all) { it.startsWith("group:") }
        assertEquals(listOf(SceneAction("p", 0, true), SceneAction("d", 0, false), SceneAction("d", 1, true)), saved)
        assertEquals(listOf(SceneAction("p", 0, true)), StateSnapshot.capture(devices, setOf("p")))
    }

    @Test fun `restore only sends changed channels`() {
        val saved = StateSnapshot.capture(devices, all) { it.startsWith("group:") }
        val now = mapOf(
            "p" to DeviceSnapshot("p", "Priz", true, mapOf(0 to false)),
            "d" to DeviceSnapshot("d", "İkili", true, mapOf(0 to false, 1 to true)),
        )
        assertEquals(listOf(SceneAction("p", 0, true)), StateSnapshot.changes(saved, now))
        assertEquals(saved, StateSnapshot.fromJson(StateSnapshot.toJson(saved)))
        assertTrue(StateSnapshot.fromJson("x").isEmpty())
    }
}

class AutomationTemplateTest {
    private val plain = DeviceSnapshot("p", "Akıllı priz", true, mapOf(0 to false))
    private val metered = DeviceSnapshot("m", "Ölçer", true, mapOf(0 to false), power = 3.0)

    @Test fun `energy templates need a metering device`() {
        assertNull(AutomationTemplate.MACHINE_DONE.pickDevice(listOf(plain)))
        assertEquals("m", AutomationTemplate.MACHINE_DONE.pickDevice(listOf(plain, metered))?.id)
        assertEquals("p", AutomationTemplate.FORGOTTEN_OFF.pickDevice(listOf(plain, metered))?.id)
    }

    @Test fun `every template builds a rule in a known tab`() {
        AutomationTemplate.entries.forEach { t ->
            val a = t.build(metered, t.name)
            assertEquals(t.name, a.name)
            // Sekmeye düşer ve JSON'a yazılıp okunabilir.
            AutomationTab.tabOf(a)
            assertEquals(a.copy(id = "x"), Automation.listFromJson(Automation.listToJson(listOf(a.copy(id = "x")))).single())
        }
    }

    @Test fun `pause and restore chains snapshot actions`() {
        val a = AutomationTemplate.PAUSE_AND_RESTORE.build(metered, "n")
        assertEquals(listOf(ActionKind.SAVE_STATE, ActionKind.TURN_OFF, ActionKind.RESTORE_STATE), a.allActions.map { it.kind })
        assertEquals(15, a.extraActions.last().waitMinutes)
        assertEquals("m", a.action.deviceId) // yalnız o cihaz, asla tüm hesap
        assertEquals(AutomationTab.AUTOMATIONS, AutomationTab.tabOf(a))
    }
}
