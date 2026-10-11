package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Alarm kapatıldı ama sorun sürüyor (cihaz hâlâ kapalı ya da kopuk): belirlenen aralıkla yeniden hatırlatır.
 * Alarmı çaldıran durum ortadan kalkınca (cihaz eski haline döndü, listeden çıktı) hatırlatma biter.
 * Hatırlatma çalarken yenisi üretilmez; kapatılınca sayaç yeniden başlar. Zaman dışarıdan verilir.
 */
class AlarmReminders {
    /** [durationMs]: ilk alarmdan bu yana geçen süre. */
    data class Reminder(val change: Change, val durationMs: Long)

    /** [nextAt] null ise alarm henüz kapatılmadı (hatırlatma beklemez). */
    private data class Entry(val change: Change, val alarmedAt: Long, val nextAt: Long?)

    private val entries = mutableListOf<Entry>()

    /** Bir değişiklik alarm çaldı: aynı cihaz/kanalın önceki kaydının yerine geçer. */
    fun onAlarm(change: Change, now: Long) {
        entries.removeAll { key(it.change) == key(change) }
        entries += Entry(change, now, null)
    }

    /** Alarm kapatıldı: bekleyen her durum [intervalMs] sonra yeniden kontrol edilir. */
    fun onDismiss(now: Long, intervalMs: Long) {
        entries.replaceAll { it.copy(nextAt = now + intervalMs) }
    }

    /** Vakti gelen hatırlatmalar. Düzelmiş durumlar listeden çıkar. [intervalMs] 0 ise hatırlatma kapalıdır. */
    fun due(current: Map<String, DeviceSnapshot>, now: Long, intervalMs: Long, alarmActive: Boolean): List<Reminder> {
        entries.removeAll { !holds(it.change, current[it.change.deviceId]) }
        if (intervalMs <= 0 || alarmActive) return emptyList()
        val out = mutableListOf<Reminder>()
        entries.replaceAll { e ->
            val next = e.nextAt
            if (next != null && now >= next) {
                out += Reminder(e.change, now - e.alarmedAt)
                e.copy(nextAt = null)
            } else e
        }
        return out
    }

    fun clear() = entries.clear()

    fun toJson(): String = JSONArray().apply {
        entries.forEach { e ->
            val o = JSONObject().put("id", e.change.deviceId).put("name", e.change.name).put("at", e.alarmedAt)
            e.nextAt?.let { o.put("next", it) }
            when (val c = e.change) {
                is Change.Switch -> o.put("t", "sw").put("ch", c.channel).put("multi", c.multiChannel).put("from", c.from).put("to", c.to)
                is Change.Connection -> o.put("t", "conn").put("from", c.from).put("to", c.to)
            }
            put(o)
        }
    }.toString()

    companion object {
        private fun key(c: Change) = when (c) {
            is Change.Switch -> "${c.deviceId}/${c.channel}"
            is Change.Connection -> "${c.deviceId}/conn"
        }

        /** Alarmı çaldıran durum hâlâ geçerli mi. */
        private fun holds(c: Change, d: DeviceSnapshot?): Boolean {
            d ?: return false
            return when (c) {
                is Change.Switch -> d.switches[c.channel] == c.to
                is Change.Connection -> d.online == c.to
            }
        }

        /** Bozuk ya da boş kayıt: hatırlatma yok. */
        fun fromJson(json: String?): AlarmReminders = AlarmReminders().apply {
            val arr = runCatching { JSONArray(json ?: return@apply) }.getOrNull() ?: return@apply
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                runCatching {
                    val id = o.getString("id")
                    val name = o.optString("name")
                    val change: Change = when (o.getString("t")) {
                        "sw" -> Change.Switch(id, name, o.getInt("ch"), o.getBoolean("multi"), o.getBoolean("from"), o.getBoolean("to"))
                        else -> Change.Connection(id, name, o.getBoolean("from"), o.getBoolean("to"))
                    }
                    entries += Entry(change, o.getLong("at"), if (o.has("next")) o.getLong("next") else null)
                }
            }
        }
    }
}
