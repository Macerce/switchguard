package com.macerce.switchguard.api

import com.macerce.switchguard.core.AlertHooks
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Kullanıcının girdiği dış adreslere istekler: canlılık adresi (GET), ntfy (düz metin POST) ve webhook (JSON POST).
 * Engelleyici: arka planda çağrılmalı. Döner: HTTP durum kodu; 2xx değilse [java.io.IOException].
 */
object HookClient {
    private val TEXT = "text/plain; charset=utf-8".toMediaType()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun ping(url: String): Int = run(Request.Builder().url(url.trim()).get())

    fun ntfy(url: String, text: String, event: AlertHooks.Event, device: String): Int {
        val b = Request.Builder().url(url.trim()).post(text.toRequestBody(TEXT))
        AlertHooks.ntfyHeaders(event, device).forEach { (k, v) -> b.header(k, AlertHooks.headerSafe(v)) }
        return run(b)
    }

    fun webhook(url: String, json: String): Int = run(Request.Builder().url(url.trim()).post(json.toRequestBody(JSON)))

    private fun run(b: Request.Builder): Int =
        EwelinkClient.http.newCall(b.header("User-Agent", "SwitchGuard").build()).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
            r.code
        }
}
