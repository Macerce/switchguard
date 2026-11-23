package com.macerce.switchguard.api

import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.LocalReading

/** Yerel ağdan eklenen cihazlar için markaya göre doğru istemciyi seçer (Sonoff → [EwelinkLan], Shelly → [ShellyLan]). */
object LocalLan {
    fun reading(d: LocalDevice): LocalReading? =
        if (d.isShelly) ShellyLan.reading(d.id) else EwelinkLan.entry(d.id)?.switches?.let { LocalReading(it) }

    /** Cihazın şu an görüldüğü adres; görülmüyorsa null. */
    fun seenHost(d: LocalDevice): String? =
        if (d.isShelly) ShellyLan.reading(d.id)?.let { ShellyLan.host(d) } else EwelinkLan.entry(d.id)?.host

    /** Bloklayıcı; ana thread'den çağırmayın. [local] yoksa Sonoff sayılır (eski kayıtlar). */
    fun trySwitch(device: DeviceSnapshot, local: LocalDevice?, channel: Int, on: Boolean): Boolean =
        if (local?.isShelly == true) ShellyLan.trySwitch(local, channel, on) else EwelinkLan.trySwitch(device, channel, on)
}
