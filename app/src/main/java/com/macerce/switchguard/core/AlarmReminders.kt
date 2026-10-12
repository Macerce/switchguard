package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Alarm kapatıldı ama sorun sürüyor (cihaz hâlâ alarmı çaldıran durumda): belirlenen aralıkla yeniden hatırlatır.
 * Alarmı çaldıran durum ortadan kalkınca (cihaz eski haline döndü, listeden çıktı, artık alarm verilmiyor)
 * hatırlatma biter. Eski duruma dönüşün kendisi hatırlatılmaz (varsayılan kurallarda o da alarm olsa da).
 * Hatırlatma çalarken yenisi üretilmez; kapatılınca sayaç o andan başlar. Zaman dışarıdan verilir.
 */
class AlarmReminders {
    /** [durationMs]: ilk alarmdan bu yana geçen süre. */
    data class Reminder(val change: Change, val durationMs: Long)

    /** [armedAt]: alarmın kapatıldığı an; null ise henüz kapatılmadı (hatırlatma beklemez). */
    private data class Entry(val change: Change, val alarmedAt: Long, val armedAt: Long?)

    private val entries = mutableListOf<Entry>()

    /**
     * Bir değişiklik alarm çaldı: aynı cihaz/kanalın önceki kaydının yerine geçer. Değişiklik önceki alarmdaki
     * durumdan geri dönüşse (kapandı, sonra yeniden açıldı) sorun bitmiştir: kayıt silinir, yenisi açılmaz.
     */
    fun onAlarm(change: Change, now: Long) {
        val previous = entries.firstOrNull { key(it.change) == key(change) }
        entries.removeAll { key(it.change) == key(change) }
        if (previous != null && reverts(previous.change, change)) return
        entries += Entry(change, now, null)
    }

    /** Alarm kapatıldı: henüz beklemeyen durumların sayacı başlar (bekleyenlerinki yeni alarmla ertelenmez). */
    fun onDismiss(now: Long) {
        entries.replaceAll { if (it.armedAt == null) it.copy(armedAt = now) else it }
    }

    /**
     * Vakti gelen hatırlatmalar. Düzelmiş ya da artık alarm verilmeyen ([stillAlarmed] false: izlenmiyor,
     * kuralı değişti) durumlar listeden çıkar. [intervalMs] 0 ise hatırlatma kapalıdır ve bekleyenler silinir.
     */
    fun due(
        current: Map<String, DeviceSnapshot>,
        now: Long,
        intervalMs: Long,
        alarmActive: Boolean,
        stillAlarmed: (Change) -> Boolean = { true },
    ): List<Reminder> {
        if (intervalMs <= 0) {
            entries.clear()
            return emptyList()
        }
        entries.removeAll { !holds(it.change, current[it.change.deviceId]) || !stillAlarmed(it.change) }
        if (alarmActive) return emptyList()
        val out = mutableListOf<Reminder>()
        entries.replaceAll { e ->
            val armed = e.armedAt
            if (armed != null && now >= armed + intervalMs) {
                out += Reminder(e.change, now - e.alarmedAt)
                e.copy(armedAt = null)
            } else e
        }
        return out
    }

    fun clear() = entries.clear()

    fun toJson(): String = JSONArray().apply {
        entries.forEach { e ->
            val o = JSONObject().put("id", e.change.deviceId).put("name", e.change.name).put("at", e.alarmedAt)
            e.armedAt?.let { o.put("armed", it) }
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

        /** [new], [old] alarmının durumundan geri dönüş mü (ör. kapandı, sonra yeniden açıldı; koptu, sonra geldi). */
        private fun reverts(old: Change, new: Change) = when {
            old is Change.Switch && new is Change.Switch -> new.to == old.from
            old is Change.Connection && new is Change.Connection -> new.to == old.from
            else -> false
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
                    entries += Entry(change, o.getLong("at"), if (o.has("armed")) o.getLong("armed") else null)
                }
            }
        }
    }
}
