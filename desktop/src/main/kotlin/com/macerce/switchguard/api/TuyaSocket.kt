package com.macerce.switchguard.api

import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.core.TuyaEvent
import com.macerce.switchguard.core.TuyaMessages
import com.macerce.switchguard.core.TuyaSign
import com.macerce.switchguard.data.Store
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Tuya mesaj servisi (Pulsar) WebSocket tüketicisi. Olaylar [listener]'a OkHttp'nin thread'inde gelir.
 * Her mesaj (tanınmasa da) onaylanır; onaylanmayan mesaj sunucuda birikir ve tekrar gönderilir.
 * Bağlantının canlılığını OkHttp'nin ping'i denetler: yanıt gelmezse onFailure çağrılır.
 */
class TuyaSocket(private val store: Store, private val listener: Listener) {

    interface Listener {
        fun onReady()
        fun onEvent(event: TuyaEvent)
        /** [rejected]: sunucu kimliği reddetti (yanlış bilgi ya da mesaj servisi kapalı); sık denemek anlamsız. */
        fun onClosed(reason: String, rejected: Boolean)
    }

    @Volatile private var socket: WebSocket? = null
    /**
     * Bağlantı kuşağı: her connect/close artırır. Geri çağrılar kendi kuşaklarını [generation] ile karşılaştırır:
     * eski soketin olayları ve kurulurken kapatılan bağlantı yok sayılır. (Soket alanıyla karşılaştırmak yarışlıydı:
     * [socket] newWebSocket döndükten sonra yazıldığından çok erken bir onOpen kendi soketini tanımayabiliyordu.)
     * Soket izleme thread'inde kapatılır, ağ thread'inde kurulur, OkHttp thread'inde okunur: alanlar @Volatile.
     */
    private val generation = java.util.concurrent.atomic.AtomicInteger()
    @Volatile var ready = false
        private set
    @Volatile var lastMessageAt = 0L
        private set

    fun connect() {
        close()
        val gen = generation.get()
        val id = store.tuyaAccessId
        val secret = store.tuyaSecret
        val url = TuyaClient.pulsarHost(store.tuyaRegion) +
            "ws/v2/consumer/persistent/$id/out/event/$id-sub?ackTimeoutMillis=3000&subscriptionType=Failover"
        val client = EwelinkClient.http.newBuilder().callTimeout(0, TimeUnit.SECONDS).pingInterval(30, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(url)
            .header("Connection-Id", "switchguard")
            .header("username", id)
            .header("password", TuyaSign.pulsarPassword(id, secret))
            .build()
        val ws = client.newWebSocket(request, object : WebSocketListener() {
            private fun stale() = gen != generation.get()

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (stale()) return
                ready = true
                lastMessageAt = System.currentTimeMillis()
                listener.onReady()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (stale()) return
                lastMessageAt = System.currentTimeMillis()
                val frame = try {
                    TuyaMessages.decode(text, secret)
                } catch (e: Exception) {
                    Log.w(TAG, "Tuya message not decoded", e)
                    runCatching { JSONObject(text).getString("messageId") }.getOrNull()?.let { ack(webSocket, it) }
                    return
                }
                ack(webSocket, frame.messageId)
                frame.event?.let(listener::onEvent)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (stale()) return
                ready = false
                val code = response?.code ?: 0
                listener.onClosed(if (code != 0) "HTTP $code" else t.message ?: "failure", rejected = code in 400..499)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (stale()) return
                ready = false
                listener.onClosed("closed $code", rejected = false)
            }
        })
        socket = ws
        // Kurulurken kapatıldıysa bu soket de kapansın.
        if (gen != generation.get()) {
            ws.cancel()
            if (socket === ws) socket = null
        }
    }

    private fun ack(webSocket: WebSocket, messageId: String) {
        webSocket.send(JSONObject().put("messageId", messageId).toString())
    }

    fun close() {
        generation.incrementAndGet()
        ready = false
        socket?.cancel()
        socket = null
    }

    private companion object {
        const val TAG = "SwitchGuard"
    }
}
