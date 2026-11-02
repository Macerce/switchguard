package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Kritik alarmda kişinin nasıl aranacağı. */
enum class CallMode { NONE, PHONE }

/**
 * Telegram'da haber verilecek kişi ya da grup. Gruplarda [id] eksi sayıdır.
 * [call]/[callTo]: kritik alarmda telefonla arama ve numara.
 */
data class Recipient(val id: Long, val name: String, val call: CallMode = CallMode.NONE, val callTo: String = "") {
    /** Arama ayarlı ve hedefi geçerli. */
    val calls get() = call != CallMode.NONE && Escalation.isCallTarget(call, callTo)

    companion object {
        fun listToJson(list: List<Recipient>): String = JSONArray(list.map {
            JSONObject().put("id", it.id).put("name", it.name).apply {
                if (it.call != CallMode.NONE) put("call", it.call.name).put("callTo", it.callTo)
            }
        }).toString()

        fun listFromJson(json: String?): List<Recipient> {
            if (json.isNullOrEmpty()) return emptyList()
            return runCatching {
                val a = JSONArray(json)
                (0 until a.length()).map {
                    a.getJSONObject(it).let { o ->
                        val call = runCatching { CallMode.valueOf(o.optString("call")) }.getOrDefault(CallMode.NONE)
                        Recipient(o.getLong("id"), o.optString("name"), call, if (call == CallMode.NONE) "" else o.optString("callTo"))
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}

/**
 * İkinci kişiye haber verme: alarm [delayMs] içinde kapatılmazsa seçilen kişilere Telegram mesajı gider.
 * Her alarm için bir kez; alarm, ilk gerçek olayın zamanıyla ([realAlarmAt]) tanınır. Test alarmları sayılmaz.
 */
object Escalation {
    /** Telegram mesajı en çok 4096 karakter. */
    private const val MAX_CHARS = 3800
    private const val MAX_LINES = 10

    /** Seçilebilecek bekleme süreleri (dakika); [NOW] = alarm çalar çalmaz. */
    const val NOW = 0
    val DELAYS = listOf(NOW, 2, 5, 10, 15, 30)

    fun due(realAlarmAt: Long, escalatedFor: Long, now: Long, delayMs: Long): Boolean =
        delayMs > 0 && realAlarmAt > 0 && escalatedFor != realAlarmAt && now - realAlarmAt >= delayMs

    /** Bir sonraki denetimin zamanı (duvar saati); haber verme gerekmiyorsa null. */
    fun nextDueAt(realAlarmAt: Long, escalatedFor: Long, delayMs: Long): Long? =
        if (delayMs > 0 && realAlarmAt > 0 && escalatedFor != realAlarmAt) realAlarmAt + delayMs else null

    /**
     * Mesaj: başlık, en yeni olay en üstte olacak şekilde son [MAX_LINES] alarm satırı, altta gönderen cihaz.
     * Telegram sınırını aşmasın diye kısaltılır.
     */
    fun message(header: String, lines: List<String>, footer: String): String {
        val recent = lines.reversed().take(MAX_LINES)
        val more = lines.size - recent.size
        val body = buildString {
            append(header)
            append("\n\n")
            recent.forEach { append("• ").append(it).append('\n') }
            if (more > 0) append("• +").append(more).append('\n')
            if (footer.isNotEmpty()) append('\n').append(footer)
        }.trimEnd()
        return if (body.length <= MAX_CHARS) body else body.take(MAX_CHARS - 1) + "…"
    }

    /**
     * Kademeli haber: vakti gelmiş ve henüz haber verilmemiş kişiler. [stages] kişi → dakika ([default] yoksa).
     * [done]: bu alarm için haber verilen ya da vazgeçilenler.
     */
    fun dueRecipients(
        recipients: List<Recipient>, stages: Map<Long, Int>, default: Int, realAlarmAt: Long, done: Set<Long>, now: Long,
    ): List<Recipient> {
        if (realAlarmAt <= 0) return emptyList()
        return recipients.filter { r ->
            val min = stages[r.id] ?: default
            min >= NOW && r.id !in done && now - realAlarmAt >= min * 60_000L
        }
    }

    /** İlk kademenin bekleme süresi (dakika); kişi yoksa null. */
    fun firstDelay(recipients: List<Recipient>, stages: Map<Long, Int>, default: Int): Int? =
        recipients.minOfOrNull { stages[it.id] ?: default }

    /** Arama hedefi: telefon numarası. */
    fun isCallTarget(mode: CallMode, to: String): Boolean {
        val t = to.trim()
        val phone = PHONE.matches(t.replace(" ", ""))
        return when (mode) {
            CallMode.NONE -> false
            CallMode.PHONE -> phone
        }
    }

    private val PHONE = Regex("""^\+?[0-9]{3,15}$""")

    /** Bot anahtarı biçimi: "123456789:AA…" (BotFather'ın verdiği). */
    fun isTokenFormat(token: String): Boolean = TOKEN.matches(token.trim())

    private val TOKEN = Regex("""^\d{5,}:[A-Za-z0-9_-]{30,}$""")

    /**
     * getUpdates yanıtından bota yazan kişileri ve botun eklendiği grupları çıkarır (en yeni bilgiyle, tekrarsız).
     * Kişinin adı yoksa kullanıcı adı, o da yoksa numarası kullanılır.
     */
    fun chatsFromUpdates(json: String): List<Recipient> {
        val result = JSONObject(json).optJSONArray("result") ?: return emptyList()
        val found = linkedMapOf<Long, Recipient>()
        for (i in 0 until result.length()) {
            val u = result.optJSONObject(i) ?: continue
            val chat = sequenceOf("message", "edited_message", "my_chat_member", "channel_post")
                .mapNotNull { u.optJSONObject(it)?.optJSONObject("chat") }
                .firstOrNull() ?: continue
            // Bot gruptan çıkarıldıysa o grup listede kalmasın.
            val status = u.optJSONObject("my_chat_member")?.optJSONObject("new_chat_member")?.optString("status")
            val id = chat.optLong("id")
            if (status == "left" || status == "kicked") { found.remove(id); continue }
            found.remove(id)
            found[id] = Recipient(id, chatName(chat))
        }
        return found.values.toList()
    }

    private fun chatName(chat: JSONObject): String {
        chat.optString("title").takeIf { it.isNotEmpty() }?.let { return it }
        val name = listOf(chat.optString("first_name"), chat.optString("last_name")).filter { it.isNotEmpty() }.joinToString(" ")
        if (name.isNotEmpty()) return name
        chat.optString("username").takeIf { it.isNotEmpty() }?.let { return "@$it" }
        return chat.optLong("id").toString()
    }
}
