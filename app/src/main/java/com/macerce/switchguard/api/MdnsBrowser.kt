package com.macerce.switchguard.api

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.net.Inet4Address
import java.util.concurrent.Executors

/**
 * Yerel ağda bir mDNS servis türünü arar ve bulunanların IPv4 adresini verir (Shelly, ileride Hue ...).
 * [accept]: servis adına göre süzgeç. Android 14 altında çözümleme tek tek ve meşgulse kısa aralıkla yeniden denenir
 * (başka bir arama aynı anda çözümlüyor olabilir). Geri çağrılar ana thread'den gelir.
 */
class MdnsBrowser(
    private val serviceType: String,
    private val accept: (String) -> Boolean,
    private val onFound: (name: String, host: String) -> Unit,
    private val onLost: (name: String) -> Unit = {},
) {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var nsd: NsdManager? = null
    private var discovery: NsdManager.DiscoveryListener? = null
    private val queue = ArrayDeque<Pair<NsdServiceInfo, Int>>()
    private val callbacks = HashMap<String, Any>()
    private var resolving = false

    val running: Boolean get() = discovery != null

    fun start(context: Context) = main.post {
        if (discovery != null) return@post
        val manager = context.applicationContext.getSystemService(NsdManager::class.java) ?: return@post
        nsd = manager
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "$serviceType discovery failed: $errorCode")
                main.post { if (discovery === this) discovery = null }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) = main.post { found(info) }.let { }
            override fun onServiceLost(info: NsdServiceInfo) = main.post { lost(info.serviceName) }.let { }
        }
        discovery = l
        runCatching { manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, l) }
            .onFailure { Log.w(TAG, "discoverServices $serviceType", it); discovery = null }
    }

    fun stop() = main.post {
        val manager = nsd
        discovery?.let { l -> runCatching { manager?.stopServiceDiscovery(l) } }
        discovery = null
        if (Build.VERSION.SDK_INT >= 34) callbacks.values.forEach { cb ->
            runCatching { manager?.unregisterServiceInfoCallback(cb as NsdManager.ServiceInfoCallback) }
        }
        callbacks.clear()
        queue.clear()
        resolving = false
        main.removeCallbacksAndMessages(RETRY_TOKEN)
    }

    private fun found(info: NsdServiceInfo) {
        if (discovery == null || !accept(info.serviceName) || callbacks.containsKey(info.serviceName)) return
        val manager = nsd ?: return
        if (Build.VERSION.SDK_INT >= 34) {
            val cb = object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) = Unit
                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress ?: return
                    main.post { onFound(info.serviceName, host) }
                }
                override fun onServiceLost() = main.post { lost(info.serviceName) }.let { }
                override fun onServiceInfoCallbackUnregistered() = Unit
            }
            runCatching { manager.registerServiceInfoCallback(info, executor, cb); callbacks[info.serviceName] = cb }
                .onFailure { Log.w(TAG, "registerServiceInfoCallback", it) }
        } else {
            callbacks[info.serviceName] = Unit
            queue.addLast(info to 0)
            resolveNext()
        }
    }

    private fun lost(name: String) {
        val cb = callbacks.remove(name) ?: return
        if (Build.VERSION.SDK_INT >= 34) runCatching { nsd?.unregisterServiceInfoCallback(cb as NsdManager.ServiceInfoCallback) }
        onLost(name)
    }

    @Suppress("DEPRECATION")
    private fun resolveNext() {
        if (resolving || discovery == null) return
        val manager = nsd ?: return
        val (next, tries) = queue.removeFirstOrNull() ?: return
        resolving = true
        val l = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = main.post {
                resolving = false
                // 3 = FAILURE_ALREADY_ACTIVE: başka çözümleme sürüyor; biraz sonra yeniden dene.
                if (tries < MAX_TRIES) main.postAtTime({ queue.addLast(next to tries + 1); resolveNext() }, RETRY_TOKEN, android.os.SystemClock.uptimeMillis() + RETRY_MS)
                resolveNext()
            }.let { }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) = main.post {
                resolving = false
                (serviceInfo.host as? Inet4Address)?.hostAddress?.let { onFound(next.serviceName, it) }
                resolveNext()
            }.let { }
        }
        runCatching { manager.resolveService(next, l) }.onFailure { resolving = false }
    }

    private companion object {
        const val TAG = "MdnsBrowser"
        const val MAX_TRIES = 5
        const val RETRY_MS = 1500L
        val RETRY_TOKEN = Any()
    }
}
