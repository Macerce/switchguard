package com.macerce.switchguard.api

import android.content.Context
import android.net.wifi.WifiManager
import com.macerce.switchguard.core.KasaProtocol
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import java.io.DataInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * TP-Link Kasa eski yerel protokolü (bkz. [KasaProtocol]): TCP 9999 sorgu/komut, UDP 9999 yayınıyla keşif.
 * Hesap ya da şifre gerekmez. Çoklu prizlerde her priz bir kanal (en çok 4).
 */
object KasaLan : PolledLan(LocalDevice.BRAND_KASA) {
    private const val PORT = 9999

    /** Bloklayıcı TCP sorgusu. */
    private fun query(host: String, json: String, quickly: Boolean): String? = runCatching {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, PORT), if (quickly) 600 else 2000)
            s.soTimeout = if (quickly) 1500 else 4000
            s.getOutputStream().apply { write(KasaProtocol.encrypt(json)); flush() }
            val input = DataInputStream(s.getInputStream())
            val n = input.readInt()
            if (n <= 0 || n > 64_000) return null
            val buf = ByteArray(n)
            input.readFully(buf)
            KasaProtocol.decrypt(buf)
        }
    }.getOrNull()

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val r = query(host, KasaProtocol.DISCOVER, quickly) ?: return null
        val i = KasaProtocol.parseInfo(r) ?: return null
        val channels = if (i.children.isEmpty()) 1 else i.children.size
        return LanFound(i.id, brand, host, i.model.ifEmpty { "Kasa" }, channels, supported = true, extra = KasaProtocol.extraFor(i), name = i.alias)
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? =
        query(host, KasaProtocol.QUERY, false)?.let { KasaProtocol.parseStatus(it, d.channels) }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean {
        val child = KasaProtocol.children(d.extra).getOrNull(channel)
        val cmd = KasaProtocol.switchCommand(on, child, KasaProtocol.isBulb(d.extra))
        return query(host, cmd, false)?.let(KasaProtocol::isOk) == true
    }

    /** Kasa mDNS yayınlamaz: ağa UDP yayını atılır, yanıt veren her cihaz listeye girer (3 kez, 2 sn arayla). */
    override fun startDiscovery(context: Context) {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        io.execute {
            val lock = runCatching { wifi?.createMulticastLock("switchguard-kasa")?.apply { setReferenceCounted(false); acquire() } }.getOrNull()
            try {
                DatagramSocket().use { sock ->
                    sock.broadcast = true
                    sock.soTimeout = 2000
                    val msg = KasaProtocol.encrypt(KasaProtocol.DISCOVER, withLength = false)
                    val buf = ByteArray(8192)
                    repeat(3) {
                        runCatching { sock.send(DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), PORT)) }
                        val until = System.currentTimeMillis() + 2000
                        while (System.currentTimeMillis() < until) {
                            val p = DatagramPacket(buf, buf.size)
                            if (runCatching { sock.receive(p) }.isFailure) break
                            val host = p.address.hostAddress ?: continue
                            val i = KasaProtocol.parseInfo(KasaProtocol.decrypt(p.data, 0, p.length)) ?: continue
                            remember(LanFound(
                                i.id, brand, host, i.model.ifEmpty { "Kasa" }, if (i.children.isEmpty()) 1 else i.children.size,
                                supported = true, extra = KasaProtocol.extraFor(i), name = i.alias,
                            ))
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { lock?.release() }
            }
        }
    }
}
