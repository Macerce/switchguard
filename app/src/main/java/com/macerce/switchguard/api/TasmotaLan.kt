package com.macerce.switchguard.api

import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import com.macerce.switchguard.core.TasmotaProtocol

/** Tasmota cihazları (bkz. [TasmotaProtocol]). Tasmota varsayılan olarak mDNS yayınlamaz: IP ya da ağ taraması. */
object TasmotaLan : PolledLan(LocalDevice.BRAND_TASMOTA) {
    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val c = cred ?: found.values.firstOrNull { it.host == host }?.let { cred(it.id) }
        val r = get("http://$host${TasmotaProtocol.statusPath(c)}", quickly = quickly, keepErrorBody = true)
        val body = r.body ?: return null
        if (r.code == 401 || TasmotaProtocol.needsPassword(body)) {
            // Şifreliyken kimlik görünmez: adres kimlik yerine geçer, şifre doğrulanınca gerçek kimlik gelir.
            return LanFound("tasmota@$host", brand, host, "Tasmota", 0, supported = false, locked = true, wrongPassword = c != null)
        }
        if (r.code !in 200..299) return null
        val info = TasmotaProtocol.parseInfo(body) ?: return null
        return LanFound(info.id, brand, host, info.model, info.channels, supported = info.channels > 0, credentials = c, name = info.name)
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? =
        get("http://$host${TasmotaProtocol.statusPath(cred)}").body?.let { TasmotaProtocol.parseStatus(it, d.channels) }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean =
        get("http://$host${TasmotaProtocol.switchPath(channel, on, cred)}").body?.let { TasmotaProtocol.isSwitchOk(it, channel, on) } == true
}
