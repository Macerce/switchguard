package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscalationTest {
    private val min5 = 5 * 60_000L

    @Test
    fun `sure dolunca bir kez`() {
        assertFalse(Escalation.due(realAlarmAt = 1_000, escalatedFor = 0, now = 1_000 + min5 - 1, delayMs = min5))
        assertTrue(Escalation.due(1_000, 0, 1_000 + min5, min5))
        // Aynı alarm için gönderildi: tekrar yok. Yeni alarm (farklı başlangıç) yeniden sayılır.
        assertFalse(Escalation.due(1_000, 1_000, 1_000 + 10 * min5, min5))
        assertTrue(Escalation.due(9_000_000, 1_000, 9_000_000 + min5, min5))
    }

    @Test
    fun `kapaliyken ya da gercek alarm yokken hic`() {
        assertFalse(Escalation.due(1_000, 0, 1_000 + min5, 0))
        assertFalse(Escalation.due(0, 0, min5 * 10, min5))
        assertNull(Escalation.nextDueAt(0, 0, min5))
        assertNull(Escalation.nextDueAt(1_000, 1_000, min5))
        assertEquals(1_000 + min5, Escalation.nextDueAt(1_000, 0, min5))
    }

    @Test
    fun `mesajda en yeni olay ustte ve uzunluk sinirli`() {
        val text = Escalation.message("Başlık", listOf("eski", "yeni"), "Gönderen: Xiaomi")
        assertEquals("Başlık\n\n• yeni\n• eski\n\nGönderen: Xiaomi", text)
        val many = Escalation.message("H", (1..25).map { "olay $it" }, "")
        assertTrue(many.contains("• olay 25") && many.contains("• +15") && !many.contains("• olay 15\n"))
        assertTrue(Escalation.message("H", listOf("x".repeat(10_000)), "").length <= 3800)
    }

    @Test
    fun `bot anahtari bicimi`() {
        assertTrue(Escalation.isTokenFormat("123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw"))
        assertTrue(Escalation.isTokenFormat("  123456789:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw "))
        assertFalse(Escalation.isTokenFormat("AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw"))
        assertFalse(Escalation.isTokenFormat("123:short"))
    }

    @Test
    fun `getUpdates kisileri ve gruplari cikarir`() {
        val json = """
        {"ok":true,"result":[
          {"update_id":1,"message":{"chat":{"id":111,"first_name":"Ayşe","last_name":"Kaya","type":"private"},"text":"/start"}},
          {"update_id":2,"message":{"chat":{"id":222,"username":"komsu","type":"private"},"text":"/start"}},
          {"update_id":3,"my_chat_member":{"chat":{"id":-333,"title":"Aile","type":"group"},"new_chat_member":{"status":"member"}}},
          {"update_id":4,"message":{"chat":{"id":111,"first_name":"Ayşe","type":"private"},"text":"merhaba"}},
          {"update_id":5,"my_chat_member":{"chat":{"id":-444,"title":"Eski grup","type":"group"},"new_chat_member":{"status":"member"}}},
          {"update_id":6,"my_chat_member":{"chat":{"id":-444,"title":"Eski grup","type":"group"},"new_chat_member":{"status":"left"}}}
        ]}
        """.trimIndent()
        assertEquals(
            listOf(Recipient(222, "@komsu"), Recipient(-333, "Aile"), Recipient(111, "Ayşe")),
            Escalation.chatsFromUpdates(json),
        )
    }

    @Test
    fun `kisi listesi kaydedilip okunur`() {
        val list = listOf(Recipient(1, "Ayşe"), Recipient(-2, "Aile \"grup\""))
        assertEquals(list, Recipient.listFromJson(Recipient.listToJson(list)))
        assertEquals(emptyList<Recipient>(), Recipient.listFromJson("bozuk"))
        assertEquals(emptyList<Recipient>(), Recipient.listFromJson(null))
    }
}
