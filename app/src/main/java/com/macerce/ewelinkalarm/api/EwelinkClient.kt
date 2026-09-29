package com.macerce.ewelinkalarm.api

import android.net.Uri
import com.macerce.ewelinkalarm.core.DeviceParser
import com.macerce.ewelinkalarm.core.DeviceSnapshot
import com.macerce.ewelinkalarm.core.Signer
import com.macerce.ewelinkalarm.data.Store
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

/** eWeLink API bir hata kodu döndürdü (internet sorunu değil). */
open class ApiException(val code: Int, message: String) : Exception("eWeLink hata $code: $message")

/** Token geçersiz ve yenilenemedi; kullanıcı yeniden giriş yapmalı. */
class AuthException(code: Int, message: String) : ApiException(code, message)

/**
 * eWeLink (CoolKit v2) istemcisi. Tüm çağrılar bloklayıcıdır; ana thread'den çağırmayın.
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

    /** Giriş sonrası dönen tek kullanımlık kodu token'a çevirir ve kaydeder. Kod ~30 sn geçerlidir. */
    fun exchangeCode(code: String, region: String) {
        store.region = region
        val body = JSONObject()
            .put("code", code)
            .put("redirectUrl", store.redirectUrl)
            .put("grantType", "authorization_code")
        val data = postSigned("/v2/user/oauth/token", body)
        saveTokens(data)
    }

    fun getDevices(): List<DeviceSnapshot> {
        val body = try {
            getWithToken("/v2/device/thing?num=0")
        } catch (e: ApiException) {
            if (e.code !in TOKEN_ERRORS) throw e
            refresh()
            getWithToken("/v2/device/thing?num=0")
        }
        return DeviceParser.parseThingList(body)
    }

    private fun refresh() {
        if (store.refreshToken.isEmpty()) throw AuthException(401, "Oturum yok")
        try {
            saveTokens(postSigned("/v2/user/refresh", JSONObject().put("rt", store.refreshToken)))
        } catch (e: ApiException) {
            store.logout()
            throw AuthException(e.code, "Oturum süresi doldu, yeniden giriş yapın")
        }
    }

    private fun saveTokens(data: JSONObject) {
        // OAuth yanıtı accessToken/refreshToken, refresh yanıtı at/rt alan adlarını kullanır.
        val at = data.optString("accessToken").ifEmpty { data.optString("at") }
        val rt = data.optString("refreshToken").ifEmpty { data.optString("rt") }
        if (at.isEmpty()) throw ApiException(-1, "Yanıtta token yok")
        store.accessToken = at
        if (rt.isNotEmpty()) store.refreshToken = rt
    }

    private fun postSigned(path: String, body: JSONObject): JSONObject {
        // İmza, gönderilen string'in birebir aynısı üzerinden hesaplanmalı.
        val raw = body.toString()
        val text = request("POST", path, raw, "Sign " + Signer.sign(raw, store.appSecret))
        return checkResponse(text).optJSONObject("data") ?: JSONObject()
    }

    private fun getWithToken(path: String): String {
        if (store.accessToken.isEmpty()) throw AuthException(401, "Giriş yapılmamış")
        val text = request("GET", path, null, "Bearer " + store.accessToken)
        checkResponse(text)
        return text
    }

    private fun checkResponse(text: String): JSONObject {
        val json = JSONObject(text)
        val error = json.optInt("error", -1)
        if (error != 0) throw ApiException(error, json.optString("msg"))
        return json
    }

    private fun request(method: String, path: String, body: String?, authorization: String): String {
        val conn = URL(host(store.region) + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("X-CK-Appid", store.appId)
            conn.setRequestProperty("Authorization", authorization)
            conn.setRequestProperty("Content-Type", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                ?: throw IOException("HTTP ${conn.responseCode}")
            return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val OAUTH_PAGE = "https://c2ccdn.coolkit.cc/oauth/index.html"
        private val TOKEN_ERRORS = setOf(401, 402)
        private val random = SecureRandom()

        fun host(region: String) =
            if (region == "cn") "https://cn-apia.coolkit.cn" else "https://$region-apia.coolkit.cc"

        fun randomNonce(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }
}
