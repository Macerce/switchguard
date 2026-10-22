package com.macerce.switchguard.core

import java.util.TimeZone

/**
 * Elektrik tarifesi: tek fiyat ya da üç zamanlı (Türkiye: gündüz 06–17, puant 17–22, gece 22–06).
 * Maliyet ölçümlerden, her dakikanın kendi zaman dilimi fiyatıyla hesaplanır.
 */
sealed class Tariff {
    abstract val hasPrice: Boolean

    /** [from, to) aralığında fiyatın zamana göre toplamı (fiyat × ms). */
    abstract fun priceMs(from: Long, to: Long, zone: TimeZone): Double

    data class Flat(val price: Double) : Tariff() {
        override val hasPrice get() = price > 0
        override fun priceMs(from: Long, to: Long, zone: TimeZone) = price * (to - from)
    }

    data class ThreeZone(val day: Double, val peak: Double, val night: Double) : Tariff() {
        override val hasPrice get() = day > 0 || peak > 0 || night > 0

        fun priceAt(minuteOfDay: Int): Double = when {
            minuteOfDay < DAY_START -> night
            minuteOfDay < PEAK_START -> day
            minuteOfDay < NIGHT_START -> peak
            else -> night
        }

        override fun priceMs(from: Long, to: Long, zone: TimeZone): Double {
            var total = 0.0
            var t = from
            while (t < to) {
                val offset = zone.getOffset(t)
                val msOfDay = Math.floorMod(t + offset, DAY_MS)
                val minute = (msOfDay / 60_000).toInt()
                val boundary = BOUNDARIES.first { it > minute } * 60_000L
                val next = minOf(to, t + (boundary - msOfDay))
                total += priceAt(minute) * (next - t)
                t = next
            }
            return total
        }
    }

    /**
     * [from, to) aralığının maliyeti. Her ölçüm bir sonrakine kadar (en çok [maxGapMs]) sabit sayılır,
     * [EnergyMath.kwh] ile aynı kural. [samples] sıralı; aralıktan önceki son ölçüm de verilmeli.
     */
    fun cost(samples: List<PowerSample>, from: Long, to: Long, zone: TimeZone = TimeZone.getDefault(), maxGapMs: Long = EnergyMath.MAX_GAP_MS): Double {
        if (!hasPrice || to <= from) return 0.0
        var i = lowerBound(samples, from) - 1
        if (i < 0) i = 0
        var wattPriceMs = 0.0
        while (i < samples.size && samples[i].time < to) {
            val s = samples[i]
            val end = minOf(samples.getOrNull(i + 1)?.time ?: to, s.time + maxGapMs, to)
            val start = maxOf(s.time, from)
            if (end > start && s.watts > 0) wattPriceMs += s.watts * priceMs(start, end, zone)
            i++
        }
        return wattPriceMs / 3_600_000.0 / 1000.0
    }

    companion object {
        const val DAY_START = 6 * 60
        const val PEAK_START = 17 * 60
        const val NIGHT_START = 22 * 60
        private val BOUNDARIES = listOf(DAY_START, PEAK_START, NIGHT_START, 24 * 60)
        private const val DAY_MS = 86_400_000L

        private fun lowerBound(samples: List<PowerSample>, t: Long): Int {
            var lo = 0
            var hi = samples.size
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (samples[mid].time < t) lo = mid + 1 else hi = mid
            }
            return lo
        }
    }
}
