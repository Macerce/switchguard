package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthTest {
    private val person = Recipient(111, "Ayşe")
    private val group = Recipient(-1001, "Ev grubu")

    @Test
    fun privateChatKeepsOldBehaviour() {
        assertTrue(TelegramAuth.canControl(person, 111, globalControl = true))
        assertFalse(TelegramAuth.canControl(person, 111, globalControl = false))
        assertFalse(TelegramAuth.canControl(person.copy(control = false), 111, globalControl = true))
    }

    @Test
    fun groupIsClosedByDefault() {
        assertFalse(TelegramAuth.canControl(group, 555, globalControl = true))
    }

    @Test
    fun inAGroupOnlyChosenPeopleCanControl() {
        val open = group.copy(control = true, controllers = setOf(555))

        assertTrue(TelegramAuth.canControl(open, 555, globalControl = true))
        // Gruba sonradan eklenen ya da seçilmemiş üye reddedilir.
        assertFalse(TelegramAuth.canControl(open, 777, globalControl = true))
        assertFalse(TelegramAuth.canControl(open, 0, globalControl = true))
        assertFalse(TelegramAuth.canControl(null, 555, globalControl = true))
    }

    @Test
    fun everyStateChangingCommandGetsTheShortAgeLimit() {
        val changing = listOf(
            ChatCommand.Intent.Switch(false, listOf("salon")),
            ChatCommand.Intent.Batch(emptyList()),
            ChatCommand.Intent.Timed(false, listOf("pompa"), 600, hold = false),
            ChatCommand.Intent.SwitchAll(false),
            ChatCommand.Intent.Confirm(true),
            ChatCommand.Intent.RunCycle(true, "sulama"),
            ChatCommand.Intent.RunScene("gece"),
            ChatCommand.Intent.SetPresence(true),
        )
        changing.forEach { assertEquals(it.toString(), TelegramBot.SWITCH_MAX_AGE_MS, TelegramAuth.maxAge(it)) }
        assertEquals(TelegramBot.STATUS_MAX_AGE_MS, TelegramAuth.maxAge(ChatCommand.Intent.WhatsOn))
        assertEquals(TelegramBot.STATUS_MAX_AGE_MS, TelegramAuth.maxAge(ChatCommand.Intent.Status("")))
    }

    @Test
    fun oldYesIsRejected() {
        // "evet" 3 dk sonra gelirse yaş sınırını aşar.
        assertTrue(3 * 60_000L > TelegramAuth.maxAge(ChatCommand.Intent.Confirm(true)))
    }

    @Test
    fun sameNameInTwoChatsDoesNotShareTheConfirmation() {
        assertNotEquals(TelegramAuth.pendingKey(111, 111), TelegramAuth.pendingKey(-1001, 222))
        assertNotEquals(TelegramAuth.pendingKey(-1001, 222), TelegramAuth.pendingKey(-1002, 222))
        assertEquals(TelegramAuth.pendingKey(-1001, 222), TelegramAuth.pendingKey(-1001, 222))
    }

    @Test
    fun recentGroupSendersAreRemembered() {
        var m = emptyMap<Long, List<Pair<Long, String>>>()
        m = TelegramAuth.rememberSender(m, -1001, 1, "A")
        m = TelegramAuth.rememberSender(m, -1001, 2, "B")
        m = TelegramAuth.rememberSender(m, -1001, 1, "A2")
        m = TelegramAuth.rememberSender(m, 111, 111, "özel sohbet sayılmaz")

        assertEquals(listOf(1L to "A2", 2L to "B"), m[-1001])
        assertFalse(111L in m)
        assertEquals(3, (1..5).fold(m) { acc, i -> TelegramAuth.rememberSender(acc, -5, i.toLong(), "x", max = 3) }[-5]!!.size)
    }

    @Test
    fun recipientControlSurvivesJson() {
        val list = listOf(person, group.copy(control = true, controllers = setOf(5, 7)))

        assertEquals(list, Recipient.listFromJson(Recipient.listToJson(list)))
        // Eski biçim (alan yok): varsayılan.
        assertEquals(listOf(Recipient(9, "Eski")), Recipient.listFromJson("""[{"id":9,"name":"Eski"}]"""))
    }

    @Test
    fun updateCarriesSenderId() {
        val json = """{"ok":true,"result":[{"update_id":5,"message":{"message_id":1,"date":1700000000,"chat":{"id":-1001},"from":{"id":555,"first_name":"Ali"},"text":"/kapat salon"}}]}"""

        val u = TelegramBot.parseUpdates(json).single()

        assertEquals(555L, u.fromId)
        assertEquals("Ali", u.from)
        assertEquals(-1001L, u.chatId)
    }
}
