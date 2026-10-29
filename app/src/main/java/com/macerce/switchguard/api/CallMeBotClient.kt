package com.macerce.switchguard.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * CallMeBot: kişiyi Telegram'dan sesli arar ve metni okur (Telegram botları kendisi arayamaz). Kişi bir kez
 * @CallMeBot_txtbot'a /start yazıp izin vermelidir. Ücretsiz paylaşılan sıra; arama birkaç saniye gecikebilir.
 * Engelleyicidir, arka planda çağrılmalı.
 */
object CallMeBotClient {
    private const val URL = "https://api.callmebot.com/start.php"
    /** CallMeBot metni 256 karakterde keser. */
    private const val MAX_CHARS = 250
    /** Paylaşılan sırada yanıt arama başlayana dek gecikebilir. */
    private val http by lazy { EwelinkClient.http.newBuilder().readTimeout(90, TimeUnit.SECONDS).build() }

    /** [to]: "@kullanıcıadı" ya da "+90…". [turkish]: Türkçe ses; değilse İngilizce. */
    fun call(to: String, text: String, turkish: Boolean) {
        val url = URL.toHttpUrl().newBuilder()
            .addQueryParameter("user", to.trim())
            .addQueryParameter("text", text.take(MAX_CHARS))
            .addQueryParameter("lang", if (turkish) "tr-TR-Standard-A" else "en-GB-Standard-B")
            .addQueryParameter("rpt", "3")
            // Açılmazsa kişiye Telegram'dan yazılı kopyası da gider.
            .addQueryParameter("cc", "missed")
            .build()
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            // Hata da 200 ile döner; metinden anlaşılır (izin verilmemiş, arama kapalı vb.).
            val plain = body.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
            if (Regex("not (been )?authori[sz]ed|error|invalid|not allowed", RegexOption.IGNORE_CASE).containsMatchIn(plain)) {
                throw IOException(plain.take(160))
            }
        }
    }
}
