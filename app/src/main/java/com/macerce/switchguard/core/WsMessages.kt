package com.macerce.switchguard.core

import org.json.JSONObject

sealed class WsEvent {
    data class HandshakeOk(val heartbeatSec: Int) : WsEvent()
    data class HandshakeFailed(val error: Int) : WsEvent()
    object Pong : WsEvent()
    /** Cihaz durumu güncellemesi; sadece mesajda bulunan alanlar dolu. */
    data class Update(
        val deviceId: String,
        val online: Boolean?,
        val switches: Map<Int, Boolean>,
        /** Mesajdaki enerji değerleri; ölçek cihazın uiid'sine bağlı olduğundan [WsMessages.apply] içinde okunur. */
        val params: JSONObject? = null,
    ) : WsEvent()
    object Other : WsEvent()
}

/** eWeLink WebSocket mesajlarını ayrıştırır ve cihaz durumuna uygular. */
object WsMessages {

    fun parse(text: String): WsEvent? {
        if (text == "pong") return WsEvent.Pong
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null

        val action = json.optString("action")
        if (action == "update" || action == "sysmsg") {
            val id = json.optString("deviceid")
            val params = json.optJSONObject("params") ?: return WsEvent.Other
            if (id.isEmpty()) return WsEvent.Other
            return WsEvent.Update(
                deviceId = id,
                online = if (params.has("online")) params.optBoolean("online") else null,
                switches = DeviceParser.parseSwitches(params),
                params = params,
            )
        }
        // Handshake yanıtı: action yok, error + config var.
        if (action.isEmpty() && json.has("error") && !json.has("deviceid")) {
            val error = json.optInt("error", -1)
            if (error != 0) return WsEvent.HandshakeFailed(error)
            val hb = json.optJSONObject("config")?.optInt("hbInterval", DEFAULT_HB) ?: DEFAULT_HB
            return WsEvent.HandshakeOk(hb)
        }
        return WsEvent.Other
    }

    fun apply(snapshot: DeviceSnapshot, update: WsEvent.Update): DeviceSnapshot {
        val energy = update.params?.let { EnergyParser.parse(snapshot.uiid, it) }
        return snapshot.copy(
            online = update.online ?: snapshot.online,
            switches = if (update.switches.isEmpty()) snapshot.switches
            else (snapshot.switches + update.switches).toSortedMap(),
            power = energy?.power ?: snapshot.power,
            voltage = energy?.voltage ?: snapshot.voltage,
            current = energy?.current ?: snapshot.current,
        )
    }

    const val DEFAULT_HB = 145
}
