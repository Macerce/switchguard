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
import org.json.JSONArray
import org.json.JSONObject

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

/** "Ve şu koşulda": eylem ancak başka bir cihaz (ya da dış sıcaklık, saat, güneş, tetikleyici) bu durumdaysa yapılır. */
enum class ConditionKind {
    IS_ON, IS_OFF, IS_ONLINE, IS_OFFLINE,
    /** Cihazın toplam gücü [Condition.value] W'ın üstünde / altında. */
    POWER_ABOVE, POWER_BELOW,
    /** Sensör değeri ([Condition.sensor]) [Condition.value]'nun üstünde / altında (ör. oda sıcaklığı). */
    SENSOR_ABOVE, SENSOR_BELOW,
    /** İkili sensör şu an etkin (kapı açık, hareket var, su kaçağı) / sakin. */
    SENSOR_ACTIVE, SENSOR_CLEAR,
    /** Dış sıcaklık [Condition.value] °C'nin üstünde / altında (internetten, konum gerekir; cihaz yok). */
    TEMP_ABOVE, TEMP_BELOW,
    /** Kanal en az [Condition.value] dakikadır açık / kapalı. */
    ON_FOR, OFF_FOR,
    /** Saat [Condition.start]–[Condition.end] arasında ve gün [Condition.days] içinde (başlangıç = bitiş: bütün gün). */
    TIME_BETWEEN,
    /** Hava karanlık (gün batımı ile gün doğumu arası) / aydınlık; konum gerekir. */
    DARK, LIGHT,
    /** Telefon evde / dışarıda (bölgelere göre). */
    HOME, AWAY,
    /** Kuralı [Condition.channel] numaralı tetikleyici başlattı (0 = ilki). */
    TRIGGERED_BY;

    val usesChannel get() = this == IS_ON || this == IS_OFF || this == ON_FOR || this == OFF_FOR
    val usesPower get() = this == POWER_ABOVE || this == POWER_BELOW
    val usesSensorValue get() = this == SENSOR_ABOVE || this == SENSOR_BELOW
    val usesSensorState get() = this == SENSOR_ACTIVE || this == SENSOR_CLEAR
    val usesSensor get() = usesSensorValue || usesSensorState
    val usesTemp get() = this == TEMP_ABOVE || this == TEMP_BELOW
    /** Süre (dakika) alır. */
    val usesDuration get() = this == ON_FOR || this == OFF_FOR
    val isTime get() = this == TIME_BETWEEN
    val isSun get() = this == DARK || this == LIGHT
    val isPresence get() = this == HOME || this == AWAY
    val isTrigger get() = this == TRIGGERED_BY
    /** Sayısal eşik alır. */
    val usesValue get() = usesPower || usesSensorValue || usesTemp || usesDuration
    /** Cihaz dışı koşul (sıcaklık, saat, güneş, evde/dışarıda, tetikleyici). */
    val deviceless get() = usesTemp || isTime || isSun || isPresence || isTrigger
    val usesDevice get() = !deviceless
    val above get() = this == POWER_ABOVE || this == SENSOR_ABOVE || this == TEMP_ABOVE
}

/** Birden çok koşul: hepsi (ve) ya da herhangi biri (veya) sağlanmalı. */
enum class ConditionMatch { ALL, ANY }

/**
 * Koşulların cihaz dışı girdileri. [sinceOf]: kanal ne zamandan beri bu durumda (motorun kaydı); [dark]: hava karanlık mı
 * (konum yoksa null); [triggerIndex]: kuralı hangi tetikleyici başlattı (elle çalıştırmada null).
 */
data class CondCtx(
    val now: Long = System.currentTimeMillis(),
    val outdoorTemp: Double? = null,
    val away: Boolean? = null,
    val sinceOf: (String, Int) -> Long? = { _, _ -> null },
    val dark: Boolean? = null,
    val triggerIndex: Int? = null,
)

data class Condition(
    val kind: ConditionKind,
    val deviceId: String,
    val channel: Int = 0,
    /** Güç (W), sensör, dış sıcaklık eşiği ya da süre (dk). */
    val value: Double = 0.0,
    val sensor: SensorKind? = null,
    /** Sonucu tersine çevir ("değilse"). */
    val negate: Boolean = false,
    /** 0 = üst düzey; 1, 2… = o numaralı koşul grubunun üyesi (grubun kendi hepsi/herhangi biri kuralı var). */
    val group: Int = 0,
    /** [ConditionKind.TIME_BETWEEN]: başlangıç ve bitiş (gece yarısından dakika) ve günler. */
    val start: Int = 0,
    val end: Int = 0,
    val days: Int = ActiveWindow.ALL_DAYS,
) {
    /** Cihaz, değer ya da dış sıcaklık bilinmiyorsa koşul sağlanmamış sayılır; çevrimdışı cihazın eski değeri sayılmaz. */
    fun holds(devices: Map<String, DeviceSnapshot>, outdoorTemp: Double? = null): Boolean =
        holds(devices, CondCtx(outdoorTemp = outdoorTemp))

    /** [negate] uygulanmış sonuç. Bilinmeyen değer "değil"de de sağlanmamış sayılır (yanlışlıkla tetiklenmesin). */
    fun holds(devices: Map<String, DeviceSnapshot>, ctx: CondCtx): Boolean {
        val raw = raw(devices, ctx) ?: return false
        return raw != negate
    }

    /** Ham sonuç; bilinmiyorsa null. */
    private fun raw(devices: Map<String, DeviceSnapshot>, ctx: CondCtx): Boolean? {
        when {
            kind.usesTemp -> {
                val t = ctx.outdoorTemp ?: return null
                return if (kind.above) t > value else t < value
            }
            kind.isTime -> return ActiveWindow(start, end, days).contains(ctx.now)
            kind.isSun -> return ctx.dark?.let { it == (kind == ConditionKind.DARK) }
            kind.isPresence -> return ctx.away?.let { it == (kind == ConditionKind.AWAY) }
            kind.isTrigger -> return ctx.triggerIndex?.let { it == channel }
        }
        val d = devices[deviceId] ?: return null
        return when (kind) {
            ConditionKind.IS_ONLINE -> d.online
            ConditionKind.IS_OFFLINE -> !d.online
            ConditionKind.IS_ON -> if (!d.online) null else d.switches[channel] == true
            ConditionKind.IS_OFF -> if (!d.online) null else d.switches[channel] == false
            ConditionKind.ON_FOR, ConditionKind.OFF_FOR -> {
                val want = kind == ConditionKind.ON_FOR
                val state = d.switches[channel]?.takeIf { d.online } ?: return null
                if (state != want) return false
                val since = ctx.sinceOf(deviceId, channel) ?: return null
                ctx.now - since >= (value * 60_000L).toLong()
            }
            ConditionKind.POWER_ABOVE, ConditionKind.POWER_BELOW -> {
                val p = d.power?.takeIf { d.online } ?: return null
                if (kind.above) p > value else p < value
            }
            ConditionKind.SENSOR_ABOVE, ConditionKind.SENSOR_BELOW -> {
                val v = sensor?.let { d.sensors[it] }?.takeIf { d.online } ?: return null
                if (kind.above) v > value else v < value
            }
            ConditionKind.SENSOR_ACTIVE, ConditionKind.SENSOR_CLEAR -> {
                val a = sensor?.let { d.sensorActive(it) }?.takeIf { d.online } ?: return null
                a == (kind == ConditionKind.SENSOR_ACTIVE)
            }
            else -> null
        }
    }

    companion object {
        /** Boş liste = koşul yok (sağlanmış). */
        fun allHold(list: List<Condition>, match: ConditionMatch, devices: Map<String, DeviceSnapshot>, outdoorTemp: Double? = null): Boolean =
            CondSet(list, match).ok(devices, CondCtx(outdoorTemp = outdoorTemp))
    }
}

/**
 * Koşul kümesi: üst düzey koşullar ve gruplar [match] ile birleşir; her grup ([Condition.group] > 0) kendi içinde
 * [groupMatch] kuralıyla (yoksa "hepsi") tek bir koşul gibi sayılır. Böylece "A ve (B veya C)" yazılabilir.
 */
data class CondSet(
    val items: List<Condition> = emptyList(),
    val match: ConditionMatch = ConditionMatch.ALL,
    val groupMatch: Map<Int, ConditionMatch> = emptyMap(),
) {
    val isEmpty get() = items.isEmpty()

    /** Koşulların tek tek sonucu (sırayla, "değil" uygulanmış). */
    fun each(devices: Map<String, DeviceSnapshot>, ctx: CondCtx): List<Boolean> = items.map { it.holds(devices, ctx) }

    fun ok(devices: Map<String, DeviceSnapshot>, ctx: CondCtx): Boolean = combine(each(devices, ctx))

    /** Tek tek sonuçlardan genel sonuç. */
    fun combine(each: List<Boolean>): Boolean {
        if (items.isEmpty()) return true
        val top = mutableListOf<Boolean>()
        val groups = LinkedHashMap<Int, MutableList<Boolean>>()
        items.forEachIndexed { i, c ->
            val r = each.getOrElse(i) { false }
            if (c.group <= 0) top += r else groups.getOrPut(c.group) { mutableListOf() } += r
        }
        groups.forEach { (g, list) -> top += if ((groupMatch[g] ?: ConditionMatch.ALL) == ConditionMatch.ALL) list.all { it } else list.any { it } }
        return if (match == ConditionMatch.ALL) top.all { it } else top.any { it }
    }

    fun toJson(): JSONObject = JSONObject().put("items", condsToJson(items)).put("match", match.name).apply {
        if (groupMatch.isNotEmpty()) put("gm", groupMatchToJson(groupMatch))
    }

    companion object {
        fun fromJson(o: JSONObject?): CondSet? = o?.let {
            CondSet(
                condsFromJson(it.optJSONArray("items")),
                runCatching { ConditionMatch.valueOf(it.optString("match")) }.getOrDefault(ConditionMatch.ALL),
                groupMatchFromJson(it.optJSONObject("gm")),
            )
        }

        fun condToJson(c: Condition): JSONObject = JSONObject().put("kind", c.kind.name).put("device", c.deviceId).put("channel", c.channel)
            .put("value", c.value).apply {
                c.sensor?.let { put("sensor", it.name) }
                if (c.negate) put("neg", true)
                if (c.group > 0) put("grp", c.group)
                if (c.kind.isTime) put("start", c.start).put("end", c.end).put("days", c.days)
            }

        fun condsToJson(list: List<Condition>): JSONArray = JSONArray().apply { list.forEach { put(condToJson(it)) } }

        /** Tanınmayan koşul (yeni sürümden) hata verir: koşulun biri atlanıp adım koşulsuz çalışmasın. */
        fun condsFromJson(arr: JSONArray?): List<Condition> = arr?.let { a ->
            (0 until a.length()).map { i -> condFromJson(a.getJSONObject(i)) }
        }.orEmpty()

        fun condFromJson(c: JSONObject) = Condition(
            ConditionKind.valueOf(c.getString("kind")), c.optString("device"), c.optInt("channel"),
            c.optDouble("value", 0.0),
            c.optString("sensor").takeIf { it.isNotEmpty() }?.let { SensorKind.valueOf(it) },
            negate = c.optBoolean("neg", false),
            group = c.optInt("grp", 0),
            start = c.optInt("start", 0), end = c.optInt("end", 0), days = c.optInt("days", ActiveWindow.ALL_DAYS),
        )

        fun groupMatchToJson(m: Map<Int, ConditionMatch>): JSONObject = JSONObject().apply { m.forEach { (k, v) -> put(k.toString(), v.name) } }

        fun groupMatchFromJson(o: JSONObject?): Map<Int, ConditionMatch> = o?.let { m ->
            m.keys().asSequence().mapNotNull { k -> k.toIntOrNull()?.let { it to runCatching { ConditionMatch.valueOf(m.getString(k)) }.getOrDefault(ConditionMatch.ALL) } }.toMap()
        }.orEmpty()
    }
}

/** Hava karanlık mı (bugünün gün batımından sonra ya da gün doğumundan önce); konum yoksa ya da kutup gününde null. */
fun isDark(now: Long, env: Environment): Boolean? {
    val lat = env.latitude ?: return null
    val lon = env.longitude ?: return null
    val rise = Sun.time(now, lat, lon, sunrise = true) ?: return null
    val set = Sun.time(now, lat, lon, sunrise = false) ?: return null
    return now < rise || now >= set
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
