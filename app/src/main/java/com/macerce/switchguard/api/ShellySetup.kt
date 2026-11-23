package com.macerce.switchguard.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.PatternMatcher
import android.util.Log
import com.macerce.switchguard.core.ShellyProtocol
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Fabrika ayarlı (ya da sıfırlanmış) Shelly'yi uygulamadan ev Wi-Fi'sine tanıtır: telefon Android'in onay
 * penceresiyle Shelly'nin kendi ağına ("shelly…") geçici bağlanır, Wi-Fi adı ve şifresi cihaza yazılır, bağlantı
 * bırakılır; cihaz yeniden başlayıp ev ağına katılır ve ekleme penceresinde belirir. Telefonun internet bağlantısı
 * bu sırada değişmez (istekler yalnız o ağa bağlanır). Android 10+ gerekir; daha eskide elle kurulum anlatılır.
 */
object ShellySetup {
    private const val TAG = "ShellySetup"

    enum class Result { OK, NO_DEVICE_NETWORK, NOT_SHELLY, REJECTED, UNSUPPORTED }

    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Bloklayıcı (en çok ~70 sn); ana thread'den çağırmayın. [onStage]: 1 = cihaz ağına bağlanılıyor, 2 = ayar gönderiliyor. */
    fun provision(context: Context, ssid: String, password: String, onStage: (Int) -> Unit = {}): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Result.UNSUPPORTED
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return Result.NO_DEVICE_NETWORK
        val spec = WifiNetworkSpecifier.Builder().setSsidPattern(PatternMatcher("shelly", PatternMatcher.PATTERN_PREFIX)).build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(spec).build()
        val latch = CountDownLatch(1)
        var network: Network? = null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) { network = n; latch.countDown() }
            override fun onUnavailable() { latch.countDown() }
        }
        onStage(1)
        cm.requestNetwork(request, callback)
        try {
            // Kullanıcı Android penceresinde cihazı seçene kadar bekle.
            if (!latch.await(60, TimeUnit.SECONDS)) return Result.NO_DEVICE_NETWORK
            val n = network ?: return Result.NO_DEVICE_NETWORK
            onStage(2)
            val http = OkHttpClient.Builder().socketFactory(n.socketFactory)
                .connectTimeout(4, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
            fun get(path: String): String? = runCatching {
                http.newCall(Request.Builder().url("http://${ShellyProtocol.SETUP_HOST}$path").build()).execute().use { r ->
                    if (r.isSuccessful) r.body?.string() else null
                }
            }.getOrNull()
            val info = get("/shelly")?.let(ShellyProtocol::parseInfo) ?: return Result.NOT_SHELLY
            val ok = get(ShellyProtocol.staConfigPath(info.gen, ssid, password))?.let { ShellyProtocol.isStaConfigOk(info.gen, it, ssid) } == true
            Log.i(TAG, "provision ${info.id} gen=${info.gen} ok=$ok")
            if (!ok) return Result.REJECTED
            // Gen1 kendiliğinden yeniden başlar; Gen2+ yeni ayarı yeniden başlayınca uygular.
            if (info.gen >= 2) get(ShellyProtocol.rebootPath(info.gen))
            return Result.OK
        } finally {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
}
