package com.macerce.switchguard.core

import java.util.Calendar
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

/**
 * "Saat" tetikleyicisinin vakitleri (saf mantık): [minuteOfDay] (SS:DD) ve [days] (bit 0 = pazartesi … bit 6 = pazar,
 * [ActiveWindow] ile aynı). Yaz saati geçişlerinde de takvimdeki saat kullanılır.
 */
object ClockTrigger {
    /** Bugünkü vakit; bugün seçili gün değilse null. */
    fun todayAt(minuteOfDay: Int, days: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): Long? {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        if (days and (1 shl ActiveWindow.dayIndex(cal)) == 0) return null
        return at(cal, minuteOfDay)
    }

    /** [now]'dan sonraki ilk vakit (bugün ya da sonraki seçili gün); hiç gün seçili değilse null. */
    fun nextAt(minuteOfDay: Int, days: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): Long? {
        if (days and ActiveWindow.ALL_DAYS == 0) return null
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        repeat(8) {
            if (days and (1 shl ActiveWindow.dayIndex(cal)) != 0) {
                val t = at(cal.clone() as Calendar, minuteOfDay)
                if (t > now) return t
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    /** Günün başlangıcı (yerel gece yarısı). */
    fun dayStart(now: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(zone).apply {
            timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun at(cal: Calendar, minuteOfDay: Int): Long {
        cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60); cal.set(Calendar.MINUTE, minuteOfDay % 60)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * Bir kuralın ek eylemleri ([Automation.extraActions]): ilk eylemden sonra sırayla; her biri kendinden öncekinden
 * [AutoAction.waitMinutes] dakika sonra. Beklemeli adımlar kalıcı listeye yazılır (servis yeniden başlasa da kaybolmaz).
 */
object ActionSteps {
    /** Vakti gelince çalıştırılacak adım: kuralın [step]. ek eylemi. */
    data class Due(val automationId: String, val step: Int, val at: Long)

    /** Kural şimdi çalıştı: hemen yapılacak ek adımların sırası ve sonraya kalanlar. */
    fun plan(a: Automation, now: Long): Pair<List<Int>, List<Due>> {
        var t = now
        val immediate = mutableListOf<Int>()
        val later = mutableListOf<Due>()
        a.extraActions.forEachIndexed { i, s ->
            t += s.waitMinutes * 60_000L
            if (t <= now) immediate += i else later += Due(a.id, i, t)
        }
        return immediate to later
    }

    fun toJson(list: List<Due>): String = JSONArray(list.map {
        JSONObject().put("id", it.automationId).put("step", it.step).put("at", it.at)
    }).toString()

    fun fromJson(json: String?): List<Due> = runCatching {
        val a = JSONArray(json ?: "[]")
        (0 until a.length()).map { i -> a.getJSONObject(i).let { Due(it.getString("id"), it.getInt("step"), it.getLong("at")) } }
    }.getOrDefault(emptyList())
}
