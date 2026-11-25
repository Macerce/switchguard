package com.macerce.switchguard.core

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2.55 kural motoru: döngü koruması hedefleri, adım yürütücü, koşul grupları, çoklu tetikleyici, yardımcılar. */
class RuleEngine2Test {
    private fun dev(id: String, on: Boolean = true, power: Double? = null, online: Boolean = true, channels: Int = 1) =
        DeviceSnapshot(id, id, online, (0 until channels).associateWith { on }, power = power)

    private fun rule(trigger: Trigger, action: AutoAction, extras: List<AutoAction> = emptyList()) =
        Automation("r", "Kural", true, trigger, action, extraActions = extras)

    // ------------------------------------------------------------ döngü koruması (kullanıcının bildirdiği hata)

    @Test fun `loop guard never turns off the trigger device - freezer stays on`() {
        val freezer = dev("freezer", on = true, power = 2.0)
        val heater = dev("heater", on = true)
        val a = rule(Trigger(TriggerKind.POWER_BELOW, "freezer", watts = 5.0), AutoAction(ActionKind.TURN_ON, "heater"))
        val plan = LoopGuard.plan(a.flatActions, mapOf("freezer" to freezer, "heater" to heater), emptyList(), emptyList()) { false }
        assertEquals(listOf("heater" to 0), plan.turnOff.map { it.first.id to it.second })
        assertTrue(plan.turnOff.none { it.first.id == "freezer" })
    }

    @Test fun `loop guard turns off nothing for alarm or notify only rules`() {
        val freezer = dev("freezer", on = true)
        val a = rule(Trigger(TriggerKind.POWER_BELOW, "freezer", watts = 5.0), AutoAction(ActionKind.ALARM))
        val plan = LoopGuard.plan(a.flatActions, mapOf("freezer" to freezer), emptyList(), emptyList()) { false }
        assertTrue(plan.turnOff.isEmpty())
        assertTrue(plan.kept.isEmpty())
    }

    @Test fun `pause and restore template still turns its own device off`() {
        val plug = dev("p", on = true, power = 2500.0)
        val a = AutomationTemplate.PAUSE_AND_RESTORE.build(plug, "Durdur")
        val plan = LoopGuard.plan(a.flatActions, mapOf("p" to plug), emptyList(), emptyList()) { false }
        assertEquals(listOf("p" to 0), plan.turnOff.map { it.first.id to it.second })
    }

    @Test fun `critical and confirm-off devices are kept, not switched`() {
        val pump = dev("pump", on = true)
        val lamp = dev("lamp", on = true)
        val a = rule(Trigger(TriggerKind.TURNED_ON, "x"), AutoAction(ActionKind.TURN_ON, "pump"), listOf(AutoAction(ActionKind.TOGGLE, "lamp")))
        val plan = LoopGuard.plan(a.flatActions, mapOf("pump" to pump, "lamp" to lamp), emptyList(), emptyList()) { it == "pump" }
        assertEquals(listOf("lamp"), plan.offDevices.map { it.id })
        assertEquals(listOf("pump"), plan.kept.map { it.id })
    }

    @Test fun `loop guard follows groups, scenes and nested steps`() {
        val a1 = dev("a1"); val a2 = dev("a2", channels = 2); val s1 = dev("s1"); val n1 = dev("n1"); val off = dev("off", on = false)
        val group = DeviceGroup("g", "Grup", listOf(GroupMember("a1", 0), GroupMember("a2", 1)))
        val scene = Scene("sc", "Sahne", listOf(SceneAction("s1", 0, true), SceneAction("off", 0, true)))
        val nested = AutoAction(ActionKind.IF, cond = CondSet(), then = listOf(AutoAction(ActionKind.TURN_ON, "n1")))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, group.deviceId), listOf(AutoAction(ActionKind.RUN_SCENE, "sc"), nested))
        val byId = listOf(a1, a2, s1, n1, off).associateBy { it.id }
        val plan = LoopGuard.plan(a.flatActions, byId, listOf(group), listOf(scene)) { false }
        assertEquals(listOf("a1" to 0, "a2" to 1, "s1" to 0, "n1" to 0), plan.turnOff.map { it.first.id to it.second })
    }

    @Test fun `category rule lockout resolves to the rule itself`() {
        assertEquals("abc", Automation.baseId("abc@dev/0"))
        assertEquals("abc", Automation.baseId("abc~2@dev"))
        assertEquals("abc@dev", Automation.ruleKey("abc~2@dev"))
        assertEquals("abc", Automation.ruleKey("abc~1"))
    }

    // ------------------------------------------------------------ adım yürütücü

    private fun run(a: Automation, now: Long = 0L) = ScriptRunner.start(a, now, "x")

    private fun ids(out: List<ScriptRunner.Out>) = out.filterIsInstance<ScriptRunner.Out.Do>().map { it.action.deviceId }

    @Test fun `delay pauses and resumes at the right step`() {
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "a"),
            listOf(AutoAction(ActionKind.DELAY, seconds = 30), AutoAction(ActionKind.TURN_OFF, "b")))
        val r1 = ScriptRunner.advance(a, run(a), 1000) { true }
        assertEquals(listOf("a"), ids(r1.out))
        assertEquals(31_000L, r1.run!!.resumeAt)
        // Erken: dokunulmaz.
        assertTrue(ScriptRunner.advance(a, r1.run!!, 20_000) { true }.out.isEmpty())
        val r2 = ScriptRunner.advance(a, r1.run!!, 31_000) { true }
        assertEquals(listOf("b"), ids(r2.out))
        assertNull(r2.run)
    }

    @Test fun `legacy wait minutes still work`() {
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "a"), listOf(AutoAction(ActionKind.TURN_OFF, "a", waitMinutes = 15)))
        val r1 = ScriptRunner.advance(a, run(a), 0) { true }
        assertEquals(15 * 60_000L, r1.run!!.resumeAt)
        assertEquals(listOf("a"), ids(ScriptRunner.advance(a, r1.run!!, 15 * 60_000L) { true }.out))
    }

    @Test fun `wait until continues when condition holds and times out otherwise`() {
        val cond = CondSet(listOf(Condition(ConditionKind.IS_OFF, "m")))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "l"),
            listOf(AutoAction(ActionKind.WAIT_UNTIL, cond = cond, seconds = 60), AutoAction(ActionKind.TURN_OFF, "l")))
        val r1 = ScriptRunner.advance(a, run(a), 0) { false }
        assertTrue(r1.run!!.waitingForCondition)
        assertEquals(60_000L, r1.run!!.untilDeadline)
        // Koşul sağlandı.
        val ok = ScriptRunner.advance(a, r1.run!!, 10_000) { true }
        assertEquals(listOf("l"), ids(ok.out)); assertNull(ok.run)
        // Süre doldu: sonraki adımla devam (stopOnTimeout yok).
        val late = ScriptRunner.advance(a, r1.run!!, 60_000) { false }
        assertTrue(late.out.any { it is ScriptRunner.Out.Note && it.result == StepResult.TIMEOUT })
        assertEquals(listOf("l"), ids(late.out))
        // Süre dolunca dur.
        val strict = a.copy(extraActions = listOf(a.extraActions[0].copy(stopOnTimeout = true), a.extraActions[1]))
        val s1 = ScriptRunner.advance(strict, run(strict), 0) { false }
        val s2 = ScriptRunner.advance(strict, s1.run!!, 60_000) { false }
        assertTrue(ids(s2.out).isEmpty()); assertNull(s2.run)
    }

    @Test fun `if picks then or else branch`() {
        val iff = AutoAction(ActionKind.IF, cond = CondSet(listOf(Condition(ConditionKind.DARK, ""))),
            then = listOf(AutoAction(ActionKind.TURN_ON, "yes")), otherwise = listOf(AutoAction(ActionKind.TURN_ON, "no")))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.NOTIFY), listOf(iff, AutoAction(ActionKind.TURN_OFF, "after")))
        assertEquals(listOf("", "yes", "after"), ids(ScriptRunner.advance(a, run(a), 0) { true }.out))
        assertEquals(listOf("", "no", "after"), ids(ScriptRunner.advance(a, run(a), 0) { false }.out))
    }

    @Test fun `repeat runs count times and stops at the safety limit`() {
        val body = listOf(AutoAction(ActionKind.TOGGLE, "t"))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.NOTIFY), listOf(AutoAction(ActionKind.REPEAT, count = 3, then = body)))
        assertEquals(3, ids(ScriptRunner.advance(a, run(a), 0) { true }.out).count { it == "t" })
        // "Sağlandıkça" ve hep doğru: sınırda kesilir, telefon kilitlenmez.
        val forever = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.NOTIFY),
            listOf(AutoAction(ActionKind.REPEAT, count = 0, cond = CondSet(listOf(Condition(ConditionKind.DARK, ""))), then = body)))
        val res = ScriptRunner.advance(forever, run(forever), 0) { true }
        assertNull(res.run)
        assertTrue(res.out.any { it is ScriptRunner.Out.Note && it.result == StepResult.LIMIT })
        assertTrue(ids(res.out).count { it == "t" } <= ScriptRunner.MAX_REPEAT)
    }

    @Test fun `repeat with delay inside survives a restart`() {
        val body = listOf(AutoAction(ActionKind.TOGGLE, "t"), AutoAction(ActionKind.DELAY, seconds = 10))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.NOTIFY), listOf(AutoAction(ActionKind.REPEAT, count = 2, then = body), AutoAction(ActionKind.TURN_OFF, "end")))
        var r = ScriptRunner.advance(a, run(a), 0) { true }
        val all = ids(r.out).toMutableList()
        while (r.run != null) {
            val saved = ScriptRunner.fromJson(ScriptRunner.toJson(listOf(r.run!!))).single()
            r = ScriptRunner.advance(a, saved, saved.resumeAt) { true }
            all += ids(r.out)
        }
        assertEquals(listOf("", "t", "t", "end"), all)
    }

    @Test fun `stop ends the run and run automation respects depth`() {
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "a"),
            listOf(AutoAction(ActionKind.RUN_AUTOMATION, "other"), AutoAction(ActionKind.STOP), AutoAction(ActionKind.TURN_ON, "never")))
        val res = ScriptRunner.advance(a, run(a), 0) { true }
        assertEquals(listOf("a"), ids(res.out))
        assertEquals("other", res.out.filterIsInstance<ScriptRunner.Out.Start>().single().automationId)
        val deep = ScriptRunner.start(a, 0, "d", depth = ScriptRunner.MAX_DEPTH - 1)
        assertTrue(ScriptRunner.advance(a, deep, 0) { true }.out.none { it is ScriptRunner.Out.Start })
    }

    @Test fun `step numbers follow flat order`() {
        val iff = AutoAction(ActionKind.IF, cond = CondSet(), then = listOf(AutoAction(ActionKind.TURN_ON, "t1")), otherwise = listOf(AutoAction(ActionKind.TURN_ON, "e1")))
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "first"), listOf(iff, AutoAction(ActionKind.TURN_OFF, "last")))
        val flat = a.flatActions.map { it.deviceId.ifEmpty { it.kind.name } }
        assertEquals(listOf("first", "IF", "t1", "e1", "last"), flat)
        val steps = ScriptRunner.advance(a, run(a), 0) { false }.out.filterIsInstance<ScriptRunner.Out.Do>().map { it.step to it.action.deviceId }
        assertEquals(listOf(0 to "first", 3 to "e1", 4 to "last"), steps)
    }

    @Test fun `edited rule with vanished position ends quietly`() {
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "a"), listOf(AutoAction(ActionKind.DELAY, seconds = 5), AutoAction(ActionKind.TURN_OFF, "b")))
        val waiting = ScriptRunner.advance(a, run(a), 0) { true }.run!!
        val shorter = a.copy(extraActions = emptyList())
        val res = ScriptRunner.advance(shorter, waiting, 10_000) { true }
        assertNull(res.run); assertTrue(ids(res.out).isEmpty())
    }

    @Test fun `legacy action steps become runs`() {
        val runs = ScriptRunner.fromLegacy(listOf(ActionSteps.Due("r", 1, 5000), ActionSteps.Due("r", 2, 9000)))
        val r = runs.single()
        assertEquals(5000L, r.resumeAt)
        val a = rule(Trigger(TriggerKind.TIME, ""), AutoAction(ActionKind.TURN_ON, "a"),
            listOf(AutoAction(ActionKind.TURN_OFF, "x", waitMinutes = 1), AutoAction(ActionKind.TURN_OFF, "y", waitMinutes = 1), AutoAction(ActionKind.TURN_OFF, "z", waitMinutes = 1)))
        // Kayıtlı vakitte ikinci ek eylem (y) çalışır, sonra z için bekler.
        val res = ScriptRunner.advance(a, r, 5000) { true }
        assertEquals(listOf("y"), ids(res.out))
        assertEquals(5000L + 60_000L, res.run!!.resumeAt)
    }

    @Test fun `run policy decisions`() {
        assertEquals(RunPolicy.Decision.START_CANCEL_OTHERS, RunPolicy.decide(RunMode.RESTART, 3, 1, 0))
        assertEquals(RunPolicy.Decision.SKIP, RunPolicy.decide(RunMode.SINGLE, 3, 1, 0))
        assertEquals(RunPolicy.Decision.START, RunPolicy.decide(RunMode.SINGLE, 3, 0, 0))
        assertEquals(RunPolicy.Decision.QUEUE, RunPolicy.decide(RunMode.QUEUED, 3, 1, 1))
        assertEquals(RunPolicy.Decision.SKIP, RunPolicy.decide(RunMode.QUEUED, 3, 1, 2))
        assertEquals(RunPolicy.Decision.START, RunPolicy.decide(RunMode.PARALLEL, 3, 2, 0))
        assertEquals(RunPolicy.Decision.SKIP, RunPolicy.decide(RunMode.PARALLEL, 3, 3, 0))
    }

    // ------------------------------------------------------------ koşullar

    @Test fun `condition groups, negation and unknown values`() {
        val devices = mapOf("a" to dev("a", on = true), "b" to dev("b", on = false), "c" to dev("c", on = true))
        val ctx = CondCtx()
        // a açık VE (b açık VEYA c açık)
        val set = CondSet(listOf(
            Condition(ConditionKind.IS_ON, "a"),
            Condition(ConditionKind.IS_ON, "b", group = 1),
            Condition(ConditionKind.IS_ON, "c", group = 1),
        ), ConditionMatch.ALL, mapOf(1 to ConditionMatch.ANY))
        assertTrue(set.ok(devices, ctx))
        assertFalse(set.copy(groupMatch = mapOf(1 to ConditionMatch.ALL)).ok(devices, ctx))
        assertTrue(Condition(ConditionKind.IS_ON, "b", negate = true).holds(devices, ctx))
        // Bilinmeyen (cihaz yok) "değil"de de sağlanmaz.
        assertFalse(Condition(ConditionKind.IS_ON, "missing", negate = true).holds(devices, ctx))
        assertTrue(CondSet().ok(devices, ctx))
    }

    @Test fun `deviceless conditions use the context`() {
        val none = emptyMap<String, DeviceSnapshot>()
        assertTrue(Condition(ConditionKind.DARK, "").holds(none, CondCtx(dark = true)))
        assertFalse(Condition(ConditionKind.LIGHT, "").holds(none, CondCtx(dark = true)))
        assertFalse(Condition(ConditionKind.DARK, "").holds(none, CondCtx(dark = null)))
        assertTrue(Condition(ConditionKind.AWAY, "").holds(none, CondCtx(away = true)))
        assertTrue(Condition(ConditionKind.TRIGGERED_BY, "", channel = 1).holds(none, CondCtx(triggerIndex = 1)))
        assertFalse(Condition(ConditionKind.TRIGGERED_BY, "", channel = 1).holds(none, CondCtx(triggerIndex = null)))
        val noon = java.util.Calendar.getInstance().apply { set(2026, 10, 25, 12, 0, 0) }.timeInMillis
        assertTrue(Condition(ConditionKind.TIME_BETWEEN, "", start = 11 * 60, end = 13 * 60).holds(none, CondCtx(now = noon)))
        assertFalse(Condition(ConditionKind.TIME_BETWEEN, "", start = 22 * 60, end = 6 * 60).holds(none, CondCtx(now = noon)))
    }

    @Test fun `on for uses the engine since time`() {
        val devices = mapOf("l" to dev("l", on = true))
        val c = Condition(ConditionKind.ON_FOR, "l", value = 10.0)
        assertTrue(c.holds(devices, CondCtx(now = 11 * 60_000L, sinceOf = { _, _ -> 0L })))
        assertFalse(c.holds(devices, CondCtx(now = 5 * 60_000L, sinceOf = { _, _ -> 0L })))
        assertFalse(Condition(ConditionKind.OFF_FOR, "l", value = 1.0).holds(devices, CondCtx(now = 99 * 60_000L, sinceOf = { _, _ -> 0L })))
    }

    // ------------------------------------------------------------ çoklu tetikleyici

    @Test fun `extra triggers fire with their index and share the loop guard`() {
        val a = Automation("m", "Çoklu", true, Trigger(TriggerKind.TURNED_ON, "a"), AutoAction(ActionKind.NOTIFY),
            extraTriggers = listOf(Trigger(TriggerKind.TURNED_ON, "b")), loopGuard = 2)
        val units = a.units()
        assertEquals(listOf("m", "m~1"), units.map { it.id })
        assertEquals(listOf(0, 1), units.map { it.triggerIndex })
        val locked = mutableListOf<String>()
        val e = AutomationEngine().apply { onLockout = { locked += it.id } }
        fun flip(id: String, at: Long): List<Automation> {
            val off = mapOf("a" to dev("a", false), "b" to dev("b", false))
            val on = off + (id to dev(id, true))
            return e.onStates(off, on, units, at)
        }
        assertEquals(0, flip("a", 100_000).single().triggerIndex)
        assertEquals(1, flip("b", 200_000).single().triggerIndex)
        // Üçüncü çalışma (iki tetikleyici birlikte sayılır) kilitlenir.
        assertTrue(flip("a", 300_000).isEmpty())
        assertEquals(listOf("m"), locked)
    }

    @Test fun `busy rule is not run and not counted`() {
        val a = Automation("s", "Tek", true, Trigger(TriggerKind.TURNED_ON, "a"), AutoAction(ActionKind.NOTIFY), loopGuard = 2)
        val traces = mutableListOf<TraceOutcome>()
        val e = AutomationEngine().apply { busy = { true }; onTrace = { traces += it.outcome } }
        val off = mapOf("a" to dev("a", false)); val on = mapOf("a" to dev("a", true))
        assertTrue(e.onStates(off, on, listOf(a), 100_000).isEmpty())
        assertEquals(TraceOutcome.BUSY, traces.last())
    }

    // ------------------------------------------------------------ JSON

    @Test fun `new rule fields round trip`() {
        val nested = AutoAction(ActionKind.IF, cond = CondSet(listOf(Condition(ConditionKind.ON_FOR, "a", value = 5.0, negate = true, group = 1)), ConditionMatch.ANY, mapOf(1 to ConditionMatch.ANY)),
            then = listOf(AutoAction(ActionKind.SET_LEVEL, "h", level = 40, color = 0xFF0000)),
            otherwise = listOf(AutoAction(ActionKind.SEND, "NTFY", text = "{device} {value}")))
        val a = Automation("j", "JSON", true, Trigger(TriggerKind.TIME, "", minutes = 420), AutoAction(ActionKind.TOGGLE, "a"),
            extraActions = listOf(AutoAction(ActionKind.WAIT_UNTIL, cond = CondSet(listOf(Condition(ConditionKind.DARK, ""))), seconds = 90, stopOnTimeout = true), nested,
                AutoAction(ActionKind.COUNTER_ADD, "helper:c", count = -2)),
            extraTriggers = listOf(Trigger(TriggerKind.SUNSET, "", minutes = -15)),
            conditions = listOf(Condition(ConditionKind.TIME_BETWEEN, "", start = 60, end = 120, days = 3), Condition(ConditionKind.IS_ON, "b", group = 2)),
            groupMatch = mapOf(2 to ConditionMatch.ANY), mode = RunMode.QUEUED, maxRuns = 4)
        val back = Automation.listFromJson(Automation.listToJson(listOf(a))).single()
        assertEquals(a, back)
    }

    @Test fun `2_54 json still reads the same`() {
        val old = """[{"id":"o","name":"Eski","enabled":true,"delay":0,"trigger":{"kind":"POWER_ABOVE","device":"p","channel":0,"minutes":0,"watts":2000,"grace":0},
            "action":{"kind":"SAVE_STATE","device":"p","channel":0},"more":[{"kind":"TURN_OFF","device":"p","channel":0,"wait":0},{"kind":"RESTORE_STATE","device":"","channel":0,"wait":15}],"guard":3}]"""
        val a = Automation.listFromJson(old).single()
        assertEquals(RunMode.RESTART, a.mode)
        assertTrue(a.extraTriggers.isEmpty())
        assertEquals(15, a.extraActions[1].waitMinutes)
        assertEquals(3, a.loopGuard)
    }

    @Test fun `unknown condition drops the rule instead of running it without the condition`() {
        val json = """[{"id":"u","name":"Yeni","enabled":true,"trigger":{"kind":"TIME","device":""},"action":{"kind":"NOTIFY"},"conds":[{"kind":"FUTURE_KIND","device":""}]}]"""
        assertTrue(Automation.listFromJson(json).isEmpty())
    }

    @Test fun `unknown extra step is skipped like before`() {
        val json = """[{"id":"u","name":"Yeni","enabled":true,"trigger":{"kind":"TIME","device":""},"action":{"kind":"NOTIFY"},"more":[{"kind":"FUTURE_STEP"},{"kind":"TURN_ON","device":"a"}]}]"""
        assertEquals(listOf(ActionKind.TURN_ON), Automation.listFromJson(json).single().extraActions.map { it.kind })
    }

    /** 2.54 / bilgisayar 0.18 okuyucusu: eylem türü eski listede yoksa kuralı atar; "neg", "grp" alanlarını tanımaz. */
    private fun legacyLoads(json: String): Boolean {
        val legacy = setOf("TURN_ON", "TURN_OFF", "ALARM", "NOTIFY", "RUN_SCENE", "START_CYCLE", "STOP_CYCLE", "SAVE_STATE", "RESTORE_STATE")
        val o = JSONArray(json).getJSONObject(0)
        return o.getJSONObject("action").getString("kind") in legacy
    }

    @Test fun `old versions refuse rules they would run differently`() {
        val base = Automation("c", "Uyum", true, Trigger(TriggerKind.TURNED_ON, "door"), AutoAction(ActionKind.TURN_ON, "lamp"),
            conditions = listOf(Condition(ConditionKind.IS_ON, "a")))
        // Sade kural eskisi gibi yazılır (eski sürüm okur).
        val plain = Automation.listToJson(listOf(base))
        assertTrue(legacyLoads(plain))
        assertFalse(org.json.JSONArray(plain).getJSONObject(0).has("action2"))
        val unsafe = listOf(
            base.copy(conditions = listOf(Condition(ConditionKind.IS_ON, "a", negate = true))),
            base.copy(conditions = listOf(Condition(ConditionKind.IS_ON, "a", group = 1), Condition(ConditionKind.IS_ON, "b", group = 1))),
            base.copy(conditions = listOf(Condition(ConditionKind.DARK, ""))),
            base.copy(extraTriggers = listOf(Trigger(TriggerKind.TURNED_ON, "other"))),
            base.copy(mode = RunMode.SINGLE),
            base.copy(extraActions = listOf(AutoAction(ActionKind.STOP), AutoAction(ActionKind.TURN_OFF, "lamp"))),
            base.copy(extraActions = listOf(AutoAction(ActionKind.DELAY, seconds = 30), AutoAction(ActionKind.TURN_OFF, "lamp"))),
        )
        for (a in unsafe) {
            val json = Automation.listToJson(listOf(a))
            assertFalse("eski sürüm yüklememeli: $a", legacyLoads(json))
            // Yeni sürüm aynen okur.
            assertEquals(a, Automation.listFromJson(json).single())
        }
    }

    // ------------------------------------------------------------ yardımcılar

    @Test fun `helpers switch, expire, count and round trip`() {
        val t = Helper("t", "Zamanlayıcı", HelperKind.TIMER, minutes = 5)
        val on = Helpers.switch(t, true, 1000)
        assertEquals(1000 + 5 * 60_000L, on.endsAt)
        val list = listOf(on, Helper("c", "Sayaç", HelperKind.COUNTER, value = 2), Helper("v", "Tatil", HelperKind.TOGGLE))
        assertTrue(Helpers.expire(list, 2000) === list)
        val expired = Helpers.expire(list, on.endsAt)
        assertFalse(expired[0].on)
        assertEquals(5, Helpers.count(list[1], add = 3).value)
        assertEquals(0, Helpers.count(list[1], set = 0).value)
        assertEquals(list, Helpers.fromJson(Helpers.toJson(list)))
        assertEquals(2.0, list[1].snapshot().sensors[SensorKind.COUNT])
        assertEquals(mapOf(0 to true), list[0].snapshot().switches)
        assertEquals(Cloud.HELPER, list[2].snapshot().cloud)
    }

    @Test fun `helper toggle fires turned on rules`() {
        val h = Helper("v", "Tatil", HelperKind.TOGGLE)
        val a = Automation("h", "Tatil açılınca", true, Trigger(TriggerKind.TURNED_ON, h.deviceId), AutoAction(ActionKind.NOTIFY))
        val e = AutomationEngine()
        val before = mapOf(h.deviceId to h.snapshot())
        val after = mapOf(h.deviceId to Helpers.switch(h, true, 0).snapshot())
        assertEquals(1, e.onStates(before, after, listOf(a), 100_000).size)
    }

    // ------------------------------------------------------------ düzenleyici yolu, ışık, metin

    @Test fun `step tree update, append, move and remove`() {
        val list = listOf(AutoAction(ActionKind.IF, cond = CondSet(), then = listOf(AutoAction(ActionKind.TURN_ON, "a"))), AutoAction(ActionKind.TURN_OFF, "z"))
        val p = StepTree.Path.parse("0.t.0")!!
        assertEquals("a", StepTree.get(list, p)?.deviceId)
        val (added, np) = StepTree.append(list, StepTree.Path(0), 't', AutoAction(ActionKind.TURN_ON, "b"))
        assertEquals("0.t.1", np.toString())
        val (moved, mp) = StepTree.move(added, np, -1)
        assertEquals("0.t.0", mp.toString())
        assertEquals(listOf("b", "a"), StepTree.get(moved, StepTree.Path(0))!!.then.map { it.deviceId })
        val removed = StepTree.remove(moved, mp)
        assertEquals(listOf("a"), StepTree.get(removed, StepTree.Path(0))!!.then.map { it.deviceId })
        val changed = StepTree.update(removed, p) { it.copy(deviceId = "c") }
        assertEquals("c", StepTree.get(changed, p)?.deviceId)
        assertNull(StepTree.Path.parse("0.x.1"))
    }

    @Test fun `light level bodies`() {
        val (x, y) = LightLevel.rgbToXy(0xFF0000)
        assertTrue(x > 0.6 && y < 0.35)
        assertEquals(Triple(0, 100, 100), LightLevel.rgbToHsv(0xFF0000))
        assertEquals(120, LightLevel.rgbToHsv(0x00FF00).first)
        assertEquals("/light/salon/turn_on?brightness=128&r=255&g=0&b=0", LightLevel.esphomePath("light/salon", 50, 0xFF0000))
        val ha = org.json.JSONObject(LightLevel.haBody("light.salon", 30, null))
        assertEquals(30, ha.getInt("brightness_pct")); assertFalse(ha.has("rgb_color"))
        val hue = org.json.JSONObject(LightLevel.hueBody(100, null))
        assertEquals(254, hue.getInt("bri")); assertTrue(hue.getBoolean("on"))
        val tapo = org.json.JSONObject(LightLevel.tapoBody(null, 0x0000FF)).getJSONObject("params")
        assertEquals(240, tapo.getInt("hue")); assertFalse(tapo.has("brightness"))
        assertTrue(LightLevel.supports(LocalDevice("h", "Lamba", brand = LocalDevice.BRAND_HA, extra = "light.salon")))
        assertFalse(LightLevel.supports(LocalDevice("h", "Priz", brand = LocalDevice.BRAND_HA, extra = "switch.priz")))
        assertFalse(LightLevel.supports(LocalDevice("s", "Sonoff")))
    }

    @Test fun `text template fills placeholders`() {
        val v = TextTemplate.Vars("Dondurucu", "Derin dondurucu", "3 W", "07:30", "25.11.2026")
        assertEquals("Derin dondurucu: 3 W (07:30)", TextTemplate.render("{device}: {değer} ({saat})", v))
        assertEquals("{unknown} Dondurucu", TextTemplate.render("{unknown} {kural}", v))
        assertEquals("düz metin", TextTemplate.render("düz metin", v))
    }

    @Test fun `trace entry keeps trigger index`() {
        val e = TraceEntry("r", 1, TraceOutcome.MANUAL, trigger = 2)
        val back = AutomationTraces.fromJson(AutomationTraces.toJson(listOf(e))).single()
        assertEquals(2, back.trigger)
        assertFalse(TraceOutcome.MANUAL.blocked)
        assertTrue(TraceOutcome.BUSY.blocked)
        assertEquals(1, JSONArray(AutomationTraces.toJson(listOf(e))).length())
    }
}
