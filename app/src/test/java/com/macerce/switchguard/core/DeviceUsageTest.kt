package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceUsageTest {
    private fun rule(id: String, trigger: Trigger, vararg steps: AutoAction) =
        Automation(id, id, true, trigger, steps.first(), extraActions = steps.drop(1))

    private val time = Trigger(TriggerKind.TIME, "", minutes = 420, days = 127)
    private val group = DeviceGroup("g1", "Koridor", listOf(GroupMember("lamp", 0), GroupMember("other", 0)))
    private val scene = Scene("s1", "Gece", listOf(SceneAction("lamp", 0, false)))

    @Test
    fun findsDirectGroupSceneAndWatchingRules() {
        val rules = listOf(
            rule("direct", time, AutoAction(ActionKind.TURN_ON, "lamp")),
            rule("viaGroup", time, AutoAction(ActionKind.TURN_OFF, group.deviceId)),
            rule("viaScene", time, AutoAction(ActionKind.RUN_SCENE, "s1")),
            rule("nested", time, AutoAction(ActionKind.IF, then = listOf(AutoAction(ActionKind.TOGGLE, "lamp")))),
            rule("watch", Trigger(TriggerKind.TURNED_ON, "lamp"), AutoAction(ActionKind.TURN_OFF, "other")),
            rule("unrelated", time, AutoAction(ActionKind.TURN_ON, "other")),
        )
        val r = DeviceUsage.find("lamp", rules, listOf(scene), emptyList(), listOf(group))
        assertEquals(setOf("direct", "viaGroup", "viaScene", "nested", "watch"), r.rules.map { it.rule.id }.toSet())
        assertTrue(r.rules.first { it.rule.id == "watch" }.let { it.watches && !it.switches })
        assertEquals(listOf("Gece"), r.scenes.map { it.first.name })
        assertEquals(listOf("Koridor"), r.groups.map { it.name })
    }

    @Test
    fun timerOwner() {
        val a = rule("abc", time, AutoAction(ActionKind.TURN_ON, "lamp"))
        assertEquals(a, DeviceUsage.timerRule(RuleToDevice.timerId("abc", 0), listOf(a)))
        assertEquals(null, DeviceUsage.timerRule(RuleToDevice.timerId("xyz", 0), listOf(a)))
        assertTrue(DeviceUsage.isAppTimer(RuleToDevice.timerId("xyz", 0)))
    }
}
