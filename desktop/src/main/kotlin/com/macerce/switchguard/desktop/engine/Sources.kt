package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.EwelinkSocket
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.api.TuyaSocket
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.Store

/** [EwelinkClient]'in motorun kullandığı kısmı. */
interface EwelinkApi {
    fun getDevices(): List<DeviceSnapshot>
    fun loadProfile()
    fun loadFamilyApiKey()
    fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean)
    fun requestLiveEnergy(d: DeviceSnapshot, seconds: Int)
}

/** [EwelinkSocket]'in motorun kullandığı kısmı. */
interface EwelinkLive {
    val ready: Boolean
    fun connect()
    fun close()
    fun sendPingIfDue(now: Long)
    fun isStale(now: Long): Boolean
}

/** [TuyaClient]'ın motorun kullandığı kısmı. */
interface TuyaApi {
    val syncServerTime: Long
    fun getDevices(): List<DeviceSnapshot>
    fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean)
    fun scalesFor(deviceId: String): Map<String, Int>
}

/** [TelegramClient]'in motorun kullandığı kısmı (haber verme, komutlar, bekçi). Hepsi engelleyicidir. */
interface TelegramApi {
    fun send(token: String, chatId: Long, text: String, markup: String? = null, silent: Boolean = false, html: Boolean = false): Long
    fun updates(token: String, offset: Long): String
    fun answerCallback(token: String, callbackId: String, text: String)
    fun removeButtons(token: String, chatId: Long, messageId: Long)
    fun edit(token: String, chatId: Long, messageId: Long, text: String)
    fun pin(token: String, chatId: Long, messageId: Long)
    fun chat(token: String, chatId: Long): String

    /** Gerçek [TelegramClient]. */
    object Real : TelegramApi {
        override fun send(token: String, chatId: Long, text: String, markup: String?, silent: Boolean, html: Boolean) =
            TelegramClient.send(token, chatId, text, markup, silent, html)
        override fun updates(token: String, offset: Long) = TelegramClient.updates(token, offset)
        override fun answerCallback(token: String, callbackId: String, text: String) = TelegramClient.answerCallback(token, callbackId, text)
        override fun removeButtons(token: String, chatId: Long, messageId: Long) = TelegramClient.removeButtons(token, chatId, messageId)
        override fun edit(token: String, chatId: Long, messageId: Long, text: String) = TelegramClient.edit(token, chatId, messageId, text)
        override fun pin(token: String, chatId: Long, messageId: Long) = TelegramClient.pin(token, chatId, messageId)
        override fun chat(token: String, chatId: Long) = TelegramClient.chat(token, chatId)
    }
}

/** [TuyaSocket]'in motorun kullandığı kısmı. */
interface TuyaLive {
    val ready: Boolean
    fun connect()
    fun close()
}

/**
 * Motorun veri kaynakları: iki bulutun HTTP istemcileri ve anlık bağlantıları. Anlık bağlantılar motorun
 * dinleyicisiyle kurulur. Testlerde sahteleri verilir; [real] gerçek `api` sınıflarını sarar (mantık yok).
 */
class Sources(
    val ewelink: EwelinkApi,
    val ewelinkLive: (EwelinkSocket.Listener) -> EwelinkLive,
    val tuya: TuyaApi,
    val tuyaLive: (TuyaSocket.Listener) -> TuyaLive,
    val telegram: TelegramApi = TelegramApi.Real,
    /** Dış sıcaklık (°C), Open-Meteo'dan. */
    val weather: (lat: Double, lon: Double) -> Double = { lat, lon -> com.macerce.switchguard.api.WeatherClient.temperature(lat, lon) },
) {
    companion object {
        fun real(store: Store): Sources {
            val ewelink = EwelinkClient(store)
            // Tek örnek: getDevices'ın sakladığı sunucu saati ve ürün kimlikleri scalesFor/syncServerTime'da kullanılır.
            val tuya = TuyaClient(store)
            return Sources(
                ewelink = object : EwelinkApi {
                    override fun getDevices() = ewelink.getDevices()
                    override fun loadProfile() = ewelink.loadProfile()
                    override fun loadFamilyApiKey() = ewelink.loadFamilyApiKey()
                    override fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean) = ewelink.setSwitch(d, channel, on)
                    override fun requestLiveEnergy(d: DeviceSnapshot, seconds: Int) = ewelink.requestLiveEnergy(d, seconds)
                },
                ewelinkLive = { listener ->
                    val socket = EwelinkSocket(store, listener)
                    object : EwelinkLive {
                        override val ready get() = socket.ready
                        override fun connect() = socket.connect()
                        override fun close() = socket.close()
                        override fun sendPingIfDue(now: Long) = socket.sendPingIfDue(now)
                        override fun isStale(now: Long) = socket.isStale(now)
                    }
                },
                tuya = object : TuyaApi {
                    override val syncServerTime get() = tuya.syncServerTime
                    override fun getDevices() = tuya.getDevices()
                    override fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean) = tuya.setSwitch(d, channel, on)
                    override fun scalesFor(deviceId: String) = tuya.scalesFor(deviceId)
                },
                tuyaLive = { listener ->
                    val socket = TuyaSocket(store, listener)
                    object : TuyaLive {
                        override val ready get() = socket.ready
                        override fun connect() = socket.connect()
                        override fun close() = socket.close()
                    }
                },
            )
        }
    }
}
