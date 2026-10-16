package com.macerce.switchguard.data

import com.macerce.switchguard.desktop.Log
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * İzleme motorunun yeniden başlatmalarda korunması gereken iç durumu: telefondaki `data/RuntimeState`'in karşılığı
 * (aynı alan adları). [Store]'dan ayrı dosyada ([file], `runtime.json`) tutulur: Store'a her yazma arayüzü yeniden
 * çizer, bu bilgiler ise sık değişir ve arayüzü ilgilendirmez. Değeri değişmeyen yazmalar diske gitmez.
 *
 * Masaüstü farkları: telefondaki `savedAlarmVolume` (sistem alarm sesi) ve `stoppedWarnedAt` (servis bekçisi) yok;
 * günlük özetin zamanlaması ([summarySentFor], [summaryKey]) burada (telefonda AlarmManager).
 * Dosya okunamaz ya da bozuksa boş başlanır: bu bilgiler kaybolsa da en kötü ihtimalle bir bekleme baştan başlar.
 */
class RuntimeState(private val file: Path) {
    private val lock = Any()
    private val root: JSONObject = load()

    private fun load(): JSONObject = try {
        JSONObject(String(Files.readAllBytes(file), UTF_8))
    } catch (_: NoSuchFileException) {
        JSONObject()
    } catch (e: Exception) {
        Log.w(TAG, "could not read $file ($e); starting empty")
        JSONObject()
    }

    private fun persist() {
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        try {
            Files.createDirectories(file.toAbsolutePath().parent)
            Files.write(tmp, root.toString().toByteArray(UTF_8))
            try {
                Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp, file, REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            Log.w(TAG, "could not write $file: $e")
            runCatching { Files.deleteIfExists(tmp) }
        }
    }

    /** [value] null ise anahtar silinir. Değer aynıysa diske yazılmaz. */
    private fun set(key: String, value: Any?) = synchronized(lock) {
        val old = if (root.has(key)) root.get(key) else null
        // Metin olarak karşılaştır: dosyadan okunan küçük sayı Integer, yazılan Long olabilir.
        if (old?.toString() == value?.toString()) return@synchronized
        if (value == null) root.remove(key) else root.put(key, value)
        persist()
    }

    private fun str(key: String): String? = synchronized(lock) { if (root.has(key)) root.optString(key) else null }
    private fun long(key: String): Long = synchronized(lock) { root.optLong(key, 0) }

    /** Süresi dolmamış "kopma" beklemeleri ([com.macerce.switchguard.core.OfflineGrace.toJson]). */
    var grace: String?
        get() = str("grace")
        set(v) = set("grace", v)

    /** Otomasyon motorunun durumu ([com.macerce.switchguard.core.AutomationEngine.saveState]). */
    var automationState: String?
        get() = str("automation")
        set(v) = set("automation", v)

    /** Hatırlatma bekleyen alarmlar ([com.macerce.switchguard.core.AlarmReminders.toJson]). */
    var reminders: String?
        get() = str("reminders")
        set(v) = set("reminders", v)

    /** Motorun son "canlıyım" işareti (duvar saati, ms); kesinti tespiti için. */
    var lastAliveAt: Long
        get() = long("aliveAt")
        set(v) = set("aliveAt", v)

    /** İzleme kullanıcı tarafından kapatıldı (beklenmedik kapanma değil). */
    var cleanStop: Boolean
        get() = synchronized(lock) { root.optBoolean("cleanStop", true) }
        set(v) = set("cleanStop", v)

    /** Çalan alarmın ilk olayının zamanı (tepki süresi için); 0 = alarm yok. */
    var alarmStartedAt: Long
        get() = long("alarmStartedAt")
        set(v) = set("alarmStartedAt", v)

    /** Çalan alarm yalnızca sessiz test alarmından oluşuyor: ses yok (gerçek bir olay gelince kalkar). */
    var silentAlarm: Boolean
        get() = synchronized(lock) { root.optBoolean("silentAlarm", false) }
        set(v) = set("silentAlarm", v)

    /** Çalan alarmdaki ilk gerçek (test olmayan) olayın zamanı; ikinci kişiye haber verme buna göre. 0 = yok. */
    var realAlarmAt: Long
        get() = long("realAlarmAt")
        set(v) = set("realAlarmAt", v)

    /** Haber verilen alarmın [realAlarmAt] değeri: aynı alarm için ikinci kez gönderilmez. */
    var escalatedFor: Long
        get() = long("escalatedFor")
        set(v) = set("escalatedFor", v)

    /** Masaüstü: gönderilen son günlük özetin zamanı (bkz. `MonitorEngine.checkSummary`). */
    var summarySentFor: Long
        get() = long("summarySentFor")
        set(v) = set("summarySentFor", v)

    /** Masaüstü: özet ayarının son görülen hali ("off" / "on/<dakika>"); değişince o anki özet atlanır. */
    var summaryKey: String?
        get() = str("summaryKey")
        set(v) = set("summaryKey", v)

    /** Kullanıcı izlemeyi kapatınca bekleyen işler de iptal olur. */
    fun clearEngine() = synchronized(lock) {
        val had = listOf("grace", "automation", "reminders").filter { root.has(it) }
        if (had.isEmpty()) return@synchronized
        had.forEach { root.remove(it) }
        persist()
    }

    private companion object {
        const val TAG = "RuntimeState"
    }
}
