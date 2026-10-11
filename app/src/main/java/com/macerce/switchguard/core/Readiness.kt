package com.macerce.switchguard.core

/** Alarmın kullanıcıya ulaşmasını engelleyebilecek telefon ayarları (önem sırasıyla). */
enum class ReadinessIssue {
    NOTIFICATIONS_OFF,
    ALARM_CHANNEL_MUTED,
    DND_TOTAL_SILENCE,
    ALARM_VOLUME_MUTED,
    BATTERY_RESTRICTED,
    FULL_SCREEN_DENIED,
    OVERLAY_DENIED,
    AUTOSTART_UNCONFIRMED,
}

/** Telefonun o anki durumu. null alanlar bu cihazda geçerli değil (ör. eski Android, TV). */
data class ReadinessInput(
    val notificationsEnabled: Boolean,
    /** "Alarm" bildirim kategorisi yüksek önemde mi (kullanıcı kısmamış). */
    val alarmChannelHigh: Boolean,
    /** Rahatsız Etmeyin "tamamen sessiz": alarm sesleri de susar. */
    val dndTotalSilence: Boolean,
    /** Telefonun alarm ses seviyesi. */
    val alarmVolume: Int,
    /** Alarm çalarken ses en yükseğe çıkarılıyor mu (bkz. [AlarmVolume]). */
    val alarmBoost: Boolean,
    val batteryExempt: Boolean,
    val fullScreenAllowed: Boolean?,
    val overlayAllowed: Boolean?,
    /** Üretici "otomatik başlatma" ayarı okunamaz; kullanıcı yaptığını onaylar. */
    val autostartConfirmed: Boolean?,
)

/** "Alarm hazır mı?" kontrolü: ana ekrandaki kart bu listeyi gösterir. */
object Readiness {
    fun issues(i: ReadinessInput): List<ReadinessIssue> = buildList {
        if (!i.notificationsEnabled) add(ReadinessIssue.NOTIFICATIONS_OFF)
        else if (!i.alarmChannelHigh) add(ReadinessIssue.ALARM_CHANNEL_MUTED)
        if (i.dndTotalSilence) add(ReadinessIssue.DND_TOTAL_SILENCE)
        if (i.alarmVolume <= 0 && !i.alarmBoost) add(ReadinessIssue.ALARM_VOLUME_MUTED)
        if (!i.batteryExempt) add(ReadinessIssue.BATTERY_RESTRICTED)
        if (i.fullScreenAllowed == false) add(ReadinessIssue.FULL_SCREEN_DENIED)
        if (i.overlayAllowed == false) add(ReadinessIssue.OVERLAY_DENIED)
        if (i.autostartConfirmed == false) add(ReadinessIssue.AUTOSTART_UNCONFIRMED)
    }
}
