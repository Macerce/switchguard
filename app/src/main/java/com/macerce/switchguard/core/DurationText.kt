package com.macerce.switchguard.core

/** Süre metinlerinin dile göre değişen kalıpları. */
class DurationStrings(
    val seconds: (Int) -> String,
    val minSec: (Int, Int) -> String,
    val minutes: (Int) -> String,
    val hourMin: (Int, Int) -> String,
    val dayHour: (Int, Int) -> String,
)

/** "9 sn", "2 dk 10 sn", "35 dk", "2 sa 10 dk", "1 g 3 sa": kısa sürelerde ayrıntı, uzunlarda yuvarlak. */
object DurationText {
    fun format(ms: Long, s: DurationStrings): String {
        val totalSec = (ms.coerceAtLeast(0) / 1000).toInt()
        val min = totalSec / 60
        val hours = min / 60
        return when {
            totalSec < 60 -> s.seconds(totalSec)
            min < 10 -> if (totalSec % 60 == 0) s.minutes(min) else s.minSec(min, totalSec % 60)
            min < 60 -> s.minutes(min)
            hours < 24 -> s.hourMin(hours, min % 60)
            else -> s.dayHour(hours / 24, hours % 24)
        }
    }
}
