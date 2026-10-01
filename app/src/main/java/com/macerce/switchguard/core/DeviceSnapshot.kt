package com.macerce.switchguard.core

/** Cihazın bağlı olduğu bulut. */
enum class Cloud { EWELINK, TUYA }

/**
 * Bir cihazın belirli bir andaki durumu.
 * [switches]: kanal numarası → açık mı. Tek kanallı cihazlarda tek anahtar 0'dır.
 * [power]/[voltage]/[current]: yalnızca enerji ölçen cihazlarda (POW, S40, DualR3...) dolu,
 * W / V / A cinsinden. Ölçüm yapmayan cihazlarda null kalır ve enerji arayüzü hiç gösterilmez.
 */
data class DeviceSnapshot(
    val id: String,
    val name: String,
    val online: Boolean,
    val switches: Map<Int, Boolean>,
    val uiid: Int = 0,
    val power: Double? = null,
    val voltage: Double? = null,
    val current: Double? = null,
    /** Kanal numarası → güç (W); yalnızca kanal başına ölçen cihazlarda (SPM-4Relay, DualR3) dolu. */
    val channelPower: Map<Int, Double> = emptyMap(),
    val cloud: Cloud = Cloud.EWELINK,
    /** Tuya: kanal numarası → veri noktası kodu ("switch_1", "switch" ...); komut gönderirken gerekir. */
    val switchCodes: Map<Int, String> = emptyMap(),
) {
    val isMultiChannel: Boolean get() = switches.size > 1
    val hasEnergy: Boolean get() = power != null
    /**
     * Kanalın gücü ölçülebiliyor mu: kanal başına ölçen cihazlar (veri geldiyse ya da SPM/DualR3 gibi
     * veriyi ancak istenince gönderen bilinen modeller) ve tek kanallı güç ölçerler (POW, Tuya prizler).
     */
    val hasChannelPower: Boolean
        get() = channelPower.isNotEmpty() || uiid in CHANNEL_METERING_UIIDS || (!isMultiChannel && hasEnergy)

    /** Kanalın anlık gücü (W): kanal başına ölçen cihazda o kanalınki, tek kanallı güç ölçerde cihazın toplamı. */
    fun channelPowerOf(channel: Int): Double? =
        channelPower[channel] ?: if (!isMultiChannel && channel == 0) power else null

    companion object {
        /** DualR3 (126), SPM-4Relay (130). */
        val CHANNEL_METERING_UIIDS = setOf(126, 130)
    }
}
