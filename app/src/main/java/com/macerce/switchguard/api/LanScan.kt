package com.macerce.switchguard.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Telefonun bağlı olduğu Wi-Fi'nin /24 alt ağını (ör. 192.168.1.1–254) tarar; her adreste [probe] çağrılır.
 * mDNS yayınlamayan cihazlar (Tasmota) için. ~254 kısa istek, 48 paralel: birkaç saniye sürer.
 */
object LanScan {
    /** Wi-Fi'deki IPv4 adresi ("192.168.1.23"); Wi-Fi yoksa null. */
    fun wifiAddress(context: Context): String? = runCatching {
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return null
        @Suppress("DEPRECATION")
        val net = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
            ?: return null
        cm.getLinkProperties(net)?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }.getOrNull()

    /** /24 adresleri (telefonun kendisi hariç). */
    fun hostsOf(address: String): List<String> {
        val parts = address.split('.')
        if (parts.size != 4) return emptyList()
        val prefix = parts.take(3).joinToString(".")
        return (1..254).map { "$prefix.$it" }.filter { it != address }
    }

    /** Bloklayıcı. [onProgress]: 0..100. Bulunan sayısını döner; Wi-Fi yoksa -1. */
    fun scan(context: Context, probe: (String) -> Boolean, onProgress: (Int) -> Unit = {}): Int {
        val me = wifiAddress(context) ?: return -1
        val hosts = hostsOf(me)
        val pool = Executors.newFixedThreadPool(48)
        val done = AtomicInteger(0)
        val hits = AtomicInteger(0)
        hosts.forEach { h ->
            pool.execute {
                if (runCatching { probe(h) }.getOrDefault(false)) hits.incrementAndGet()
                onProgress(done.incrementAndGet() * 100 / hosts.size)
            }
        }
        pool.shutdown()
        pool.awaitTermination(60, TimeUnit.SECONDS)
        return hits.get()
    }
}
