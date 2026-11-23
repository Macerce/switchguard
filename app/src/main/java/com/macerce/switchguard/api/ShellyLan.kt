package com.macerce.switchguard.api

import android.content.Context
import android.util.Log
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyProtocol
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Shelly cihazlarını yerel ağda bulur (mDNS `_http._tcp`, adı "shelly…"), eklenenleri birkaç saniyede bir yoklar
 * ve aç/kapa komutunu doğrudan cihaza gönderir (bkz. [ShellyProtocol]). Bulut ve hesap gerekmez.
 * IP değişirse (router yeni adres verdiyse) mDNS'ten yeni adres öğrenilir.
 */
object ShellyLan {
    private const val TAG = "ShellyLan"
    private const val POLL_MS = 5_000L
    /** Arka arkaya bu kadar yanıtsız yoklamadan sonra çevrimdışı sayılır (tek kayıp paket alarm çaldırmasın). */
    private const val MISSES_OFFLINE = 3

    /** Ağda bulunan ve tanınan Shelly. [supported]: rölesi var ve şifresiz (eklenebilir). */
    data class Found(val host: String, val info: ShellyProtocol.Info, val supported: Boolean)

    private val found = ConcurrentHashMap<String, Found>()
    private val readings = ConcurrentHashMap<String, LocalReading>()
    private val misses = ConcurrentHashMap<String, Int>()
    @Volatile private var devices: List<LocalDevice> = emptyList()
    @Volatile private var onChange: (() -> Unit)? = null

    private val io = Executors.newScheduledThreadPool(2)
    private var poll: ScheduledFuture<*>? = null
    private var browser: MdnsBrowser? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS)
        // Shelly (özellikle Gen1) bağlantıyı yanıttan sonra kapatabilir: havuzdaki bayat bağlantıda yeniden dene.
        .retryOnConnectionFailure(true).build()

    fun all(): Map<String, Found> = HashMap(found)
    fun reading(id: String): LocalReading? = readings[id]

    /** Cihazın şu anki adresi: mDNS'te görüldüyse oradan, değilse eklenirken kaydedilen IP. */
    fun host(device: LocalDevice): String? = found[device.id]?.host ?: device.ip

    /** Ağda arama (ekleme penceresi ve izleme). Çağrı birden fazla kez yapılabilir. */
    @Synchronized
    fun startDiscovery(context: Context) {
        val b = browser ?: MdnsBrowser("_http._tcp", ShellyProtocol::isShellyServiceName, onFound = { _, host ->
            io.execute { probe(host)?.let { f -> found[f.info.id] = f; onChange?.invoke() } }
        }).also { browser = it }
        b.start(context)
    }

    /** İzleme: [list]'teki Shelly'leri yoklar; durum değişince ya da cihaz görünüp kaybolunca [listener]. */
    @Synchronized
    fun start(context: Context, list: List<LocalDevice>, listener: () -> Unit) {
        devices = list.filter { it.isShelly }
        onChange = listener
        if (devices.isEmpty()) { stop(); return }
        startDiscovery(context)
        if (poll == null) poll = io.scheduleWithFixedDelay({ pollAll() }, 0, POLL_MS, TimeUnit.MILLISECONDS)
    }

    @Synchronized
    fun stop() {
        poll?.cancel(false)
        poll = null
        browser?.stop()
        browser = null
        onChange = null
        devices = emptyList()
        readings.clear()
        misses.clear()
    }

    /** Ekleme penceresi kapanınca: izleme Shelly yoklamıyorsa aramayı da durdur. */
    @Synchronized
    fun stopDiscoveryIfIdle() {
        if (poll == null) { browser?.stop(); browser = null }
    }

    /** Bir yoklama turu (bilinen sonuç da hemen ekrana gitsin diye anında komuttan sonra da çağrılır). */
    fun pollNow() = io.execute { pollAll() }

    private fun pollAll() {
        var changed = false
        for (d in devices) {
            val host = host(d) ?: continue
            val r = get(host, ShellyProtocol.statusPath(d.gen))?.let { ShellyProtocol.parseStatus(d.gen, it, d.channels) }
            if (r != null) {
                misses.remove(d.id)
                if (readings.put(d.id, r) != r) changed = true
            } else {
                val n = (misses[d.id] ?: 0) + 1
                misses[d.id] = n
                if (n >= MISSES_OFFLINE && readings.remove(d.id) != null) {
                    changed = true
                    Log.i(TAG, "${d.id} unreachable at $host")
                }
            }
        }
        if (changed) onChange?.invoke()
    }

    /**
     * Verilen adreste Shelly var mı (IP ile ekleme ve mDNS'ten bulunan). Bloklayıcı; ana thread'den çağırmayın.
     * Gen2'de kanal sayısı durumdan sayılır; ölçen/ölçmeyen ayrımı yoklamada anlaşılır.
     */
    fun probe(host: String): Found? {
        val info = get(host, "/shelly")?.let(ShellyProtocol::parseInfo) ?: return null
        val full = if (info.gen >= 2 && !info.auth) {
            info.copy(channels = get(host, ShellyProtocol.statusPath(info.gen))?.let(ShellyProtocol::channelsOf) ?: 0)
        } else info
        return Found(host, full, supported = !full.auth && full.channels > 0)
    }

    /** Döner: cihaz kabul etti mi. Bloklayıcı. */
    fun trySwitch(device: LocalDevice, channel: Int, on: Boolean): Boolean {
        val host = host(device) ?: return false
        val ok = get(host, ShellyProtocol.switchPath(device.gen, channel, on))?.let { ShellyProtocol.isSwitchOk(device.gen, it, on) } == true
        Log.i(TAG, "switch ${device.id} ch=$channel on=$on ok=$ok")
        if (ok) {
            readings[device.id]?.let { readings[device.id] = it.copy(switches = it.switches + (channel to on)) }
            pollNow()
        }
        return ok
    }

    private fun get(host: String, path: String): String? = runCatching {
        http.newCall(Request.Builder().url("http://$host$path").build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }
    }.getOrNull()
}
