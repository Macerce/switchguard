package com.macerce.switchguard.api

import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import com.macerce.switchguard.core.TapoProtocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * TP-Link Tapo (ve KLAP kullanan yeni Kasa) cihazları (bkz. [TapoProtocol]). TP-Link hesabının e-postası/şifresi
 * gerekir; bir kez girilince [ACCOUNT] adıyla saklanır ve aynı hesaptaki diğer cihazlar kendiliğinden açılır.
 * Keşif: ağ taraması (`/app/handshake1`'e 48 bayt yanıt veren cihaz) ya da IP.
 */
object TapoLan : PolledLan(LocalDevice.BRAND_TAPO) {
    /** Gizli depoda TP-Link hesabının anahtarı (tüm Tapo cihazlarında ortak). */
    const val ACCOUNT = "tapo-account"

    private val sessions = ConcurrentHashMap<String, TapoProtocol.Session>()
    private val random = SecureRandom()
    private val octet = "application/octet-stream".toMediaType()

    private fun post(host: String, path: String, body: ByteArray, cookie: String?, quickly: Boolean): Triple<Int, ByteArray?, String?> = runCatching {
        val b = Request.Builder().url("http://$host$path").post(body.toRequestBody(octet))
        if (cookie != null) b.header("Cookie", cookie)
        (if (quickly) quick else http).newCall(b.build()).execute().use { r ->
            val setCookie = r.headers("Set-Cookie").firstOrNull()?.substringBefore(';')
            Triple(r.code, if (r.isSuccessful) r.body?.bytes() else null, setCookie)
        }
    }.getOrElse { Triple(0, null, null) }

    /** Sonuç: oturum; ya da null + [wrong] (cihaz KLAP konuşuyor ama kimlik yanlış). */
    private class Handshake(val session: TapoProtocol.Session?, val isKlap: Boolean, val wrong: Boolean)

    private fun handshake(host: String, c: ShellyAuth.Credentials?, quickly: Boolean): Handshake {
        val local = ByteArray(16).also(random::nextBytes)
        val (code, body, cookie) = post(host, "/app/handshake1", local, null, quickly)
        if (code != 200 || body == null || body.size != 48) return Handshake(null, false, false)
        if (c == null) return Handshake(null, true, false)
        val remote = body.copyOfRange(0, 16)
        val server = body.copyOfRange(16, 48)
        val v = TapoProtocol.verifyServer(local, remote, server, c.user, c.password)
        if (v == 0) return Handshake(null, true, true)
        val auth = TapoProtocol.authFor(v, c.user, c.password)
        val (code2, _, _) = post(host, "/app/handshake2", TapoProtocol.handshake2Payload(v, local, remote, auth), cookie, quickly)
        if (code2 != 200) return Handshake(null, true, true)
        return Handshake(TapoProtocol.Session(local, remote, auth, cookie), true, false)
    }

    /** Şifreli istek; oturum yoksa/düştüyse bir kez yeniden el sıkışılır. */
    private fun request(host: String, json: String, c: ShellyAuth.Credentials?, quickly: Boolean = false): String? {
        repeat(2) { attempt ->
            val s = sessions[host] ?: handshake(host, c, quickly).session?.also { sessions[host] = it } ?: return null
            val (seq, payload) = s.encrypt(json)
            val (code, body, _) = post(host, "/app/request?seq=$seq", payload, s.cookie, quickly)
            if (code == 200 && body != null && body.size > 32) return runCatching { s.decrypt(seq, body) }.getOrNull()
            sessions.remove(host)
            if (attempt == 1) return null
        }
        return null
    }

    private fun credFor(id: String?, given: ShellyAuth.Credentials?) = given ?: id?.let { cred(it) } ?: cred(ACCOUNT)

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val c = credFor(found.values.firstOrNull { it.host == host }?.id, cred)
        val hs = handshake(host, c, quickly)
        if (!hs.isKlap) return null
        val s = hs.session ?: return LanFound("tapo@$host", brand, host, "Tapo", 0, supported = false, locked = true, wrongPassword = hs.wrong)
        sessions[host] = s
        val info = request(host, TapoProtocol.INFO, c, quickly)?.let(TapoProtocol::parseInfo) ?: return null
        return LanFound(info.id, brand, host, info.model.ifEmpty { "Tapo" }, 1, supported = true, credentials = c, name = info.nickname)
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? {
        val c = credFor(d.id, cred)
        val info = request(host, TapoProtocol.INFO, c)?.let(TapoProtocol::parseInfo) ?: return null
        val power = if (d.extra == "e" || d.extra.isEmpty()) request(host, TapoProtocol.POWER, c)?.let(TapoProtocol::parsePower) else null
        return LocalReading(mapOf(0 to info.on), power = power)
    }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean =
        request(host, TapoProtocol.setOn(on), credFor(d.id, cred))?.let(TapoProtocol::isOk) == true
}
