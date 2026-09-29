package com.macerce.ewelinkalarm.core

/**
 * Bir cihazın belirli bir andaki durumu.
 * [switches]: kanal numarası → açık mı. Tek kanallı cihazlarda tek anahtar 0'dır.
 */
data class DeviceSnapshot(
    val id: String,
    val name: String,
    val online: Boolean,
    val switches: Map<Int, Boolean>,
) {
    val isMultiChannel: Boolean get() = switches.size > 1
}
