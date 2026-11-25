package com.macerce.switchguard.api

import android.content.Context
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading
import com.macerce.switchguard.core.ShellyAuth

/**
 * Yerel ağdan eklenen cihazlar için markaya göre doğru istemciyi seçer: Sonoff → [EwelinkLan], Shelly → [ShellyLan],
 * diğerleri [drivers] ([PolledLan]).
 */
object LocalLan {
    /** Yoklamalı markalar; yeni marka buraya eklenir. */
    val drivers: Map<String, PolledLan> by lazy {
        listOf<PolledLan>(TasmotaLan, EsphomeLan, HueLan, KasaLan, TapoLan, HaLan).associateBy { it.brand }
    }

    fun driver(brand: String): PolledLan? = drivers[brand]

    fun reading(d: LocalDevice): LocalReading? = when {
        d.isShelly -> ShellyLan.reading(d.id)
        d.isSonoff -> EwelinkLan.entry(d.id)?.switches?.let { LocalReading(it) }
        else -> drivers[d.brand]?.reading(d.id)
    }

    /** Cihazın şu an görüldüğü adres; görülmüyorsa null. */
    fun seenHost(d: LocalDevice): String? = when {
        d.isShelly -> ShellyLan.reading(d.id)?.let { ShellyLan.host(d) }
        d.isSonoff -> EwelinkLan.entry(d.id)?.host
        else -> drivers[d.brand]?.let { dr -> dr.reading(d.id)?.let { dr.seenHost(d) } }
    }

    /** Bloklayıcı; ana thread'den çağırmayın. [local] yoksa Sonoff sayılır (eski kayıtlar). */
    fun trySwitch(device: DeviceSnapshot, local: LocalDevice?, channel: Int, on: Boolean): Boolean = when {
        local == null || local.isSonoff -> EwelinkLan.trySwitch(device, channel, on)
        local.isShelly -> ShellyLan.trySwitch(local, channel, on)
        else -> drivers[local.brand]?.trySwitch(local, channel, on) == true
    }

    /** Işık parlaklığı/rengi; Sonoff ve Shelly'de yok. */
    fun trySetLevel(local: LocalDevice?, channel: Int, level: Int?, color: Int?): Boolean =
        local != null && drivers[local.brand]?.trySetLevel(local, channel, level, color) == true

    /** İzleme: her yoklamalı markayı kendi cihazlarıyla başlatır, cihazı olmayanı durdurur. */
    fun startDrivers(context: Context, devices: List<LocalDevice>, auth: Map<String, ShellyAuth.Credentials>, listener: () -> Unit) {
        drivers.values.forEach { dr ->
            val mine = devices.filter { it.brand == dr.brand }
            if (mine.isEmpty()) dr.stop() else {
                dr.setCredentials(auth)
                dr.start(context, mine, listener)
            }
        }
    }

    fun stopDrivers() = drivers.values.forEach { it.stop() }
}
