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
