package com.macerce.switchguard.api

import com.macerce.switchguard.core.HaProtocol
import com.macerce.switchguard.core.LanFound
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Home Assistant sunucusu (bkz. [HaProtocol]). Keşif mDNS `_home-assistant._tcp` (ya da IP/tarama, port 8123).
 * Sunucu listede kilitli görünür; adres + erişim anahtarıyla bağlanınca aç/kapa yapılabilen varlıkları listelenir.
 * Hangi adresin kullanılacağı her 30 sn'de bir seçilir: yerel adres yanıt veriyorsa o, yoksa dış adres.
 * Durumlar WebSocket aboneliğiyle (`state_changed`) anında gelir; bağlantı kurulamazsa ya da koparsa 5 sn'lik
 * yoklamaya düşülür, kopukluk artan aralıklarla yeniden denenir. Anlık bağlantı varken yoklama dakikada bire iner.
 */
object HaLan : PolledLan(LocalDevice.BRAND_HA) {
    override val mdnsType: String = "_home-assistant._tcp"

    @Volatile var server: HaProtocol.Server? = null
        set(v) {
            val changed = field != v
            field = v
            if (changed) { closeLive(); chosen = null; if (running) openLive() }
        }

    @Volatile private var running = false
    @Volatile private var socket: WebSocket? = null
    /** Kimlik doğrulandı ve abonelik kabul edildi: durumlar anında geliyor. */
    @Volatile var live = false
        private set
    @Volatile private var lastFullPoll = 0L
    @Volatile private var retryMs = RETRY_MIN_MS
    private var retry: ScheduledFuture<*>? = null

    private val wsClient: OkHttpClient by lazy {
        http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(30, TimeUnit.SECONDS).build()
    }

    override fun onStarted() {
        running = true
        openLive()
    }

    override fun onStopped() {
        running = false
        closeLive()
    }

    /** Anlık bağlantı varken tam yoklama dakikada bir (kaçan olay olursa düzelsin). */
    override fun skipPoll(): Boolean {
        if (!live) return false
        val now = System.currentTimeMillis()
        if (now - lastFullPoll < LIVE_POLL_MS) return true
        lastFullPoll = now
        return false
    }

    @Volatile private var connecting = false
    /** [closeLive] her çağrıda artar: o sırada kurulmakta olan eski bağlantı (eski anahtarla) kullanılmaz. */
    @Volatile private var generation = 0

    @Synchronized
    private fun openLive() {
        if (socket != null || connecting || server == null || !running) return
        retry?.cancel(false); retry = null
        connecting = true
        val gen = generation
        io.execute {
            val s = server
            val b = runCatching { base() }.getOrNull()
            val ws = if (s == null || b.isNullOrEmpty() || !running) null
            else runCatching { wsClient.newWebSocket(Request.Builder().url(HaProtocol.wsUrl(b)).build(), Listener(s.token)) }.getOrNull()
            val stale: Boolean
            synchronized(this) {
                connecting = false
                stale = gen != generation
                if (ws != null && !stale && running && socket == null) socket = ws else ws?.cancel()
            }
            if (stale) openLive() else if (ws == null) scheduleRetry()
        }
    }

    @Synchronized
    private fun closeLive() {
        retry?.cancel(false); retry = null
        generation++
        socket?.cancel()
        socket = null
        live = false
        retryMs = RETRY_MIN_MS
    }

    @Synchronized
    private fun scheduleRetry() {
        if (!running || retry != null) return
        val wait = retryMs
        retryMs = (retryMs * 2).coerceAtMost(RETRY_MAX_MS)
        retry = io.schedule({ synchronized(this) { retry = null }; openLive() }, wait, TimeUnit.MILLISECONDS)
    }

    private class Listener(private val token: String) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            for (m in HaProtocol.parseWsBatch(text)) when (m) {
                HaProtocol.WsMessage.AuthRequired -> webSocket.send(HaProtocol.wsAuth(token))
                HaProtocol.WsMessage.AuthOk -> webSocket.send(HaProtocol.wsSubscribe(SUBSCRIBE_ID))
                is HaProtocol.WsMessage.AuthInvalid -> {
                    Log.w(tag, "websocket auth invalid: ${m.message}")
                    retryMs = RETRY_MAX_MS
                    webSocket.close(1000, null)
                }
                is HaProtocol.WsMessage.Result -> if (m.id == SUBSCRIBE_ID) {
                    live = m.success
                    if (m.success) { retryMs = RETRY_MIN_MS; lastFullPoll = System.currentTimeMillis(); Log.i(tag, "websocket live") }
                }
                is HaProtocol.WsMessage.StateChanged -> onState(m)
                else -> Unit
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = dropped(webSocket, t.message)
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = dropped(webSocket, "closed $code")
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(1000, null) }
    }

    private fun dropped(ws: WebSocket, why: String?) {
        synchronized(this) {
            if (socket !== ws) return
            socket = null
            live = false
        }
        Log.i(tag, "websocket dropped: $why")
        forgetRoute()
        pollNow()
        scheduleRetry()
    }

    private fun onState(m: HaProtocol.WsMessage.StateChanged) {
        for (d in watched()) {
            if (d.extra != m.entityId) continue
            pushReading(d.id, m.state?.takeIf { it.available }?.let { LocalReading(mapOf(0 to it.on)) })
        }
    }

    @Volatile private var chosen: String? = null
    @Volatile private var chosenAt = 0L

    private fun auth(token: String) = mapOf("Authorization" to "Bearer $token")

    /** Kullanılacak adres (yerel önce). Bloklayıcı olabilir (en çok bir kısa istek). */
    fun base(): String? {
        val s = server ?: return null
        val now = System.currentTimeMillis()
        chosen?.takeIf { now - chosenAt < 30_000 }?.let { return it }
        val local = HaProtocol.normalize(s.local)
        val remote = HaProtocol.normalize(s.remote)
        val pick = when {
            local.isNotEmpty() && get("$local/api/", auth(s.token), quickly = true).code == 200 -> local
            remote.isNotEmpty() -> remote
            else -> local
        }
        chosen = pick
        chosenAt = now
        return pick
    }

    /** Adres hangisi (bağlantı testi ve detay kartı için): "local", "remote" ya da null. */
    fun route(): String? = server?.let { s ->
        when (chosen) {
            null -> null
            HaProtocol.normalize(s.local) -> "local"
            else -> "remote"
        }
    }

    fun forgetRoute() { chosen = null }

    override fun host(d: LocalDevice): String? = base()

    override fun seenHost(d: LocalDevice): String? = chosen

    /**
     * Sunucu sınaması: 200 = bağlandı, 401 = anahtar yanlış, 0 = ulaşılamadı (diğer kodlar olduğu gibi). Bloklayıcı.
     */
    fun test(url: String, token: String): Int = get("${HaProtocol.normalize(url)}/api/", auth(token)).code

    /** Bağlı sunucudaki aç/kapa yapılabilen varlıkları listeye koyar. Döner: sayı (bağlanamazsa -1). */
    fun loadEntities(): Int {
        val s = server ?: return -1
        val b = base() ?: return -1
        val body = get("$b/api/states", auth(s.token)).body ?: return -1
        val list = HaProtocol.parseStates(body)
        list.forEach { e ->
            remember(LanFound(HaProtocol.idFor(e.id), brand, b, e.domain, 1, supported = true, extra = e.id, name = e.name))
        }
        return list.size
    }

    override fun probe(host: String, cred: ShellyAuth.Credentials?, quickly: Boolean): LanFound? {
        val url = HaProtocol.normalize(host)
        val r = get("$url/api/", server?.let { auth(it.token) }.orEmpty(), quickly = quickly, keepErrorBody = true)
        val isHa = r.code == 401 || (r.code == 200 && r.body?.contains("API running") == true)
        if (!isHa) return null
        val connected = r.code == 200
        if (connected) io.execute { loadEntities() }
        return LanFound(
            "ha-server@" + url.substringAfter("://"), brand, url, "Home Assistant", 0, supported = false,
            locked = !connected, extra = HueLan.BRIDGE, name = "Home Assistant",
        )
    }

    override fun read(d: LocalDevice, host: String, cred: ShellyAuth.Credentials?): LocalReading? {
        val s = server ?: return null
        val r = get("$host/api/states/${d.extra}", auth(s.token))
        if (r.code == 0) forgetRoute()
        val e = r.body?.let(HaProtocol::parseState) ?: return null
        if (!e.available) return null
        return LocalReading(mapOf(0 to e.on))
    }

    override fun level(d: LocalDevice, host: String, channel: Int, level: Int?, color: Int?, cred: ShellyAuth.Credentials?): Boolean {
        val s = server ?: return false
        if (!d.extra.startsWith("light.")) return false
        return send("POST", "$host/api/services/light/turn_on", com.macerce.switchguard.core.LightLevel.haBody(d.extra, level, color), auth(s.token)).code in 200..299
    }

    override fun switch(d: LocalDevice, host: String, channel: Int, on: Boolean, cred: ShellyAuth.Credentials?): Boolean {
        val s = server ?: return false
        return send("POST", host + HaProtocol.servicePath(d.extra, on), HaProtocol.serviceBody(d.extra), auth(s.token)).code in 200..299
    }

    private const val SUBSCRIBE_ID = 1
    private const val RETRY_MIN_MS = 5_000L
    private const val RETRY_MAX_MS = 120_000L
    private const val LIVE_POLL_MS = 60_000L
}
