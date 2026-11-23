package com.macerce.switchguard.api

import com.macerce.switchguard.core.WsEvent
import com.macerce.switchguard.core.WsMessages
import com.macerce.switchguard.data.Store
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * eWeLink WebSocket bağlantısı. Olaylar [listener]'a OkHttp'nin thread'inde gelir;
 * çağıran taraf kendi thread'ine aktarmalıdır.
 * Heartbeat ("ping") gönderimi dışarıdan [sendPingIfDue] ile tetiklenir,
 * çünkü telefon uyurken yerel zamanlayıcılar çalışmaz (bkz. MonitorService).
 */
class EwelinkSocket(private val store: Store, private val listener: Listener) {

    interface Listener {
        fun onReady()
        fun onEvent(event: WsEvent)
        /** Bağlantı koptu. [fatal] true ise yeniden denemek anlamsız (ör. uygulama yetkisi yok). */
        fun onClosed(reason: String, fatal: Boolean)
        /** Sunucu token'ı reddetti (401/402): token yenilenmeli ya da oturum başka cihazda açılmış olabilir. */
        fun onAuthRejected()
    }

    @Volatile private var socket: WebSocket? = null
    /**
     * Bağlantı kuşağı: her connect/close artırır. Geri çağrılar kendi kuşaklarını [generation] ile karşılaştırır:
     * eski soketin olayları ve kurulurken kapatılan bağlantı yok sayılır. (Soket alanıyla karşılaştırmak yarışlıydı:
     * [socket] newWebSocket döndükten sonra yazıldığından çok erken bir onOpen kendi soketini tanımayabiliyordu.)
     * Soket izleme thread'inde kapatılır, ağ thread'inde kurulur, OkHttp thread'inde okunur: alanlar @Volatile.
     */
    private val generation = java.util.concurrent.atomic.AtomicInteger()
    @Volatile private var heartbeatMs = WsMessages.DEFAULT_HB * 1000L
    @Volatile private var lastPingAt = 0L
    @Volatile var lastMessageAt = 0L
        private set
    @Volatile var ready = false
        private set

    /** Bloklayıcı: dispatch adresini alır ve bağlanır. */
    fun connect() {
        close()
        val gen = generation.get()
        val url = EwelinkClient(store).dispatchWebSocketUrl()
        // Dispatch sürerken kapatıldı (izleme durdu ya da yeniden bağlanılıyor): bağlanma.
        if (gen != generation.get()) return
        val client = EwelinkClient.http.newBuilder().callTimeout(0, TimeUnit.SECONDS).pingInterval(0, TimeUnit.SECONDS).build()
        val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            private fun stale() = gen != generation.get()

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (stale()) return
                val now = System.currentTimeMillis()
                lastMessageAt = now
                webSocket.send(
                    JSONObject()
                        .put("action", "userOnline")
                        .put("version", 8)
                        .put("ts", now / 1000)
                        .put("at", store.accessToken)
                        .put("userAgent", "app")
                        .put("apikey", store.userApiKey)
                        .put("appid", store.appId)
                        .put("nonce", EwelinkClient.randomNonce())
                        .put("sequence", now.toString())
                        .toString()
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (stale()) return
                lastMessageAt = System.currentTimeMillis()
                when (val e = WsMessages.parse(text) ?: return) {
                    is WsEvent.HandshakeOk -> {
                        heartbeatMs = e.heartbeatSec * 1000L
                        lastPingAt = lastMessageAt
                        ready = true
                        listener.onReady()
                    }
                    is WsEvent.HandshakeFailed -> {
                        ready = false
                        // Sonraki onClosed geri çağrısı yok sayılsın, olay iki kez bildirilmesin.
                        generation.incrementAndGet()
                        socket = null
                        webSocket.close(1000, null)
                        // 401/402: token sorunu (servis yeniler); diğerleri: bu App ID için WS yok.
                        if (e.error in setOf(401, 402)) listener.onAuthRejected()
                        else listener.onClosed("handshake ${e.error}", fatal = true)
                    }
                    else -> listener.onEvent(e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (stale()) return
                ready = false
                listener.onClosed(t.message ?: "failure", fatal = false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (stale()) return
                ready = false
                listener.onClosed("closed $code", fatal = false)
            }
        })
        socket = ws
        // Kurulurken kapatıldıysa bu soket de kapansın.
        if (gen != generation.get()) {
            ws.cancel()
            if (socket === ws) socket = null
        }
    }

    /** Heartbeat zamanı geldiyse "ping" gönderir. Sunucu aralığının ~%60'ında bir, güvenli pay bırakır. */
    fun sendPingIfDue(now: Long) {
        val s = socket ?: return
        if (ready && now - lastPingAt >= heartbeatMs * 6 / 10) {
            lastPingAt = now
            s.send("ping")
        }
    }

    /** Uzun süredir hiç mesaj (pong dahil) gelmediyse bağlantı ölü kabul edilir. */
    fun isStale(now: Long) = ready && now - lastMessageAt > heartbeatMs * 2

    fun close() {
        generation.incrementAndGet()
        ready = false
        socket?.cancel()
        socket = null
    }
}
