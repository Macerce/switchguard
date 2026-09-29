package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

object DeviceParser {

    /** `GET /v2/device/thing` yanıtını cihaz listesine çevirir; grupları (deviceid'siz) atlar. */
    fun parseThingList(body: String): List<DeviceSnapshot> {
        val list = JSONObject(body).getJSONObject("data").optJSONArray("thingList") ?: return emptyList()
        val result = mutableListOf<DeviceSnapshot>()
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i).optJSONObject("itemData") ?: continue
            val id = item.optString("deviceid")
            if (id.isEmpty()) continue
            result += DeviceSnapshot(
                id = id,
                name = item.optString("name", id),
                online = item.optBoolean("online", false),
                switches = parseSwitches(item.optJSONObject("params")),
            )
        }
        return result
    }

    private fun parseSwitches(params: JSONObject?): Map<Int, Boolean> {
        if (params == null) return emptyMap()
        val multi = params.optJSONArray("switches")
        if (multi != null && multi.length() > 0) {
            val map = sortedMapOf<Int, Boolean>()
            for (i in 0 until multi.length()) {
                val s = multi.getJSONObject(i)
                map[s.optInt("outlet", i)] = s.optString("switch") == "on"
            }
            return map
        }
        val single = params.optString("switch", "")
        return if (single.isEmpty()) emptyMap() else mapOf(0 to (single == "on"))
    }

    fun snapshotsToJson(devices: List<DeviceSnapshot>): String {
        val arr = JSONArray()
        for (d in devices) {
            val sw = JSONObject()
            d.switches.forEach { (k, v) -> sw.put(k.toString(), v) }
            arr.put(JSONObject().put("id", d.id).put("name", d.name).put("online", d.online).put("switches", sw))
        }
        return arr.toString()
    }

    fun snapshotsFromJson(json: String): List<DeviceSnapshot> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val sw = o.getJSONObject("switches")
            DeviceSnapshot(
                id = o.getString("id"),
                name = o.getString("name"),
                online = o.getBoolean("online"),
                switches = sw.keys().asSequence().associate { it.toInt() to sw.getBoolean(it) }.toSortedMap(),
            )
        }
    }
}
