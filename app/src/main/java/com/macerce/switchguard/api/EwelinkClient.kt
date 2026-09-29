package com.macerce.switchguard.api

import android.net.Uri
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Signer
import com.macerce.switchguard.data.Store
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/** eWeLink API bir hata kodu döndürdü (internet sorunu değil). */
open class ApiException(val code: Int, message: String) : Exception("eWeLink $code: $message")

/** Token geçersiz ve yenilenemedi; kullanıcı yeniden giriş yapmalı. */
class AuthException(code: Int, message: String) : ApiException(code, message)

/**
 * eWeLink (CoolKit v2) HTTP istemcisi. Tüm çağrılar bloklayıcıdır; ana thread'den çağırmayın.
 * İnternet sorunlarında [IOException], API hatalarında [ApiException] fırlatır.
 */
class EwelinkClient(private val store: Store) {

    /** OAuth giriş sayfasının adresi. İmza "{appId}_{seq}" üzerinden hesaplanır. */
    fun buildLoginUrl(state: String): String {
        val seq = System.currentTimeMillis().toString()
        return Uri.parse(OAUTH_PAGE).buildUpon()
            .appendQueryParameter("clientId", store.appId)
            .appendQueryParameter("redirectUrl", store.redirectUrl)
            .appendQueryParameter("grantType", "authorization_code")
            .appendQueryParameter("state", state)
            .appendQueryParameter("nonce", randomNonce())
            .appendQueryParameter("seq", seq)
            .appendQueryParameter("showQRCode", "false")
            .appendQueryParameter("authorization", Signer.sign("${store.appId}_$seq", store.appSecret))
            .build().toString()
    }

    /** Giriş sonrası dönen tek kullanımlık kodu token'a çevirir, kaydeder ve profili çeker. */
    fun exchangeCode(code: String, region: String) {
        store.region = region
        val body = JSONObject()
            .put("code", code)
            .put("redirectUrl", store.redirectUrl)
            .put("grantType", "authorization_code")
        saveTokens(postSigned("/v2/user/oauth/token", body))
        runCatching { loadProfile() }
    }

    /** Kullanıcının apikey'ini (WebSocket için gerekli) ve e-postasını kaydeder. */
    fun loadProfile() {
        val data = withTokenRetry { authed("GET", "/v2/user/profile", null) }
        val user = JSONObject(data).optJSONObject("data")?.optJSONObject("user") ?: return
        user.optString("apikey").takeIf { it.isNotEmpty() }?.let { store.userApiKey = it }
        user.optString("email").takeIf { it.isNotEmpty() }?.let { store.accountEmail = it }
    }

    fun getDevices(): List<DeviceSnapshot> =
        DeviceParser.parseThingList(withTokenRetry { authed("GET", "/v2/device/thing?num=0", null) })

    /** Cihazı açar/kapatır. Çok kanallı cihazlarda [channel] 0'dan başlar. */
    fun setSwitch(device: DeviceSnapshot, channel: Int, on: Boolean) {
        val value = if (on) "on" else "off"
        val params = if (device.isMultiChannel) {
            JSONObject().put("switches", JSONArray().put(JSONObject().put("switch", value).put("outlet", channel)))
        } else {
            JSONObject().put("switch", value)
        }
        val body = JSONObject().put("type", 1).put("id", device.id).put("params", params)
        withTokenRetry { authed("POST", "/v2/device/thing/status", body.toString()) }
    }

    /** WebSocket sunucusunun adresini döndürür: wss://domain:port/api/ws */
    fun dispatchWebSocketUrl(): String {
        val tld = if (store.region == "cn") "cn" else "cc"
        val req = Request.Builder().url("https://${store.region}-dispa.coolkit.$tld/dispatch/app").build()
        val json = http.newCall(req).execute().use { JSONObject(it.body!!.string()) }
        val domain = json.optString("domain").ifEmpty { throw ApiException(json.optInt("error", -1), "dispatch") }
        return "wss://$domain:${json.optInt("port", 443)}/api/ws"
    }

    private fun <T> withTokenRetry(call: () -> T): T = try {
        call()
    } catch (e: ApiException) {
        if (e.code !in TOKEN_ERRORS) throw e
        refresh()
        call()
    }

    private fun refresh() {
        if (store.refreshToken.isEmpty()) throw AuthException(401, "no session")
        try {
            saveTokens(postSigned("/v2/user/refresh", JSONObject().put("rt", store.refreshToken)))
        } catch (e: ApiException) {
            store.logout()
            throw AuthException(e.code, "session expired")
        }
    }

    private fun saveTokens(data: JSONObject) {
        // OAuth yanıtı accessToken/refreshToken, refresh yanıtı at/rt alan adlarını kullanır.
        val at = data.optString("accessToken").ifEmpty { data.optString("at") }
        val rt = data.optString("refreshToken").ifEmpty { data.optString("rt") }
        if (at.isEmpty()) throw ApiException(-1, "no token in response")
        store.accessToken = at
        if (rt.isNotEmpty()) store.refreshToken = rt
        data.optJSONObject("user")?.optString("apikey")?.takeIf { it.isNotEmpty() }?.let { store.userApiKey = it }
    }

    private fun postSigned(path: String, body: JSONObject): JSONObject {
        // İmza, gönderilen string'in birebir aynısı üzerinden hesaplanmalı.
        val raw = body.toString()
        val text = send("POST", path, raw, "Sign " + Signer.sign(raw, store.appSecret))
        return checkResponse(text).optJSONObject("data") ?: JSONObject()
    }

    private fun authed(method: String, path: String, body: String?): String {
        if (store.accessToken.isEmpty()) throw AuthException(401, "not logged in")
        val text = send(method, path, body, "Bearer " + store.accessToken)
        checkResponse(text)
        return text
    }

    private fun checkResponse(text: String): JSONObject {
        val json = runCatching { JSONObject(text) }.getOrElse { throw IOException("invalid response") }
        val error = json.optInt("error", -1)
        if (error != 0) throw ApiException(error, json.optString("msg"))
        return json
    }

    private fun send(method: String, path: String, body: String?, authorization: String): String {
        val request = Request.Builder()
            .url(host(store.region) + path)
            .header("X-CK-Appid", store.appId)
            .header("Authorization", authorization)
            .method(method, body?.toRequestBody(JSON))
            .build()
        return http.newCall(request).execute().use { it.body?.string() ?: throw IOException("HTTP ${it.code}") }
    }

    companion object {
        const val OAUTH_PAGE = "https://c2ccdn.coolkit.cc/oauth/index.html"
        private val TOKEN_ERRORS = setOf(401, 402)
        private val JSON = "application/json".toMediaType()
        private val random = SecureRandom()

        /** Uygulama genelinde tek bağlantı havuzu; WebSocket de bunu kullanır. */
        val http: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        fun host(region: String) =
            if (region == "cn") "https://cn-apia.coolkit.cn" else "https://$region-apia.coolkit.cc"

        fun randomNonce(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }
}
