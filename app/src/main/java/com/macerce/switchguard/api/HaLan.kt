package com.macerce.switchguard.api

import com.macerce.switchguard.core.HaProtocol
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth

/**
 * Home Assistant sunucusu (bkz. [HaProtocol]). Keşif mDNS `_home-assistant._tcp` (ya da IP/tarama, port 8123).
 * Sunucu listede kilitli görünür; adres + erişim anahtarıyla bağlanınca aç/kapa yapılabilen varlıkları listelenir.
 * Hangi adresin kullanılacağı her 30 sn'de bir seçilir: yerel adres yanıt veriyorsa o, yoksa dış adres.
 */
object HaLan : PolledLan(LocalDevice.BRAND_HA) {
    override val mdnsType: String = "_home-assistant._tcp"

    @Volatile var server: HaProtocol.Server? = null

    @Volatile private var chosen: String? = null
    @Volatile private var chosenAt = 0L

    private fun auth(token: String) = mapOf("Authorization" to "Bearer $token")

    /** Kullanılacak adres (yerel önce). Bloklayıcı olabilir (en çok bir kısa istek). */
    fun base(): String? {
        val s = server ?: return null
        val now = System.currentTimeMillis()
        chosen?.takeIf { now - chosenAt < 30_000 }?.let { return it }
        val local = HaProtocol.normalize(s.local)
        val remote = HaProtocol.normalize(s.remote)
        val pick = when {
            local.isNotEmpty() && get("$local/api/", auth(s.token), quickly = true).code == 200 -> local
            remote.isNotEmpty() -> remote
            else -> local
        }
        chosen = pick
        chosenAt = now
        return pick
    }

    /** Adres hangisi (bağlantı testi ve detay kartı için): "local", "remote" ya da null. */
    fun route(): String? = server?.let { s ->
        when (chosen) {
            null -> null
            HaProtocol.normalize(s.local) -> "local"
            else -> "remote"
        }
    }

    fun forgetRoute() { chosen = null }

    override fun host(d: LocalDevice): String? = base()

    override fun seenHost(d: LocalDevice): String? = chosen

    /**
     * Sunucu sınaması: 200 = bağlandı, 401 = anahtar yanlış, 0 = ulaşılamadı (diğer kodlar olduğu gibi). Bloklayıcı.
     */
    fun test(url: String, token: String): Int = get("${HaProtocol.normalize(url)}/api/", auth(token)).code

    /** Bağlı sunucudaki aç/kapa yapılabilen varlıkları listeye koyar. Döner: sayı (bağlanamazsa -1). */
    fun loadEntities(): Int {
        val s = server ?: return -1
        val b = base() ?: return -1
        val body = get("$b/api/states", auth(s.token)).body ?: return -1
        val list = HaProtocol.parseStates(body)
        list.forEach { e ->
            remember(LanFound(HaProtocol.idFor(e.id), brand, b, e.domain, 1, supported = true, extra = e.id, name = e.name))
        }
        return list.size
    }

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val url = HaProtocol.normalize(host)
        val r = get("$url/api/", server?.let { auth(it.token) }.orEmpty(), quickly = quickly, keepErrorBody = true)
        val isHa = r.code == 401 || (r.code == 200 && r.body?.contains("API running") == true)
        if (!isHa) return null
        val connected = r.code == 200
        if (connected) io.execute { loadEntities() }
        return LanFound(
            "ha-server@" + url.substringAfter("://"), brand, url, "Home Assistant", 0, supported = false,
            locked = !connected, extra = HueLan.BRIDGE, name = "Home Assistant",
        )
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? {
        val s = server ?: return null
        val r = get("$host/api/states/${d.extra}", auth(s.token))
        if (r.code == 0) forgetRoute()
        val e = r.body?.let(HaProtocol::parseState) ?: return null
        if (!e.available) return null
        return LocalReading(mapOf(0 to e.on))
    }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean {
        val s = server ?: return false
        return send("POST", host + HaProtocol.servicePath(d.extra, on), HaProtocol.serviceBody(d.extra), auth(s.token)).code in 200..299
    }
}
