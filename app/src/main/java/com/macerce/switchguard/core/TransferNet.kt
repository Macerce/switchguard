package com.macerce.switchguard.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/** Ağda bulunan, aktarma bekleyen bir SwitchGuard. */
data class TransferPeer(val name: String, val host: String, val port: Int)

/**
 * Alıcı taraf (TV/tablet): ev ağında NSD ile görünür, tek satırlık şifreli JSON bekler.
 * Doğru kodla açılan veri [onReceived] ile ana iş parçacığında uygulanır.
 * Yanlış kod denemeleri sayılır; [MAX_ATTEMPTS] sonra kod yenilenir ([onNewCode]).
 */
class TransferReceiver(
    context: Context,
    private val onNewCode: (String) -> Unit,
    private val onReceived: (JSONObject) -> Unit,
) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private var server: ServerSocket? = null
    private var registration: NsdManager.RegistrationListener? = null
    @Volatile private var code = Transfer.newCode()
    @Volatile private var attempts = 0

    fun start() {
        main.post { onNewCode(code) }
        io.execute {
            val srv = runCatching { ServerSocket(0) }.getOrElse { Log.w(TAG, "server", it); return@execute }
            server = srv
            register(srv.localPort)
            while (!srv.isClosed) {
                val s = runCatching { srv.accept() }.getOrNull() ?: break
                runCatching { handle(s) }.onFailure { Log.w(TAG, "handle", it) }
                runCatching { s.close() }
            }
        }
    }

    private fun handle(s: Socket) {
        s.soTimeout = 15_000
        val line = BoundedLine.read(BufferedInputStream(s.getInputStream()), MAX_BYTES) ?: return
        val out = OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)
        val reply = try {
            val plain = Transfer.decrypt(code, JSONObject(line))
            val data = JSONObject(String(plain, Charsets.UTF_8))
            main.post { onReceived(data) }
            JSONObject().put("ok", true).put("device", Build.MODEL)
        } catch (e: Exception) {
            if (++attempts >= MAX_ATTEMPTS) {
                attempts = 0
                code = Transfer.newCode()
                main.post { onNewCode(code) }
            }
            JSONObject().put("ok", false).put("err", "code")
        }
        out.write(reply.toString() + "\n")
        out.flush()
    }

    private fun register(port: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "SwitchGuard · ${Build.MODEL}"
            serviceType = Transfer.SERVICE_TYPE
            setPort(port)
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) { Log.w(TAG, "register failed $e") }
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
        }
        registration = l
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stop() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
        runCatching { server?.close() }
        io.shutdown()
    }

    companion object {
        private const val TAG = "SwitchGuard"
        private const val MAX_ATTEMPTS = 5
        private const val MAX_BYTES = 512 * 1024
    }
}

/** Gönderen taraf: ağdaki alıcıları bulur ([onPeers] ana iş parçacığında, güncel liste). */
class TransferDiscovery(context: Context, private val onPeers: (List<TransferPeer>) -> Unit) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val peers = linkedMapOf<String, TransferPeer>()
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var listener: NsdManager.DiscoveryListener? = null

    fun start() {
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) { Log.w("SwitchGuard", "discovery failed $e") }
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) = main.post { pending.add(info); resolveNext() }.let { }
            override fun onServiceLost(info: NsdServiceInfo) = main.post {
                if (peers.remove(info.serviceName) != null) onPeers(peers.values.toList())
            }.let { }
        }
        listener = l
        runCatching { nsd.discoverServices(Transfer.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    /** NsdManager aynı anda tek çözümleme yapabilir: sırayla. */
    private fun resolveNext() {
        if (resolving) return
        val info = pending.removeFirstOrNull() ?: return
        resolving = true
        @Suppress("DEPRECATION")
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(i: NsdServiceInfo, e: Int) = main.post { resolving = false; resolveNext() }.let { }
            override fun onServiceResolved(i: NsdServiceInfo) = main.post {
                @Suppress("DEPRECATION")
                val host = i.host?.hostAddress
                if (host != null) {
                    peers[i.serviceName] = TransferPeer(i.serviceName.removePrefix("SwitchGuard · "), host, i.port)
                    onPeers(peers.values.toList())
                }
                resolving = false
                resolveNext()
            }.let { }
        })
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }
}

object TransferSender {
    private const val MAX_REPLY_BYTES = 4 * 1024

    sealed interface Result {
        data class Ok(val device: String) : Result
        data object WrongCode : Result
        data class Failed(val error: Throwable) : Result
    }

    /** Engelleyici: arka planda çağrılmalı. */
    fun send(peer: TransferPeer, code: String, payload: JSONObject): Result = try {
        val msg = Transfer.encrypt(code, payload.toString().toByteArray(Charsets.UTF_8))
        Socket().use { s ->
            s.connect(InetSocketAddress(peer.host, peer.port), 5_000)
            s.soTimeout = 15_000
            val out = OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)
            out.write(msg.toString() + "\n")
            out.flush()
            // Ağda alıcı gibi görünen başka bir cihaz da cevap verebilir: cevap kısa olmalı.
            val reply = JSONObject(BoundedLine.read(BufferedInputStream(s.getInputStream()), MAX_REPLY_BYTES) ?: error("no reply"))
            if (reply.optBoolean("ok")) Result.Ok(reply.optString("device")) else Result.WrongCode
        }
    } catch (e: Exception) {
        Result.Failed(e)
    }
}
