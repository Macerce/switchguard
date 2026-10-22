package com.macerce.switchguard.desktop.platform

import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.win32.StdCallLibrary

/** Bilgisayarın güç kaynağı (Windows `GetSystemPowerStatus`): prizden kesinti algılama için. */
object PowerSource {
    @Suppress("PropertyName", "unused")
    @Structure.FieldOrder("ACLineStatus", "BatteryFlag", "BatteryLifePercent", "SystemStatusFlag", "BatteryLifeTime", "BatteryFullLifeTime")
    class SystemPowerStatus : Structure() {
        @JvmField var ACLineStatus: Byte = 0
        @JvmField var BatteryFlag: Byte = 0
        @JvmField var BatteryLifePercent: Byte = 0
        @JvmField var SystemStatusFlag: Byte = 0
        @JvmField var BatteryLifeTime: Int = 0
        @JvmField var BatteryFullLifeTime: Int = 0
    }

    private interface Kernel : StdCallLibrary {
        fun GetSystemPowerStatus(status: SystemPowerStatus): Boolean
    }

    private val kernel: Kernel? by lazy { runCatching { Native.load("kernel32", Kernel::class.java) }.getOrNull() }

    /**
     * Prizde ise true, pilde ise false. Pili olmayan masaüstü bilgisayarda (her zaman prizde, kesintide zaten kapanır)
     * ya da durum okunamazsa null: o bilgisayar bu algılamayı kullanamaz.
     */
    fun onAc(): Boolean? = runCatching {
        val status = SystemPowerStatus()
        if (kernel?.GetSystemPowerStatus(status) != true) return null
        // BatteryFlag 128 = pil yok; ACLineStatus 0 = pilde, 1 = prizde, 255 = bilinmiyor.
        if (status.BatteryFlag.toInt() and 0xFF == 128) return null
        when (status.ACLineStatus.toInt() and 0xFF) {
            0 -> false
            1 -> true
            else -> null
        }
    }.getOrNull()

    /** Bu bilgisayarda pil var mı (Ayarlar'da seçenek yalnızca o zaman gösterilir). */
    val hasBattery: Boolean get() = onAc() != null
}
