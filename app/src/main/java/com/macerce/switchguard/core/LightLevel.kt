package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Işık parlaklığı ve rengi ([ActionKind.SET_LEVEL]): hangi yerel cihaz destekliyor ve markaya göre istek gövdesi. Saf.
 * [level] %1–100 (null = değiştirme), [color] 0xRRGGBB (null = değiştirme). İkisi de verilince ışık açılır.
 */
object LightLevel {
    /** Parlaklık/renk ayarlanabilen yerel cihaz: Hue lambası, ESPHome "light", Home Assistant "light.", Tapo ampulü. */
    fun supports(d: LocalDevice?, channel: Int = 0): Boolean = when (d?.brand) {
        LocalDevice.BRAND_HUE, LocalDevice.BRAND_TAPO -> true
        LocalDevice.BRAND_ESPHOME -> EsphomeProtocol.entitiesFromJson(d.extra).switches.getOrNull(channel)?.startsWith("light/") == true
        LocalDevice.BRAND_HA -> d.extra.startsWith("light.")
        else -> false
    }

    private fun pct(level: Int) = level.coerceIn(1, 100)

    /** Hue v1: {"on":true,"bri":1..254,"xy":[x,y]}. */
    fun hueBody(level: Int?, color: Int?): String = JSONObject().put("on", true).apply {
        level?.let { put("bri", (pct(it) * 254 / 100.0).roundToInt().coerceIn(1, 254)) }
        color?.let { val (x, y) = rgbToXy(it); put("xy", JSONArray().put(x).put(y)) }
    }.toString()

    /** ESPHome web_server: POST /light/<ad>/turn_on?brightness=0..255&r=&g=&b=. */
    fun esphomePath(path: String, level: Int?, color: Int?): String {
        val q = mutableListOf<String>()
        level?.let { q += "brightness=" + (pct(it) * 255 / 100.0).roundToInt() }
        color?.let { q += "r=${(it shr 16) and 255}"; q += "g=${(it shr 8) and 255}"; q += "b=${it and 255}" }
        return "/$path/turn_on" + if (q.isEmpty()) "" else "?" + q.joinToString("&")
    }

    /** Home Assistant: light.turn_on {"entity_id":..,"brightness_pct":..,"rgb_color":[r,g,b]}. */
    fun haBody(entityId: String, level: Int?, color: Int?): String = JSONObject().put("entity_id", entityId).apply {
        level?.let { put("brightness_pct", pct(it)) }
        color?.let { put("rgb_color", JSONArray().put((it shr 16) and 255).put((it shr 8) and 255).put(it and 255)) }
    }.toString()

    /** Tapo ampul: set_device_info {"device_on":true,"brightness":1..100,"hue":0..360,"saturation":0..100,"color_temp":0}. */
    fun tapoBody(level: Int?, color: Int?): String {
        val p = JSONObject().put("device_on", true)
        level?.let { p.put("brightness", pct(it)) }
        color?.let { val (h, s, _) = rgbToHsv(it); p.put("hue", h).put("saturation", s).put("color_temp", 0) }
        return JSONObject().put("method", "set_device_info").put("params", p).toString()
    }

    /** sRGB → CIE 1931 xy (Philips'in Hue için önerdiği dönüşüm, geniş gam). Siyah → beyaz noktası. */
    fun rgbToXy(rgb: Int): Pair<Double, Double> {
        fun lin(c: Int): Double { val v = c / 255.0; return if (v > 0.04045) ((v + 0.055) / 1.055).pow(2.4) else v / 12.92 }
        val r = lin((rgb shr 16) and 255); val g = lin((rgb shr 8) and 255); val b = lin(rgb and 255)
        val x = r * 0.664511 + g * 0.154324 + b * 0.162028
        val y = r * 0.283881 + g * 0.668433 + b * 0.047685
        val z = r * 0.000088 + g * 0.072310 + b * 0.986039
        val sum = x + y + z
        if (sum <= 0.0) return 0.3227 to 0.329
        return round4(x / sum) to round4(y / sum)
    }

    /** RGB → (ton 0..360, doygunluk 0..100, parlaklık 0..100). */
    fun rgbToHsv(rgb: Int): Triple<Int, Int, Int> {
        val r = ((rgb shr 16) and 255) / 255.0; val g = ((rgb shr 8) and 255) / 255.0; val b = (rgb and 255) / 255.0
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
        val h = when {
            d == 0.0 -> 0.0
            mx == r -> 60 * (((g - b) / d) % 6)
            mx == g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }.let { if (it < 0) it + 360 else it }
        val s = if (mx == 0.0) 0.0 else d / mx
        return Triple(h.roundToInt() % 360, (s * 100).roundToInt(), (mx * 100).roundToInt())
    }

    private fun round4(v: Double) = (v * 10000).roundToInt() / 10000.0

    /** Düzenleyicideki hazır renkler (sıcak beyaz, beyaz, kırmızı, turuncu, sarı, yeşil, mavi, mor). */
    val PRESETS = listOf(0xFFD8A8, 0xFFFFFF, 0xFF2A1A, 0xFF8A00, 0xFFD400, 0x22CC55, 0x2A6BFF, 0xA040FF)
}
