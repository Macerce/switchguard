package com.macerce.switchguard.core

import java.util.Calendar
import java.util.TimeZone

/** Enerji raporunun dönemi: gün (saat saat), ay (gün gün), yıl (ay ay). */
enum class EnergyPeriod { DAY, MONTH, YEAR }

/** Bir dilimin (saat, gün ya da ay) tüketimi. */
data class EnergyBucket(
    val start: Long,
    val end: Long,
    val kwh: Double,
    /** Ölçüm olan süre boyunca ortalama güç (W); ölçüm yoksa null. */
    val avgWatts: Double?,
    /** En yüksek ölçüm (W) ve zamanı; ölçüm yoksa null. */
    val peakWatts: Double?,
    val peakAt: Long?,
    /** Ölçümle kapsanan süre (ms): izleme kapalıyken geçen süre buna girmez. */
    val coveredMs: Long,
    /** Tahmini maliyet (tarifeyle, her dakika kendi zaman dilimi fiyatıyla); tarife yoksa 0. */
    val cost: Double = 0.0,
)

/** Bir dönemin raporu: dilimler ve toplamlar. */
data class EnergySummary(
    val period: EnergyPeriod,
    val start: Long,
    val end: Long,
    val buckets: List<EnergyBucket>,
) {
    val kwh get() = buckets.sumOf { it.kwh }
    val cost get() = buckets.sumOf { it.cost }
    val coveredMs get() = buckets.sumOf { it.coveredMs }
    val avgWatts get() = if (coveredMs > 0) kwh * 1000 * 3_600_000.0 / coveredMs else null
    val peak get() = buckets.filter { it.peakWatts != null }.maxByOrNull { it.peakWatts!! }
    /** En çok tüketilen dilim (hiç tüketim yoksa null). */
    val busiest get() = buckets.filter { it.kwh > 0 }.maxByOrNull { it.kwh }
    /** Ölçüm olan dilimlerin ortalaması (gün/ay ortalaması). */
    val avgPerActiveBucket get() = buckets.filter { it.coveredMs > 0 }.takeIf { it.isNotEmpty() }?.let { kwh / it.size }
}

object EnergyReport {

    /** [anchor] anını içeren dönemin [başlangıç, bitiş) aralığı. */
    fun range(period: EnergyPeriod, anchor: Long, zone: TimeZone = TimeZone.getDefault()): Pair<Long, Long> {
        val c = Calendar.getInstance(zone).apply {
            timeInMillis = anchor
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (period != EnergyPeriod.DAY) set(Calendar.DAY_OF_MONTH, 1)
            if (period == EnergyPeriod.YEAR) set(Calendar.MONTH, Calendar.JANUARY)
        }
        val start = c.timeInMillis
        c.add(unit(period), 1)
        return start to c.timeInMillis
    }

    /** Önceki (-1) ya da sonraki (+1) dönemin içindeki bir an. */
    fun shift(period: EnergyPeriod, anchor: Long, by: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val start = range(period, anchor, zone).first
        return Calendar.getInstance(zone).apply { timeInMillis = start; add(unit(period), by) }.timeInMillis
    }

    /** Dönemin dilimleri: günde 24 saat (yaz saati günlerinde 23/25), ayda gün sayısı, yılda 12 ay. */
    fun slices(period: EnergyPeriod, anchor: Long, zone: TimeZone = TimeZone.getDefault()): List<Pair<Long, Long>> {
        val (start, end) = range(period, anchor, zone)
        val step = when (period) {
            EnergyPeriod.DAY -> Calendar.HOUR_OF_DAY
            EnergyPeriod.MONTH -> Calendar.DAY_OF_MONTH
            EnergyPeriod.YEAR -> Calendar.MONTH
        }
        val c = Calendar.getInstance(zone).apply { timeInMillis = start }
        return buildList {
            while (c.timeInMillis < end) {
                val from = c.timeInMillis
                c.add(step, 1)
                add(from to minOf(c.timeInMillis, end))
            }
        }
    }

    /**
     * Ölçümlerden dönem raporu. [samples] zamana göre sıralı; dönemden hemen önceki ölçüm de verilmeli.
     * [now] sonrası (gelecek) hesaba katılmaz. Her ölçüm bir sonrakine kadar, en fazla [maxGapMs] sabit sayılır.
     */
    fun summarize(
        period: EnergyPeriod,
        anchor: Long,
        samples: List<PowerSample>,
        now: Long,
        zone: TimeZone = TimeZone.getDefault(),
        maxGapMs: Long = EnergyMath.MAX_GAP_MS,
        tariff: Tariff? = null,
    ): EnergySummary {
        val (start, end) = range(period, anchor, zone)
        val buckets = slices(period, anchor, zone).map { (from, to) ->
            val until = minOf(to, now)
            bucket(samples, from, until, to, maxGapMs).copy(cost = tariff?.cost(samples, from, until, zone, maxGapMs) ?: 0.0)
        }
        return EnergySummary(period, start, end, buckets)
    }

    private fun bucket(samples: List<PowerSample>, from: Long, until: Long, end: Long, maxGapMs: Long): EnergyBucket {
        if (until <= from) return EnergyBucket(from, end, 0.0, null, null, null, 0)
        // İlk ilgili ölçüm: aralığın başından önceki son ölçüm.
        var i = lowerBound(samples, from) - 1
        if (i < 0) i = 0
        var wattMs = 0.0
        var covered = 0L
        var peak: PowerSample? = null
        while (i < samples.size && samples[i].time < until) {
            val s = samples[i]
            val segEnd = minOf(samples.getOrNull(i + 1)?.time ?: until, s.time + maxGapMs, until)
            val segStart = maxOf(s.time, from)
            if (segEnd > segStart) {
                wattMs += s.watts * (segEnd - segStart)
                covered += segEnd - segStart
                if (peak == null || s.watts > peak.watts) peak = PowerSample(segStart, s.watts)
            }
            i++
        }
        val kwh = wattMs / 3_600_000.0 / 1000.0
        return EnergyBucket(
            from, end, kwh,
            avgWatts = if (covered > 0) wattMs / covered else null,
            peakWatts = peak?.watts, peakAt = peak?.time, coveredMs = covered,
        )
    }

    /** time >= [t] olan ilk ölçümün sırası. */
    private fun lowerBound(samples: List<PowerSample>, t: Long): Int {
        var lo = 0
        var hi = samples.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (samples[mid].time < t) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun unit(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> Calendar.DAY_OF_MONTH
        EnergyPeriod.MONTH -> Calendar.MONTH
        EnergyPeriod.YEAR -> Calendar.YEAR
    }
}
