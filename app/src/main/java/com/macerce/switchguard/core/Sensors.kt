package com.macerce.switchguard.core

import org.json.JSONObject

/**
 * Cihazın bildirdiği sensör değeri türü. Sayısal olanlar eşikle ([TriggerKind.SENSOR_ABOVE]/[TriggerKind.SENSOR_BELOW]),
 * ikili olanlar (değer 1.0 = etkin, 0.0 = değil) durum değişimiyle ([TriggerKind.SENSOR_ACTIVE]/[TriggerKind.SENSOR_CLEAR])
 * otomasyonda kullanılır.
 */
enum class SensorKind(val binary: Boolean, val unit: String = "", val hysteresis: Double = 0.0) {
    TEMPERATURE(false, "°C", 0.5),
    HUMIDITY(false, "%", 2.0),
    BATTERY(false, "%", 2.0),
    ILLUMINANCE(false, "lx", 10.0),
    /** Kapı/pencere açık. */
    DOOR(true),
    /** Hareket ya da varlık algılandı. */
    MOTION(true),
    /** Su kaçağı algılandı. */
    WATER(true),
}

/**
 * eWeLink cihaz parametrelerinden sensör değerlerini okur. Hangi modelin (uiid) hangi alanı hangi ölçekle gönderdiği
 * AlexxIT/SonoffLAN'ın cihaz tablosundan alınmıştır (MIT lisansı, https://github.com/AlexxIT/SonoffLAN,
 * custom_components/sonoff/core/devices.py). Tabloda olmayan modellerde yaygın alan adları denenir.
 *
 * Yalnızca iletide bulunan alanlar döner; anlık (WebSocket) iletiler kısmi olduğundan sonuç önceki değerlerin üstüne
 * yazılır.
 */
object Sensors {

    /** Kapı durumunu `switch` ("on" = açık) alanında gönderen Wi-Fi kapı sensörleri (DW2, DW2-Wi-Fi-L): röle değildir. */
    val WIFI_DOOR_UIIDS = setOf(102, 154)

    /** Sıcaklık/nemi yüzde birlik tamsayı gönderen Zigbee sensörleri (SNZB-02, 02D, 02B, 02M, 02DR2 ...). */
    private val HUNDREDTHS = setOf(242, 1770, 1771, 7014, 7038, 7052, 7057)
    /** Sıcaklığı onda birlik gönderenler: TRVZB termostatik vana. */
    private val TENTHS = setOf(7017, 7049)
    /** Nem alanı anlamsız olanlar (NSPanel hep 50 gönderir). */
    private val NO_HUMIDITY = setOf(133, 7017, 7049)
    /** `water` alanı su kaçağı değil (YB-01 buhar makinesinin su düzeyi). */
    private val NOT_LEAK = setOf(25)
    /** `battery` alanı yüzde değil (DW2'de volt). */
    private val BATTERY_NOT_PERCENT = setOf(102)

    fun parse(uiid: Int, params: JSONObject?): Map<SensorKind, Double> {
        if (params == null) return emptyMap()
        val out = linkedMapOf<SensorKind, Double>()
        temperature(uiid, params)?.let { out[SensorKind.TEMPERATURE] = it }
        if (uiid !in NO_HUMIDITY) humidity(uiid, params)?.let { out[SensorKind.HUMIDITY] = it }
        if (uiid !in BATTERY_NOT_PERCENT) {
            params.number("battery")?.let { b -> (if (uiid == 1514) b * 2 else b).takeIf { it in 0.0..100.0 }?.let { out[SensorKind.BATTERY] = it } }
        }
        params.number("illumination")?.let { out[SensorKind.ILLUMINANCE] = it }
        when {
            uiid in WIFI_DOOR_UIIDS -> params.optString("switch").takeIf { it.isNotEmpty() }?.let { out[SensorKind.DOOR] = flag(it == "on") }
            params.has("lock") && params.number("lock") != null -> out[SensorKind.DOOR] = flag(params.number("lock") == 1.0)
        }
        // Hareket (SNZB-03) ya da varlık (SNZB-06P) aynı tür sayılır.
        params.number("motion")?.let { out[SensorKind.MOTION] = flag(it == 1.0) }
        params.number("human")?.let { out[SensorKind.MOTION] = flag(it == 1.0) }
        if (uiid !in NOT_LEAK) params.number("water")?.let { out[SensorKind.WATER] = flag(it == 1.0) }
        return out
    }

    private fun temperature(uiid: Int, p: JSONObject): Double? {
        // TH16/THR320: "unavailable" yazısı ya da 0 gelirse sensör takılı değildir.
        p.number("currentTemperature")?.let { v -> return v.takeIf { it != 0.0 && it > -270 && it < 270 }?.round1() }
        var v = p.number("temperature") ?: return null
        v = when (uiid) {
            in HUNDREDTHS -> v * 0.01
            in TENTHS -> v * 0.1
            // Tabloda olmayan Zigbee sensörü yüzde birlik gönderiyor olabilir (2350 = 23,5 °C).
            else -> if (kotlin.math.abs(v) >= 150) v * 0.01 else v
        }
        p.number("tempCorrection")?.let { v += it }
        return v.takeIf { it > -270 && it < 270 && it != 0.0 }?.round1()
    }

    private fun humidity(uiid: Int, p: JSONObject): Double? {
        p.number("currentHumidity")?.let { v -> return v.takeIf { it > 0.0 && it <= 100.0 }?.round1() }
        var v = p.number("humidity") ?: return null
        if (uiid in HUNDREDTHS || v > 100) v *= 0.01
        p.number("humCorrection")?.let { v += it }
        return v.takeIf { it > 0.0 && it <= 100.0 }?.round1()
    }

    private fun flag(b: Boolean) = if (b) 1.0 else 0.0
    private fun Double.round1() = Math.round(this * 10) / 10.0

    /** Sayı ya da sayı yazısı; "unavailable" gibi değerler null. */
    private fun JSONObject.number(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val v = opt(key)) {
            is Number -> v.toDouble()
            is String -> v.trim().replace(',', '.').toDoubleOrNull()
            is Boolean -> if (v) 1.0 else 0.0
            else -> null
        }?.takeIf { !it.isNaN() && !it.isInfinite() }
    }
}
