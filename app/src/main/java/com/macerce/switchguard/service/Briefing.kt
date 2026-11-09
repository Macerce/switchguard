package com.macerce.switchguard.service

import android.content.Context
import android.text.format.DateFormat
import com.macerce.switchguard.R
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.StandbyBoard
import com.macerce.switchguard.core.StandbyBoard.UpcomingKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Sözlü özetler: şu anki durum (hava, açık cihazlar, çevrimdışı olanlar, güç, yakında çalışacak işler) ve günün özeti.
 * Metin hem ekranda gösterilir hem sesli okunur; satırlar nokta ile biter ki okuyucu duraklasın.
 * Ağ ve veritabanı okur: ana iş parçacığından çağırma.
 */
object Briefing {

    /** "Açık (3): Salon, Pompa (1, 3), Klima." Açık cihaz yoksa null. */
    fun onLine(context: Context): String? {
        val labels = EventLog.strings(context).labels
        val on = Store.get(context).snapshots.filter { !DeviceGroup.isGroup(it.id) && it.online && it.switches.values.any { s -> s } }
        if (on.isEmpty()) return null
        val names = on.joinToString(", ") { d ->
            if (d.isMultiChannel) d.name + " (" + d.switches.filterValues { it }.keys.sorted().joinToString(", ") { (it + 1).toString() } + ")" else d.name
        }
        return "${capital(context, labels.on)} (${on.size}): $names."
    }

    fun weatherLabel(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        51, 53, 55, 56, 57 -> R.string.weather_drizzle
        61, 63, 65, 66, 67 -> R.string.weather_rain
        71, 73, 75, 77, 85, 86 -> R.string.weather_snow
        80, 81, 82 -> R.string.weather_showers
        95, 96, 99 -> R.string.weather_storm
        else -> R.string.weather_cloudy
    }

    /** Şu anki durum. */
    fun status(context: Context, now: Long = System.currentTimeMillis()): String {
        val store = Store.get(context)
        val labels = EventLog.strings(context).labels
        val lines = mutableListOf<String>()
        val lat = store.latitude
        val lon = store.longitude
        if (lat != null && lon != null) {
            runCatching { WeatherClient.today(lat, lon) }.getOrNull()?.let { w ->
                val range = if (w.min != null && w.max != null) " (${w.min.roundToInt()}° – ${w.max.roundToInt()}°)" else ""
                val rain = w.rainChance?.takeIf { it > 0 }?.let { ", " + context.getString(R.string.board_rain, it) }.orEmpty()
                lines += "${context.getString(weatherLabel(w.code))}, ${w.temp.roundToInt()}°$range$rain."
            }
        }
        store.pendingAlarm.lastOrNull()?.let { lines += context.getString(R.string.board_alarm) + ": $it." }
        lines += context.getString(if (store.isAway(now)) R.string.board_away else R.string.board_home) + "."
        lines += onLine(context) ?: context.getString(R.string.chat_none_on)
        val snapshots = store.snapshots.filterNot { DeviceGroup.isGroup(it.id) }
        val monitored = store.monitoredIds()
        val offline = snapshots.filter { it.id in monitored && !it.online }
        if (offline.isNotEmpty()) lines += context.getString(R.string.board_offline, offline.size) + ": " + offline.joinToString(", ") { it.name } + "."
        val energy = snapshots.filter { it.hasEnergy }
        if (energy.isNotEmpty()) {
            val metrics = MetricsLog.get(context)
            val from = DailySummary.startOfDay(now)
            val kwh = energy.sumOf { runCatching { metrics.kwh(it.id, from, now) }.getOrDefault(0.0) }
            val watts = energy.filter { it.online }.sumOf { it.power ?: 0.0 }
            lines += "${ChatRunner.watts(watts)} W, " +
                context.getString(R.string.board_today_kwh, String.format(context.resources.configuration.locales[0], "%.1f", kwh)) + "."
        }
        val upcoming = StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            RuntimeState.get(context).templateRuns, lat, lon, now,
        ).take(5)
        if (upcoming.isEmpty()) lines += context.getString(R.string.board_no_upcoming) + "."
        else {
            val time = DateFormat.getTimeFormat(context)
            lines += context.getString(R.string.board_upcoming) + ": " + upcoming.joinToString("; ") { u ->
                val kind = context.getString(
                    when (u.kind) {
                        UpcomingKind.SUNRISE -> R.string.board_kind_sunrise
                        UpcomingKind.SUNSET -> R.string.board_kind_sunset
                        UpcomingKind.CYCLE_START -> R.string.board_kind_cycle_start
                        UpcomingKind.CYCLE_STEP -> R.string.board_kind_cycle_step
                        UpcomingKind.CYCLE_END -> R.string.board_kind_cycle_end
                        UpcomingKind.TEMPLATE -> R.string.board_kind_template
                    }
                )
                "${time.format(Date(u.time))} ${u.name} ($kind)"
            } + "."
        }
        return lines.joinToString("\n")
    }

    /**
     * Günün ayrıntılı özeti: alarm ve kopma sayısı, toplam tüketim ve maliyet, dünle karşılaştırma,
     * cihaz cihaz kaç kez açıldığı / ne kadar açık kaldığı / ne harcadığı, en uzun açık kalan ve şu an açık olanlar.
     */
    fun summaryLines(context: Context, now: Long = System.currentTimeMillis()): List<String> {
        val store = Store.get(context)
        val metrics = MetricsLog.get(context)
        val doc = DailySummary.doc(context, now)
        val lines = mutableListOf<String>()
        lines += doc.kpis.joinToString(" · ") { "${it.label}: ${it.value}" } + "."
        val energy = store.snapshots.filter { it.hasEnergy && !DeviceGroup.isGroup(it.id) }
        if (energy.isNotEmpty()) {
            val day = 86_400_000L
            val from = DailySummary.startOfDay(now - day)
            val yesterday = energy.sumOf { runCatching { metrics.kwh(it.id, from, now - day) }.getOrDefault(0.0) }
            if (yesterday > 0.0005) lines += context.getString(R.string.summary_yesterday, DailySummary.formatKwh(yesterday))
        }
        val devices = DailySummary.build(context, now)
        if (devices == null) lines += context.getString(R.string.summary_no_data) else lines += devices.map { "$it." }
        doc.sections.flatMap { it.findings }.map { it.text }.filter { it != context.getString(R.string.summary_no_data) }.forEach { lines += it }
        onLine(context)?.let { lines += it }
        return lines
    }

    fun summary(context: Context, now: Long = System.currentTimeMillis()): String =
        context.getString(R.string.summary_title) + ".\n" + summaryLines(context, now).joinToString("\n")

    private fun capital(context: Context, word: String): String {
        val locale: Locale = context.resources.configuration.locales[0]
        return word.lowercase(locale).replaceFirstChar { it.titlecase(locale) }
    }
}
