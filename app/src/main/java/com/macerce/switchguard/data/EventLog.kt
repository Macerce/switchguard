package com.macerce.switchguard.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
)

/** Olay geçmişi (SQLite). En fazla [MAX_ROWS] kayıt tutulur. */
class EventLog private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "events.db", null, 1) {

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE events(
                 _id INTEGER PRIMARY KEY AUTOINCREMENT,
                 ts INTEGER NOT NULL, device_id TEXT NOT NULL, device_name TEXT NOT NULL,
                 kind TEXT NOT NULL, text TEXT NOT NULL, action TEXT NOT NULL)"""
        )
        db.execSQL("CREATE INDEX idx_events_ts ON events(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun add(deviceId: String, deviceName: String, kind: String, text: String, action: String, time: Long = System.currentTimeMillis()) {
        writableDatabase.apply {
            insert("events", null, ContentValues().apply {
                put("ts", time); put("device_id", deviceId); put("device_name", deviceName)
                put("kind", kind); put("text", text); put("action", action)
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
                    action = c.getString(6),
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

        @Volatile private var instance: EventLog? = null
        fun get(context: Context): EventLog =
            instance ?: synchronized(this) { instance ?: EventLog(context).also { instance = it } }
    }
}
