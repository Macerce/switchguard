package com.macerce.switchguard.core

import org.json.JSONObject
import java.time.Instant

/**
 * Dış canlılık adresi (healthchecks.io, Uptime Kuma "push" vb.): izleme sağlıklıyken düzenli GET gönderilir. İzleme
 * durur, telefon kapanır ya da izleme kör kalırsa istek gitmez ve dış servis kullanıcıya haber verir. Tek cihaz
 * kullananlar için bekçi. Yalnızca kullanıcının girdiği adres kullanılır.
 */
object Heartbeat {
    /** Seçilebilen aralıklar (dakika). */
    val INTERVALS = listOf(1, 5, 15, 60)
    const val DEFAULT_MIN = 5

    /** Şimdi istek gönderilmeli mi: adres girili, izleme sağlıklı ve aralık doldu. */
    fun due(lastAt: Long, now: Long, intervalMin: Int, healthy: Boolean, url: String): Boolean =
        healthy && isValidUrl(url) && (lastAt == 0L || now - lastAt >= intervalMin.coerceAtLeast(1) * 60_000L)

    /** Yalnızca http(s) adresleri (boşluksuz, en az bir nokta ya da port içeren ana makine). */
    fun isValidUrl(url: String): Boolean {
        val u = url.trim()
        if (u.any { it.isWhitespace() }) return false
        val rest = when {
            u.startsWith("https://") -> u.removePrefix("https://")
            u.startsWith("http://") -> u.removePrefix("http://")
            else -> return false
        }
        val host = rest.substringBefore('/').substringBefore('?')
        return host.isNotEmpty() && (host.contains('.') || host.contains(':'))
    }
}

/**
 * Telegram dışında ikinci "haber ver" kanalı: ntfy (konu adresine düz metin POST; telefona ntfy uygulamasıyla anlık
 * bildirim) ve genel webhook (JSON POST; Home Assistant, Node-RED). Gerçek alarm başlayınca hemen, kapatılınca kısa
 * bir mesaj; test alarmı göndermez (ayarlardaki "Dene" ayrı).
 */
object AlertHooks {
    enum class Event { ALARM, DISMISSED, TEST, RULE }

    /** ntfy başlıkları: başlık, öncelik (alarm en yüksek) ve simge etiketi. */
    fun ntfyHeaders(event: Event, device: String): Map<String, String> = mapOf(
        "Title" to "SwitchGuard" + if (device.isNotBlank()) " - $device" else "",
        "Priority" to when (event) {
            Event.ALARM -> "5"
            Event.DISMISSED -> "3"
            Event.TEST -> "3"
            Event.RULE -> "3"
        },
        "Tags" to when (event) {
            Event.ALARM -> "rotating_light"
            Event.DISMISSED -> "white_check_mark"
            Event.TEST -> "test_tube"
            Event.RULE -> "robot"
        },
    )

    /** Webhook gövdesi: {"app","event","device","text","time"}; olay adı küçük harf ("alarm", "dismissed", "test"). */
    fun webhookJson(event: Event, device: String, text: String, time: Long): String = JSONObject()
        .put("app", "SwitchGuard")
        .put("event", event.name.lowercase())
        .put("device", device)
        .put("text", text)
        .put("time", Instant.ofEpochMilli(time).toString())
        .toString()

    /** HTTP başlığına yazılabilir mi (ntfy başlıkları ASCII olmalı; Türkçe cihaz adı ise gövdeye yazılır). */
    fun headerSafe(value: String): String = value.map { c ->
        when {
            c.code in 32..126 -> c
            else -> TRANSLIT[c] ?: '?'
        }
    }.joinToString("")

    /** Başlıkta Türkçe harflerin okunur karşılığı. */
    private val TRANSLIT = mapOf(
        'ç' to 'c', 'Ç' to 'C', 'ğ' to 'g', 'Ğ' to 'G', 'ı' to 'i', 'İ' to 'I',
        'ö' to 'o', 'Ö' to 'O', 'ş' to 's', 'Ş' to 'S', 'ü' to 'u', 'Ü' to 'U',
    )
}
