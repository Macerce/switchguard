package com.macerce.switchguard.core

import org.json.JSONObject

/** Bir mesajdan okunan enerji değerleri; mesajda olmayan alan null. */
data class Energy(val power: Double?, val voltage: Double?, val current: Double?) {
    val isEmpty get() = power == null && voltage == null && current == null
}

/**
 * Sonoff cihazlarının enerji alanlarını okur. Modeller farklı biçim kullanır:
 * - POW R2 (uiid 32): "power":"123.45" (W, ondalıklı metin)
 * - POW R3 / S40 / S60 vb.: "power": 12345 (yüzde bir W, tam sayı)
 * - DualR3 / SPM: kanal başına "actPow_00", "voltage_00", "current_00" (yüzde bir)
 * Ölçek, bilinen uiid'lerden ve gerilimin makul aralıkta olup olmamasından çıkarılır.
 */
object EnergyParser {

    /** Değerleri yüzde bir birimle gönderen modeller. */
    private val HUNDREDTHS_UIIDS = setOf(126, 130, 182, 190, 276, 277)

    fun parse(uiid: Int, params: JSONObject): Energy {
        val channelPowers = (0..3).mapNotNull { number(params, "actPow_%02d".format(it)) }
        val rawPower = number(params, "power") ?: channelPowers.takeIf { it.isNotEmpty() }?.sum()
        val rawVoltage = number(params, "voltage") ?: number(params, "voltage_00")
        val rawCurrent = number(params, "current")
            ?: (0..3).mapNotNull { number(params, "current_%02d".format(it)) }.takeIf { it.isNotEmpty() }?.sum()

        val scale = when {
            // Şebeke gerilimi 100–260 V; 1000'in üstü yüzde bir birim demektir.
            rawVoltage != null -> if (rawVoltage > 1000) 100.0 else 1.0
            uiid in HUNDREDTHS_UIIDS || channelPowers.isNotEmpty() -> 100.0
            else -> 1.0
        }
        return Energy(rawPower?.div(scale), rawVoltage?.div(scale), rawCurrent?.div(scale))
    }

    private fun number(params: JSONObject, key: String): Double? {
        if (!params.has(key)) return null
        return when (val v = params.opt(key)) {
            is Number -> v.toDouble()
            is String -> v.trim().toDoubleOrNull()
            else -> null
        }
    }
}
