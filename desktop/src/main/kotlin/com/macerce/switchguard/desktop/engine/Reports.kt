package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Compare
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Signal
import com.macerce.switchguard.core.Standby
import com.macerce.switchguard.core.Tone
import com.macerce.switchguard.core.Trend
import com.macerce.switchguard.core.WeeklyStats
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.devices.formatWatts
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Raporlar: telefondaki `service/WeeklyReport.kt` ve `DailySummary.doc` (aynı metinler, göstergeler ve bulgular).
 * Zamanlama masaüstünde motorun tick'inde ([weeklyDueAt], bkz. `MonitorEngine.checkWeekly`).
 */
object Reports {
    private const val DAY_MS = 86_400_000L
    /** Rapor saati: 09:00. */
    private const val MINUTE = 9 * 60

    /** [now]'dan önceki (ya da eşit) en son rapor zamanı: pazartesi 09:00 ya da ([daily]) her gün 09:00. */
    fun weeklyDueAt(now: Long, daily: Boolean = false, zone: ZoneId = ZoneId.systemDefault()): Long {
        var day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        while (true) {
            val at = day.atStartOfDay(zone).plusMinutes(MINUTE.toLong()).toInstant().toEpochMilli()
            if (at <= now && (daily || day.dayOfWeek == DayOfWeek.MONDAY)) return at
            day = day.minusDays(1)
        }
    }

    /** Bildirimde: göstergeler tek satır, sonra ilk öneri. */
    fun notificationLines(doc: ReportDoc): List<String> =
        doc.kpis.map { k -> "${k.label}: ${k.value}" + (k.trend?.let { " (${it.text})" } ?: "") } + doc.actions.take(1).map { "→ $it" }

    private fun money(amount: Double, locale: Locale) = com.macerce.switchguard.desktop.ui.energy.EnergyFormat.money(amount, locale)

    /** "13–19 Eki 2026" gibi aralık. */
    private fun dateRange(from: Long, to: Long, locale: Locale): String {
        val f = SimpleDateFormat("d MMM", locale)
        val y = SimpleDateFormat("yyyy", locale)
        return "${f.format(Date(from))} – ${f.format(Date(to))} ${y.format(Date(to))}"
    }

    fun daily(store: Store, metrics: MetricsLog, log: EventLog, s: Strings, now: Long): ReportDoc {
        val from = DailySummary.startOfDay(now)
        val locale = s.locale
        val rows = mutableListOf<List<String>>()
        var longest: Pair<String, Long>? = null
        var totalKwh = 0.0
        var totalCost = 0.0
        for (d in store.snapshots) {
            for (ch in d.switches.keys.sorted()) {
                val u = metrics.usage(d.id, ch, from, now)
                val name = if (d.isMultiChannel) s.format("label_channel", d.name, ch + 1) else d.name
                val kwh = if (d.hasEnergy && ch == d.switches.keys.minOrNull()) metrics.kwh(d.id, from, now) else null
                if (kwh != null) {
                    totalKwh += kwh
                    totalCost += metrics.cost(d.id, from, now, store.tariffFor(d.id))
                }
                if (u.onCount == 0 && u.onMillis == 0L && (kwh ?: 0.0) < 0.0005) continue
                rows += listOf(name, u.onCount.toString(), DailySummary.duration(s, u.onMillis), kwh?.let(s::formatKwh) ?: "—")
                if (longest == null || u.onMillis > longest!!.second) longest = name to u.onMillis
            }
        }
        val events = log.export(null, from)
        val alarms = events.count { it.kind == EventKind.DISMISSED || it.kind == EventKind.TELEGRAM && it.args?.contains("tg_ack") == true }
        val drops = events.count { it.kind == EventType.WENT_OFFLINE.name }
        val kpis = buildList {
            add(Kpi(s["kpi_alarms"], alarms.toString(), s["kpi_alarms_today_caption"]))
            add(Kpi(s["kpi_drops"], drops.toString(), s["kpi_drops_caption"],
                tone = if (drops >= Signal.FREQUENT_DROPS) Tone.WARN else Tone.NEUTRAL))
            if (totalKwh > 0.0005) add(Kpi(s["kpi_energy"], s.formatKwh(totalKwh), s["kpi_energy_today_caption"]))
            if (totalCost > 0) add(Kpi(s["kpi_cost"], money(totalCost, locale), s["kpi_cost_today_caption"]))
        }
        val findings = buildList {
            if (rows.isEmpty()) add(Finding(Severity.INFO, s["summary_no_data"]))
            longest?.takeIf { it.second >= 60_000 }?.let { (n, ms) ->
                add(Finding(Severity.INFO, s.format("daily_longest", n, DailySummary.duration(s, ms))))
            }
        }
        return ReportDoc(
            title = s["summary_title"],
            subtitle = java.text.SimpleDateFormat(if (s.language == "tr") "d MMMM yyyy, EEEE" else "EEEE, MMMM d, yyyy", locale).format(java.util.Date(now)),
            intro = s["daily_intro"],
            kpis = kpis,
            sections = listOf(
                ReportSection(
                    s["daily_devices"], s["daily_devices_explain"], findings,
                    rows.takeIf { it.isNotEmpty() }?.let {
                        ReportTable(
                            listOf(s["rep_col_device"], s["daily_col_on"], s["daily_col_time"], s["rep_col_kwh"]),
                            it, s["daily_table_note"],
                        )
                    },
                ),
            ),
            footnote = s["report_footnote"],
        )
    }


    /** Son [days] günün raporu; aynı uzunluktaki önceki dönemle karşılaştırılır. */
    fun weekly(store: Store, metrics: MetricsLog, log: EventLog, s: Strings, now: Long, days: Int = 7): ReportDoc {
        val locale = s.locale
        val durations = s.durations()
        fun dur(ms: Long) = DurationText.format(ms, durations)
        val period = days * DAY_MS
        val from = now - period
        val prevFrom = from - period
        val all = log.export(null, prevFrom).reversed()
        val current = all.filter { it.time >= from }
        val previous = all.filter { it.time < from }
        fun stats(list: List<LoggedEvent>) = WeeklyStats.from(list.map { WeeklyStats.Entry(it.kind, it.deviceName, EventArgs.parse(it.args)) })
        val week = stats(current)
        val prev = stats(previous)
        val month = stats(log.export(null, EnergyWatch.startOfMonth(now)).reversed())
        val weekly = days >= 7
        val prevLabel = s.str(if (weekly) "report_vs_last_week" else "report_vs_yesterday")

        fun trend(nowV: Double, beforeV: Double, lowerIsBetter: Boolean, format: (Double) -> String): Trend? {
            if (nowV == 0.0 && beforeV == 0.0) return null
            val pct = Compare.change(nowV, beforeV)
            val tone = Compare.tone(pct, lowerIsBetter)
            val text = when {
                pct == null -> s.format("report_trend_new", prevLabel, format(beforeV))
                kotlin.math.abs(pct) < Compare.SAME_PERCENT -> s.format("report_trend_same", prevLabel)
                pct > 0 -> s.format("report_trend_up", prevLabel, pct)
                else -> s.format("report_trend_down", prevLabel, -pct)
            }
            return Trend(text, tone)
        }

        // --------------------------------------------------- göstergeler
        val kpis = mutableListOf<Kpi>()
        kpis += Kpi(
            s["kpi_outage"],
            if (week.outages == 0) s["report_none"] else s.plural("kpi_outage_value", week.outages, week.outages, dur(week.outageMs)),
            s["kpi_outage_caption"],
            trend(week.outages.toDouble(), prev.outages.toDouble(), true) { it.toInt().toString() },
            if (week.outages > 0) Tone.WARN else Tone.GOOD,
        )
        kpis += Kpi(
            s["kpi_alarms"],
            week.alarms.toString(),
            s["kpi_alarms_caption"],
            trend(week.alarms.toDouble(), prev.alarms.toDouble(), true) { it.toInt().toString() },
        )
        val avg = week.avgResponseMs
        kpis += Kpi(
            s["kpi_response"],
            avg?.let(::dur) ?: "—",
            s["kpi_response_caption"],
            if (avg != null && prev.avgResponseMs != null) trend(avg.toDouble(), prev.avgResponseMs.toDouble(), true) { dur(it.toLong()) } else null,
            when { avg == null -> Tone.NEUTRAL; avg > 10 * 60_000L -> Tone.WARN; else -> Tone.GOOD },
        )
        val uptime = ((1 - week.gapMs.toDouble() / period) * 100).coerceIn(0.0, 100.0)
        kpis += Kpi(
            s["kpi_uptime"],
            s.format("volume_pct", uptime.toInt()),
            s["kpi_uptime_caption"],
            tone = if (uptime < 95) Tone.WARN else Tone.GOOD,
        )

        // Tüketim
        val energyDevices = store.snapshots.filter { it.hasEnergy }
        data class Use(val name: String, val id: String, val kwh: Double, val prevKwh: Double, val cost: Double)
        val uses = energyDevices.map { d ->
            val tariff = store.tariffFor(d.id)
            Use(d.name, d.id, metrics.kwh(d.id, from, now), metrics.kwh(d.id, prevFrom, from), metrics.cost(d.id, from, now, tariff))
        }.filter { it.kwh > 0.0005 || it.prevKwh > 0.0005 }
        val totalKwh = uses.sumOf { it.kwh }
        val totalPrev = uses.sumOf { it.prevKwh }
        val totalCost = uses.sumOf { it.cost }
        if (uses.isNotEmpty()) {
            kpis += Kpi(
                s["kpi_energy"],
                s.formatKwh(totalKwh),
                s["kpi_energy_caption"],
                trend(totalKwh, totalPrev, true) { s.formatKwh(it) },
            )
            if (totalCost > 0) {
                kpis += Kpi(
                    s["kpi_cost"],
                    money(totalCost, locale),
                    s.str(if (weekly) "kpi_cost_caption_week" else "kpi_cost_caption_day",
                        money(totalCost * 30.0 / days, locale)),
                )
            }
        }

        // --------------------------------------------------- bölümler
        val sections = mutableListOf<ReportSection>()
        val fmtTime = { t: Long -> java.text.SimpleDateFormat("d MMM HH:mm", locale).format(java.util.Date(t)) }

        // Kesintiler ve alarmlar
        val events = mutableListOf<Finding>()
        current.mapNotNull { e -> (EventArgs.parse(e.args) as? EventArgs.OutageEnded)?.let { e to it } }.take(5).forEach { (_, a) ->
            events += Finding(Severity.WARN, s.format("rep_outage_item", fmtTime(a.from), dur(a.to - a.from)))
        }
        if (week.outages == 0) events += Finding(Severity.GOOD, s["rep_no_outage"])
        if (month.outages > 0) events += Finding(Severity.INFO, s.plural("rep_month_outages", month.outages, month.outages, dur(month.outageMs)))
        when {
            week.alarms == 0 -> events += Finding(Severity.GOOD, s["rep_no_alarms"])
            avg != null && avg > 10 * 60_000L -> events += Finding(
                Severity.WARN, s.format("rep_slow_response", dur(avg)),
                s.str(if (store.escalationReady) "act_check_sound" else "act_add_second_person"),
            )
            avg != null -> events += Finding(Severity.GOOD, s.format("rep_good_response", dur(avg)))
        }
        if (week.escalations > 0) events += Finding(Severity.INFO, s.plural("rep_escalations", week.escalations, week.escalations))
        sections += ReportSection(s["rep_sec_events"], s["rep_sec_events_explain"], events)

        // Bağlantı
        val conn = mutableListOf<Finding>()
        val drops = week.drops.toMap()
        val connRows = store.snapshots.filter { it.cloud != com.macerce.switchguard.core.Cloud.DEMO }.map { d ->
            val n = drops[d.name] ?: 0
            val signal = d.rssi?.let { s.format("rep_signal_cell", s.str(qualityLabel(Signal.quality(it))), it) } ?: "—"
            when (Signal.dropCause(n, d.rssi)) {
                Signal.Cause.WEAK_SIGNAL -> conn += Finding(Severity.WARN, s.format("rep_drops_signal", d.name, n, d.rssi ?: 0), s["act_move_closer"])
                Signal.Cause.POWER_OR_ROUTER -> conn += Finding(Severity.WARN, s.format("rep_drops_power", d.name, n), s["act_check_power"])
                Signal.Cause.UNKNOWN -> conn += Finding(Severity.WARN, s.format("rep_drops_unknown", d.name, n), s["act_check_wifi"])
                null -> if (d.rssi != null && Signal.quality(d.rssi) == Signal.Quality.WEAK) {
                    conn += Finding(Severity.INFO, s.format("rep_weak_no_drops", d.name, d.rssi), s["act_move_closer"])
                }
            }
            listOf(d.name, n.toString(), signal)
        }.sortedByDescending { it[1].toInt() }
        if (conn.isEmpty() && connRows.isNotEmpty()) conn += Finding(Severity.GOOD, s["rep_conn_ok"])
        if (connRows.isNotEmpty()) {
            sections += ReportSection(
                s["rep_sec_conn"], s["rep_sec_conn_explain"], conn,
                ReportTable(
                    listOf(s["rep_col_device"], s["rep_col_drops"], s["rep_col_signal"]),
                    connRows.take(20), s["rep_conn_note"],
                ),
            )
        }

        // Tüketim
        if (uses.isNotEmpty()) {
            val energy = mutableListOf<Finding>()
            current.filter { it.kind == EventKind.ENERGY }.mapNotNull { e -> (EventArgs.parse(e.args) as? EventArgs.EnergyAnomaly)?.let { e to it } }.take(5).forEach { (e, a) ->
                energy += Finding(
                    if (a.high) Severity.WARN else Severity.ALERT, e.text(s.eventStrings()),
                    s.str(if (a.high) "act_check_high" else "act_check_low", e.deviceName),
                )
            }
            uses.forEach { u ->
                runCatching { metrics.standby(u.id, now) }.getOrNull()?.let { st ->
                    val cost = store.tariffFor(u.id).takeIf { it.hasPrice }?.let { Standby.monthlyCost(st.watts, it, now) } ?: 0.0
                    if (st.watts >= STANDBY_NOTE_W) energy += Finding(
                        Severity.INFO,
                        s.format("rep_standby", u.name, formatWatts(st.watts, locale), s.formatKwh(st.monthlyKwh)) +
                            (if (cost > 0) " (≈ ${money(cost, locale)})" else ""),
                        s["act_standby"],
                    )
                }
            }
            val top = uses.maxBy { it.kwh }
            if (totalKwh > 0 && uses.size > 1) energy += Finding(Severity.INFO, s.format("rep_top_consumer", top.name, (top.kwh * 100 / totalKwh).toInt()))
            val withCost = totalCost > 0
            val headers = listOfNotNull(
                s["rep_col_device"], s["rep_col_kwh"], s["rep_col_share"],
                s["rep_col_cost"].takeIf { withCost },
                s.str(if (weekly) "rep_col_last_week" else "rep_col_yesterday"),
            )
            val rows = uses.sortedByDescending { it.kwh }.map { u ->
                listOfNotNull(
                    u.name, s.formatKwh(u.kwh),
                    if (totalKwh > 0) s.format("volume_pct", (u.kwh * 100 / totalKwh).toInt()) else "—",
                    money(u.cost, locale).takeIf { withCost },
                    s.formatKwh(u.prevKwh),
                )
            }
            sections += ReportSection(
                s["rep_sec_energy"], s["rep_sec_energy_explain"], energy,
                ReportTable(headers, rows, s["rep_energy_note"]),
            )
        }

        // İzleme
        val watch = mutableListOf<Finding>()
        if (week.gapMs >= 60_000) watch += Finding(Severity.WARN, s.format("rep_gap", dur(week.gapMs)), s["act_battery"])
        else watch += Finding(Severity.GOOD, s["rep_no_gap"])
        sections += ReportSection(s["rep_sec_monitor"], s["rep_sec_monitor_explain"], watch)

        val range = dateRange(from, now, locale)
        return ReportDoc(
            title = s.str(if (weekly) "weekly_title" else "daily_report_title"),
            subtitle = s.format("report_subtitle", range, store.snapshots.size),
            intro = s.str(if (weekly) "weekly_intro" else "daily_report_intro"),
            kpis = kpis,
            sections = sections,
            glossary = glossary(s),
            footnote = s["report_footnote"],
        )
    }

    /** Raporlarda geçen terimler. */
    fun glossary(s: Strings): List<Pair<String, String>> = listOf(
        "gl_kwh" to "gl_kwh_def",
        "gl_w" to "gl_w_def",
        "gl_outage" to "gl_outage_def",
        "gl_response" to "gl_response_def",
        "gl_uptime" to "gl_uptime_def",
        "gl_dbm" to "gl_dbm_def",
        "gl_standby" to "gl_standby_def",
    ).map { (k, v) -> s.str(k) to s.str(v) }

    fun qualityLabel(q: Signal.Quality): String = when (q) {
        Signal.Quality.EXCELLENT -> "signal_excellent"
        Signal.Quality.GOOD -> "signal_good"
        Signal.Quality.FAIR -> "signal_fair"
        Signal.Quality.WEAK -> "signal_weak"
    }

    /** Bunun altındaki bekleme tüketimi raporda anılmaz. */
    private const val STANDBY_NOTE_W = 3.0
}
