package com.macerce.switchguard.data

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.format.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.DurationStrings
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventStrings
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import java.util.Locale

/** Olay türleri: EventType adları + uygulamaya özgü türler. */
object EventKind {
    const val USER = "USER"            // Uygulamadan yapılan aç/kapa
    const val SHORT_DROP = "SHORT_DROP" // Bekleme süresinde geri gelen kısa kopma
    const val TEST = "TEST"
    const val AUTOMATION = "AUTOMATION" // Otomasyonun yaptığı işlem
    const val MONITOR_GAP = "MONITOR_GAP" // İzlemenin çalışmadığı aralık
    const val MONITOR_BLIND = "MONITOR_BLIND" // İzleme kör kaldı / yeniden çalışıyor
    const val DISMISSED = "DISMISSED" // Alarm kapatıldı (tepki süresiyle)
    const val REMINDER = "REMINDER" // Alarm kapatıldı ama durum sürüyor
    const val ESCALATION = "ESCALATION" // Alarm kapatılmadı, ikinci kişiye haber verildi
    const val OUTAGE = "OUTAGE" // Elektrik kesintisi başladı / bitti
    const val ENERGY = "ENERGY" // Anormal tüketim, aylık bütçe
    const val TELEGRAM = "TELEGRAM" // Telegram'dan gelen işlem (alarmı üstlenme, aç/kapa)
    const val WATCHDOG = "WATCHDOG" // Bekçi: diğer cihaz sinyal vermiyor / yeniden veriyor
    const val SCENE = "SCENE" // Sahne çalıştırıldı
    const val PRESENCE = "PRESENCE" // Evde / dışarıda değişti, tatil bitti
}

data class LoggedEvent(
    val id: Long,
    val time: Long,
    val deviceId: String,
    val deviceName: String,
    val kind: String,
    val text: String,
    /** ALARM / NOTIFY / IGNORE ya da boş. */
    val action: String,
    /** Dilden bağımsız tarif ([EventArgs] JSON'u); varsa metin gösterilirken seçili dille yeniden yazılır. */
    val args: String? = null,
) {
    /** Seçili dildeki metin; tarif yoksa kaydedildiği andaki metin. */
    fun text(strings: EventStrings): String = EventArgs.parse(args)?.render(deviceName, strings) ?: text
}

/** Olay geçmişi (SQLite). En fazla [MAX_ROWS] kayıt tutulur. */
class EventLog private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "events.db", null, 2) {

    private val appContext = context.applicationContext

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE events(
                 _id INTEGER PRIMARY KEY AUTOINCREMENT,
                 ts INTEGER NOT NULL, device_id TEXT NOT NULL, device_name TEXT NOT NULL,
                 kind TEXT NOT NULL, text TEXT NOT NULL, action TEXT NOT NULL, args TEXT)"""
        )
        db.execSQL("CREATE INDEX idx_events_ts ON events(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE events ADD COLUMN args TEXT")
            // Eski kayıtlar yalnızca metin olarak tutuluyordu: metni her iki dilin kalıplarıyla
            // eşleştirip tarifini çıkar ki geçmiş de seçili dilde gösterilebilsin.
            val languages = listOf("tr", "en").map { strings(localized(appContext, it)) }
            db.rawQuery("SELECT _id, kind, device_name, text FROM events", null).use { c ->
                while (c.moveToNext()) {
                    val args = EventArgs.fromLegacy(c.getString(1), c.getString(2), c.getString(3), languages) ?: continue
                    db.update("events", ContentValues().apply { put("args", args.toJson()) }, "_id = ?", arrayOf(c.getLong(0).toString()))
                }
            }
        }
    }

    fun add(
        deviceId: String, deviceName: String, kind: String, text: String, action: String,
        args: EventArgs? = null, time: Long = System.currentTimeMillis(),
    ) {
        writableDatabase.apply {
            insert("events", null, ContentValues().apply {
                put("ts", time); put("device_id", deviceId); put("device_name", deviceName)
                put("kind", kind); put("text", text); put("action", action); put("args", args?.toJson())
            })
            execSQL("DELETE FROM events WHERE _id NOT IN (SELECT _id FROM events ORDER BY ts DESC LIMIT $MAX_ROWS)")
        }
        _version.value++
    }

    fun recent(deviceId: String? = null, limit: Int = 500): List<LoggedEvent> {
        val where = if (deviceId != null) "device_id = ?" else null
        val args = if (deviceId != null) arrayOf(deviceId) else null
        readableDatabase.query("events", null, where, args, null, null, "ts DESC", limit.toString()).use { c ->
            val out = ArrayList<LoggedEvent>(c.count)
            while (c.moveToNext()) {
                out += LoggedEvent(
                    id = c.getLong(0), time = c.getLong(1), deviceId = c.getString(2),
                    deviceName = c.getString(3), kind = c.getString(4), text = c.getString(5),
                    action = c.getString(6), args = c.getString(7),
                )
            }
            return out
        }
    }

    /** Dışa aktarma: [deviceId] (null = hepsi) ve [since] (null = en baştan) için tüm kayıtlar, yeniden eskiye. */
    fun export(deviceId: String?, since: Long?): List<LoggedEvent> {
        val clauses = listOfNotNull(deviceId?.let { "device_id = ?" }, since?.let { "ts >= ?" })
        val args = listOfNotNull(deviceId, since?.toString()).toTypedArray()
        readableDatabase.query(
            "events", null, clauses.joinToString(" AND ").ifEmpty { null }, args.takeIf { it.isNotEmpty() },
            null, null, "ts DESC", MAX_ROWS.toString(),
        ).use { c ->
            val out = ArrayList<LoggedEvent>(c.count)
            while (c.moveToNext()) {
                out += LoggedEvent(
                    id = c.getLong(0), time = c.getLong(1), deviceId = c.getString(2),
                    deviceName = c.getString(3), kind = c.getString(4), text = c.getString(5),
                    action = c.getString(6), args = c.getString(7),
                )
            }
            return out
        }
    }

    /** [deviceId] için [since]'den beri [kind] türündeki kayıt sayısı (ör. haftalık kopma). */
    fun count(deviceId: String, kind: String, since: Long): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM events WHERE device_id = ? AND kind = ? AND ts >= ?", arrayOf(deviceId, kind, since.toString()),
    ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** En son kaydın kimliği; kayıt yoksa 0. */
    fun latestId(): Long =
        readableDatabase.rawQuery("SELECT COALESCE(MAX(_id), 0) FROM events", null).use { c -> if (c.moveToFirst()) c.getLong(0) else 0 }

    /** [afterId]'den sonra eklenen ve alarm/bildirim üretmiş kayıt sayısı (Geçmiş sekmesindeki rozet). */
    fun unseenAlertCount(afterId: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM events WHERE _id > ? AND action IN ('ALARM', 'NOTIFY')",
            arrayOf(afterId.toString()),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun clear() {
        writableDatabase.delete("events", null, null)
        _version.value++
    }

    companion object {
        const val MAX_ROWS = 2000

        /** [context]'in dilindeki olay kalıpları. */
        fun strings(context: Context) = EventStrings(
            labels = ChangeLabels(
                on = context.getString(R.string.label_on),
                off = context.getString(R.string.label_off),
                online = context.getString(R.string.label_online),
                offline = context.getString(R.string.label_offline),
            ) { name, n -> context.getString(R.string.label_channel, name, n) },
            shortDrop = { context.getString(R.string.event_short_drop, it) },
            test = context.getString(R.string.test_alarm_line),
            automationFired = { context.getString(R.string.automation_fired, it) },
            deviceMissing = context.getString(R.string.device_missing),
            automationFailed = { t, e -> context.getString(R.string.automation_failed, t, e) },
            powerStale = { context.getString(R.string.power_data_stale, it) },
            monitorGap = { from, to ->
                context.getString(R.string.event_monitor_gap, timeRange(context, from, to), DurationText.format(to - from, durations(context)))
            },
            monitorBlind = { context.getString(R.string.event_monitor_blind, DateUtils.formatDateTime(context, it, DateUtils.FORMAT_SHOW_TIME)) },
            monitorRestored = { from, to -> context.getString(R.string.event_monitor_restored, DurationText.format(to - from, durations(context))) },
            reminder = { label, state, ms -> context.getString(R.string.event_reminder, label, state, DurationText.format(ms, durations(context))) },
            escalation = { n, min, err ->
                if (err == null) context.resources.getQuantityString(R.plurals.event_escalated, n, n, min)
                else context.getString(R.string.event_escalation_failed, min, err)
            },
            alarmDismissed = { n, ms ->
                context.resources.getQuantityString(R.plurals.event_alarm_dismissed, n, n, DurationText.format(ms, durations(context)))
            },
            outage = { n -> context.resources.getQuantityString(R.plurals.event_outage, n, n) },
            outageEnded = { from, to ->
                context.getString(R.string.event_outage_ended, DurationText.format(to - from, durations(context)), timeRange(context, from, to))
            },
            energyAnomaly = { name, high, kwh, avg ->
                val pct = kotlin.math.abs(((kwh / avg - 1) * 100).toInt())
                context.getString(
                    if (high) R.string.event_energy_high else R.string.event_energy_low,
                    name, pct, com.macerce.switchguard.service.DailySummary.formatKwh(kwh), com.macerce.switchguard.service.DailySummary.formatKwh(avg),
                )
            },
            budget = { cost, budget ->
                val locale = context.resources.configuration.locales[0]
                context.getString(R.string.event_budget, com.macerce.switchguard.ui.EnergyFormat.money(cost, locale), com.macerce.switchguard.ui.EnergyFormat.money(budget, locale))
            },
            telegramAck = { who, ms -> context.getString(R.string.event_tg_ack, who, DurationText.format(ms, durations(context))) },
            telegramSwitch = { who, label, on, err ->
                val text = context.getString(R.string.event_tg_switch, who, label, context.getString(if (on) R.string.label_on else R.string.label_off))
                if (err != null) context.getString(R.string.automation_failed, text, err) else text
            },
            charger = { lost, ms ->
                if (lost) context.getString(R.string.event_charger_lost)
                else context.getString(R.string.event_charger_back, DurationText.format(ms, durations(context)))
            },
            scene = { name, ok, failed, by ->
                val base = context.resources.getQuantityString(R.plurals.event_scene, ok, name, ok)
                val fail = if (failed > 0) context.resources.getQuantityString(R.plurals.event_scene_failed, failed, failed) else ""
                base + fail + (by?.let { " · $it" } ?: "")
            },
            presence = { away, by, ended ->
                when {
                    ended -> context.getString(R.string.event_vacation_ended)
                    else -> context.getString(if (away) R.string.event_presence_away else R.string.event_presence_home) + (by?.let { " · $it" } ?: "")
                }
            },
            watchdog = { name, lost, ms ->
                context.getString(if (lost) R.string.event_watchdog_lost else R.string.event_watchdog_back, name, DurationText.format(ms, durations(context)))
            },
        )

        /** [context]'in dilindeki süre kalıpları ("2 dk 10 sn" gibi). */
        fun durations(context: Context) = DurationStrings(
            seconds = { context.getString(R.string.dur_seconds, it) },
            minSec = { m, s -> context.getString(R.string.dur_min_sec, m, s) },
            minutes = { context.getString(R.string.dur_minutes, it) },
            hourMin = { h, m -> context.getString(R.string.dur_hour_min, h, m) },
            dayHour = { d, h -> context.getString(R.string.dur_day_hour, d, h) },
        )

        /** "14:02 – 14:37"; farklı günlerdeyse tarihle birlikte. */
        private fun timeRange(context: Context, from: Long, to: Long): String {
            fun day(t: Long) = Calendar.getInstance().run { timeInMillis = t; get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR) }
            val date = if (day(from) == day(to)) 0 else DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
            return DateUtils.formatDateRange(context, from, to, DateUtils.FORMAT_SHOW_TIME or date)
        }

        private fun localized(context: Context, tag: String): Context =
            context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

        @Volatile private var instance: EventLog? = null
        fun get(context: Context): EventLog =
            instance ?: synchronized(this) { instance ?: EventLog(context).also { instance = it } }
    }
}
