package com.macerce.switchguard.core

/** Haftalık raporun geçmişten çıkan sayıları. Saf; kayıtlar dışarıdan verilir. */
data class WeeklyStats(
    val outages: Int = 0,
    val outageMs: Long = 0,
    /** Kapatılan alarmlar ve ortalama tepki süresi. */
    val alarms: Int = 0,
    val avgResponseMs: Long? = null,
    val escalations: Int = 0,
    /** Cihaz adı → kopma sayısı (en çoktan aza). */
    val drops: List<Pair<String, Int>> = emptyList(),
    /** İzlemenin çalışmadığı toplam süre. */
    val gapMs: Long = 0,
) {
    /** Geçmiş kaydı: tür, cihaz adı, tarif. */
    data class Entry(val kind: String, val deviceName: String, val args: EventArgs?)

    companion object {
        /** [entries] rapor aralığındaki kayıtlar. Süren kesinti sayılır; süresi bitince (elektrik gelince) eklenir. */
        fun from(entries: List<Entry>): WeeklyStats {
            var outages = 0
            var outageMs = 0L
            var alarms = 0
            var responseTotal = 0L
            var escalations = 0
            var gapMs = 0L
            val drops = mutableMapOf<String, Int>()
            for (e in entries) {
                when (val a = e.args) {
                    is EventArgs.Outage -> outages++
                    is EventArgs.OutageEnded -> outageMs += a.to - a.from
                    is EventArgs.Charger -> if (a.lost) outages++ else outageMs += a.ms
                    is EventArgs.AlarmDismissed -> { alarms++; responseTotal += a.responseMs }
                    is EventArgs.TelegramAck -> { alarms++; responseTotal += a.responseMs }
                    is EventArgs.Escalation -> if (a.recipients > 0) escalations++
                    is EventArgs.MonitorGap -> gapMs += a.to - a.from
                    is EventArgs.Connection -> if (a.from && !a.to) drops[e.deviceName] = (drops[e.deviceName] ?: 0) + 1
                    else -> Unit
                }
            }
            return WeeklyStats(
                outages = outages,
                outageMs = outageMs,
                alarms = alarms,
                avgResponseMs = if (alarms > 0) responseTotal / alarms else null,
                escalations = escalations,
                drops = drops.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key to it.value },
                gapMs = gapMs,
            )
        }
    }
}
