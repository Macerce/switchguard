package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleShareTest {
    private val rule = Automation(
        id = "r1", name = "Pompa durdu", enabled = true,
        trigger = Trigger(TriggerKind.POWER_BELOW, "pompa-a", watts = 50.0, minutes = 5),
        action = AutoAction(ActionKind.ALARM),
        extraActions = listOf(
            AutoAction(ActionKind.IF, cond = CondSet(listOf(Condition(ConditionKind.IS_ON, "isik-a"))),
                then = listOf(AutoAction(ActionKind.TURN_OFF, "isik-a"))),
        ),
    )

    @Test
    fun roundTripSameIds() {
        val code = RuleShare.encode(rule, mapOf("pompa-a" to "Pompa", "isik-a" to "Bahçe ışığı"))
        assertTrue(code.startsWith(RuleShare.PREFIX))
        val r = RuleShare.decode("Bak: $code teşekkürler", mapOf("pompa-a" to "Pompa", "isik-a" to "Bahçe ışığı"), newId = "x")!!
        assertEquals("x", r.rule.id)
        assertFalse(r.rule.enabled)
        assertEquals(0, r.unresolved)
        assertEquals("pompa-a", r.rule.trigger.deviceId)
        assertEquals(50.0, r.rule.trigger.watts, 0.0)
        assertEquals(ActionKind.IF, r.rule.extraActions.single().kind)
    }

    @Test
    fun remapsByNameAndCountsMissing() {
        val code = RuleShare.encode(rule, mapOf("pompa-a" to "Pompa", "isik-a" to "Bahçe ışığı"))
        val r = RuleShare.decode(code, mapOf("pompa-b" to " pompa "), newId = "y")!!
        assertEquals("pompa-b", r.rule.trigger.deviceId)
        assertEquals(1, r.renamed)
        assertEquals(1, r.unresolved)
        val inner = r.rule.extraActions.single()
        assertEquals("isik-a", inner.then.single().deviceId)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(RuleShare.decode("merhaba", emptyMap()))
        assertNull(RuleShare.decode(RuleShare.PREFIX + "AAAA", emptyMap()))
    }
}
