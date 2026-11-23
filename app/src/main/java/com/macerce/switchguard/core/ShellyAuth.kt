package com.macerce.switchguard.core

import java.security.MessageDigest
import java.util.Base64

/**
 * Şifreli Shelly'ye kimlik doğrulama (saf; ağ işi [com.macerce.switchguard.api.ShellyLan]).
 * - Gen1: HTTP Basic, kullanıcı adı ayarlanabilir (varsayılan "admin").
 * - Gen2+: HTTP Digest (RFC 7616), kullanıcı her zaman "admin", realm = cihaz kimliği, algoritma SHA-256, qop=auth.
 * `/shelly` iki kuşakta da şifresiz açıktır; yalnızca durum ve komut adresleri korunur.
 */
object ShellyAuth {
    const val DEFAULT_USER = "admin"

    /** Kayıtlı giriş bilgisi. Gen2'de [user] kullanılmaz (her zaman admin). */
    data class Credentials(val user: String, val password: String)

    /** `WWW-Authenticate: Digest ...` başlığı. */
    data class Challenge(val realm: String, val nonce: String, val algorithm: String, val qop: String?, val opaque: String?)

    fun basicHeader(c: Credentials): String =
        "Basic " + Base64.getEncoder().encodeToString("${c.user}:${c.password}".toByteArray(Charsets.UTF_8))

    /** Digest sorgusunu ayrıştırır; Digest değilse null. Tırnaklı ve tırnaksız değerler, virgüllü qop listesi. */
    fun parseChallenge(header: String?): Challenge? {
        if (header == null) return null
        val h = header.trim()
        if (!h.startsWith("Digest", ignoreCase = true)) return null
        val params = HashMap<String, String>()
        Regex("""(\w+)\s*=\s*(?:"((?:[^"\\]|\\.)*)"|([^,\s]+))""").findAll(h.substring(6)).forEach { m ->
            params[m.groupValues[1].lowercase()] = m.groups[2]?.value ?: m.groupValues[3]
        }
        val nonce = params["nonce"] ?: return null
        val qop = params["qop"]?.split(',')?.map { it.trim() }?.let { list -> if ("auth" in list) "auth" else list.firstOrNull() }
        return Challenge(params["realm"].orEmpty(), nonce, params["algorithm"] ?: "MD5", qop, params["opaque"])
    }

    /**
     * `Authorization: Digest ...` başlığı. [nc]: aynı nonce ile kaçıncı istek (1'den başlar), [cnonce]: istemci
     * rastgelesi. SHA-256 ve MD5 desteklenir (Shelly SHA-256 kullanır).
     */
    fun digestHeader(c: Credentials, ch: Challenge, method: String, uri: String, nc: Int, cnonce: String): String {
        val algo = if (ch.algorithm.startsWith("SHA-256", ignoreCase = true)) "SHA-256" else "MD5"
        fun h(s: String) = hex(MessageDigest.getInstance(algo).digest(s.toByteArray(Charsets.UTF_8)))
        val ha1 = h("${c.user}:${ch.realm}:${c.password}")
        val ha2 = h("$method:$uri")
        val ncs = "%08x".format(nc)
        val response = if (ch.qop != null) h("$ha1:${ch.nonce}:$ncs:$cnonce:${ch.qop}:$ha2") else h("$ha1:${ch.nonce}:$ha2")
        return buildString {
            append("Digest username=\"${c.user}\", realm=\"${ch.realm}\", nonce=\"${ch.nonce}\", uri=\"$uri\", algorithm=${ch.algorithm}, response=\"$response\"")
            if (ch.qop != null) append(", qop=${ch.qop}, nc=$ncs, cnonce=\"$cnonce\"")
            ch.opaque?.let { append(", opaque=\"$it\"") }
        }
    }

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    /** Kayıt biçimi: cihaz → {u, p}. Bozuk girdiler atlanır. */
    fun mapToJson(map: Map<String, Credentials>): String = org.json.JSONObject().apply {
        map.forEach { (id, c) -> put(id, org.json.JSONObject().put("u", c.user).put("p", c.password)) }
    }.toString()

    fun mapFromJson(json: String?): Map<String, Credentials> {
        if (json.isNullOrEmpty()) return emptyMap()
        val o = runCatching { org.json.JSONObject(json) }.getOrNull() ?: return emptyMap()
        return o.keys().asSequence().mapNotNull { id ->
            o.optJSONObject(id)?.let { c -> id to Credentials(c.optString("u").ifEmpty { DEFAULT_USER }, c.optString("p")) }
        }.toMap()
    }
}
