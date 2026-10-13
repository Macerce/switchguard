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

    fun send(token: String, chatId: Long, text: String) {
        call(token, "sendMessage", FormBody.Builder().add("chat_id", chatId.toString()).add("text", text).build())
    }

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
