package com.macerce.switchguard.core

import kotlin.math.abs

/** Bir sensör ölçümü (ör. 23,5 °C, şu anda). */
data class SensorSample(val time: Long, val value: Double)

/** Sensör geçmişinde gösterilen süre (şimdiden geriye). */
enum class SensorRange(val hours: Int, val buckets: Int) {
    DAY(24, 48), WEEK(24 * 7, 56), MONTH(24 * 30, 60);

    val ms get() = hours * 3_600_000L
}

/** Grafikteki bir dilim: dilimdeki ölçümlerin en düşüğü, en yükseği ve ortalaması. */
data class SensorPoint(val start: Long, val min: Double, val max: Double, val avg: Double)

/**
 * Aralığın özeti: en düşük/yüksek (zamanıyla), ortalama ve grafik dilimleri ([points]; ölçüm olmayan dilim null).
 */
data class SensorStats(
    val from: Long,
    val to: Long,
    val min: SensorSample,
    val max: SensorSample,
    val avg: Double,
    val points: List<SensorPoint?>,
)

/**
 * Sayısal sensör değerlerinin (sıcaklık, nem, ışık, pil) geçmişi: ne zaman yazılacağı ve aralık özeti. Güç geçmişi gibi
 * yalnız izleme açıkken kaydedilir; ikili sensörler (kapı, hareket, su) olay geçmişinde zaten var, burada tutulmaz.
 * Telefon ve masaüstünde aynı.
 */
object SensorHistory {
    /** Değer değişmese de en geç bu arayla bir satır yazılır (grafikte boşluk kalmasın). */
    const val EVERY_MS = 10 * 60_000L
    /** Değer değiştiyse bile bundan sık yazılmaz. */
    const val MIN_GAP_MS = 30_000L

    fun recordable(kind: SensorKind) = !kind.binary

    /** Kayda değer değişim: sıcaklık 0,1 °C, nem/pil 1 %, ışık %10 (en az 1 lx). */
    fun step(kind: SensorKind, last: Double): Double = when (kind) {
        SensorKind.TEMPERATURE -> 0.1
        SensorKind.ILLUMINANCE -> maxOf(1.0, abs(last) * 0.1)
        else -> 1.0
    }

    fun shouldWrite(kind: SensorKind, last: SensorSample?, value: Double, now: Long): Boolean {
        last ?: return true
        if (now - last.time >= EVERY_MS) return true
        return now - last.time >= MIN_GAP_MS && abs(value - last.value) >= step(kind, last.value) - 1e-9
    }

    /** [from, to) aralığının özeti; aralıkta ölçüm yoksa null. */
    fun stats(samples: List<SensorSample>, from: Long, to: Long, buckets: Int): SensorStats? {
        val inRange = samples.filter { it.time in from until to }
        if (inRange.isEmpty() || to <= from || buckets <= 0) return null
        val width = (to - from).toDouble() / buckets
        val groups = inRange.groupBy { ((it.time - from) / width).toInt().coerceIn(0, buckets - 1) }
        val points = (0 until buckets).map { i ->
            groups[i]?.let { g -> SensorPoint(from + (i * width).toLong(), g.minOf { it.value }, g.maxOf { it.value }, g.sumOf { it.value } / g.size) }
        }
        return SensorStats(
            from, to,
            min = inRange.minBy { it.value },
            max = inRange.maxBy { it.value },
            avg = inRange.sumOf { it.value } / inRange.size,
            points = points,
        )
    }
}
