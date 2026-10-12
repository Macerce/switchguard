package com.macerce.switchguard.core

import org.json.JSONObject

/**
 * SharedPreferences değerlerini türleriyle JSON'a yazar ve geri okur (yedek ve cihazlar arası aktarım için).
 * Yalnızca izin listesindeki anahtarlar yazılır ve okunur: oturum token'ları gibi gizliler hiçbir zaman girmez.
 */
object TypedPrefs {
    fun encode(all: Map<String, *>, keys: Set<String>): JSONObject = JSONObject().apply {
        all.forEach { (k, v) ->
            if (k !in keys || v == null) return@forEach
            val typed = when (v) {
                is String -> JSONObject().put("t", "s").put("v", v)
                is Boolean -> JSONObject().put("t", "b").put("v", v)
                is Int -> JSONObject().put("t", "i").put("v", v)
                is Long -> JSONObject().put("t", "l").put("v", v)
                is Float -> JSONObject().put("t", "f").put("v", v.toDouble())
                else -> null
            }
            if (typed != null) put(k, typed)
        }
    }

    /** Tanınmayan tür ya da izin listesi dışındaki anahtar atlanır. */
    fun decode(json: JSONObject, keys: Set<String>): Map<String, Any> = buildMap {
        json.keys().forEach { k ->
            if (k !in keys) return@forEach
            val o = json.optJSONObject(k) ?: return@forEach
            val v: Any? = runCatching {
                when (o.getString("t")) {
                    "s" -> o.getString("v")
                    "b" -> o.getBoolean("v")
                    "i" -> o.getInt("v")
                    "l" -> o.getLong("v")
                    "f" -> o.getDouble("v").toFloat()
                    else -> null
                }
            }.getOrNull()
            if (v != null) put(k, v)
        }
    }
}
