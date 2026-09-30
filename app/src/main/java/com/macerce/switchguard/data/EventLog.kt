package com.macerce.switchguard.data

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventStrings
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Olay türleri: EventType adları + uygulamaya özgü türler. */
object EventKind {
    const val USER = "USER"            // Uygulamadan yapılan aç/kapa
    const val SHORT_DROP = "SHORT_DROP" // Bekleme süresinde geri gelen kısa kopma
    const val TEST = "TEST"
    const val AUTOMATION = "AUTOMATION" // Otomasyonun yaptığı işlem
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
        )

        private fun localized(context: Context, tag: String): Context =
            context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

        @Volatile private var instance: EventLog? = null
        fun get(context: Context): EventLog =
            instance ?: synchronized(this) { instance ?: EventLog(context).also { instance = it } }
    }
}
