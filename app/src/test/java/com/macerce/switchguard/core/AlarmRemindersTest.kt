package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRemindersTest {
    private val min = 60_000L
    private val every = 30 * min
    private val off = Change.Switch("r", "Rögar", 0, multiChannel = true, from = true, to = false)
    private val drop = Change.Connection("p", "Pompa", from = true, to = false)

    private fun rogar(on: Boolean, online: Boolean = true) =
        DeviceSnapshot("r", "Rögar", online, mapOf(0 to on, 1 to false))
    private fun pompa(online: Boolean) = DeviceSnapshot("p", "Pompa", online, mapOf(0 to true))
    private fun state(vararg d: DeviceSnapshot) = d.associateBy { it.id }

    @Test
    fun `alarm kapatildiktan sonra durum surerse araliklarla hatirlatir`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        r.onDismiss(now = 1 * min, intervalMs = every)
        assertTrue(r.due(state(rogar(false)), now = 20 * min, intervalMs = every, alarmActive = false).isEmpty())
        val first = r.due(state(rogar(false)), now = 31 * min, intervalMs = every, alarmActive = false)
        assertEquals(1, first.size)
        assertEquals(off, first[0].change)
        assertEquals(31 * min, first[0].durationMs)
        // Hatırlatma çalarken (kapatılmadan) yenisi gelmez; kapatılınca sayaç yeniden başlar.
        assertTrue(r.due(state(rogar(false)), now = 70 * min, intervalMs = every, alarmActive = true).isEmpty())
        r.onDismiss(now = 72 * min, intervalMs = every)
        assertTrue(r.due(state(rogar(false)), now = 100 * min, intervalMs = every, alarmActive = false).isEmpty())
        assertEquals(1, r.due(state(rogar(false)), now = 102 * min, intervalMs = every, alarmActive = false).size)
    }

    @Test
    fun `durum duzelince hatirlatma biter`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        r.onDismiss(now = 1 * min, intervalMs = every)
        assertTrue(r.due(state(rogar(true)), now = 31 * min, intervalMs = every, alarmActive = false).isEmpty())
        // Sonra yeniden kapansa da eski alarmın hatırlatması dönmez (yeni değişiklik kendi alarmını çalar).
        assertTrue(r.due(state(rogar(false)), now = 90 * min, intervalMs = every, alarmActive = false).isEmpty())
    }

    @Test
    fun `kopma suruyorsa hatirlatir, cihaz donunce biter`() {
        val r = AlarmReminders()
        r.onAlarm(drop, now = 0)
        r.onDismiss(now = 0, intervalMs = every)
        assertEquals(1, r.due(state(pompa(false)), now = 30 * min, intervalMs = every, alarmActive = false).size)
        r.onDismiss(now = 31 * min, intervalMs = every)
        assertTrue(r.due(state(pompa(true)), now = 70 * min, intervalMs = every, alarmActive = false).isEmpty())
    }

    @Test
    fun `kapatilmamis alarm hatirlatma uretmez`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        assertTrue(r.due(state(rogar(false)), now = 120 * min, intervalMs = every, alarmActive = false).isEmpty())
    }

    @Test
    fun `hatirlatma kapaliysa hic uretmez`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        r.onDismiss(now = 1 * min, intervalMs = 0)
        assertTrue(r.due(state(rogar(false)), now = 600 * min, intervalMs = 0, alarmActive = false).isEmpty())
    }

    @Test
    fun `listeden kalkan cihazin hatirlatmasi biter`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        r.onDismiss(now = 0, intervalMs = every)
        assertTrue(r.due(emptyMap(), now = 31 * min, intervalMs = every, alarmActive = false).isEmpty())
        assertTrue(r.due(state(rogar(false)), now = 62 * min, intervalMs = every, alarmActive = false).isEmpty())
    }

    @Test
    fun `durum kaydedilip geri yuklenince devam eder`() {
        val r = AlarmReminders()
        r.onAlarm(off, now = 0)
        r.onAlarm(drop, now = 2 * min)
        r.onDismiss(now = 3 * min, intervalMs = every)
        val back = AlarmReminders.fromJson(r.toJson())
        assertEquals(2, back.due(state(rogar(false), pompa(false)), now = 33 * min, intervalMs = every, alarmActive = false).size)
        assertTrue(AlarmReminders.fromJson("bozuk").due(state(rogar(false)), now = 99 * min, intervalMs = every, alarmActive = false).isEmpty())
    }
}
