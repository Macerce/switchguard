package com.macerce.switchguard.core

/**
 * Yerel ağda bulunan, eklenmeye hazır bir cihaz (Tasmota, ESPHome, Hue, Kasa, Tapo, Home Assistant ...).
 * [extra]: markaya özel kalıcı bilgi ([LocalDevice.extra]'ya geçer; ör. ESPHome varlık yolları, Hue lamba kimliği).
 * [locked]: şifre gerekiyor; [credentials]: doğrulanmış giriş bilgisi (eklenince gizli depoya yazılır).
 */
data class LanFound(
    val id: String,
    val brand: String,
    val host: String,
    val model: String,
    val channels: Int,
    val supported: Boolean,
    val locked: Boolean = false,
    val wrongPassword: Boolean = false,
    val credentials: ShellyAuth.Credentials? = null,
    val extra: String = "",
    val name: String? = null,
) {
    fun toDevice(name: String, ssid: String?, now: Long = System.currentTimeMillis()) =
        LocalDevice(id, name, channels.coerceIn(1, 4), ssid, host, now, brand, 0, extra)
}
