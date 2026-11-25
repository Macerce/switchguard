package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Sahne: birden çok cihazı/kanalı tek seferde açma-kapatma ("Hepsini kapat", "Tatil", "Akşam"). */
data class Scene(val id: String, val name: String, val actions: List<SceneAction>) {
    companion object {
        fun listToJson(list: List<Scene>): String = JSONArray().apply {
            list.forEach { s ->
                put(JSONObject().put("id", s.id).put("name", s.name).put("actions", JSONArray().apply {
                    s.actions.forEach { a -> put(JSONObject().put("device", a.deviceId).put("channel", a.channel).put("on", a.on)) }
                }))
            }
        }.toString()

        /** Bozuk kayıtlar atlanır. */
        fun listFromJson(json: String?): List<Scene> {
            if (json.isNullOrEmpty()) return emptyList()
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    val acts = o.optJSONArray("actions") ?: JSONArray()
                    Scene(o.getString("id"), o.optString("name"), (0 until acts.length()).map { j ->
                        val a = acts.getJSONObject(j)
                        SceneAction(a.getString("device"), a.optInt("channel"), a.optBoolean("on"))
                    })
                }.getOrNull()
            }
        }

        /** "Tatil", "tatil", "TATİL" aynı sahne. */
        fun find(query: String, scenes: List<Scene>): Scene? {
            val q = DeviceKinds.words(query).joinToString(" ")
            if (q.isEmpty()) return null
            val named = scenes.map { it to DeviceKinds.words(it.name).joinToString(" ") }
            return named.firstOrNull { it.second == q }?.first
                ?: named.filter { it.second.startsWith(q) }.singleOrNull()?.first
        }
    }
}

data class SceneAction(val deviceId: String, val channel: Int, val on: Boolean)

/** Evde / Dışarıda modu ve tatil. Saf: zaman dışarıdan verilir. */
object Presence {
    /** Tatil sürüyor mu (bitiş anı [until], 0 = kapalı). */
    fun vacation(until: Long, now: Long): Boolean = until > now

    /** Tatilde her zaman "dışarıda" sayılır. */
    fun away(awayMode: Boolean, vacationUntil: Long, now: Long): Boolean = awayMode || vacation(vacationUntil, now)
}

/**
 * "Durumu hatırla / geri getir" (Home Assistant'ın anlık sahnesi gibi): cihazların şu anki aç/kapalı durumu
 * kaydedilir, sonra aynı duruma dönülür. Saf.
 */
object StateSnapshot {
    /**
     * [only] cihazlarının (tek cihaz ya da bir sahnenin cihazları) çevrimiçi kanal durumları. Liste zorunludur:
     * "hesaptaki tüm cihazlar" bilerek yoktur (otel gibi çok cihazlı hesaplarda yanlışlıkla her şeyi açıp kapatmasın).
     * Grup ve kategori gibi sanal kayıtlar ([virtualIds]) alınmaz.
     */
    fun capture(devices: List<DeviceSnapshot>, only: Set<String>, virtualIds: (String) -> Boolean = { false }): List<SceneAction> =
        devices.filter { d -> d.online && !d.sensorOnly && !virtualIds(d.id) && d.id in only }
            .flatMap { d -> d.switches.entries.sortedBy { it.key }.map { (ch, on) -> SceneAction(d.id, ch, on) } }

    /** Geri dönerken yalnız durumu değişmiş kanallara komut gider. */
    fun changes(saved: List<SceneAction>, devices: Map<String, DeviceSnapshot>): List<SceneAction> =
        saved.filter { a -> devices[a.deviceId]?.switches?.get(a.channel)?.let { it != a.on } ?: true }

    fun toJson(list: List<SceneAction>): String =
        JSONArray().apply { list.forEach { a -> put(JSONObject().put("device", a.deviceId).put("channel", a.channel).put("on", a.on)) } }.toString()

    fun fromJson(json: String?): List<SceneAction> {
        val arr = runCatching { JSONArray(json ?: return emptyList()) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { arr.getJSONObject(i).let { SceneAction(it.getString("device"), it.optInt("channel"), it.optBoolean("on")) } }.getOrNull()
        }
    }
}
