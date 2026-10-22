package com.macerce.switchguard.core

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Gün doğumu / batımı (NOAA'nın basitleştirilmiş güneş denklemi, ±1-2 dk). Konum kullanıcıdan gelir, internet gerekmez.
 */
object Sun {
    private const val ZENITH = 90.833

    /**
     * [day] gününün (yerel saat) gün doğumu ya da batımı, ms. Kutup gecesi/gündüzünde null.
     * [zone]: hesaplanan zamanın yerel gününü bulmak için.
     */
    fun time(day: Long, latitude: Double, longitude: Double, sunrise: Boolean, zone: TimeZone = TimeZone.getDefault()): Long? {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = day
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val n = cal.get(Calendar.DAY_OF_YEAR)
        val lngHour = longitude / 15
        val t = n + ((if (sunrise) 6.0 else 18.0) - lngHour) / 24
        val m = 0.9856 * t - 3.289
        var l = m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634
        l = norm(l, 360.0)
        var ra = deg(atan2(0.91764 * tan(rad(l)), 1.0))
        ra = norm(ra, 360.0)
        ra += floor(l / 90) * 90 - floor(ra / 90) * 90
        ra /= 15
        val sinDec = 0.39782 * sin(rad(l))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(rad(ZENITH)) - sinDec * sin(rad(latitude))) / (cosDec * cos(rad(latitude)))
        if (cosH > 1 || cosH < -1) return null
        val h = (if (sunrise) 360 - deg(acos(cosH)) else deg(acos(cosH))) / 15
        val localMean = h + ra - 0.06571 * t - 6.622
        val utcHours = norm(localMean - lngHour, 24.0)
        // UTC gece yarısı + saat; yerel günden kayarsa bir gün düzeltilir.
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        var result = utcMidnight + (utcHours * 3_600_000).toLong()
        val start = cal.timeInMillis
        if (result < start) result += 86_400_000L
        if (result >= start + 86_400_000L) result -= 86_400_000L
        return result
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun norm(v: Double, m: Double) = ((v % m) + m) % m
}

/** Kuralın geçerli olduğu saatler ve günler. [days]: bit 0 = pazartesi … bit 6 = pazar. Başlangıç = bitiş ise bütün gün. */
data class ActiveWindow(val startMinute: Int = 0, val endMinute: Int = 0, val days: Int = ALL_DAYS) {
    fun contains(time: Long, zone: TimeZone = TimeZone.getDefault()): Boolean {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = time }
        val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        // Gece yarısını geçen aralıkta (22:00–06:00) sabah kısmı önceki günün aralığıdır.
        val crossing = startMinute > endMinute
        val day = dayIndex(cal) .let { if (crossing && minute < endMinute) (it + 6) % 7 else it }
        if (days and (1 shl day) == 0) return false
        return when {
            startMinute == endMinute -> true
            !crossing -> minute in startMinute until endMinute
            else -> minute >= startMinute || minute < endMinute
        }
    }

    companion object {
        const val ALL_DAYS = 0b1111111
        /** Pazartesi = 0. */
        fun dayIndex(cal: Calendar): Int = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }
}

/** "Ve şu koşulda": eylem ancak başka bir cihaz bu durumdaysa yapılır. */
enum class ConditionKind { IS_ON, IS_OFF, IS_ONLINE, IS_OFFLINE }

data class Condition(val kind: ConditionKind, val deviceId: String, val channel: Int = 0) {
    /** Cihaz yoksa koşul sağlanmamış sayılır. */
    fun holds(devices: Map<String, DeviceSnapshot>): Boolean {
        val d = devices[deviceId] ?: return false
        return when (kind) {
            ConditionKind.IS_ONLINE -> d.online
            ConditionKind.IS_OFFLINE -> !d.online
            ConditionKind.IS_ON -> d.online && d.switches[channel] == true
            ConditionKind.IS_OFF -> d.online && d.switches[channel] == false
        }
    }
}

/** Kuralların cihaz dışı girdileri: konum (gün doğumu/batımı) ve dış sıcaklık (°C). */
data class Environment(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val outdoorTemp: Double? = null,
    /** Evde (false) / dışarıda (true); null = bilinmiyor (kural evde/dışarıda sınırına bakılmaz). */
    val away: Boolean? = null,
) {
    val hasLocation get() = latitude != null && longitude != null
}
