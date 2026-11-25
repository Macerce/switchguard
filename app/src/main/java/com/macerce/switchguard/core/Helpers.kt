package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Yardımcı türü (Home Assistant input_boolean / counter / timer). */
enum class HelperKind {
    /** Sanal anahtar: "tatil modu", "misafir var". */
    TOGGLE,
    /** Sayaç: tam sayı; kural ekler/çıkarır, eşik tetikleyicisi ve koşulu olur. */
    COUNTER,
    /** Zamanlayıcı: açılınca [Helper.minutes] sonra kendiliğinden kapanır ("bitti" = kapandı). */
    TIMER,
}

/**
 * Kullanıcının tanımladığı sanal varlık. Motora ve düzenleyiciye sanal cihaz olarak görünür ([snapshot]): anahtar ve
 * zamanlayıcı tek kanallı röle, sayaç [SensorKind.COUNT] sensörü. Böylece bütün tetikleyici, koşul ve eylemler
 * (aç, kapat, değiştir, açık/kapalı kaldı, eşik) yardımcılarla da çalışır.
 */
data class Helper(
    val id: String,
    val name: String,
    val kind: HelperKind,
    val on: Boolean = false,
    val value: Int = 0,
    /** Zamanlayıcı süresi (dk). */
    val minutes: Int = 10,
    /** Çalışan zamanlayıcının bitişi (0 = çalışmıyor). */
    val endsAt: Long = 0,
) {
    val deviceId: String get() = Helpers.PREFIX + id

    fun snapshot(): DeviceSnapshot = when (kind) {
        HelperKind.COUNTER -> DeviceSnapshot(deviceId, name, true, emptyMap(), cloud = Cloud.HELPER, sensors = mapOf(SensorKind.COUNT to value.toDouble()))
        else -> DeviceSnapshot(deviceId, name, true, mapOf(0 to on), cloud = Cloud.HELPER)
    }
}

object Helpers {
    const val PREFIX = "helper:"

    fun isHelper(deviceId: String): Boolean = deviceId.startsWith(PREFIX)

    fun idOf(deviceId: String): String = deviceId.removePrefix(PREFIX)

    /** Aç/kapat: zamanlayıcı açılınca bitişi kurulur, kapanınca silinir. Sayaçta anlamsız (değişmez). */
    fun switch(h: Helper, on: Boolean, now: Long): Helper = when (h.kind) {
        HelperKind.COUNTER -> h
        HelperKind.TOGGLE -> h.copy(on = on)
        HelperKind.TIMER -> if (on) h.copy(on = true, endsAt = now + h.minutes.coerceAtLeast(1) * 60_000L) else h.copy(on = false, endsAt = 0)
    }

    /** Sayaç: ekle (eksi: çıkar) ya da değer ata. */
    fun count(h: Helper, add: Int? = null, set: Int? = null): Helper =
        if (h.kind != HelperKind.COUNTER) h else h.copy(value = set ?: (h.value + (add ?: 0)))

    /** Süresi dolan zamanlayıcıları kapatır; değişmediyse aynı liste. */
    fun expire(list: List<Helper>, now: Long): List<Helper> =
        if (list.none { it.kind == HelperKind.TIMER && it.on && it.endsAt in 1..now }) list
        else list.map { if (it.kind == HelperKind.TIMER && it.on && it.endsAt in 1..now) it.copy(on = false, endsAt = 0) else it }

    /** Sonraki zamanlayıcı bitişi (servisin uyanması için). */
    fun nextDueAt(list: List<Helper>): Long? = list.filter { it.kind == HelperKind.TIMER && it.on && it.endsAt > 0 }.minOfOrNull { it.endsAt }

    fun toJson(list: List<Helper>): String = JSONArray().apply {
        list.forEach { h ->
            put(JSONObject().put("id", h.id).put("name", h.name).put("kind", h.kind.name).put("on", h.on).put("value", h.value)
                .put("min", h.minutes).apply { if (h.endsAt > 0) put("ends", h.endsAt) })
        }
    }.toString()

    fun fromJson(json: String?): List<Helper> = runCatching {
        val arr = JSONArray(json ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                Helper(o.getString("id"), o.optString("name"), HelperKind.valueOf(o.getString("kind")), o.optBoolean("on"),
                    o.optInt("value"), o.optInt("min", 10), o.optLong("ends", 0))
            }.getOrNull()
        }
    }.getOrDefault(emptyList())
}

/**
 * Bildirim/gönderim metnindeki yer tutucular: {kural}/{rule}, {cihaz}/{device} (tetikleyen cihaz), {deger}/{value}
 * (tetikleyicinin ölçümü), {saat}/{time}, {tarih}/{date}. Tanınmayanlar olduğu gibi kalır.
 */
object TextTemplate {
    data class Vars(val rule: String, val device: String = "", val value: String = "", val time: String = "", val date: String = "")

    fun render(text: String, v: Vars): String {
        if ('{' !in text) return text
        val map = mapOf(
            "kural" to v.rule, "rule" to v.rule,
            "cihaz" to v.device, "device" to v.device,
            "deger" to v.value, "değer" to v.value, "value" to v.value,
            "saat" to v.time, "time" to v.time,
            "tarih" to v.date, "date" to v.date,
        )
        return Regex("\\{([^{}]{1,20})\\}").replace(text) { m -> map[m.groupValues[1].trim().lowercase()] ?: m.value }
    }

    /** Düzenleyicideki "ekle" düğmeleri (dilden bağımsız adlar). */
    val TOKENS = listOf("{rule}", "{device}", "{value}", "{time}", "{date}")
}
