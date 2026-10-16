package com.macerce.switchguard.data

import com.macerce.switchguard.core.EventArgs
import com.macerce.switchguard.core.EventStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types

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
    const val SYSTEM = "SYSTEM"        // Uygulamanın kendi durumu (masaüstüne özgü)
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

/**
 * Olay geçmişi (SQLite, [file]). En fazla [MAX_ROWS] kayıt tutulur. Şema telefondakiyle (sürüm 2) aynıdır.
 * Dosya başına tek bağlantı açılır; yöntemler birbirini bekletir. Kullanım bitince [close] çağrılır.
 */
class EventLog(file: Path) : AutoCloseable {

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private val db: Connection = open(file)

    @Synchronized
    fun add(
        deviceId: String, deviceName: String, kind: String, text: String, action: String,
        args: EventArgs? = null, time: Long = System.currentTimeMillis(),
    ) {
        db.prepareStatement("INSERT INTO events(ts, device_id, device_name, kind, text, action, args) VALUES (?, ?, ?, ?, ?, ?, ?)").use {
            it.setLong(1, time); it.setString(2, deviceId); it.setString(3, deviceName)
            it.setString(4, kind); it.setString(5, text); it.setString(6, action)
            val json = args?.toJson()
            if (json != null) it.setString(7, json) else it.setNull(7, Types.VARCHAR)
            it.executeUpdate()
        }
        db.createStatement().use {
            it.executeUpdate("DELETE FROM events WHERE _id NOT IN (SELECT _id FROM events ORDER BY ts DESC LIMIT $MAX_ROWS)")
        }
        _version.value++
    }

    @Synchronized
    fun recent(deviceId: String? = null, limit: Int = 500): List<LoggedEvent> = query(deviceId, null, limit)

    /** Dışa aktarma: [deviceId] (null = hepsi) ve [since] (null = en baştan) için tüm kayıtlar, yeniden eskiye. */
    @Synchronized
    fun export(deviceId: String?, since: Long?): List<LoggedEvent> = query(deviceId, since, MAX_ROWS)

    private fun query(deviceId: String?, since: Long?, limit: Int): List<LoggedEvent> {
        val clauses = listOfNotNull(deviceId?.let { "device_id = ?" }, since?.let { "ts >= ?" })
        val where = if (clauses.isEmpty()) "" else " WHERE " + clauses.joinToString(" AND ")
        db.prepareStatement("SELECT * FROM events$where ORDER BY ts DESC LIMIT ?").use { ps ->
            var i = 1
            if (deviceId != null) ps.setString(i++, deviceId)
            if (since != null) ps.setLong(i++, since)
            ps.setInt(i, limit)
            ps.executeQuery().use { rs ->
                val out = ArrayList<LoggedEvent>()
                while (rs.next()) {
                    out += LoggedEvent(
                        id = rs.getLong("_id"), time = rs.getLong("ts"), deviceId = rs.getString("device_id"),
                        deviceName = rs.getString("device_name"), kind = rs.getString("kind"), text = rs.getString("text"),
                        action = rs.getString("action"), args = rs.getString("args"),
                    )
                }
                return out
            }
        }
    }

    /** En son kaydın kimliği; kayıt yoksa 0. */
    @Synchronized
    fun latestId(): Long =
        db.createStatement().use { st -> st.executeQuery("SELECT COALESCE(MAX(_id), 0) FROM events").use { if (it.next()) it.getLong(1) else 0 } }

    /** [afterId]'den sonra eklenen ve alarm/bildirim üretmiş kayıt sayısı (Geçmiş sekmesindeki rozet). */
    @Synchronized
    fun unseenAlertCount(afterId: Long): Int =
        db.prepareStatement("SELECT COUNT(*) FROM events WHERE _id > ? AND action IN ('ALARM', 'NOTIFY')").use { ps ->
            ps.setLong(1, afterId)
            ps.executeQuery().use { if (it.next()) it.getInt(1) else 0 }
        }

    @Synchronized
    fun clear() {
        db.createStatement().use { it.executeUpdate("DELETE FROM events") }
        _version.value++
    }

    /** Bağlantıyı kapatır (Windows açık dosyayı silemez/taşıyamaz); sonrasında başka yöntem çağrılmaz. */
    @Synchronized
    override fun close() = db.close()

    companion object {
        const val MAX_ROWS = 2000
        private const val SCHEMA_VERSION = 2

        private fun open(file: Path): Connection {
            val abs = file.toAbsolutePath()
            Files.createDirectories(abs.parent)
            val db = DriverManager.getConnection("jdbc:sqlite:$abs")
            try {
                db.createStatement().use { st ->
                    st.execute("PRAGMA journal_mode=WAL")
                    st.executeUpdate(
                        """CREATE TABLE IF NOT EXISTS events(
                             _id INTEGER PRIMARY KEY AUTOINCREMENT,
                             ts INTEGER NOT NULL, device_id TEXT NOT NULL, device_name TEXT NOT NULL,
                             kind TEXT NOT NULL, text TEXT NOT NULL, action TEXT NOT NULL, args TEXT)"""
                    )
                    st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_ts ON events(ts)")
                    if (st.executeQuery("PRAGMA user_version").use { it.next() && it.getInt(1) == 0 }) st.execute("PRAGMA user_version=$SCHEMA_VERSION")
                }
            } catch (e: Throwable) {
                db.close()
                throw e
            }
            return db
        }
    }
}
