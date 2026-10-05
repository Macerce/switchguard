package com.macerce.switchguard.core

/** Bir andaki güç ölçümü (W). */
data class PowerSample(val time: Long, val watts: Double)

/** Bir kanalın bir andaki durumu (durum geçmişi için). */
data class StateSample(val time: Long, val on: Boolean)

/** Bir zaman aralığında bir kanalın kullanımı. */
data class Usage(val onCount: Int, val onMillis: Long)

object EnergyMath {
    /** Bu süreden uzun ölçüm boşlukları (izleme kapalı, internet yok) enerjiye katılmaz. */
    const val MAX_GAP_MS = 15 * 60_000L

    /**
     * [from, to) aralığındaki enerji (kWh). Her ölçüm bir sonrakine kadar sabit kabul edilir
     * (en fazla [maxGapMs]). [samples] zamana göre sıralı olmalı; aralıktan hemen önceki ölçüm de
     * verilirse aralığın başı doğru hesaplanır.
     */
    fun kwh(samples: List<PowerSample>, from: Long, to: Long, maxGapMs: Long = MAX_GAP_MS): Double {
        var wattMs = 0.0
        for (i in samples.indices) {
            val s = samples[i]
            val end = minOf(samples.getOrNull(i + 1)?.time ?: to, s.time + maxGapMs, to)
            val start = maxOf(s.time, from)
            if (end > start) wattMs += s.watts * (end - start)
        }
        return wattMs / 3_600_000.0 / 1000.0
    }
}

object UsageMath {
    /**
     * [from, to) aralığında kaç kez açıldığı ve toplam açık kalma süresi.
     * [initial]: aralık başındaki durum (bilinmiyorsa null → kapalı sayılır).
     * [samples] sıralı; aynı durumun tekrarı yeni açılma sayılmaz.
     */
    fun usage(initial: Boolean?, samples: List<StateSample>, from: Long, to: Long): Usage {
        var on = initial == true
        var since = from
        var count = 0
        var total = 0L
        for (s in samples) {
            if (s.time < from || s.time >= to || s.on == on) continue
            if (on) total += s.time - since
            else count++
            on = s.on
            since = s.time
        }
        if (on) total += to - since
        return Usage(count, total)
    }
}
