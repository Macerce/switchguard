package com.macerce.switchguard.core

import org.json.JSONObject

enum class EventType { TURNED_OFF, TURNED_ON, WENT_OFFLINE, CAME_ONLINE }

/** Bir olay olduğunda ne yapılacağı. */
enum class AlertAction { ALARM, NOTIFY, IGNORE }

data class DeviceRules(
    val monitored: Boolean = true,
    val actions: Map<EventType, AlertAction> = emptyMap(),
    /** Bu cihazın alarm/bildirim sesi; null ise genel ayardaki ses. */
    val soundUri: String? = null,
) {
    fun actionFor(type: EventType): AlertAction = actions[type] ?: DEFAULTS.getValue(type)

    fun with(type: EventType, action: AlertAction) = copy(actions = actions + (type to action))

    companion object {
        /** Tekrar online olmak iyi haber: alarm yerine bildirim yeterli. */
        val DEFAULTS = mapOf(
            EventType.TURNED_OFF to AlertAction.ALARM,
            EventType.TURNED_ON to AlertAction.ALARM,
            EventType.WENT_OFFLINE to AlertAction.ALARM,
            EventType.CAME_ONLINE to AlertAction.NOTIFY,
        )

        fun mapToJson(rules: Map<String, DeviceRules>): String {
            val root = JSONObject()
            for ((id, r) in rules) {
                val actions = JSONObject()
                r.actions.forEach { (t, a) -> actions.put(t.name, a.name) }
                val o = JSONObject().put("monitored", r.monitored).put("actions", actions)
                r.soundUri?.let { o.put("sound", it) }
                root.put(id, o)
            }
            return root.toString()
        }

        fun mapFromJson(json: String): Map<String, DeviceRules> {
            val root = JSONObject(json)
            return root.keys().asSequence().associateWith { id ->
                val o = root.getJSONObject(id)
                val a = o.optJSONObject("actions") ?: JSONObject()
                DeviceRules(
                    monitored = o.optBoolean("monitored", true),
                    actions = a.keys().asSequence().mapNotNull { k ->
                        val type = runCatching { EventType.valueOf(k) }.getOrNull()
                        val action = runCatching { AlertAction.valueOf(a.getString(k)) }.getOrNull()
                        if (type != null && action != null) type to action else null
                    }.toMap(),
                    soundUri = o.optString("sound").ifEmpty { null },
                )
            }
        }
    }
}

/** Günün dakikası cinsinden [startMinute, endMinute) aralığı; gece yarısını aşabilir. */
data class QuietHours(val enabled: Boolean, val startMinute: Int, val endMinute: Int) {
    fun contains(minuteOfDay: Int): Boolean {
        if (!enabled || startMinute == endMinute) return false
        return if (startMinute < endMinute) minuteOfDay in startMinute until endMinute
        else minuteOfDay >= startMinute || minuteOfDay < endMinute
    }
}

object RuleEngine {
    /** Sessiz saatlerde alarm, sessiz bildirime düşürülür; olay yine de kaydedilir. */
    fun effective(action: AlertAction, quiet: Boolean): AlertAction =
        if (quiet && action == AlertAction.ALARM) AlertAction.NOTIFY else action
}
