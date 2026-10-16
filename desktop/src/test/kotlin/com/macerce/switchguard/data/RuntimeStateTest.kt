package com.macerce.switchguard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class RuntimeStateTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file() = tmp.root.toPath().resolve("runtime.json")

    @Test
    fun valuesSurviveARestart() {
        RuntimeState(file()).apply {
            grace = "[1]"
            automationState = "{}"
            reminders = "[]"
            lastAliveAt = 123
            cleanStop = false
            alarmStartedAt = 5
            silentAlarm = true
            realAlarmAt = 7
            escalatedFor = 7
        }
        val r = RuntimeState(file())
        assertEquals("[1]", r.grace)
        assertEquals("{}", r.automationState)
        assertEquals("[]", r.reminders)
        assertEquals(123, r.lastAliveAt)
        assertFalse(r.cleanStop)
        assertEquals(5, r.alarmStartedAt)
        assertTrue(r.silentAlarm)
        assertEquals(7, r.realAlarmAt)
        assertEquals(7, r.escalatedFor)
    }

    @Test
    fun defaultsWhenThereIsNoFile() {
        val r = RuntimeState(file())
        assertNull(r.grace)
        assertEquals(0, r.lastAliveAt)
        assertTrue("a first run is not an unexpected stop", r.cleanStop)
        assertFalse(r.silentAlarm)
    }

    @Test
    fun corruptFileStartsEmptyInsteadOfFailing() {
        Files.writeString(file(), "not json")
        val r = RuntimeState(file())
        assertNull(r.grace)
        r.lastAliveAt = 9
        assertEquals(9, RuntimeState(file()).lastAliveAt)
    }

    @Test
    fun unchangedValueIsNotWrittenAgain() {
        val r = RuntimeState(file())
        r.grace = "[]"
        val before = Files.getLastModifiedTime(file())
        Files.setLastModifiedTime(file(), java.nio.file.attribute.FileTime.fromMillis(1000))
        r.grace = "[]"
        assertEquals(1000, Files.getLastModifiedTime(file()).toMillis())
        assertTrue(before.toMillis() > 1000)
    }

    @Test
    fun clearEngineDropsOnlyThePendingWork() {
        val r = RuntimeState(file())
        r.grace = "[1]"; r.automationState = "{}"; r.reminders = "[2]"; r.lastAliveAt = 4
        r.clearEngine()
        val again = RuntimeState(file())
        assertNull(again.grace)
        assertNull(again.automationState)
        assertNull(again.reminders)
        assertEquals(4, again.lastAliveAt)
    }
}
