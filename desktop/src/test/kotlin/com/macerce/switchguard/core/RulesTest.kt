package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    @Test
    fun `varsayilan kurallar - online olma sadece bildirim, digerleri alarm`() {
        val r = DeviceRules()
        assertEquals(AlertAction.ALARM, r.actionFor(EventType.TURNED_OFF))
        assertEquals(AlertAction.ALARM, r.actionFor(EventType.TURNED_ON))
        assertEquals(AlertAction.ALARM, r.actionFor(EventType.WENT_OFFLINE))
        assertEquals(AlertAction.NOTIFY, r.actionFor(EventType.CAME_ONLINE))
    }

    @Test
    fun `gece yarisini asan sessiz saatler`() {
        val q = QuietHours(enabled = true, startMinute = 23 * 60, endMinute = 7 * 60)
        assertTrue(q.contains(23 * 60 + 30))
        assertTrue(q.contains(3 * 60))
        assertFalse(q.contains(7 * 60))
        assertFalse(q.contains(12 * 60))
    }

    @Test
    fun `ayni gun icindeki sessiz saatler`() {
        val q = QuietHours(enabled = true, startMinute = 13 * 60, endMinute = 14 * 60)
        assertTrue(q.contains(13 * 60 + 5))
        assertFalse(q.contains(14 * 60 + 5))
    }

    @Test
    fun `kapali sessiz saat hicbir zaman aktif degil`() {
        assertFalse(QuietHours(enabled = false, startMinute = 0, endMinute = 1439).contains(100))
    }

    @Test
    fun `sessiz saatte alarm bildirime duser, digerleri degismez`() {
        assertEquals(AlertAction.NOTIFY, RuleEngine.effective(AlertAction.ALARM, quiet = true))
        assertEquals(AlertAction.ALARM, RuleEngine.effective(AlertAction.ALARM, quiet = false))
        assertEquals(AlertAction.IGNORE, RuleEngine.effective(AlertAction.IGNORE, quiet = true))
    }

    @Test
    fun `kurallar json gidis donus`() {
        val rules = mapOf(
            "a" to DeviceRules(monitored = false, actions = mapOf(EventType.TURNED_ON to AlertAction.IGNORE)),
            "b" to DeviceRules(),
            "c" to DeviceRules(soundUri = "content://media/internal/audio/media/42"),
        )
        assertEquals(rules, DeviceRules.mapFromJson(DeviceRules.mapToJson(rules)))
    }

    @Test
    fun `degisiklik olay turune donusur`() {
        assertEquals(EventType.TURNED_OFF, Change.Switch("a", "A", 0, false, from = true, to = false).eventType)
        assertEquals(EventType.CAME_ONLINE, Change.Connection("a", "A", from = false, to = true).eventType)
    }
}
