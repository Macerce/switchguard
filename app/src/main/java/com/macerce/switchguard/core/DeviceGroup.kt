package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Gruptaki bir cihazın bir kanalı. */
data class GroupMember(val deviceId: String, val channel: Int)

/**
 * Grup: birden çok cihazı/kanalı tek düğmeyle açıp kapatan sanal cihaz ("Bahçe ışıkları", "Bütün klimalar").
 * Cihazlar ekranında ve otomasyonlarda [snapshot] ile sıradan tek kanallı bir cihaz gibi görünür; kimliği
 * [PREFIX] ile başlar. Üyelerden biri açıksa grup açık sayılır, biri çevrimiçiyse grup çevrimiçidir.
 */
data class DeviceGroup(val id: String, val name: String, val members: List<GroupMember>) {
    val deviceId: String get() = PREFIX + id

    /** Ekrandaki sanal cihaz; bulunamayan üyeler sayılmaz. */
    fun snapshot(byId: Map<String, DeviceSnapshot>): DeviceSnapshot {
        val states = states(byId)
        return DeviceSnapshot(
            id = deviceId,
            name = name,
            online = states.any { it.online },
            switches = mapOf(0 to states.any { it.online && it.on }),
            cloud = Cloud.DEMO,
        )
    }

    /** (açık, toplam) üye sayısı: kartta "2/5 açık" için. */
    fun counts(byId: Map<String, DeviceSnapshot>): Pair<Int, Int> {
        val states = states(byId)
        return states.count { it.online && it.on } to states.size
    }

    /** Grubu [on] yapmak için değişmesi gereken çevrimiçi üyeler (zaten öyle olanlar atlanır). */
    fun targets(byId: Map<String, DeviceSnapshot>, on: Boolean): List<Pair<DeviceSnapshot, Int>> =
        members.mapNotNull { m ->
            val d = byId[m.deviceId] ?: return@mapNotNull null
            val state = d.switches[m.channel] ?: return@mapNotNull null
            (d to m.channel).takeIf { d.online && state != on }
        }

    private class State(val online: Boolean, val on: Boolean)

    private fun states(byId: Map<String, DeviceSnapshot>): List<State> = members.mapNotNull { m ->
        val d = byId[m.deviceId] ?: return@mapNotNull null
        d.switches[m.channel]?.let { State(d.online, it) }
    }

    companion object {
        const val PREFIX = "group:"

        fun isGroup(deviceId: String): Boolean = deviceId.startsWith(PREFIX)

        /** Gerçek cihazlar + grupların sanal cihazları (otomasyon ve Cihazlar ekranı için). */
        fun withGroups(devices: List<DeviceSnapshot>, groups: List<DeviceGroup>): List<DeviceSnapshot> {
            if (groups.isEmpty()) return devices
            val byId = devices.associateBy { it.id }
            return devices + groups.map { it.snapshot(byId) }
        }

        fun listToJson(list: List<DeviceGroup>): String = JSONArray().apply {
            list.forEach { g ->
                put(JSONObject().put("id", g.id).put("name", g.name).put("members", JSONArray().apply {
                    g.members.forEach { m -> put(JSONObject().put("device", m.deviceId).put("channel", m.channel)) }
                }))
            }
        }.toString()

        /** Bozuk kayıtlar atlanır. */
        fun listFromJson(json: String?): List<DeviceGroup> {
            if (json.isNullOrEmpty()) return emptyList()
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    val ms = o.optJSONArray("members") ?: JSONArray()
                    DeviceGroup(o.getString("id"), o.optString("name"), (0 until ms.length()).map { j ->
                        val m = ms.getJSONObject(j)
                        GroupMember(m.getString("device"), m.optInt("channel"))
                    })
                }.getOrNull()
            }
        }
    }
}
