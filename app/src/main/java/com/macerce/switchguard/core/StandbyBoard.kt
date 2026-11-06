package com.macerce.switchguard.core

import java.util.TimeZone

/**
 * Bekleme ekranının saf hesapları: yakında çalışacak zamanlı işler ve toplam güç eğrisi.
 * Veriler dışarıdan verilir (Store/MetricsLog), burada yalnızca hesap var.
 */
object StandbyBoard {
    enum class UpcomingKind { SUNRISE, SUNSET, CYCLE_START, CYCLE_STEP, CYCLE_END, TEMPLATE }

    data class Upcoming(val time: Long, val name: String, val kind: UpcomingKind)

    /** Bekleme ekranının bakacağı ileri süre. */
    const val HORIZON_MS = 24 * 3_600_000L

    /**
     * Önümüzdeki [horizonMs] içinde kendiliğinden çalışacak işler, zamana göre sıralı:
     * gün doğumu/batımı otomasyonları (konum varsa, etkin saat/gün aralığına uyanlar), saatli döngüler,
     * süren döngülerin sonraki adımı/bitişi ve saatli sohbet şablonları.
     */
    fun upcoming(
        automations: List<Automation>,
        cycles: List<Cycle>,
        runs: Map<String, CycleRun>,
        templates: List<ChatTemplate>,
        templateRuns: Map<String, Long>,
        latitude: Double?,
        longitude: Double?,
        now: Long,
        horizonMs: Long = HORIZON_MS,
        zone: TimeZone = TimeZone.getDefault(),
    ): List<Upcoming> {
        val until = now + horizonMs
        val out = mutableListOf<Upcoming>()
        if (latitude != null && longitude != null) {
            for (a in automations) {
                if (!a.enabled || !a.trigger.kind.isSun) continue
                val sunrise = a.trigger.kind == TriggerKind.SUNRISE
                // Kayma yüzünden bugünkü vakit dünün ya da yarının tarafına düşebilir: üç gün denenir.
                val at = (-1..2).asSequence()
                    .mapNotNull { d -> Sun.time(now + d * 86_400_000L, latitude, longitude, sunrise, zone)?.plus(a.trigger.minutes * 60_000L) }
                    .filter { it in (now + 1)..until && (a.window == null || a.window.contains(it, zone)) }
                    .minOrNull() ?: continue
                out += Upcoming(at, a.name, if (sunrise) UpcomingKind.SUNRISE else UpcomingKind.SUNSET)
            }
        }
        for (c in cycles) {
            if (!c.enabled) continue
            val run = runs[c.id]
            if (run != null) {
                val last = c.repeat > 0 && run.round >= c.repeat - 1 && run.step >= c.steps.size - 1
                val endsBy = run.windowEnd.takeIf { it > 0 }
                val next = if (endsBy != null && endsBy < run.stepEndsAt) endsBy else run.stepEndsAt
                if (next in (now + 1)..until) {
                    out += Upcoming(next, c.name, if (last || next == endsBy) UpcomingKind.CYCLE_END else UpcomingKind.CYCLE_STEP)
                }
                continue
            }
            val start = CycleEngine.nextStart(c, now, zone) ?: continue
            if (start <= until) out += Upcoming(start, c.name, UpcomingKind.CYCLE_START)
        }
        for (t in templates) {
            val at = ChatTemplate.nextAt(listOf(t), templateRuns, now) ?: continue
            if (at in (now + 1)..until) out += Upcoming(at, t.name, UpcomingKind.TEMPLATE)
        }
        return out.sortedBy { it.time }
    }

    /**
     * Cihazların toplam gücünün [from]..[to] arasında [points] noktalık eğrisi. Her noktada her cihazın o andaki
     * son ölçümü alınır; ölçümü [maxAgeMs]'den eskiyse (izleme durmuştu) o cihaz 0 sayılır.
     */
    fun totalPower(
        samples: Collection<List<PowerSample>>,
        from: Long,
        to: Long,
        points: Int,
        maxAgeMs: Long = 15 * 60_000L,
    ): List<Double> {
        if (points < 2 || to <= from) return emptyList()
        val step = (to - from).toDouble() / (points - 1)
        val out = DoubleArray(points)
        for (list in samples) {
            if (list.isEmpty()) continue
            val sorted = if (list.zipWithNext().all { (a, b) -> a.time <= b.time }) list else list.sortedBy { it.time }
            var j = -1
            for (i in 0 until points) {
                val t = from + (step * i).toLong()
                while (j + 1 < sorted.size && sorted[j + 1].time <= t) j++
                if (j >= 0 && t - sorted[j].time <= maxAgeMs) out[i] += sorted[j].watts
            }
        }
        return out.toList()
    }

    /** Yarım daire göstergesinin üst sınırı: görülen en yüksek değerin biraz üstünde, okunaklı bir yuvarlak sayı. */
    fun gaugeMax(peak: Double): Double {
        val target = maxOf(peak * 1.15, 500.0)
        val steps = listOf(500.0, 1000.0, 1500.0, 2000.0, 3000.0, 4000.0, 5000.0, 7500.0, 10_000.0, 15_000.0, 20_000.0)
        return steps.firstOrNull { it >= target } ?: (kotlin.math.ceil(target / 10_000.0) * 10_000.0)
    }
}
