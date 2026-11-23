package com.macerce.switchguard.api

import android.content.Context
import android.util.Log
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Yerel ağdan yoklanarak izlenen markaların ortak tabanı (Tasmota, ESPHome, Hue, Kasa, Tapo, Home Assistant).
 * Eklenen cihazlar [pollMs]'de bir okunur; arka arkaya [MISSES_OFFLINE] yanıtsızlıktan sonra çevrimdışı sayılır.
 * Alt sınıf [probe], [read] ve [switch]'i yazar; isterse [discover] ile mDNS araması ekler.
 */
abstract class PolledLan(val brand: String) {
    protected val tag = "Lan-$brand"
    open val pollMs = 5_000L

    protected val found = ConcurrentHashMap<String, LanFound>()
    private val readings = ConcurrentHashMap<String, LocalReading>()
    private val misses = ConcurrentHashMap<String, Int>()
    @Volatile private var devices: List<LocalDevice> = emptyList()
    @Volatile private var onChange: (() -> Unit)? = null
    @Volatile private var credentials: Map<String, ShellyAuth.Credentials> = emptyMap()

    protected val io = Executors.newScheduledThreadPool(2)
    private var poll: ScheduledFuture<*>? = null
    private var browser: MdnsBrowser? = null

    open val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS).readTimeout(4, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()

    /** Ağ taramasında kullanılan kısa zaman aşımlı istemci. */
    val quick: OkHttpClient by lazy {
        http.newBuilder().connectTimeout(600, TimeUnit.MILLISECONDS).readTimeout(1500, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
    }

    /** mDNS servis türü ve süzgeci (yoksa null: yalnız IP / ağ taraması). */
    open val mdnsType: String? = null
    open fun mdnsAccept(name: String): Boolean = true

    fun all(): Map<String, LanFound> = HashMap(found)
    fun reading(id: String): LocalReading? = readings[id]
    open fun host(d: LocalDevice): String? = found[d.id]?.host ?: d.ip

    /** Detay kartında gösterilen adres; ağ isteği yapmaz (ana thread'den çağrılabilir). */
    open fun seenHost(d: LocalDevice): String? = host(d)
    fun setCredentials(map: Map<String, ShellyAuth.Credentials>) { credentials = map }
    protected fun cred(id: String): ShellyAuth.Credentials? = credentials[id]

    /** Bulunanı listeye ekler (mDNS, tarama ya da IP ile). */
    fun remember(f: LanFound) { found[f.id] = f; onChange?.invoke() }

    /** Listeden çıkarır (ör. şifre doğrulanınca kilitli geçici kayıt). */
    fun forget(id: String) { found.remove(id) }

    @Synchronized
    open fun startDiscovery(context: Context) {
        val type = mdnsType ?: return
        val b = browser ?: MdnsBrowser(type, ::mdnsAccept, onFound = { _, host ->
            io.execute { probe(host, null, quickly = false)?.let(::remember) }
        }).also { browser = it }
        b.start(context)
    }

    @Synchronized
    fun start(context: Context, list: List<LocalDevice>, listener: () -> Unit) {
        devices = list.filter { it.brand == brand }
        onChange = listener
        if (devices.isEmpty()) { stop(); return }
        startDiscovery(context)
        if (poll == null) poll = io.scheduleWithFixedDelay({ pollAll() }, 0, pollMs, TimeUnit.MILLISECONDS)
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

    @Synchronized
    fun stopDiscoveryIfIdle() {
        if (poll == null) { browser?.stop(); browser = null }
    }

    fun pollNow() = io.execute { pollAll() }

    private fun pollAll() {
        var changed = false
        for (d in devices) {
            val host = host(d) ?: continue
            val r = runCatching { read(d, host, cred(d.id)) }.getOrNull()
            if (r != null) {
                misses.remove(d.id)
                if (readings.put(d.id, r) != r) changed = true
            } else {
                val n = (misses[d.id] ?: 0) + 1
                misses[d.id] = n
                if (n >= MISSES_OFFLINE && readings.remove(d.id) != null) {
                    changed = true
                    Log.i(tag, "${d.id} unreachable at $host")
                }
            }
        }
        if (changed) onChange?.invoke()
    }

    /**
     * Adreste bu markadan cihaz var mı. Şifreliyse [cred] (yoksa kayıtlı şifre) denenir: şifre yoksa
     * [LanFound.locked], yanlışsa ayrıca [LanFound.wrongPassword]. [quickly]: ağ taraması (kısa zaman aşımı).
     * Bloklayıcı; ana thread'den çağırmayın. Bulunamazsa null.
     */
    abstract fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound?

    protected abstract fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading?

    protected abstract fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean

    /** Bloklayıcı. Kabul edilirse okuma hemen güncellenir ve bir yoklama turu başlar. */
    fun trySwitch(d: LocalDevice, channel: Int, on: Boolean): Boolean {
        val host = host(d) ?: return false
        val ok = runCatching { switch(d, host, channel, on, cred(d.id)) }.getOrDefault(false)
        Log.i(tag, "switch ${d.id} ch=$channel on=$on ok=$ok")
        if (ok) {
            readings[d.id]?.let { readings[d.id] = it.copy(switches = it.switches + (channel to on)) }
            pollNow()
        }
        return ok
    }

    /** HTTP sonucu: [code] 0 = ulaşılamadı; [body] yalnız başarılı yanıtta (ya da [keepErrorBody] ile her zaman). */
    class Res(val code: Int, val body: String?)

    protected fun get(
        url: String, headers: Map<String, String> = emptyMap(), quickly: Boolean = false, keepErrorBody: Boolean = false,
        client: OkHttpClient? = null,
    ): Res = call(Request.Builder().url(url).also { b -> headers.forEach { (k, v) -> b.header(k, v) } }.build(), quickly, keepErrorBody, client)

    protected fun send(
        method: String, url: String, body: String, headers: Map<String, String> = emptyMap(), quickly: Boolean = false,
        keepErrorBody: Boolean = false, client: OkHttpClient? = null,
    ): Res {
        val rb = body.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).method(method, rb).also { b -> headers.forEach { (k, v) -> b.header(k, v) } }.build()
        return call(req, quickly, keepErrorBody, client)
    }

    private fun call(req: Request, quickly: Boolean, keepErrorBody: Boolean, client: OkHttpClient?): Res = runCatching {
        (client ?: if (quickly) quick else http).newCall(req).execute().use { r ->
            Res(r.code, if (r.isSuccessful || keepErrorBody) r.body?.string() else null)
        }
    }.getOrElse { Res(0, null) }

    companion object {
        const val MISSES_OFFLINE = 3
    }
}
