package com.macerce.switchguard.api

import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.Recipient
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/** Telegram'ın verdiği hata: [code] HTTP kodu (401 = anahtar geçersiz, 403 = kişi botu engelledi, 400 = sohbet yok). */
class TelegramException(val code: Int, message: String) : IOException(message)

/**
 * Telegram Bot API: kullanıcının kendi botu üzerinden mesaj. Sunucu yok; istekler doğrudan api.telegram.org'a gider.
 * Tüm çağrılar engelleyicidir, arka planda çağrılmalı.
 */
object TelegramClient {
    private const val BASE = "https://api.telegram.org/bot"

    /** Anahtarı dener; botun kullanıcı adını döner ("t.me/<ad>" bağlantısı için). */
    fun botUsername(token: String): String = call(token, "getMe").getJSONObject("result").getString("username")

    /** Bota yazan kişiler ve botun eklendiği gruplar (Telegram son 24 saati tutar). */
    fun findChats(token: String): List<Recipient> = Escalation.chatsFromUpdates(call(token, "getUpdates").toString())
        .let { found -> (seenChats + found).associateBy { it.id }.values.toList() }

    /**
     * İzleme sırasında komutları okurken görülen sohbetler: eski güncellemeler onaylanıp silindiğinden "Kişileri bul"
     * onları artık göremez.
     */
    @Volatile var seenChats: List<Recipient> = emptyList()

    /** Mesaj gönderir; [markup] satır içi düğmeler (JSON). Gönderilen mesajın numarasını döner. */
    fun send(token: String, chatId: Long, text: String, markup: String? = null, silent: Boolean = false, html: Boolean = false): Long {
        val body = FormBody.Builder().add("chat_id", chatId.toString()).add("text", text)
        if (html) body.add("parse_mode", "HTML")
        if (markup != null) body.add("reply_markup", markup)
        if (silent) body.add("disable_notification", "true")
        return call(token, "sendMessage", body.build()).optJSONObject("result")?.optLong("message_id") ?: 0
    }

    /** Onaylanmamış güncellemeler (bekletmeden). [offset]'ten küçükler Telegram'da silinir. */
    fun updates(token: String, offset: Long): String =
        call(token, "getUpdates", FormBody.Builder().add("offset", offset.toString()).add("timeout", "0").build()).toString()

    /** Düğmeye dokunan kişiye kısa yanıt (Telegram'da küçük bir uyarı olarak görünür). */
    fun answerCallback(token: String, callbackId: String, text: String) {
        call(token, "answerCallbackQuery", FormBody.Builder().add("callback_query_id", callbackId).add("text", text).build())
    }

    /** Mesajdaki düğmeleri kaldırır. */
    fun removeButtons(token: String, chatId: Long, messageId: Long) {
        call(token, "editMessageReplyMarkup", FormBody.Builder().add("chat_id", chatId.toString()).add("message_id", messageId.toString()).build())
    }

    fun edit(token: String, chatId: Long, messageId: Long, text: String) {
        call(token, "editMessageText", FormBody.Builder().add("chat_id", chatId.toString()).add("message_id", messageId.toString()).add("text", text).build())
    }

    /** Mesajı sohbetin başına sabitler (bildirim göndermeden). */
    fun pin(token: String, chatId: Long, messageId: Long) {
        call(token, "pinChatMessage", FormBody.Builder().add("chat_id", chatId.toString()).add("message_id", messageId.toString())
            .add("disable_notification", "true").build())
    }

    /** Sohbet bilgisi (sabit mesaj dahil), JSON. */
    fun chat(token: String, chatId: Long): String =
        call(token, "getChat", FormBody.Builder().add("chat_id", chatId.toString()).build()).toString()

    private fun call(token: String, method: String, body: FormBody? = null): JSONObject {
        val request = Request.Builder().url("$BASE${token.trim()}/$method")
            .apply { if (body != null) post(body) }
            .build()
        EwelinkClient.http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (json?.optBoolean("ok") == true) return json
            throw TelegramException(json?.optInt("error_code") ?: response.code, json?.optString("description").orEmpty().ifEmpty { "HTTP ${response.code}" })
        }
    }
}
