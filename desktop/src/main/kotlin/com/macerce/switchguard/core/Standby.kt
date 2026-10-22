package com.macerce.switchguard.core

/**
 * Bekleme (temel) tüketimi: cihazın hiç inmediği en düşük güç. Son günlerin saatlik ortalamalarının en düşüğü alınır;
 * TV/modem/şarj aleti gibi "kapalı" görünen ama akım çeken cihazları ortaya çıkarır. Saf; dilimler dışarıdan verilir.
 */
object Standby {
    /** Değerlendirme için en az bu kadar tam ölçülmüş saat. */
    const val MIN_HOURS = 24
    /** Saatin "ölçülmüş" sayılması için kapsama. */
    const val MIN_COVER_MS = 50 * 60_000L
    /** Bunun altı önemsiz (ölçüm hassasiyeti, prizin kendi tüketimi). */
    const val THRESHOLD_W = 2.0

    data class Result(val watts: Double, val monthlyKwh: Double)

    fun baseline(hourly: List<EnergyBucket>): Result? {
        val valid = hourly.filter { it.coveredMs >= MIN_COVER_MS && it.avgWatts != null }
        if (valid.size < MIN_HOURS) return null
        val w = valid.minOf { it.avgWatts!! }
        if (w < THRESHOLD_W) return null
        return Result(w, monthlyKwh(w))
    }

    /** Sürekli [watts] çeken bir yükün 30 günlük tüketimi. */
    fun monthlyKwh(watts: Double): Double = watts * 24 * 30 / 1000

    /** Aynı yükün 30 günlük maliyeti (üç zamanlı tarifede saat dilimlerine göre). */
    fun monthlyCost(watts: Double, tariff: Tariff, now: Long): Double {
        val from = now - 30 * 86_400_000L
        return tariff.cost(listOf(PowerSample(from, watts)), from, now, maxGapMs = Long.MAX_VALUE / 4)
    }
}
