package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Günlük özet: telefondaki `service/DailySummary.kt` (aynı metinler ve kurallar). Gönderim zamanı Android'de
 * AlarmManager'la kurulur; masaüstünde motor her tick'te [dueAt]'e bakar (bkz. `MonitorEngine.checkSummary`).
 */
object DailySummary {

    /** Bugünün (gece yarısından şimdiye) özeti; kayıt yoksa null (izleme açıksa "hiçbir şey olmadı"). */
    fun build(store: Store, metrics: MetricsLog, s: Strings, now: Long): List<String>? {
        val from = startOfDay(now)
        val lines = mutableListOf<String>()
        for (d in store.snapshots) {
            val parts = mutableListOf<String>()
            for (ch in d.switches.keys.sorted()) {
                val u = metrics.usage(d.id, ch, from, now)
                if (u.onCount == 0 && u.onMillis == 0L) continue
                val label = if (d.isMultiChannel) s.format("channel_short", ch + 1) + " " else ""
                parts += label + s.format("summary_usage", u.onCount, duration(s, u.onMillis))
            }
            if (d.hasEnergy) {
                val kwh = metrics.kwh(d.id, from, now)
                if (kwh > 0.0005) parts += formatKwh(kwh, s.locale)
            }
            if (parts.isNotEmpty()) lines += "${d.name}: ${parts.joinToString(", ")}"
        }
        if (lines.isEmpty()) {
            // Hiç kayıt yoksa izleme bugün hiç çalışmamıştır; boş özet gönderme.
            return if (store.monitoringEnabled) listOf(s["summary_nothing"]) else null
        }
        return lines
    }

    fun duration(s: Strings, ms: Long): String {
        val totalMin = ms / 60_000
        return if (totalMin >= 60) s.format("duration_hm", totalMin / 60, totalMin % 60) else s.format("duration_m", totalMin)
    }

    fun formatKwh(kwh: Double, locale: Locale): String =
        if (kwh < 1) String.format(locale, "%.0f Wh", kwh * 1000) else String.format(locale, "%.2f kWh", kwh)

    fun startOfDay(time: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(time).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    /** [now]'dan önceki (ya da [now]'a eşit) en son özet zamanı: bugün [minuteOfDay] geçtiyse bugün, geçmediyse dün. */
    fun dueAt(now: Long, minuteOfDay: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        fun at(day: LocalDate) = day.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()
        val t = at(today)
        return if (t <= now) t else at(today.minusDays(1))
    }

    /** Bilgisayar özet saatinde uyuyorduysa uyanınca en çok bu kadar geç gönderilir; daha geçse o günün özeti atlanır. */
    const val LATE_MS = 2 * 3_600_000L
}
