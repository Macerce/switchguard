package com.macerce.switchguard.api

import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.TuyaParser
import com.macerce.switchguard.core.TuyaSign
import com.macerce.switchguard.data.Store
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/** Tuya API bir hata kodu döndürdü. */
open class TuyaException(val code: Int, message: String) : Exception("Tuya $code: $message")

/** Access ID / Secret yanlış ya da proje silinmiş: kullanıcı bilgileri düzeltmeli. */
class TuyaAuthException(code: Int, message: String) : TuyaException(code, message)

/** Ücretsiz deneme süresi (IoT Core) dolmuş ya da kota bitmiş: platformdan uzatılmalı. */
class TuyaExpiredException(code: Int, message: String) : TuyaException(code, message)

/**
 * Tuya Cloud (OpenAPI) istemcisi; kullanıcının kendi Cloud projesinin Access ID / Secret'ı ile çalışır.
 * Tüm çağrılar bloklayıcıdır. Ücretsiz planın aylık API kotası sınırlı olduğundan (≈26 bin) çağrılar
 * toplu yapılır: liste + tek seferde tüm durumlar; ürün tanımı her ürün için bir kez.
 */
class TuyaClient(private val store: Store) {

    /** Son yanıttaki Tuya sunucu saati (ms). */
    @Volatile var lastServerTime = 0L
        private set

    /**
     * Son [getDevices]'ın durumları okumaya başladığı an (Tuya saatiyle). Bundan eski mesaj servisi
     * olayları zaten okunan duruma dahildir; telefonun saatine güvenilmez.
     */
    @Volatile var syncServerTime = 0L
        private set

    /** Bilgileri doğrular; bağlı cihaz sayısını döndürür. */
    fun test(): Int {
        store.tuyaToken = ""
        return getDevices().size
    }

    fun getDevices(): List<DeviceSnapshot> {
        val devices = listDevices()
        syncServerTime = lastServerTime
        if (devices.isEmpty()) return emptyList()
        val statusById = mutableMapOf<String, JSONArray>()
        for (chunk in devices.map { it.getString("id") }.chunked(20)) {
            val result = get("/v1.0/iot-03/devices/status?device_ids=${chunk.joinToString(",")}").optJSONArray("result") ?: continue
            for (i in 0 until result.length()) {
                val o = result.getJSONObject(i)
                statusById[o.getString("id")] = o.optJSONArray("status") ?: JSONArray()
            }
        }
        val scales = store.tuyaScales.toMutableMap()
        var scalesChanged = false
        val out = devices.map { d ->
            val pid = d.optString("productId")
            if (pid.isNotEmpty() && pid !in scales) {
                scales[pid] = runCatching {
                    TuyaParser.scales(get("/v1.0/iot-03/devices/${d.getString("id")}/specification").getJSONObject("result"))
                }.getOrDefault(emptyMap())
                scalesChanged = true
            }
            TuyaParser.snapshot(d, statusById[d.getString("id")] ?: JSONArray(), scales[pid].orEmpty())
        }
        if (scalesChanged) store.tuyaScales = scales
        return out
    }

    /** Mesaj servisinden gelen değerleri ölçeklemek için ürün tanımı (önbellekten). */
    fun scalesFor(deviceId: String, productIdHint: String? = null): Map<String, Int> =
        store.tuyaScales[productIdHint ?: productIds[deviceId]].orEmpty()

    fun setSwitch(device: DeviceSnapshot, channel: Int, on: Boolean) {
        val code = device.switchCodes[channel] ?: throw TuyaException(-1, "no switch code for channel $channel")
        val body = JSONObject().put("commands", JSONArray().put(JSONObject().put("code", code).put("value", on)))
        call("POST", "/v1.0/iot-03/devices/${device.id}/commands", body.toString())
    }

    /** "Cihaz verisini kopyala" için ham durum. */
    fun rawProperties(deviceId: String): JSONObject = get("/v2.0/cloud/thing/$deviceId/shadow/properties")

    // ---------------------------------------------------------------- iç

    private fun listDevices(): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        var lastId: String? = null
        while (true) {
            // Sorgu anahtarları imza için alfabetik sırada olmalı.
            val path = "/v2.0/cloud/thing/device?" + (lastId?.let { "last_id=$it&" } ?: "") + "page_size=$PAGE"
            val result = get(path).optJSONArray("result") ?: break
            for (i in 0 until result.length()) out += result.getJSONObject(i)
            if (result.length() < PAGE || out.size >= MAX_DEVICES) break
            lastId = out.last().getString("id")
        }
        out.forEach { productIds[it.getString("id")] = it.optString("productId") }
        return out
    }

    private fun get(path: String) = call("GET", path, null)

    private fun call(method: String, path: String, body: String?): JSONObject {
        val json = send(method, path, body, token())
        if (json.optInt("code") in TOKEN_ERRORS) {
            store.tuyaToken = ""
            return check(send(method, path, body, token()))
        }
        return check(json)
    }

    private fun token(): String {
        val cached = store.tuyaToken
        if (cached.isNotEmpty() && System.currentTimeMillis() < store.tuyaTokenExpiry) return cached
        val result = check(send("GET", "/v1.0/token?grant_type=1", null, "")).getJSONObject("result")
        store.tuyaToken = result.getString("access_token")
        store.tuyaTokenExpiry = System.currentTimeMillis() + result.optLong("expire_time", 7200) * 1000 - 60_000
        return store.tuyaToken
    }

    private fun check(json: JSONObject): JSONObject {
        if (json.optBoolean("success")) return json
        val code = json.optInt("code", -1)
        val msg = json.optString("msg")
        throw when {
            code in AUTH_ERRORS -> TuyaAuthException(code, msg)
            code in EXPIRED_ERRORS || msg.contains("expired", ignoreCase = true) -> TuyaExpiredException(code, msg)
            else -> TuyaException(code, msg)
        }
    }

    private fun send(method: String, path: String, body: String?, token: String): JSONObject {
        val t = System.currentTimeMillis().toString()
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val sign = TuyaSign.sign(store.tuyaAccessId, store.tuyaSecret, token, t, nonce, method, body.orEmpty(), path)
        val request = Request.Builder()
            .url(apiHost(store.tuyaRegion) + path)
            .header("client_id", store.tuyaAccessId)
            .header("sign", sign)
            .header("t", t)
            .header("nonce", nonce)
            .header("sign_method", "HMAC-SHA256")
            .apply { if (token.isNotEmpty()) header("access_token", token) }
            .method(method, body?.toRequestBody(JSON))
            .build()
        val text = EwelinkClient.http.newCall(request).execute().use { it.body?.string() ?: throw IOException("HTTP ${it.code}") }
        val json = runCatching { JSONObject(text) }.getOrElse { throw IOException("invalid response") }
        json.optLong("t").takeIf { it > 0 }?.let { lastServerTime = it }
        return json
    }

    companion object {
        private const val PAGE = 20
        private const val MAX_DEVICES = 200
        private val JSON = "application/json".toMediaType()
        /** 1010: token geçersiz, 1011: token süresi doldu. */
        private val TOKEN_ERRORS = setOf(1010, 1011)
        /** 1004: imza yanlış (Secret hatalı), 1005: client_id yanlış, 2009: proje/istemci yok. */
        private val AUTH_ERRORS = setOf(1004, 1005, 2009)
        /** Deneme planı bitti / yetki yok. */
        private val EXPIRED_ERRORS = setOf(28841002, 28841004, 28841101)

        /** Cihaz → ürün kimliği; mesaj servisinden gelen değerleri doğru ölçeklemek için. */
        private val productIds = java.util.concurrent.ConcurrentHashMap<String, String>()

        val REGIONS = listOf("eu", "us", "cn", "in")

        fun apiHost(region: String) = when (region) {
            "us" -> "https://openapi.tuyaus.com"
            "cn" -> "https://openapi.tuyacn.com"
            "in" -> "https://openapi.tuyain.com"
            else -> "https://openapi.tuyaeu.com"
        }

        fun pulsarHost(region: String) = when (region) {
            "us" -> "wss://mqe.tuyaus.com:8285/"
            "cn" -> "wss://mqe.tuyacn.com:8285/"
            "in" -> "wss://mqe.tuyain.com:8285/"
            else -> "wss://mqe.tuyaeu.com:8285/"
        }
    }
}
