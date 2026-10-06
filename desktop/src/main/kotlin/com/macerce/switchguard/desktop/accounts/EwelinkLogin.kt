package com.macerce.switchguard.desktop.accounts

import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom

sealed interface Parsed {
    /** Dönüş adresindeki tek kullanımlık kod ve (varsa) hesabın bölgesi. */
    data class Code(val code: String, val region: String?) : Parsed

    /** Dönüş adresi ama bu girişe ait değil (eski bir giriş ya da başka bir pencere): kullanılmaz. */
    data object WrongState : Parsed

    /** Dönüş adresi değil (kullanıcı başka bir şey kopyaladı): sessizce yok sayılır. */
    data object NotRedirect : Parsed
}

/**
 * eWeLink tarayıcı girişinin dönüş adresi (`spec.md` §6 yedek yol): tarayıcı girişten sonra `<redirectUrl>/?code=…
 * &region=…&state=…` adresinde "bağlanılamıyor" sayfasında kalır; kullanıcı adresi kopyalar. Adres birebir
 * [redirectUrl] ile başlamalı (ardından `/`, `?` ya da son), `code` içermeli ve `state` bu girişinkiyle aynı olmalı.
 */
object EwelinkRedirect {
    private val random = SecureRandom()

    fun newState(): String = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    fun parse(text: String, redirectUrl: String, expectedState: String): Parsed {
        val t = text.trim()
        val base = redirectUrl.trim().trimEnd('/')
        if (!t.startsWith(base)) return Parsed.NotRedirect
        val rest = t.substring(base.length)
        if (rest.isNotEmpty() && rest[0] != '/' && rest[0] != '?') return Parsed.NotRedirect
        val query = try {
            URI(t).rawQuery
        } catch (_: Exception) {
            null
        } ?: return Parsed.NotRedirect
        val params = query.split('&').mapNotNull {
            val i = it.indexOf('=')
            if (i <= 0) null else it.substring(0, i) to URLDecoder.decode(it.substring(i + 1), UTF_8)
        }.toMap()
        val code = params["code"]?.takeIf { it.isNotEmpty() } ?: return Parsed.NotRedirect
        if (params["state"] != expectedState) return Parsed.WrongState
        return Parsed.Code(code, params["region"]?.takeIf { it.isNotEmpty() })
    }
}

sealed interface LoginResult {
    data object Ok : LoginResult
    data class Rejected(val message: String) : LoginResult
    data object NoInternet : LoginResult
}

/**
 * eWeLink hesabı: kodun oturuma çevrilmesi ([complete]) ve hesabın bu bilgisayardan kaldırılması. Ağ çağrısı yapar:
 * arayüz iş parçacığından çağırmayın. Kod ve token günlüğe yazılmaz.
 */
class EwelinkLogin(
    private val store: Store,
    private val exchange: (String, String) -> Unit = { code, region -> EwelinkClient(store).exchangeCode(code, region) },
) {
    /** [region] dönüş adresinde yoksa kayıtlı bölge kullanılır. */
    fun complete(code: String, region: String?): LoginResult = try {
        exchange(code, region ?: store.region)
        LoginResult.Ok
    } catch (e: IOException) {
        LoginResult.NoInternet
    } catch (e: ApiException) {
        Log.w(TAG, "eWeLink sign-in rejected: ${e.code}")
        LoginResult.Rejected(e.message ?: "eWeLink ${e.code}")
    } catch (e: Exception) {
        Log.w(TAG, "eWeLink sign-in failed: ${e.javaClass.simpleName}")
        LoginResult.Rejected(e.message ?: e.javaClass.simpleName)
    }

    /** Oturumu kapatır ve App ID / App Secret'ı siler. */
    fun removeAccount() {
        store.logout()
        store.appId = ""
        store.appSecret = ""
    }

    companion object {
        private const val TAG = "EwelinkLogin"

        /** `macerce@gmail.com` → `ma***@gmail.com`; boşsa boş. */
        fun maskEmail(email: String): String {
            val at = email.indexOf('@')
            if (email.isEmpty() || at < 0) return email.take(2).let { if (it.isEmpty()) "" else "$it***" }
            return email.substring(0, at).take(2) + "***" + email.substring(at)
        }
    }
}
