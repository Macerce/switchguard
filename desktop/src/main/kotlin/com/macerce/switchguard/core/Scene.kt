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
