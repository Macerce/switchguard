package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * eWeLink bulutunda, cihazın kendisinde çalışan haftalık zamanlayıcı. Telefon kapalıyken de çalışır.
 * [days]: 0 = Pazar … 6 = Cumartesi (cron düzeni). Saat yereldir; cihaza UTC olarak yazılır.
 */
data class RepeatTimer(
    val id: String,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val channel: Int,
    val on: Boolean,
)

/** Cihazdaki bir zamanlayıcı kaydı. [timer] null ise bu uygulamanın düzenleyemediği bir tür (korunur). */
data class TimerEntry(val raw: JSONObject, val timer: RepeatTimer?)

/**
 * Cihazın `timers` parametresini okur/yazar. Kayıt biçimi:
 * `{"mId":"…","type":"repeat","coolkit_timer_type":"repeat","enabled":1,"at":"30 5 * * 1,2,3","do":{"switch":"on"}}`
 * `at` alanı UTC cron'dur: dakika saat * * günler. Çok kanallı cihazlarda `do` normal durum
 * güncellemesiyle aynı biçimdedir: `{"switches":[{"switch":"on","outlet":1}]}`.
 */
object DeviceTimers {

    const val MAX_TIMERS = 8

    fun parse(timers: JSONArray?, utcOffsetMinutes: Int): List<TimerEntry> {
        timers ?: return emptyList()
        return (0 until timers.length()).mapNotNull { i ->
            val raw = timers.optJSONObject(i) ?: return@mapNotNull null
            TimerEntry(raw, runCatching { parseRepeat(raw, utcOffsetMinutes) }.getOrNull())
        }
    }

    private fun parseRepeat(raw: JSONObject, offset: Int): RepeatTimer? {
        if (raw.optString("type") != "repeat") return null
        val parts = raw.getString("at").trim().split(Regex("\\s+"))
        if (parts.size != 5 || parts[2] != "*" || parts[3] != "*") return null
        val utcDays = if (parts[4] == "*") ALL_DAYS else parts[4].split(',').map { it.toInt() % 7 }.toSet()
        val (local, days) = shift(parts[1].toInt() * 60 + parts[0].toInt(), utcDays, offset)
        val action = raw.getJSONObject("do")
        val (channel, value) = action.optJSONArray("switches")?.optJSONObject(0)?.let {
            it.optInt("outlet", 0) to it.getString("switch")
        } ?: (action.optInt("outlet", 0) to action.getString("switch"))
        return RepeatTimer(
            id = raw.optString("mId").ifEmpty { UUID.randomUUID().toString() },
            enabled = raw.optInt("enabled", 1) == 1,
            hour = local / 60,
            minute = local % 60,
            days = days,
            channel = channel,
            on = value == "on",
        )
    }

    fun toJson(t: RepeatTimer, multiChannel: Boolean, utcOffsetMinutes: Int): JSONObject {
        val (utc, days) = shift(t.hour * 60 + t.minute, t.days, -utcOffsetMinutes)
        val value = if (t.on) "on" else "off"
        val action = if (multiChannel) {
            JSONObject().put("switches", JSONArray().put(JSONObject().put("switch", value).put("outlet", t.channel)))
        } else {
            JSONObject().put("switch", value)
        }
        return JSONObject()
            .put("mId", t.id)
            .put("type", "repeat")
            .put("coolkit_timer_type", "repeat")
            .put("enabled", if (t.enabled) 1 else 0)
            .put("at", "${utc % 60} ${utc / 60} * * ${days.sorted().joinToString(",")}")
            .put("do", action)
    }

    /**
     * Yeni `timers` dizisi: [entries] sırası korunur, [id]'si eşleşen kayıt [replacement] ile
     * değiştirilir (null ise silinir). Uygulamanın tanımadığı kayıtlar olduğu gibi kalır.
     */
    fun rebuild(
        entries: List<TimerEntry>,
        id: String,
        replacement: RepeatTimer?,
        multiChannel: Boolean,
        utcOffsetMinutes: Int,
    ): JSONArray {
        val out = JSONArray()
        var replaced = false
        for (e in entries) {
            if (e.timer?.id == id) {
                replaced = true
                replacement?.let { out.put(toJson(it, multiChannel, utcOffsetMinutes)) }
            } else {
                out.put(e.raw)
            }
        }
        if (!replaced && replacement != null) out.put(toJson(replacement, multiChannel, utcOffsetMinutes))
        return out
    }

    /** Günün dakikasını [offset] kadar kaydırır; gün sınırı aşılırsa günler de kayar. */
    private fun shift(minuteOfDay: Int, days: Set<Int>, offset: Int): Pair<Int, Set<Int>> {
        val total = minuteOfDay + offset
        val dayShift = Math.floorDiv(total, 1440)
        val minute = Math.floorMod(total, 1440)
        return minute to days.map { Math.floorMod(it + dayShift, 7) }.toSet()
    }

    val ALL_DAYS = (0..6).toSet()
}
