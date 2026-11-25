package com.macerce.switchguard.api

import com.macerce.switchguard.core.EsphomeProtocol
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * ESPHome cihazları (`web_server:` bileşeni açık olmalı; bkz. [EsphomeProtocol]). Keşif mDNS `_esphomelib._tcp`.
 * `web_server` kullanıcı adı/şifresi ayarlıysa HTTP Basic ile girilir.
 */
object EsphomeLan : PolledLan(LocalDevice.BRAND_ESPHOME) {
    override val mdnsType: String = "_esphomelib._tcp"

    private fun auth(c: ShellyAuth.Credentials?) = c?.let { mapOf("Authorization" to ShellyAuth.basicHeader(it)) }.orEmpty()

    /** SSE akışının ilk ~2,5 saniyesini okur (ping + tüm varlıkların ilk durumu). */
    private fun events(host: String, c: ShellyAuth.Credentials?, quickly: Boolean): Res = runCatching {
        val client = (if (quickly) quick else http).newBuilder().readTimeout(3, TimeUnit.SECONDS).build()
        val req = Request.Builder().url("http://$host/events").also { b -> auth(c).forEach { (k, v) -> b.header(k, v) } }.build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@use Res(r.code, null)
            if (r.header("Content-Type")?.contains("event-stream") != true) return@use Res(r.code, null)
            val src = r.body?.source() ?: return@use Res(r.code, null)
            val sb = StringBuilder()
            val until = System.currentTimeMillis() + 2_500
            while (System.currentTimeMillis() < until && sb.length < 64_000) {
                val line = runCatching { src.readUtf8Line() }.getOrNull() ?: break
                sb.append(line).append('\n')
            }
            Res(r.code, sb.toString())
        }
    }.getOrElse { Res(0, null) }

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val c = cred ?: found.values.firstOrNull { it.host == host }?.let { cred(it.id) }
        val r = events(host, c, quickly)
        if (r.code == 401) return LanFound("esphome@$host", brand, host, "ESPHome", 0, supported = false, locked = true, wrongPassword = c != null)
        val e = r.body?.let(EsphomeProtocol::parseEvents) ?: return null
        return LanFound(
            EsphomeProtocol.idFor(e.title, host), brand, host, e.title ?: "ESPHome", e.switches.size,
            supported = e.switches.isNotEmpty(), credentials = c, extra = e.toJson(), name = e.title,
        )
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? {
        val e = EsphomeProtocol.entitiesFromJson(d.extra)
        val sw = LinkedHashMap<Int, Boolean>()
        e.switches.take(d.channels).forEachIndexed { i, p ->
            val on = get("http://$host/$p", auth(cred)).body?.let(EsphomeProtocol::parseOn) ?: return null
            sw[i] = on
        }
        if (sw.isEmpty()) return null
        val power = e.power?.let { get("http://$host/$it", auth(cred)).body?.let(EsphomeProtocol::parseValue) }
        return LocalReading(sw, power = power)
    }

    override fun level(d: LocalDevice, host: String, channel: Int, level: Int?, color: Int?, cred: ShellyAuth.Credentials?): Boolean {
        val p = EsphomeProtocol.entitiesFromJson(d.extra).switches.getOrNull(channel)?.takeIf { it.startsWith("light/") } ?: return false
        return send("POST", "http://$host${com.macerce.switchguard.core.LightLevel.esphomePath(p, level, color)}", "", auth(cred)).code in 200..299
    }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean {
        val p = EsphomeProtocol.entitiesFromJson(d.extra).switches.getOrNull(channel) ?: return false
        return send("POST", "http://$host${EsphomeProtocol.switchPath(p, on)}", "", auth(cred)).code in 200..299
    }
}
