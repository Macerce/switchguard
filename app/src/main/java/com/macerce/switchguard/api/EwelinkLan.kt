package com.macerce.switchguard.api

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.LanProtocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * eWeLink cihazlarını yerel ağda (mDNS) bulur, TXT duyurularından durumlarını okur ve komutu buluta uğramadan
 * doğrudan cihaza gönderir (bkz. [LanProtocol]). Yalnızca izleme açıkken ve ayar açıksa çalışır.
 * Bulut kapalıyken (internet yok, eWeLink sunucusu çöktü) aç/kapa ve durum değişiklikleri yerel ağdan sürer.
 */
object EwelinkLan {
    private const val TAG = "EwelinkLan"
    /** Android 14 altında TXT güncellemesi kendiliğinden gelmez: bilinen cihazlar bu aralıkla yeniden okunur. */
    private const val RERESOLVE_MS = 30_000L

    /** Yerel ağda görülen cihaz. */
    data class Entry(val host: String, val port: Int, val seq: Long, val switches: Map<Int, Boolean>, val seenAt: Long)

    private val entries = ConcurrentHashMap<String, Entry>()
    /** mDNS servis adı → cihaz kimliği (servis kaybolunca kaydı silmek için). */
    private val serviceIds = ConcurrentHashMap<String, String>()
    @Volatile private var keys: Map<String, String> = emptyMap()
    @Volatile private var onAnnounce: ((String, Map<Int, Boolean>) -> Unit)? = null

    private var nsd: NsdManager? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private val infoCallbacks = HashMap<String, Any>()
    private val known = LinkedHashMap<String, NsdServiceInfo>()
    private val queue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val http = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).writeTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()

    /** Yerel ağda bulunan cihaz sayısı (ayarlar ekranı için). */
    val count: Int get() = entries.size

    fun entry(deviceId: String): Entry? = entries[deviceId]

    val running: Boolean get() = discovery != null

    /** Cihaz anahtarları (buluttan gelen devicekey). Şifreli duyurular bunlarla çözülür. */
    fun setKeys(map: Map<String, String>) {
        keys = map
    }

    /**
     * Keşfi başlatır. [listener]: bir cihazın anahtar durumu yerel ağdan okununca (yalnızca yeni duyuruda) çağrılır;
     * herhangi bir thread'den gelebilir.
     */
    fun start(context: Context, listener: (deviceId: String, switches: Map<Int, Boolean>) -> Unit) = main.post {
        onAnnounce = listener
        if (discovery != null) return@post
        val manager = context.applicationContext.getSystemService(NsdManager::class.java) ?: return@post
        nsd = manager
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "discovery failed: $errorCode")
                main.post { if (discovery === this) discovery = null }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) = main.post { found(info) }.let { }
            override fun onServiceLost(info: NsdServiceInfo) = main.post { lost(info.serviceName) }.let { }
        }
        discovery = l
        runCatching { manager.discoverServices(LanProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
            .onFailure { Log.w(TAG, "discoverServices", it); discovery = null }
        main.removeCallbacks(reresolve)
        if (Build.VERSION.SDK_INT < 34) main.postDelayed(reresolve, RERESOLVE_MS)
    }

    fun stop() = main.post {
        onAnnounce = null
        main.removeCallbacks(reresolve)
        val manager = nsd
        discovery?.let { l -> runCatching { manager?.stopServiceDiscovery(l) } }
        discovery = null
        if (Build.VERSION.SDK_INT >= 34) infoCallbacks.values.forEach { cb ->
            runCatching { manager?.unregisterServiceInfoCallback(cb as NsdManager.ServiceInfoCallback) }
        }
        infoCallbacks.clear()
        known.clear()
        queue.clear()
        resolving = false
        entries.clear()
        serviceIds.clear()
    }

    private val reresolve = object : Runnable {
        override fun run() {
            if (discovery == null) return
            known.values.forEach { s -> if (queue.none { it.serviceName == s.serviceName }) queue.addLast(s) }
            resolveNext()
            main.postDelayed(this, RERESOLVE_MS)
        }
    }

    private fun found(info: NsdServiceInfo) {
        if (discovery == null || known.containsKey(info.serviceName)) return
        known[info.serviceName] = info
        val manager = nsd ?: return
        if (Build.VERSION.SDK_INT >= 34) {
            val cb = object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Log.w(TAG, "info callback failed: $errorCode").let { }
                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) = read(serviceInfo)
                override fun onServiceLost() = main.post { lost(info.serviceName) }.let { }
                override fun onServiceInfoCallbackUnregistered() = Unit
            }
            runCatching { manager.registerServiceInfoCallback(info, executor, cb); infoCallbacks[info.serviceName] = cb }
                .onFailure { Log.w(TAG, "registerServiceInfoCallback", it) }
        } else {
            queue.addLast(info)
            resolveNext()
        }
    }

    private fun lost(serviceName: String) {
        known.remove(serviceName)
        if (Build.VERSION.SDK_INT >= 34) (infoCallbacks.remove(serviceName) as? NsdManager.ServiceInfoCallback)?.let { cb ->
            runCatching { nsd?.unregisterServiceInfoCallback(cb) }
        }
        serviceIds.remove(serviceName)?.let { entries.remove(it) }
    }

    @Suppress("DEPRECATION")
    private fun resolveNext() {
        if (resolving || Build.VERSION.SDK_INT >= 34) return
        val manager = nsd ?: return
        val next = queue.removeFirstOrNull() ?: return
        resolving = true
        val l = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = main.post { resolving = false; resolveNext() }.let { }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                executor.execute { read(serviceInfo) }
                main.post { resolving = false; resolveNext() }
            }
        }
        runCatching { manager.resolveService(next, l) }.onFailure { resolving = false }
    }

    @Suppress("DEPRECATION")
    private fun read(info: NsdServiceInfo) {
        val txt = info.attributes.mapValues { (_, v) -> v?.toString(Charsets.UTF_8).orEmpty() }
        val id = txt["id"] ?: return
        val announce = LanProtocol.parseTxt(txt, keys[id]) ?: return
        val address: InetAddress = (if (Build.VERSION.SDK_INT >= 34) info.hostAddresses.firstOrNull { it is Inet4Address } ?: info.hostAddresses.firstOrNull() else info.host)
            ?: return
        val host = address.hostAddress?.let { if (address is Inet4Address) it else "[${it.substringBefore('%')}]" } ?: return
        serviceIds[info.serviceName] = id
        val previous = entries[id]
        val switches = com.macerce.switchguard.core.DeviceParser.parseSwitches(announce.params)
        entries[id] = Entry(host, info.port.takeIf { it > 0 } ?: LanProtocol.DEFAULT_PORT, announce.seq,
            switches.ifEmpty { previous?.switches.orEmpty() }, System.currentTimeMillis())
        if (switches.isNotEmpty() && (previous == null || previous.seq != announce.seq || previous.switches != switches)) {
            onAnnounce?.invoke(id, switches)
        }
    }

    /**
     * Komutu yerel ağdan dener. Döner: cihaz kabul etti mi. Cihaz yerel ağda görülmediyse ya da yanıt vermediyse
     * false (çağıran buluta düşer). Bloklayıcıdır; ana thread'den çağırmayın.
     */
    fun trySwitch(device: DeviceSnapshot, channel: Int, on: Boolean): Boolean {
        val e = entries[device.id] ?: return false
        val cmd = LanProtocol.switchCommand(device.id, keys[device.id], channel, on, multi = device.usesSwitchArray || channel > 0)
        val req = Request.Builder().url("http://${e.host}:${e.port}${cmd.path}")
            .post(cmd.body.toRequestBody("application/json".toMediaType())).build()
        return runCatching { http.newCall(req).execute().use { it.isSuccessful && LanProtocol.isOk(it.body?.string().orEmpty()) } }
            .onFailure { Log.w(TAG, "LAN switch failed for ${device.id}: ${it.javaClass.simpleName}") }
            .getOrDefault(false)
            .also { ok -> if (ok) entries[device.id] = e.copy(switches = e.switches + (channel to on)) }
    }
}
