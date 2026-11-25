package com.macerce.switchguard.core

/**
 * Tüm evin enerji panosu: enerji ölçen cihazların aynı dönemdeki raporları dilim dilim toplanır.
 * Ana sayaç (rölesiz ölçer: Shelly EM / Pro 3EM, PM Mini) varsa evin toplamı odur; prizlerin toplamı ondan
 * ayrı tutulur (iki kez sayılmasın) ve aradaki fark "ölçülmeyen yükler" olarak gösterilir.
 */
data class HomeEnergy(
    /** Ölçen cihazların (sayaçlar hariç) birleşik raporu. */
    val consumers: EnergySummary,
    /** Ana sayaçların birleşik raporu; sayaç yoksa null. */
    val meter: EnergySummary?,
    /** Cihaz payları, çoktan aza (yalnız tüketimi olanlar). */
    val shares: List<Share>,
) {
    data class Share(val id: String, val kwh: Double, val cost: Double, val share: Double)

    /** Evin toplamı: sayaç varsa sayaç, yoksa cihazların toplamı. */
    val total: EnergySummary get() = meter ?: consumers

    /** Sayacın ölçüp prizlerin ölçmediği tüketim (sayaç yoksa null). */
    val untrackedKwh: Double? get() = meter?.let { (it.kwh - consumers.kwh).coerceAtLeast(0.0) }

    companion object {
        /** Ana sayaç sayılan cihaz: rölesi yok, yalnız güç ölçüyor. */
        fun isMeter(d: DeviceSnapshot): Boolean = d.switches.isEmpty() && d.sensors.isEmpty() && d.power != null

        /**
         * Aynı dönemin raporlarından pano. [parts]: cihaz kimliği → rapor; [meters]: sayaç cihazların kimlikleri.
         * Raporların hepsi aynı [period] ve başlangıçla üretilmiş olmalı (dilim sayıları eşit).
         */
        fun build(period: EnergyPeriod, start: Long, end: Long, parts: Map<String, EnergySummary>, meters: Set<String>): HomeEnergy {
            val consumerParts = parts.filterKeys { it !in meters }
            val meterParts = parts.filterKeys { it in meters }
            val consumers = combine(period, start, end, consumerParts.values.toList())
            val meter = if (meterParts.isEmpty()) null else combine(period, start, end, meterParts.values.toList())
            val base = consumers.kwh
            val shares = consumerParts.map { (id, s) -> Share(id, s.kwh, s.cost, if (base > 0) s.kwh / base else 0.0) }
                .filter { it.kwh > 0 }.sortedByDescending { it.kwh }
            return HomeEnergy(consumers, meter, shares)
        }

        /**
         * Raporları dilim dilim toplar: kWh ve maliyet toplanır; kapsanan süre en uzun olanınki (aynı saatte iki cihaz
         * ölçülüyorsa saat iki kez sayılmaz); en yüksek anlık güç toplanamayacağı için yazılmaz.
         */
        fun combine(period: EnergyPeriod, start: Long, end: Long, parts: List<EnergySummary>): EnergySummary {
            val n = parts.maxOfOrNull { it.buckets.size } ?: 0
            val buckets = (0 until n).map { i ->
                val slice = parts.mapNotNull { it.buckets.getOrNull(i) }
                val kwh = slice.sumOf { it.kwh }
                val covered = slice.maxOfOrNull { it.coveredMs } ?: 0L
                EnergyBucket(
                    slice.first().start, slice.first().end, kwh,
                    avgWatts = if (covered > 0) kwh * 1000 * 3_600_000.0 / covered else null,
                    peakWatts = null, peakAt = null, coveredMs = covered, cost = slice.sumOf { it.cost },
                )
            }
            return EnergySummary(period, start, end, buckets)
        }
    }
}
