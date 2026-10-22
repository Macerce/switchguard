package com.macerce.switchguard.data

import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EnergyBucket
import com.macerce.switchguard.core.EnergyMath
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.EnergySummary
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.Standby
import com.macerce.switchguard.core.Tariff
import com.macerce.switchguard.core.PowerSample
import com.macerce.switchguard.core.StateSample
import com.macerce.switchguard.core.Usage
import com.macerce.switchguard.core.UsageMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * Güç ölçümleri ve kanal durum geçişleri (SQLite, [file]). Enerji (kWh) ve günlük özet bundan hesaplanır.
 * Yalnızca izleme açıkken kayıt yapılır; [KEEP_DAYS] günden eski kayıtlar silinir.
 * Şema telefondakiyle (sürüm 1) aynıdır. Dosya başına tek bağlantı açılır; yöntemler birbirini bekletir.
 * Kullanım bitince [close] çağrılır.
 */
class MetricsLog(file: Path) : AutoCloseable {

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    /** cihaz → son yazılan ölçüm; gereksiz satır yazmamak için. */
    private val lastPower = mutableMapOf<String, PowerSample>()

    private val db: Connection = open(file)

    /**
     * Yeni durumları kaydeder: değişen kanallar ve güç ölçümleri.
     * Güç, değer belirgin değiştiğinde ya da en geç [POWER_EVERY_MS]'de bir yazılır.
     */
    @Synchronized
    fun record(previous: Map<String, DeviceSnapshot>, current: List<DeviceSnapshot>, now: Long = System.currentTimeMillis()) {
        var changed = false
        db.autoCommit = false
        try {
            db.prepareStatement("INSERT INTO state(ts, device_id, channel, is_on) VALUES (?, ?, ?, ?)").use { state ->
                db.prepareStatement("INSERT INTO power(ts, device_id, watts) VALUES (?, ?, ?)").use { power ->
                    for (d in current) {
                        val before = previous[d.id]
                        for ((ch, on) in d.switches) {
                            if (before?.switches?.get(ch) == on) continue
                            state.setLong(1, now); state.setString(2, d.id); state.setInt(3, ch); state.setInt(4, if (on) 1 else 0)
                            state.executeUpdate()
                            changed = true
                        }
                        // Çevrimdışı cihazın son değeri bayattır; 0 W yazılır ki enerji birikmesin.
                        val watts = if (d.online) d.power else if (d.hasEnergy) 0.0 else null
                        if (watts != null && shouldWritePower(d.id, watts, now)) {
                            power.setLong(1, now); power.setString(2, d.id); power.setDouble(3, watts)
                            power.executeUpdate()
                            lastPower[d.id] = PowerSample(now, watts)
                            changed = true
                        }
                    }
                }
            }
            db.commit()
        } catch (e: Throwable) {
            runCatching { db.rollback() }
            throw e
        } finally {
            db.autoCommit = true
        }
        if (changed) _version.value++
    }

    private fun shouldWritePower(id: String, watts: Double, now: Long): Boolean {
        val last = lastPower[id] ?: return true
        if (now - last.time >= POWER_EVERY_MS) return true
        val diff = kotlin.math.abs(watts - last.watts)
        return now - last.time >= 5_000 && diff >= maxOf(5.0, last.watts * 0.05)
    }

    @Synchronized
    fun powerSamples(deviceId: String, from: Long, to: Long): List<PowerSample> {
        // Aralıktan hemen önceki ölçüm de gerekir: başlangıçtaki güç onunla bilinir.
        val before = db.prepareStatement("SELECT ts, watts FROM power WHERE device_id=? AND ts<? ORDER BY ts DESC LIMIT 1").use { ps ->
            ps.setString(1, deviceId); ps.setLong(2, from)
            ps.executeQuery().use { rs -> if (rs.next()) PowerSample(rs.getLong(1), rs.getDouble(2)) else null }
        }
        val list = db.prepareStatement("SELECT ts, watts FROM power WHERE device_id=? AND ts>=? AND ts<? ORDER BY ts").use { ps ->
            ps.setString(1, deviceId); ps.setLong(2, from); ps.setLong(3, to)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(PowerSample(rs.getLong(1), rs.getDouble(2))) } }
        }
        return listOfNotNull(before) + list
    }

    @Synchronized
    fun kwh(deviceId: String, from: Long, to: Long): Double = EnergyMath.kwh(powerSamples(deviceId, from, to), from, to)

    /** [anchor] anını içeren gün/ay/yıl için dilim dilim tüketim raporu. */
    fun summary(
        deviceId: String, period: EnergyPeriod, anchor: Long, now: Long = System.currentTimeMillis(), tariff: Tariff? = null,
    ): EnergySummary {
        val (from, to) = EnergyReport.range(period, anchor)
        return EnergyReport.summarize(period, anchor, powerSamples(deviceId, from, minOf(to, now)), now, tariff = tariff)
    }

    /** [from, to) aralığının tahmini maliyeti. */
    fun cost(deviceId: String, from: Long, to: Long, tariff: Tariff): Double =
        if (!tariff.hasPrice) 0.0 else tariff.cost(powerSamples(deviceId, from, to), from, to)

    /** En eski ölçümün zamanı (raporda geriye gitme sınırı); kayıt yoksa null. */
    @Synchronized
    fun firstPowerTime(deviceId: String): Long? =
        db.prepareStatement("SELECT MIN(ts) FROM power WHERE device_id=?").use { ps ->
            ps.setString(1, deviceId)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1).takeUnless { rs.wasNull() } else null }
        }

    /** Son [days] günün saatlik dilimleri (bekleme tüketimi için). */
    fun hourly(deviceId: String, now: Long = System.currentTimeMillis(), days: Int = 7): List<EnergyBucket> =
        (1..days).flatMap { d -> summary(deviceId, EnergyPeriod.DAY, EnergyWatch.dayStart(now, -d), now).buckets }

    fun standby(deviceId: String, now: Long = System.currentTimeMillis()) = Standby.baseline(hourly(deviceId, now))

    /** Aralıktaki güç ölçümü sayısı: izlemenin o gün ne kadar açık olduğunun ölçüsü (bkz. EnergyWatch.MIN_SAMPLES). */
    @Synchronized
    fun sampleCount(deviceId: String, from: Long, to: Long): Int =
        db.prepareStatement("SELECT COUNT(*) FROM power WHERE device_id=? AND ts>=? AND ts<?").use { ps ->
            ps.setString(1, deviceId); ps.setLong(2, from); ps.setLong(3, to)
            ps.executeQuery().use { if (it.next()) it.getInt(1) else 0 }
        }

    @Synchronized
    fun usage(deviceId: String, channel: Int, from: Long, to: Long): Usage {
        val initial = db.prepareStatement("SELECT is_on FROM state WHERE device_id=? AND channel=? AND ts<? ORDER BY ts DESC LIMIT 1").use { ps ->
            ps.setString(1, deviceId); ps.setInt(2, channel); ps.setLong(3, from)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) == 1 else null }
        }
        val samples = db.prepareStatement("SELECT ts, is_on FROM state WHERE device_id=? AND channel=? AND ts>=? AND ts<? ORDER BY ts").use { ps ->
            ps.setString(1, deviceId); ps.setInt(2, channel); ps.setLong(3, from); ps.setLong(4, to)
            ps.executeQuery().use { rs -> buildList { while (rs.next()) add(StateSample(rs.getLong(1), rs.getInt(2) == 1)) } }
        }
        return UsageMath.usage(initial, samples, from, to)
    }

    /** Kayıt var mı (hiç ölçüm yoksa "veri yok" gösterilir). */
    @Synchronized
    fun hasPowerData(deviceId: String): Boolean =
        db.prepareStatement("SELECT 1 FROM power WHERE device_id=? LIMIT 1").use { ps ->
            ps.setString(1, deviceId)
            ps.executeQuery().use { it.next() }
        }

    @Synchronized
    fun prune(now: Long = System.currentTimeMillis()) {
        val cutoff = now - KEEP_DAYS * 86_400_000L
        for (table in listOf("power", "state")) {
            db.prepareStatement("DELETE FROM $table WHERE ts<?").use { ps ->
                ps.setLong(1, cutoff)
                ps.executeUpdate()
            }
        }
    }

    @Synchronized
    fun clear() {
        db.createStatement().use { st -> st.executeUpdate("DELETE FROM power"); st.executeUpdate("DELETE FROM state") }
        lastPower.clear()
        _version.value++
    }

    /** Bağlantıyı kapatır (Windows açık dosyayı silemez/taşıyamaz); sonrasında başka yöntem çağrılmaz. */
    @Synchronized
    override fun close() = db.close()

    companion object {
        const val POWER_EVERY_MS = 60_000L
        const val KEEP_DAYS = 400
        private const val SCHEMA_VERSION = 1

        private fun open(file: Path): Connection {
            val abs = file.toAbsolutePath()
            Files.createDirectories(abs.parent)
            val db = DriverManager.getConnection("jdbc:sqlite:$abs")
            try {
                db.createStatement().use { st ->
                    st.execute("PRAGMA journal_mode=WAL")
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS power(ts INTEGER NOT NULL, device_id TEXT NOT NULL, watts REAL NOT NULL)")
                    st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_power ON power(device_id, ts)")
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS state(ts INTEGER NOT NULL, device_id TEXT NOT NULL, channel INTEGER NOT NULL, is_on INTEGER NOT NULL)")
                    st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_state ON state(device_id, channel, ts)")
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
