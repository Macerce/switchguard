package com.macerce.switchguard.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EnergyMath
import com.macerce.switchguard.core.PowerSample
import com.macerce.switchguard.core.StateSample
import com.macerce.switchguard.core.Usage
import com.macerce.switchguard.core.UsageMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Güç ölçümleri ve kanal durum geçişleri (SQLite). Enerji (kWh) ve günlük özet bundan hesaplanır.
 * Yalnızca izleme açıkken kayıt yapılır; [KEEP_DAYS] günden eski kayıtlar silinir.
 */
class MetricsLog private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "metrics.db", null, 1) {

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    /** cihaz → son yazılan ölçüm; gereksiz satır yazmamak için. */
    private val lastPower = mutableMapOf<String, PowerSample>()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE power(ts INTEGER NOT NULL, device_id TEXT NOT NULL, watts REAL NOT NULL)")
        db.execSQL("CREATE INDEX idx_power ON power(device_id, ts)")
        db.execSQL("CREATE TABLE state(ts INTEGER NOT NULL, device_id TEXT NOT NULL, channel INTEGER NOT NULL, is_on INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX idx_state ON state(device_id, channel, ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /**
     * Yeni durumları kaydeder: değişen kanallar ve güç ölçümleri.
     * Güç, değer belirgin değiştiğinde ya da en geç [POWER_EVERY_MS]'de bir yazılır.
     */
    @Synchronized
    fun record(previous: Map<String, DeviceSnapshot>, current: List<DeviceSnapshot>, now: Long = System.currentTimeMillis()) {
        var changed = false
        writableDatabase.apply {
            beginTransaction()
            try {
                for (d in current) {
                    val before = previous[d.id]
                    for ((ch, on) in d.switches) {
                        if (before?.switches?.get(ch) == on) continue
                        insert("state", null, ContentValues().apply {
                            put("ts", now); put("device_id", d.id); put("channel", ch); put("is_on", if (on) 1 else 0)
                        })
                        changed = true
                    }
                    // Çevrimdışı cihazın son değeri bayattır; 0 W yazılır ki enerji birikmesin.
                    val watts = if (d.online) d.power else if (d.hasEnergy) 0.0 else null
                    if (watts != null && shouldWritePower(d.id, watts, now)) {
                        insert("power", null, ContentValues().apply {
                            put("ts", now); put("device_id", d.id); put("watts", watts)
                        })
                        lastPower[d.id] = PowerSample(now, watts)
                        changed = true
                    }
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
        if (changed) _version.value++
    }

    private fun shouldWritePower(id: String, watts: Double, now: Long): Boolean {
        val last = lastPower[id] ?: return true
        if (now - last.time >= POWER_EVERY_MS) return true
        val diff = kotlin.math.abs(watts - last.watts)
        return now - last.time >= 5_000 && diff >= maxOf(5.0, last.watts * 0.05)
    }

    fun powerSamples(deviceId: String, from: Long, to: Long): List<PowerSample> {
        // Aralıktan hemen önceki ölçüm de gerekir: başlangıçtaki güç onunla bilinir.
        val before = readableDatabase.rawQuery(
            "SELECT ts, watts FROM power WHERE device_id=? AND ts<? ORDER BY ts DESC LIMIT 1",
            arrayOf(deviceId, from.toString()),
        ).use { c -> if (c.moveToFirst()) PowerSample(c.getLong(0), c.getDouble(1)) else null }
        val list = readableDatabase.rawQuery(
            "SELECT ts, watts FROM power WHERE device_id=? AND ts>=? AND ts<? ORDER BY ts",
            arrayOf(deviceId, from.toString(), to.toString()),
        ).use { c -> buildList { while (c.moveToNext()) add(PowerSample(c.getLong(0), c.getDouble(1))) } }
        return listOfNotNull(before) + list
    }

    fun kwh(deviceId: String, from: Long, to: Long): Double = EnergyMath.kwh(powerSamples(deviceId, from, to), from, to)

    fun usage(deviceId: String, channel: Int, from: Long, to: Long): Usage {
        val args = arrayOf(deviceId, channel.toString(), from.toString())
        val initial = readableDatabase.rawQuery(
            "SELECT is_on FROM state WHERE device_id=? AND channel=? AND ts<? ORDER BY ts DESC LIMIT 1", args,
        ).use { c -> if (c.moveToFirst()) c.getInt(0) == 1 else null }
        val samples = readableDatabase.rawQuery(
            "SELECT ts, is_on FROM state WHERE device_id=? AND channel=? AND ts>=? AND ts<? ORDER BY ts",
            args + to.toString(),
        ).use { c -> buildList { while (c.moveToNext()) add(StateSample(c.getLong(0), c.getInt(1) == 1)) } }
        return UsageMath.usage(initial, samples, from, to)
    }

    /** Kayıt var mı (hiç ölçüm yoksa "veri yok" gösterilir). */
    fun hasPowerData(deviceId: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM power WHERE device_id=? LIMIT 1", arrayOf(deviceId),
    ).use { it.moveToFirst() }

    fun prune(now: Long = System.currentTimeMillis()) {
        val cutoff = (now - KEEP_DAYS * 86_400_000L).toString()
        writableDatabase.apply {
            delete("power", "ts<?", arrayOf(cutoff))
            delete("state", "ts<?", arrayOf(cutoff))
        }
    }

    fun clear() {
        writableDatabase.apply { delete("power", null, null); delete("state", null, null) }
        lastPower.clear()
        _version.value++
    }

    companion object {
        const val POWER_EVERY_MS = 60_000L
        const val KEEP_DAYS = 400

        @Volatile private var instance: MetricsLog? = null
        fun get(context: Context): MetricsLog =
            instance ?: synchronized(this) { instance ?: MetricsLog(context).also { instance = it } }
    }
}
