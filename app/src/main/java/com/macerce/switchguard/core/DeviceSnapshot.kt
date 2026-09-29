package com.macerce.switchguard.core

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
) {
    val isMultiChannel: Boolean get() = switches.size > 1
    val hasEnergy: Boolean get() = power != null
}
