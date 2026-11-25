package com.macerce.switchguard.core

/**
 * Hazır kural şablonları (Home Assistant'ın "blueprint"leri gibi): sık kurulan kurallar tek dokunuşla doldurulur,
 * kullanıcı düzenleyicide cihazı ve sayıları değiştirip kaydeder. Saf; adlar arayüzde çevrilir.
 */
enum class AutomationTemplate(
    /** Yalnız enerji ölçen cihazlarda anlamlı. */
    val needsEnergy: Boolean = false,
    /** Tetikleyici cihaz ister (saat ve gün batımı istemez, eylem cihazı ister). */
    val needsDevice: Boolean = true,
) {
    /** Unutulan cihaz: 60 dk açık kalırsa kendiliğinden kapat. */
    FORGOTTEN_OFF,
    /** 2 saattir açık: bildirim (bildirimde "Kapat" düğmesi çıkar). */
    ON_TOO_LONG_NOTIFY,
    /** Cihaz çevrimdışı oldu (elektrik/Wi-Fi gitti): alarm. */
    OFFLINE_ALARM,
    /** Makine bitti: güç 5 dk boyunca 5 W altında. */
    MACHINE_DONE(needsEnergy = true),
    /** Dondurucu/pompa durdu: güç 4 saattir 5 W altında. */
    APPLIANCE_STOPPED(needsEnergy = true),
    /** Yüksek tüketim: güç 2000 W üstünde. */
    HIGH_POWER(needsEnergy = true),
    /** Güç 2000 W'ı aşınca durumu hatırla, cihazı 15 dk kapat, sonra eski hâline getir. */
    PAUSE_AND_RESTORE(needsEnergy = true),
    /** Gün batımında aç. */
    SUNSET_ON(needsDevice = false),
    /** Her gece 23:30'da kapat. */
    NIGHT_OFF(needsDevice = false),
    /** Hafta içi 07:00'de aç. */
    MORNING_ON(needsDevice = false);

    /** Uygun cihaz: enerji gerekiyorsa ölçen, değilse açılıp kapatılabilen ilk cihaz. */
    fun pickDevice(devices: List<DeviceSnapshot>, preferred: String? = null): DeviceSnapshot? {
        val ok = devices.filter { !it.sensorOnly && (!needsEnergy || it.hasEnergy) }
        return ok.firstOrNull { it.id == preferred } ?: ok.firstOrNull()
    }

    /** [device] için doldurulmuş kural (kimlik boş: düzenleyici kaydederken verir). */
    fun build(device: DeviceSnapshot, name: String): Automation {
        val id = device.id
        val base = Automation("", name, true, Trigger(TriggerKind.TURNED_ON, id), AutoAction(ActionKind.NOTIFY))
        return when (this) {
            FORGOTTEN_OFF -> base.copy(trigger = Trigger(TriggerKind.STAYED_ON, id, minutes = 60), action = AutoAction(ActionKind.TURN_OFF, id))
            ON_TOO_LONG_NOTIFY -> base.copy(trigger = Trigger(TriggerKind.STAYED_ON, id, minutes = 120), action = AutoAction(ActionKind.NOTIFY))
            OFFLINE_ALARM -> base.copy(trigger = Trigger(TriggerKind.WENT_OFFLINE, id), action = AutoAction(ActionKind.ALARM))
            MACHINE_DONE -> base.copy(trigger = Trigger(TriggerKind.POWER_BELOW, id, minutes = 5, watts = 5.0), action = AutoAction(ActionKind.NOTIFY))
            APPLIANCE_STOPPED -> base.copy(trigger = Trigger(TriggerKind.POWER_BELOW, id, minutes = 240, watts = 5.0), action = AutoAction(ActionKind.ALARM))
            HIGH_POWER -> base.copy(trigger = Trigger(TriggerKind.POWER_ABOVE, id, watts = 2000.0), action = AutoAction(ActionKind.ALARM))
            PAUSE_AND_RESTORE -> base.copy(
                trigger = Trigger(TriggerKind.POWER_ABOVE, id, watts = 2000.0),
                action = AutoAction(ActionKind.SAVE_STATE, id),
                extraActions = listOf(AutoAction(ActionKind.TURN_OFF, id), AutoAction(ActionKind.RESTORE_STATE, waitMinutes = 15)),
            )
            SUNSET_ON -> base.copy(trigger = Trigger(TriggerKind.SUNSET, ""), action = AutoAction(ActionKind.TURN_ON, id))
            NIGHT_OFF -> base.copy(trigger = Trigger(TriggerKind.TIME, "", minutes = 23 * 60 + 30), action = AutoAction(ActionKind.TURN_OFF, id))
            MORNING_ON -> base.copy(trigger = Trigger(TriggerKind.TIME, "", minutes = 7 * 60, days = WEEKDAYS), action = AutoAction(ActionKind.TURN_ON, id))
        }
    }

    companion object {
        /** Pazartesi–cuma (bit 0 = pazartesi). */
        const val WEEKDAYS = 0b0011111
    }
}
