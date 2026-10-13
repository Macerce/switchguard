package com.macerce.switchguard.core

/** Alarmın kullanıcıya ulaşmasını engelleyebilecek telefon ayarları (önem sırasıyla). */
enum class ReadinessIssue {
    /** Bu, uygulamanın ikiz kopyası (bkz. [TwinApp]). İkiz, diğer sorunların da kaynağı olabileceğinden en başta. */
    TWIN_THIS_COPY,
    /** Telefonda bu uygulamanın başka bir kopyası (ikiz, iş profili) daha var. */
    TWIN_OTHER_COPY,
    NOTIFICATIONS_OFF,
    ALARM_CHANNEL_MUTED,
    DND_TOTAL_SILENCE,
    ALARM_VOLUME_LOW,
    BATTERY_RESTRICTED,
    FULL_SCREEN_DENIED,
    LOCK_SCREEN_POPUP_DENIED,
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
    /** Telefonun alarm ses seviyesi ve en yüksek değeri. */
    val alarmVolume: Int,
    val alarmVolumeMax: Int,
    /** Alarm çalarken ses en yükseğe çıkarılıyor mu (bkz. [AlarmVolume]). */
    val alarmBoost: Boolean,
    val batteryExempt: Boolean,
    val fullScreenAllowed: Boolean?,
    val overlayAllowed: Boolean?,
    /**
     * Xiaomi'nin "Kilit ekranında görüntüle" ve "Arka planda çalışırken yeni pencereler açın" izinleri. HyperOS'ta
     * tam ekran bildirim izni de bunlara bağlı: kapalıysa alarm ekranı kilitliyken açılmaz.
     */
    val lockScreenPopupAllowed: Boolean?,
    /** Üretici "otomatik başlatma" ayarı: Xiaomi'de okunur, diğer markalarda kullanıcı yaptığını onaylar. */
    val autostartConfirmed: Boolean?,
    /** Bu kopya ikiz mi ([TwinApp.isTwin]). */
    val runningAsTwin: Boolean = false,
    /** Başka bir profilde (ikiz, iş profili) bu uygulamanın bir kopyası daha kurulu mu. */
    val otherCopyInstalled: Boolean = false,
    /** Kullanıcı iki kopyayı bilerek kullandığını söyledi (ör. her kopyada ayrı eWeLink hesabı). */
    val twinAcknowledged: Boolean = false,
)

/** "Alarm hazır mı?" kontrolü: ana ekrandaki kart bu listeyi gösterir. */
object Readiness {
    /** İzleme kapalıyken de gösterilenler: ikiz kopya izlemeyi açmadan önce de (giriş yaparken) sorun çıkarır. */
    val SHOWN_WHEN_STOPPED = setOf(ReadinessIssue.TWIN_THIS_COPY, ReadinessIssue.TWIN_OTHER_COPY)

    fun issues(i: ReadinessInput): List<ReadinessIssue> = buildList {
        if (!i.twinAcknowledged) {
            if (i.runningAsTwin) add(ReadinessIssue.TWIN_THIS_COPY)
            else if (i.otherCopyInstalled) add(ReadinessIssue.TWIN_OTHER_COPY)
        }
        if (!i.notificationsEnabled) add(ReadinessIssue.NOTIFICATIONS_OFF)
        else if (!i.alarmChannelHigh) add(ReadinessIssue.ALARM_CHANNEL_MUTED)
        if (i.dndTotalSilence) add(ReadinessIssue.DND_TOTAL_SILENCE)
        // Yeni Android'lerde alarm sesi 0 olamaz ama en düşük seviye de neredeyse duyulmaz: %20'nin altı kısık sayılır.
        if (!i.alarmBoost && (i.alarmVolume <= 0 || i.alarmVolume * 5 < i.alarmVolumeMax)) add(ReadinessIssue.ALARM_VOLUME_LOW)
        if (!i.batteryExempt) add(ReadinessIssue.BATTERY_RESTRICTED)
        if (i.fullScreenAllowed == false) add(ReadinessIssue.FULL_SCREEN_DENIED)
        if (i.lockScreenPopupAllowed == false) add(ReadinessIssue.LOCK_SCREEN_POPUP_DENIED)
        if (i.overlayAllowed == false) add(ReadinessIssue.OVERLAY_DENIED)
        if (i.autostartConfirmed == false) add(ReadinessIssue.AUTOSTART_UNCONFIRMED)
    }
}
