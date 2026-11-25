package com.macerce.switchguard.api

import android.content.Context
import android.util.Log
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import com.macerce.switchguard.core.ShellyProtocol
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * Ağda bulunan ve tanınan Shelly. [supported]: rölesi var ve (şifreliyse) şifre doğrulandı, eklenebilir.
     * [credentials]: şifreli cihazda doğrulanan giriş bilgisi; [wrongPassword]: denenen şifreyi cihaz reddetti.
     */
    data class Found(
        val host: String, val info: ShellyProtocol.Info, val supported: Boolean,
        val credentials: ShellyAuth.Credentials? = null, val wrongPassword: Boolean = false,
    ) {
        /** Şifreli ve geçerli şifre henüz yok: eklemek için şifre sorulur. */
        val locked: Boolean get() = info.auth && credentials == null
    }

    /** Shelly Gen2 Digest sorgusu adres başına saklanır: her istekte iki tur (401 + yeniden) olmasın. */
    private class Digest(val challenge: ShellyAuth.Challenge) { val nc = AtomicInteger(0) }
    private val digests = ConcurrentHashMap<String, Digest>()
    private val random = SecureRandom()
    @Volatile private var credentials: Map<String, ShellyAuth.Credentials> = emptyMap()

    /** Kayıtlı şifreler (Store.shellyAuth); izleme başlarken ve şifre eklenince verilir. */
    fun setCredentials(map: Map<String, ShellyAuth.Credentials>) { credentials = map }

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
            val r = get(host, ShellyProtocol.statusPath(d.gen), d.gen, credentials[d.id]).body?.let { ShellyProtocol.parseStatus(d.gen, it, d.channels) }
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
     * Şifreliyse [cred] (yoksa kayıtlı şifre) ile durum okunarak şifre doğrulanır. Gen2'de kanal sayısı durumdan
     * sayılır; ölçen/ölçmeyen ayrımı yoklamada anlaşılır.
     */
    fun probe(host: String, cred: ShellyAuth.Credentials? = null): Found? {
        val info = get(host, "/shelly", 0, null).body?.let(ShellyProtocol::parseInfo) ?: return null
        val c = if (info.auth) cred ?: credentials[info.id] else null
        if (info.auth && c == null) return Found(host, info, supported = false)
        val status = get(host, ShellyProtocol.statusPath(info.gen), info.gen, c)
        if (info.auth && status.code == 401) return Found(host, info, supported = false, wrongPassword = cred != null)
        val full = if (info.gen >= 2) info.copy(channels = status.body?.let(ShellyProtocol::channelsOf) ?: 0) else info
        val ok = !info.auth || status.body != null
        // Rölesiz ölçer/sensör (EM, PM Mini, Add-on, H&T) de eklenir: durumda okunacak bir şey varsa.
        val readable = full.channels > 0 || status.body?.let { ShellyProtocol.parseStatus(info.gen, it, 0) } != null
        return Found(host, full, supported = ok && readable, credentials = c.takeIf { ok })
    }

    /**
     * Gen2+ Shelly'nin kendi Wi-Fi erişim noktasını ev ağı üzerinden kapatır (kurulumdan sonra açık kalmasın).
     * Döner: cihaz kabul etti mi. Bloklayıcı.
     */
    fun disableAp(host: String, gen: Int, cred: ShellyAuth.Credentials? = null): Boolean {
        val r = get(host, ShellyProtocol.apOffPath(), gen, cred).body ?: return false
        if (r.contains("\"code\"")) return false
        if (ShellyProtocol.restartRequired(r)) get(host, ShellyProtocol.rebootPath(gen), gen, cred)
        Log.i(TAG, "AP disabled at $host")
        return true
    }

    /** Döner: cihaz kabul etti mi. Bloklayıcı. */
    fun trySwitch(device: LocalDevice, channel: Int, on: Boolean): Boolean {
        val host = host(device) ?: return false
        val ok = get(host, ShellyProtocol.switchPath(device.gen, channel, on), device.gen, credentials[device.id]).body?.let { ShellyProtocol.isSwitchOk(device.gen, it, on) } == true
        Log.i(TAG, "switch ${device.id} ch=$channel on=$on ok=$ok")
        if (ok) {
            readings[device.id]?.let { readings[device.id] = it.copy(switches = it.switches + (channel to on)) }
            pollNow()
        }
        return ok
    }

    /** HTTP sonucu: [code] 0 = ulaşılamadı; [body] yalnızca başarılı yanıtta. */
    private class Res(val code: Int, val body: String?)

    /**
     * GET; [cred] varsa kimlik doğrulamalı. Gen1 Basic başlığı ilk istekte gider. Gen2 Digest: bilinen sorgu varsa
     * ilk istekte kullanılır; 401 gelirse (yeni ya da süresi dolmuş nonce) sorgu yenilenip bir kez daha denenir.
     */
    private fun get(host: String, path: String, gen: Int, cred: ShellyAuth.Credentials?): Res = runCatching {
        fun call(auth: String?): Pair<Res, String?> {
            val b = Request.Builder().url("http://$host$path")
            if (auth != null) b.header("Authorization", auth)
            return http.newCall(b.build()).execute().use { r ->
                Res(r.code, if (r.isSuccessful) r.body?.string() else null) to r.header("WWW-Authenticate")
            }
        }
        if (cred == null) return@runCatching call(null).first
        if (gen <= 1) return@runCatching call(ShellyAuth.basicHeader(cred)).first
        val known = digests[host]
        val (first, challenge) = call(known?.let { digestFor(it, cred, path) })
        if (first.code != 401) return@runCatching first
        val fresh = ShellyAuth.parseChallenge(challenge)?.let(::Digest) ?: return@runCatching first
        digests[host] = fresh
        call(digestFor(fresh, cred, path)).first
    }.getOrElse { Res(0, null) }

    private fun digestFor(d: Digest, cred: ShellyAuth.Credentials, uri: String): String {
        val cnonce = ByteArray(8).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        // Gen2'de kullanıcı adı her zaman "admin".
        return ShellyAuth.digestHeader(cred.copy(user = ShellyAuth.DEFAULT_USER), d.challenge, "GET", uri, d.nc.incrementAndGet(), cnonce)
    }
}
