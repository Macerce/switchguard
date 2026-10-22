package com.macerce.switchguard.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import com.macerce.switchguard.R
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.Compare
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.ReportText
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
import com.macerce.switchguard.ui.EnergyFormat
import java.util.Calendar

/** Haftalık rapor: pazartesi 09:00; tatildeyken her sabah 09:00 günlük. */
object WeeklyScheduler {
    private const val MINUTE = 9 * 60

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        val store = Store.get(context)
        if (!store.weeklyReport && !store.onVacation()) {
            am.cancel(pi)
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(System.currentTimeMillis(), daily = store.onVacation()), pi)
    }

    fun nextAt(now: Long, daily: Boolean = false): Long = Calendar.getInstance().run {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, MINUTE / 60); set(Calendar.MINUTE, MINUTE % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        while ((!daily && get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) || timeInMillis <= now + 1_000) add(Calendar.DAY_OF_MONTH, 1)
        timeInMillis
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 3, Intent(context, WeeklyReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

class WeeklyReceiver : BroadcastReceiver() {
    override fun onReceive(receiverContext: Context, intent: Intent) {
        val context = AppLanguage.wrap(receiverContext)
        val result = goAsync()
        Thread {
            try {
                val store = Store.get(context)
                val vacation = store.onVacation()
                // Tatilde her gün, değilse yalnızca pazartesi (haftalık rapor açıksa).
                val calendar = Calendar.getInstance()
                val monday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                if (vacation || (store.weeklyReport && monday)) {
                    val doc = WeeklyReport.build(context, days = if (vacation) 1 else 7)
                    Notifier(context, store).notifySummary(doc.title, WeeklyReport.notificationLines(doc))
                    if (store.escalationReady && (store.telegramForward || vacation)) WeeklyReport.sendTelegram(context, doc)
                }
            } finally {
                WeeklyScheduler.schedule(context)
                result.finish()
            }
        }.start()
    }
}

object WeeklyReport {
    private const val DAY_MS = 86_400_000L

    /** Bildirimde: göstergeler tek satır, sonra ilk öneri. */
    fun notificationLines(doc: ReportDoc): List<String> =
        doc.kpis.map { k -> "${k.label}: ${k.value}" + (k.trend?.let { " (${it.text})" } ?: "") } + doc.actions.take(1).map { "→ $it" }

    fun sendTelegram(context: Context, doc: ReportDoc) {
        val store = Store.get(context)
        val text = ReportText.telegramHtml(doc, context.getString(R.string.report_actions_title), context.getString(R.string.report_what_to_do_short))
        val token = store.telegramToken
        store.telegramRecipients.forEach { r -> runCatching { TelegramClient.send(token, r.id, text, html = true) } }
    }

    /** Son [days] günün raporu; aynı uzunluktaki önceki dönemle karşılaştırılır. */
    fun build(context: Context, now: Long = System.currentTimeMillis(), days: Int = 7): ReportDoc {
        val store = Store.get(context)
        val log = EventLog.get(context)
        val metrics = MetricsLog.get(context)
        val res = context.resources
        val locale = res.configuration.locales[0]
        val durations = EventLog.durations(context)
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
        val prevLabel = context.getString(if (weekly) R.string.report_vs_last_week else R.string.report_vs_yesterday)

        fun trend(nowV: Double, beforeV: Double, lowerIsBetter: Boolean, format: (Double) -> String): Trend? {
            if (nowV == 0.0 && beforeV == 0.0) return null
            val pct = Compare.change(nowV, beforeV)
            val tone = Compare.tone(pct, lowerIsBetter)
            val text = when {
                pct == null -> context.getString(R.string.report_trend_new, prevLabel, format(beforeV))
                kotlin.math.abs(pct) < Compare.SAME_PERCENT -> context.getString(R.string.report_trend_same, prevLabel)
                pct > 0 -> context.getString(R.string.report_trend_up, prevLabel, pct)
                else -> context.getString(R.string.report_trend_down, prevLabel, -pct)
            }
            return Trend(text, tone)
        }

        // --------------------------------------------------- göstergeler
        val kpis = mutableListOf<Kpi>()
        kpis += Kpi(
            context.getString(R.string.kpi_outage),
            if (week.outages == 0) context.getString(R.string.report_none) else res.getQuantityString(R.plurals.kpi_outage_value, week.outages, week.outages, dur(week.outageMs)),
            context.getString(R.string.kpi_outage_caption),
            trend(week.outages.toDouble(), prev.outages.toDouble(), true) { it.toInt().toString() },
            if (week.outages > 0) Tone.WARN else Tone.GOOD,
        )
        kpis += Kpi(
            context.getString(R.string.kpi_alarms),
            week.alarms.toString(),
            context.getString(R.string.kpi_alarms_caption),
            trend(week.alarms.toDouble(), prev.alarms.toDouble(), true) { it.toInt().toString() },
        )
        val avg = week.avgResponseMs
        kpis += Kpi(
            context.getString(R.string.kpi_response),
            avg?.let(::dur) ?: "—",
            context.getString(R.string.kpi_response_caption),
            if (avg != null && prev.avgResponseMs != null) trend(avg.toDouble(), prev.avgResponseMs.toDouble(), true) { dur(it.toLong()) } else null,
            when { avg == null -> Tone.NEUTRAL; avg > 10 * 60_000L -> Tone.WARN; else -> Tone.GOOD },
        )
        val uptime = ((1 - week.gapMs.toDouble() / period) * 100).coerceIn(0.0, 100.0)
        kpis += Kpi(
            context.getString(R.string.kpi_uptime),
            context.getString(R.string.volume_pct, uptime.toInt()),
            context.getString(R.string.kpi_uptime_caption),
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
                context.getString(R.string.kpi_energy),
                DailySummary.formatKwh(totalKwh),
                context.getString(R.string.kpi_energy_caption),
                trend(totalKwh, totalPrev, true) { DailySummary.formatKwh(it) },
            )
            if (totalCost > 0) {
                kpis += Kpi(
                    context.getString(R.string.kpi_cost),
                    EnergyFormat.money(totalCost, locale),
                    context.getString(if (weekly) R.string.kpi_cost_caption_week else R.string.kpi_cost_caption_day,
                        EnergyFormat.money(totalCost * 30.0 / days, locale)),
                )
            }
        }

        // --------------------------------------------------- bölümler
        val sections = mutableListOf<ReportSection>()
        val fmtTime = { t: Long -> DateUtils.formatDateTime(context, t, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_TIME) }

        // Kesintiler ve alarmlar
        val events = mutableListOf<Finding>()
        current.mapNotNull { e -> (EventArgs.parse(e.args) as? EventArgs.OutageEnded)?.let { e to it } }.take(5).forEach { (_, a) ->
            events += Finding(Severity.WARN, context.getString(R.string.rep_outage_item, fmtTime(a.from), dur(a.to - a.from)))
        }
        if (week.outages == 0) events += Finding(Severity.GOOD, context.getString(R.string.rep_no_outage))
        if (month.outages > 0) events += Finding(Severity.INFO, res.getQuantityString(R.plurals.rep_month_outages, month.outages, month.outages, dur(month.outageMs)))
        when {
            week.alarms == 0 -> events += Finding(Severity.GOOD, context.getString(R.string.rep_no_alarms))
            avg != null && avg > 10 * 60_000L -> events += Finding(
                Severity.WARN, context.getString(R.string.rep_slow_response, dur(avg)),
                context.getString(if (store.escalationReady) R.string.act_check_sound else R.string.act_add_second_person),
            )
            avg != null -> events += Finding(Severity.GOOD, context.getString(R.string.rep_good_response, dur(avg)))
        }
        if (week.escalations > 0) events += Finding(Severity.INFO, res.getQuantityString(R.plurals.rep_escalations, week.escalations, week.escalations))
        sections += ReportSection(context.getString(R.string.rep_sec_events), context.getString(R.string.rep_sec_events_explain), events)

        // Bağlantı
        val conn = mutableListOf<Finding>()
        val drops = week.drops.toMap()
        val connRows = store.snapshots.filter { it.cloud != com.macerce.switchguard.core.Cloud.DEMO }.map { d ->
            val n = drops[d.name] ?: 0
            val signal = d.rssi?.let { context.getString(R.string.rep_signal_cell, context.getString(qualityLabel(Signal.quality(it))), it) } ?: "—"
            when (Signal.dropCause(n, d.rssi)) {
                Signal.Cause.WEAK_SIGNAL -> conn += Finding(Severity.WARN, context.getString(R.string.rep_drops_signal, d.name, n, d.rssi ?: 0), context.getString(R.string.act_move_closer))
                Signal.Cause.POWER_OR_ROUTER -> conn += Finding(Severity.WARN, context.getString(R.string.rep_drops_power, d.name, n), context.getString(R.string.act_check_power))
                Signal.Cause.UNKNOWN -> conn += Finding(Severity.WARN, context.getString(R.string.rep_drops_unknown, d.name, n), context.getString(R.string.act_check_wifi))
                null -> if (d.rssi != null && Signal.quality(d.rssi) == Signal.Quality.WEAK) {
                    conn += Finding(Severity.INFO, context.getString(R.string.rep_weak_no_drops, d.name, d.rssi), context.getString(R.string.act_move_closer))
                }
            }
            listOf(d.name, n.toString(), signal)
        }.sortedByDescending { it[1].toInt() }
        if (conn.isEmpty() && connRows.isNotEmpty()) conn += Finding(Severity.GOOD, context.getString(R.string.rep_conn_ok))
        if (connRows.isNotEmpty()) {
            sections += ReportSection(
                context.getString(R.string.rep_sec_conn), context.getString(R.string.rep_sec_conn_explain), conn,
                ReportTable(
                    listOf(context.getString(R.string.rep_col_device), context.getString(R.string.rep_col_drops), context.getString(R.string.rep_col_signal)),
                    connRows.take(20), context.getString(R.string.rep_conn_note),
                ),
            )
        }

        // Tüketim
        if (uses.isNotEmpty()) {
            val energy = mutableListOf<Finding>()
            current.filter { it.kind == EventKind.ENERGY }.mapNotNull { e -> (EventArgs.parse(e.args) as? EventArgs.EnergyAnomaly)?.let { e to it } }.take(5).forEach { (e, a) ->
                energy += Finding(
                    if (a.high) Severity.WARN else Severity.ALERT, e.text(EventLog.strings(context)),
                    context.getString(if (a.high) R.string.act_check_high else R.string.act_check_low, e.deviceName),
                )
            }
            uses.forEach { u ->
                runCatching { metrics.standby(u.id, now) }.getOrNull()?.let { s ->
                    val cost = store.tariffFor(u.id).takeIf { it.hasPrice }?.let { Standby.monthlyCost(s.watts, it, now) } ?: 0.0
                    if (s.watts >= STANDBY_NOTE_W) energy += Finding(
                        Severity.INFO,
                        context.getString(R.string.rep_standby, u.name, com.macerce.switchguard.ui.formatWatts(s.watts), DailySummary.formatKwh(s.monthlyKwh)) +
                            (if (cost > 0) " (≈ ${EnergyFormat.money(cost, locale)})" else ""),
                        context.getString(R.string.act_standby),
                    )
                }
            }
            val top = uses.maxBy { it.kwh }
            if (totalKwh > 0 && uses.size > 1) energy += Finding(Severity.INFO, context.getString(R.string.rep_top_consumer, top.name, (top.kwh * 100 / totalKwh).toInt()))
            val withCost = totalCost > 0
            val headers = listOfNotNull(
                context.getString(R.string.rep_col_device), context.getString(R.string.rep_col_kwh), context.getString(R.string.rep_col_share),
                context.getString(R.string.rep_col_cost).takeIf { withCost },
                context.getString(if (weekly) R.string.rep_col_last_week else R.string.rep_col_yesterday),
            )
            val rows = uses.sortedByDescending { it.kwh }.map { u ->
                listOfNotNull(
                    u.name, DailySummary.formatKwh(u.kwh),
                    if (totalKwh > 0) context.getString(R.string.volume_pct, (u.kwh * 100 / totalKwh).toInt()) else "—",
                    EnergyFormat.money(u.cost, locale).takeIf { withCost },
                    DailySummary.formatKwh(u.prevKwh),
                )
            }
            sections += ReportSection(
                context.getString(R.string.rep_sec_energy), context.getString(R.string.rep_sec_energy_explain), energy,
                ReportTable(headers, rows, context.getString(R.string.rep_energy_note)),
            )
        }

        // İzleme
        val watch = mutableListOf<Finding>()
        if (week.gapMs >= 60_000) watch += Finding(Severity.WARN, context.getString(R.string.rep_gap, dur(week.gapMs)), context.getString(R.string.act_battery))
        else watch += Finding(Severity.GOOD, context.getString(R.string.rep_no_gap))
        sections += ReportSection(context.getString(R.string.rep_sec_monitor), context.getString(R.string.rep_sec_monitor_explain), watch)

        val range = DateUtils.formatDateRange(context, from, now, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
        return ReportDoc(
            title = context.getString(if (weekly) R.string.weekly_title else R.string.daily_report_title),
            subtitle = context.getString(R.string.report_subtitle, range, store.snapshots.size),
            intro = context.getString(if (weekly) R.string.weekly_intro else R.string.daily_report_intro),
            kpis = kpis,
            sections = sections,
            glossary = glossary(context),
            footnote = context.getString(R.string.report_footnote),
        )
    }

    /** Raporlarda geçen terimler. */
    fun glossary(context: Context): List<Pair<String, String>> = listOf(
        R.string.gl_kwh to R.string.gl_kwh_def,
        R.string.gl_w to R.string.gl_w_def,
        R.string.gl_outage to R.string.gl_outage_def,
        R.string.gl_response to R.string.gl_response_def,
        R.string.gl_uptime to R.string.gl_uptime_def,
        R.string.gl_dbm to R.string.gl_dbm_def,
        R.string.gl_standby to R.string.gl_standby_def,
    ).map { (k, v) -> context.getString(k) to context.getString(v) }

    fun qualityLabel(q: Signal.Quality): Int = when (q) {
        Signal.Quality.EXCELLENT -> R.string.signal_excellent
        Signal.Quality.GOOD -> R.string.signal_good
        Signal.Quality.FAIR -> R.string.signal_fair
        Signal.Quality.WEAK -> R.string.signal_weak
    }

    /** Bunun altındaki bekleme tüketimi raporda anılmaz. */
    private const val STANDBY_NOTE_W = 3.0
}
