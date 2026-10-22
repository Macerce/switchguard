package com.macerce.switchguard.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import java.util.Calendar
import java.util.Locale

/** Günlük özet bildiriminin zamanlanması. Kesin olmayan alarm: özel izin gerektirmez, birkaç dk kayabilir. */
object SummaryScheduler {

    fun schedule(context: Context) {
        val store = Store.get(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        if (!store.dailySummaryEnabled) {
            am.cancel(pi)
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(store.dailySummaryMinute), pi)
    }

    /** Bugün [minuteOfDay] geçmediyse bugün, geçtiyse yarın. */
    private fun nextAt(minuteOfDay: Int): Long = Calendar.getInstance().run {
        val now = timeInMillis
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now + 1_000) add(Calendar.DAY_OF_MONTH, 1)
        timeInMillis
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, 2, Intent(context, SummaryReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(receiverContext: Context, intent: Intent) {
        val context = AppLanguage.wrap(receiverContext)
        val result = goAsync()
        Thread {
            try {
                DailySummary.build(context)?.let { lines ->
                    Notifier(context, Store.get(context)).notifySummary(context.getString(R.string.summary_title), lines)
                }
            } finally {
                SummaryScheduler.schedule(context)
                result.finish()
            }
        }.start()
    }
}

object DailySummary {

    /** Bugünün (gece yarısından şimdiye) özeti; kayıt yoksa null. */
    fun build(context: Context, now: Long = System.currentTimeMillis()): List<String>? {
        val store = Store.get(context)
        val metrics = MetricsLog.get(context)
        val from = startOfDay(now)
        val lines = mutableListOf<String>()
        for (d in store.snapshots) {
            val parts = mutableListOf<String>()
            for (ch in d.switches.keys.sorted()) {
                val u = metrics.usage(d.id, ch, from, now)
                if (u.onCount == 0 && u.onMillis == 0L) continue
                val label = if (d.isMultiChannel) context.getString(R.string.channel_short, ch + 1) + " " else ""
                parts += label + context.getString(R.string.summary_usage, u.onCount, duration(context, u.onMillis))
            }
            if (d.hasEnergy) {
                val kwh = metrics.kwh(d.id, from, now)
                if (kwh > 0.0005) parts += formatKwh(kwh)
            }
            if (parts.isNotEmpty()) lines += "${d.name}: ${parts.joinToString(", ")}"
        }
        if (lines.isEmpty()) {
            // Hiç kayıt yoksa izleme bugün hiç çalışmamıştır; boş özet gönderme.
            return if (store.monitoringEnabled) listOf(context.getString(R.string.summary_nothing)) else null
        }
        return lines
    }

    /**
     * Bugünün raporu (önizleme penceresi): göstergeler, cihaz cihaz kullanım tablosu, öne çıkanlar.
     * Hiç kayıt yoksa göstergeler boş kalır ve bunu söyleyen tek bulgu olur.
     */
    fun doc(context: Context, now: Long = System.currentTimeMillis()): com.macerce.switchguard.core.ReportDoc {
        val store = Store.get(context)
        val metrics = MetricsLog.get(context)
        val log = com.macerce.switchguard.data.EventLog.get(context)
        val from = startOfDay(now)
        val locale = context.resources.configuration.locales[0]
        val rows = mutableListOf<List<String>>()
        var longest: Pair<String, Long>? = null
        var totalKwh = 0.0
        var totalCost = 0.0
        for (d in store.snapshots) {
            for (ch in d.switches.keys.sorted()) {
                val u = metrics.usage(d.id, ch, from, now)
                val name = if (d.isMultiChannel) context.getString(R.string.label_channel, d.name, ch + 1) else d.name
                val kwh = if (d.hasEnergy && ch == d.switches.keys.minOrNull()) metrics.kwh(d.id, from, now) else null
                if (kwh != null) {
                    totalKwh += kwh
                    totalCost += metrics.cost(d.id, from, now, store.tariffFor(d.id))
                }
                if (u.onCount == 0 && u.onMillis == 0L && (kwh ?: 0.0) < 0.0005) continue
                rows += listOf(name, u.onCount.toString(), duration(context, u.onMillis), kwh?.let(::formatKwh) ?: "—")
                if (longest == null || u.onMillis > longest!!.second) longest = name to u.onMillis
            }
        }
        val events = log.export(null, from)
        val alarms = events.count { it.kind == com.macerce.switchguard.data.EventKind.DISMISSED || it.kind == com.macerce.switchguard.data.EventKind.TELEGRAM && it.args?.contains("tg_ack") == true }
        val drops = events.count { it.kind == com.macerce.switchguard.core.EventType.WENT_OFFLINE.name }
        val kpis = buildList {
            add(com.macerce.switchguard.core.Kpi(context.getString(R.string.kpi_alarms), alarms.toString(), context.getString(R.string.kpi_alarms_today_caption)))
            add(com.macerce.switchguard.core.Kpi(context.getString(R.string.kpi_drops), drops.toString(), context.getString(R.string.kpi_drops_caption),
                tone = if (drops >= com.macerce.switchguard.core.Signal.FREQUENT_DROPS) com.macerce.switchguard.core.Tone.WARN else com.macerce.switchguard.core.Tone.NEUTRAL))
            if (totalKwh > 0.0005) add(com.macerce.switchguard.core.Kpi(context.getString(R.string.kpi_energy), formatKwh(totalKwh), context.getString(R.string.kpi_energy_today_caption)))
            if (totalCost > 0) add(com.macerce.switchguard.core.Kpi(context.getString(R.string.kpi_cost), com.macerce.switchguard.ui.EnergyFormat.money(totalCost, locale), context.getString(R.string.kpi_cost_today_caption)))
        }
        val findings = buildList {
            if (rows.isEmpty()) add(com.macerce.switchguard.core.Finding(com.macerce.switchguard.core.Severity.INFO, context.getString(R.string.summary_no_data)))
            longest?.takeIf { it.second >= 60_000 }?.let { (n, ms) ->
                add(com.macerce.switchguard.core.Finding(com.macerce.switchguard.core.Severity.INFO, context.getString(R.string.daily_longest, n, duration(context, ms))))
            }
        }
        return com.macerce.switchguard.core.ReportDoc(
            title = context.getString(R.string.summary_title),
            subtitle = android.text.format.DateUtils.formatDateTime(context, now, android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY),
            intro = context.getString(R.string.daily_intro),
            kpis = kpis,
            sections = listOf(
                com.macerce.switchguard.core.ReportSection(
                    context.getString(R.string.daily_devices), context.getString(R.string.daily_devices_explain), findings,
                    rows.takeIf { it.isNotEmpty() }?.let {
                        com.macerce.switchguard.core.ReportTable(
                            listOf(context.getString(R.string.rep_col_device), context.getString(R.string.daily_col_on), context.getString(R.string.daily_col_time), context.getString(R.string.rep_col_kwh)),
                            it, context.getString(R.string.daily_table_note),
                        )
                    },
                ),
            ),
            footnote = context.getString(R.string.report_footnote),
        )
    }

    fun duration(context: Context, ms: Long): String {
        val totalMin = ms / 60_000
        return if (totalMin >= 60) context.getString(R.string.duration_hm, totalMin / 60, totalMin % 60)
        else context.getString(R.string.duration_m, totalMin)
    }

    fun formatKwh(kwh: Double): String =
        if (kwh < 1) String.format(Locale.getDefault(), "%.0f Wh", kwh * 1000)
        else String.format(Locale.getDefault(), "%.2f kWh", kwh)

    fun startOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}
