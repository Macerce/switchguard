package com.macerce.switchguard.api

import com.macerce.switchguard.core.HueProtocol
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import java.util.concurrent.ConcurrentHashMap

/**
 * Philips Hue köprüsü (bkz. [HueProtocol]). Keşif mDNS `_hue._tcp`. Köprü listede kilitli görünür; düğmesine basılıp
 * [pair] edilince anahtar `hue-bridge-<kimlik>` adıyla gizli depoya yazılır ve köprünün lambaları ayrı ayrı listelenir.
 * Yoklamada aynı köprünün lambaları tek istekle okunur (kısa önbellek).
 */
object HueLan : PolledLan(LocalDevice.BRAND_HUE) {
    override val mdnsType: String = "_hue._tcp"

    /** Köprü satırlarının [LanFound.extra] işareti (lamba satırlarından ayırmak için). */
    const val BRIDGE = "bridge"

    private class Cache(val at: Long, val lights: List<HueProtocol.Light>)
    private val cache = ConcurrentHashMap<String, Cache>()
    private val bridgeHosts = ConcurrentHashMap<String, String>()

    private fun keyFor(bridgeId: String): String? = cred(HueProtocol.bridgeKey(bridgeId))?.user

    override fun host(d: LocalDevice): String? =
        HueProtocol.parseExtra(d.extra)?.first?.let { bridgeHosts[it] } ?: d.ip

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val b = get("http://$host/api/config", quickly = quickly).body?.let(HueProtocol::parseConfig) ?: return null
        bridgeHosts[b.id] = host
        val key = cred?.user ?: keyFor(b.id)
        if (key != null) {
            val lights = fetch(host, key, force = true)
            if (lights != null) {
                lights.forEach { l ->
                    remember(LanFound(
                        HueProtocol.lightId(b.id, l.num), brand, host, l.type.ifEmpty { "Hue" }, 1, supported = true,
                        extra = HueProtocol.extraFor(b.id, l.num), name = l.name,
                        credentials = ShellyAuth.Credentials(key, ""),
                    ))
                }
                return LanFound(HueProtocol.bridgeKey(b.id), brand, host, b.name, 0, supported = false, extra = BRIDGE, name = b.name,
                    credentials = ShellyAuth.Credentials(key, ""))
            }
        }
        return LanFound(HueProtocol.bridgeKey(b.id), brand, host, b.name, 0, supported = false, locked = true, extra = BRIDGE, name = b.name)
    }

    /**
     * Köprüyle eşleşir (düğmeye basılmış olmalı). Döner: anahtar, ya da null ([linkButton] = düğmeye henüz basılmadı).
     * Bloklayıcı.
     */
    fun pair(host: String): Pair<String?, Boolean> {
        val r = send("POST", "http://$host/api", HueProtocol.pairBody()).body ?: return null to false
        val (key, err) = HueProtocol.parsePair(r)
        return key to (err == HueProtocol.ERROR_LINK_BUTTON)
    }

    private fun fetch(host: String, key: String, force: Boolean = false): List<HueProtocol.Light>? {
        val now = System.currentTimeMillis()
        cache[host]?.takeIf { !force && now - it.at < 2_000 }?.let { return it.lights }
        val body = get("http://$host/api/$key/lights").body ?: return null
        val lights = HueProtocol.parseLights(body) ?: return null
        cache[host] = Cache(now, lights)
        return lights
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? {
        val (bridge, num) = HueProtocol.parseExtra(d.extra) ?: return null
        val key = keyFor(bridge) ?: return null
        val l = fetch(host, key)?.firstOrNull { it.num == num } ?: return null
        // Elektriği kesilen lamba köprüde "reachable=false" görünür: çevrimdışı sayılır.
        if (!l.reachable) return null
        return LocalReading(mapOf(0 to l.on))
    }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean {
        val (bridge, num) = HueProtocol.parseExtra(d.extra) ?: return false
        val key = keyFor(bridge) ?: return false
        cache.remove(host)
        return send("PUT", "http://$host/api/$key/lights/$num/state", HueProtocol.stateBody(on)).body?.let(HueProtocol::isStateOk) == true
    }
}
