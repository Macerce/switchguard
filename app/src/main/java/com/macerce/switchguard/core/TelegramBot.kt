package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Telegram'dan gelen komutlar ve "Gördüm" düğmesi. Yalnızca haber verilecek kişiler listesindeki sohbetlerden
 * gelenler dikkate alınır. Saf: güncellemeler JSON'dan çıkarılır, ağ işi [com.macerce.switchguard.api.TelegramClient]'ta.
 */
object TelegramBot {

    /** getUpdates'ten gelen tek güncelleme: mesaj ya da düğmeye dokunma. [date] saniye değil ms. */
    data class Update(
        val id: Long,
        val chatId: Long,
        val from: String,
        val date: Long,
        val text: String? = null,
        val callbackId: String? = null,
        val callbackData: String? = null,
        /** Düğmenin bulunduğu mesaj (düğmeyi kaldırmak için). */
        val messageId: Long = 0,
    )

    sealed class Command {
        data object Status : Command()
        data object Help : Command()
        /** Çalan alarmı üstlen (düğmenin komut karşılığı). */
        data object Ack : Command()
        data class Switch(val on: Boolean, val query: String) : Command()
        /** /evde, /disarida */
        data class SetPresence(val away: Boolean) : Command()
        /** /sahne ad */
        data class RunScene(val query: String) : Command()
    }

    fun parseUpdates(json: String): List<Update> {
        val result = JSONObject(json).optJSONArray("result") ?: return emptyList()
        return (0 until result.length()).mapNotNull { i -> result.optJSONObject(i)?.let(::parseUpdate) }
    }

    private fun parseUpdate(u: JSONObject): Update? {
        val id = u.optLong("update_id")
        u.optJSONObject("callback_query")?.let { q ->
            val message = q.optJSONObject("message")
            val chat = message?.optJSONObject("chat") ?: return null
            return Update(
                id, chat.optLong("id"), personName(q.optJSONObject("from")), message.optLong("date") * 1000,
                callbackId = q.optString("id"), callbackData = q.optString("data"), messageId = message.optLong("message_id"),
            )
        }
        val m = u.optJSONObject("message") ?: return null
        val chat = m.optJSONObject("chat") ?: return null
        val text = m.optString("text").takeIf { it.isNotEmpty() } ?: return null
        return Update(id, chat.optLong("id"), personName(m.optJSONObject("from")), m.optLong("date") * 1000, text = text)
    }

    private fun personName(from: JSONObject?): String {
        from ?: return ""
        val name = listOf(from.optString("first_name"), from.optString("last_name")).filter { it.isNotEmpty() }.joinToString(" ")
        if (name.isNotEmpty()) return name
        return from.optString("username").takeIf { it.isNotEmpty() }?.let { "@$it" } ?: ""
    }

    /** "/ac Salon 2", "/kapat@bot dondurucu", "/durum". Komut değilse null. */
    fun parseCommand(text: String): Command? {
        val t = text.trim()
        if (!t.startsWith("/")) return null
        val head = t.substringBefore(' ').substringBefore('@').lowercase()
        val rest = t.substringAfter(' ', "").trim()
        return when (DeviceKinds.words(head).firstOrNull()) {
            "durum", "status" -> Command.Status
            "yardim", "help", "start", "komutlar" -> Command.Help
            "gordum", "ack", "tamam" -> Command.Ack
            "ac", "on" -> Command.Switch(true, rest)
            "kapat", "off" -> Command.Switch(false, rest)
            "evde", "evdeyim", "home" -> Command.SetPresence(false)
            "disarida", "disaridayim", "away" -> Command.SetPresence(true)
            "sahne", "scene" -> Command.RunScene(rest)
            else -> null
        }
    }

    sealed class Match {
        data class Found(val device: DeviceSnapshot, val channel: Int) : Match()
        data class Ambiguous(val names: List<String>) : Match()
        data object NotFound : Match()
        /** Çok kanallı cihazda kanal belirtilmedi ya da yok. */
        data class NeedChannel(val device: DeviceSnapshot) : Match()
    }

    /**
     * Cihazı adından bulur (büyük/küçük harf ve Türkçe harf farkı gözetmez): önce tam ad, sonra baştan, sonra içinde.
     * Sondaki sayı çok kanallı cihazda kanal numarasıdır (1'den başlar).
     */
    fun match(query: String, devices: List<DeviceSnapshot>): Match {
        val words = DeviceKinds.words(query)
        if (words.isEmpty()) return Match.NotFound
        val number = words.last().toIntOrNull()
        fun find(q: List<String>): List<DeviceSnapshot> {
            val key = q.joinToString(" ")
            if (key.isEmpty()) return emptyList()
            val named = devices.map { it to DeviceKinds.words(it.name).joinToString(" ") }
            return named.filter { it.second == key }.map { it.first }.ifEmpty {
                named.filter { it.second.startsWith(key) }.map { it.first }
            }.ifEmpty {
                named.filter { it.second.contains(key) }.map { it.first }
            }
        }
        // Önce sayıyla birlikte ad olarak ("Pompa 2" adlı cihaz), olmazsa sayı kanal olarak.
        val whole = find(words)
        if (whole.size == 1 && (number == null || !whole[0].isMultiChannel || DeviceKinds.words(whole[0].name).lastOrNull() == words.last())) {
            val d = whole[0]
            return if (d.isMultiChannel) Match.NeedChannel(d) else Match.Found(d, d.switches.keys.minOrNull() ?: 0)
        }
        if (number != null && words.size > 1) {
            val byName = find(words.dropLast(1))
            if (byName.size == 1) {
                val d = byName[0]
                val ch = number - 1
                return if (d.isMultiChannel && ch in d.switches) Match.Found(d, ch) else if (d.isMultiChannel) Match.NeedChannel(d)
                else Match.Found(d, d.switches.keys.minOrNull() ?: 0)
            }
            if (byName.size > 1) return Match.Ambiguous(byName.map { it.name })
        }
        if (whole.size > 1) return Match.Ambiguous(whole.map { it.name })
        return Match.NotFound
    }

    /**
     * Onaylanacak kayıt (getUpdates "offset"): [KEEP_MS]'den eski güncellemeler. Daha yenileri onaylanmaz ki aynı botu
     * kullanan başka cihaz (TV, ikinci telefon) ve kurulum ekranındaki "Kişileri bul" da görebilsin.
     */
    fun confirmOffset(updates: List<Update>, now: Long, current: Long): Long {
        val old = updates.filter { now - it.date >= KEEP_MS }.maxOfOrNull { it.id } ?: return current
        return maxOf(current, old + 1)
    }

    /** Aç/kapa komutu bu kadar eskiyse uygulanmaz (telefon çevrimdışıyken yazılmış; artık geçerli olmayabilir). */
    const val SWITCH_MAX_AGE_MS = 2 * 60_000L
    /** Durum sorusu bu kadar eskiyse yanıtlanmaz. */
    const val STATUS_MAX_AGE_MS = 10 * 60_000L
    const val KEEP_MS = 10 * 60_000L

    /** "Gördüm" düğmesinin verisi: hangi cihazın hangi alarmı ("ack:<cihaz>:<alarm>"). */
    fun ackData(tag: String, alarmAt: Long) = "ack:$tag:$alarmAt"

    fun parseAck(data: String?): Pair<String, Long>? {
        val parts = data?.split(':') ?: return null
        if (parts.size != 3 || parts[0] != "ack") return null
        return parts[1] to (parts[2].toLongOrNull() ?: return null)
    }

    /** Tek düğmeli satır içi klavye. */
    fun button(text: String, data: String): String =
        JSONObject().put("inline_keyboard", JSONArray().put(JSONArray().put(JSONObject().put("text", text).put("callback_data", data)))).toString()
}
