package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleToDeviceTest {
    private val dev = mapOf(
        "a" to DeviceSnapshot("a", "Bahçe", true, mapOf(0 to false)),
        "t" to DeviceSnapshot("t", "Tuya", true, mapOf(0 to false), cloud = Cloud.TUYA),
    )
    // Hafta içi (pzt–cum) 23:50.
    private val weekdays = 0b0011111
    private fun rule(vararg steps: AutoAction, days: Int = weekdays, conds: List<Condition> = emptyList()) = Automation(
        "rule-1", "Sulama", true, Trigger(TriggerKind.TIME, "", minutes = 23 * 60 + 50, days = days),
        steps.first(), extraActions = steps.drop(1), conditions = conds,
    )

    @Test
    fun onThenOffAfterWaitCrossesMidnight() {
        val p = RuleToDevice.plan(rule(AutoAction(ActionKind.TURN_ON, "a"), AutoAction(ActionKind.DELAY, seconds = 20 * 60), AutoAction(ActionKind.TURN_OFF, "a")), dev)!!
        assertEquals(2, p.size)
        assertEquals(23, p[0].timer.hour); assertEquals(50, p[0].timer.minute)
        assertEquals(setOf(1, 2, 3, 4, 5), p[0].timer.days)
        // 00:10 ertesi gün: salı–cumartesi.
        assertEquals(0, p[1].timer.hour); assertEquals(10, p[1].timer.minute)
        assertEquals(setOf(2, 3, 4, 5, 6), p[1].timer.days)
        assertEquals(false, p[1].timer.on)
        assertEquals(RuleToDevice.timerId("rule-1", 2), p[1].timer.id)
    }

    @Test
    fun notEligible() {
        assertNull(RuleToDevice.plan(rule(AutoAction(ActionKind.TURN_ON, "t")), dev))
        assertNull(RuleToDevice.plan(rule(AutoAction(ActionKind.ALARM)), dev))
        assertNull(RuleToDevice.plan(rule(AutoAction(ActionKind.TURN_ON, "a"), conds = listOf(Condition(ConditionKind.IS_ON, "a"))), dev))
        assertNull(RuleToDevice.plan(rule(AutoAction(ActionKind.TURN_ON, "a"), AutoAction(ActionKind.DELAY, seconds = 30)), dev))
    }

    @Test
    fun ownsOnlyTimersOfThatRule() {
        assert(RuleToDevice.owns(RuleToDevice.timerId("rule-1", 7), "rule-1"))
        assert(!RuleToDevice.owns(RuleToDevice.timerId("rule-12", 0), "rule-1"))
        assert(!RuleToDevice.owns("3f2a-uuid", "rule-1"))
    }
}
