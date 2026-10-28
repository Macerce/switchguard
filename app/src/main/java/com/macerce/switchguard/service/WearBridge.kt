package com.macerce.switchguard.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saat uygulamasıyla (Wear OS) köprü. Telefon durumu ([PATH_STATUS] veri öğesi) yayınlar; saat alarmı kapatma, cihaz
 * açıp kapatma ve tazeleme isteğini mesajla gönderir. Saat yoksa ya da Google Play hizmetleri yoksa sessizce hiçbir şey olmaz.
 */
object WearBridge {
    const val PATH_STATUS = "/switchguard/status"
    const val PATH_DISMISS = "/switchguard/dismiss"
    const val PATH_TOGGLE = "/switchguard/toggle"
    const val PATH_SYNC = "/switchguard/sync"
    const val PATH_SCENE = "/switchguard/scene"
    const val PATH_PRESENCE = "/switchguard/presence"
    private const val MAX_DEVICES = 30

    @Volatile private var lastJson: String? = null

    /** Durum değişince çağrılır; içerik aynıysa gönderilmez. */
    fun publish(context: Context, force: Boolean = false) {
        runCatching {
            val json = statusJson(context)
            if (!force && json == lastJson) return
            lastJson = json
            val request = PutDataMapRequest.create(PATH_STATUS).apply {
                dataMap.putString("json", json)
                // Aynı içerik yeniden istenince de saate ulaşsın.
                dataMap.putLong("sent", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request)
                .addOnFailureListener { Log.w("SwitchGuard", "wear publish failed: ${it.javaClass.simpleName}") }
        }
    }

    private fun statusJson(context: Context): String {
        val store = Store.get(context)
        val (kind, text) = StatusSurfaces.current(context)
        // Gruplar saatte tek kanallı cihaz gibi görünür; açma/kapama isteği telefonda bütün üyelere dağıtılır.
        val devices = store.deviceLayout.sections(store.snapshotsWithGroups).flatMap { it.devices }.take(MAX_DEVICES)
        return JSONObject()
            .put("kind", kind.name)
            .put("state", text)
            .put("alarm", JSONArray(store.pendingAlarm))
            .put("alarmId", RuntimeState.get(context).alarmStartedAt)
            .put("away", store.isAway())
            .put("vacation", store.onVacation())
            .put("scenes", JSONArray().apply { store.scenes.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } })
            .put("devices", JSONArray().apply {
                devices.forEach { d ->
                    put(JSONObject().put("id", d.id).put("name", d.name).put("online", d.online)
                        .put("sw", JSONObject().apply { d.switches.toSortedMap().forEach { (ch, on) -> put(ch.toString(), on) } })
                        .apply { d.power?.takeIf { d.online }?.let { put("w", it) } })
                }
            })
            .toString()
    }
}

/** Saatten gelen istekler. */
class WearListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            WearBridge.PATH_DISMISS -> MonitorService.send(this, MonitorService.ACTION_DISMISS)
            WearBridge.PATH_SYNC -> WearBridge.publish(this, force = true)
            WearBridge.PATH_PRESENCE -> {
                Actions.setAway(this, String(event.data, Charsets.UTF_8) == "1", by = getString(com.macerce.switchguard.R.string.wear_source))
                WearBridge.publish(this)
            }
            WearBridge.PATH_SCENE -> {
                val id = String(event.data, Charsets.UTF_8)
                val scene = Store.get(this).scenes.firstOrNull { it.id == id } ?: return
                runCatching { runBlocking { Actions.runScene(this@WearListener, scene, getString(com.macerce.switchguard.R.string.wear_source)) } }
                WearBridge.publish(this)
            }
            WearBridge.PATH_TOGGLE -> {
                // "cihaz|kanal|açık"
                val parts = String(event.data, Charsets.UTF_8).split('|')
                val id = parts.getOrNull(0) ?: return
                val channel = parts.getOrNull(1)?.toIntOrNull() ?: return
                val on = parts.getOrNull(2) == "1"
                val device = Store.get(this).snapshotsWithGroups.firstOrNull { it.id == id } ?: return
                runCatching { runBlocking { Actions.setSwitch(this@WearListener, device, channel, on) } }
                WearBridge.publish(this)
            }
        }
    }
}
