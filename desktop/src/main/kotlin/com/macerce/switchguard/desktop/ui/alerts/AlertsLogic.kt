package com.macerce.switchguard.desktop.ui.alerts

import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.i18n.Strings

// Uyarılar sekmesinin arayüzsüz mantığı (telefon 2.13 ile aynı kurallar): cihaz uyarıları satırının özeti ve durum
// kartının hali. Ekran yalnızca bunları çizer; testler doğrudan buraya bakar.

/** "Cihaz uyarıları" satırında bir cihazın özeti. */
sealed interface DeviceAlertSummary {
    /** Cihaz izlenmiyor: hiçbir değişiklik alarm ya da bildirim vermez. */
    data object NotMonitored : DeviceAlertSummary

    /**
     * Cihaz izleniyor: alarm ve bildirim verilen olaylar ([EventType] sırasıyla). İkisi de boşsa olaylar yalnızca
     * geçmişe yazılır.
     */
    data class Monitored(val alarm: List<EventType>, val notify: List<EventType>) : DeviceAlertSummary
}

fun deviceAlertSummary(rules: DeviceRules): DeviceAlertSummary =
    if (!rules.monitored) {
        DeviceAlertSummary.NotMonitored
    } else {
        DeviceAlertSummary.Monitored(
            alarm = EventType.entries.filter { rules.actionFor(it) == AlertAction.ALARM },
            notify = EventType.entries.filter { rules.actionFor(it) == AlertAction.NOTIFY },
        )
    }

/** Satırın bir metni; [action] alarm ya da bildirim satırıysa onu söyler (ekranda renkli nokta), özet satırında null. */
data class DeviceAlertLine(val text: String, val action: AlertAction?)

/**
 * Satırın metinleri: "İzlenmiyor", "İzleniyor · alarm ya da bildirim yok" ya da "Alarm: Kapanınca, Açılınca" ve ayrı
 * satırda "Bildirim: Çevrimiçi olunca" (olayların kısa adları, virgülle).
 */
fun deviceAlertLines(rules: DeviceRules, strings: Strings): List<DeviceAlertLine> = when (val s = deviceAlertSummary(rules)) {
    DeviceAlertSummary.NotMonitored -> listOf(DeviceAlertLine(strings["device_alerts_not_monitored"], null))
    is DeviceAlertSummary.Monitored ->
        if (s.alarm.isEmpty() && s.notify.isEmpty()) {
            listOf(DeviceAlertLine(strings["device_alerts_none"], null))
        } else {
            listOfNotNull(
                s.alarm.takeIf { it.isNotEmpty() }
                    ?.let { DeviceAlertLine(strings.format("device_alerts_alarm", names(it, strings)), AlertAction.ALARM) },
                s.notify.takeIf { it.isNotEmpty() }
                    ?.let { DeviceAlertLine(strings.format("device_alerts_notify", names(it, strings)), AlertAction.NOTIFY) },
            )
        }
}

private fun names(types: List<EventType>, strings: Strings): String = types.joinToString(", ") { strings[it.shortKey()] }

/** Olayın kısa adı (düzenleyicinin olay kutucuklarıyla aynı: "Kapanınca"). */
internal fun EventType.shortKey(): String = when (this) {
    EventType.TURNED_OFF -> "evt_turned_off"
    EventType.TURNED_ON -> "evt_turned_on"
    EventType.WENT_OFFLINE -> "evt_went_offline"
    EventType.CAME_ONLINE -> "evt_came_online"
}

/** Durum kartının hali. */
enum class AlertReadiness {
    /** İzleme kapalı: uyarılar çalışmaz, kartta "İzlemeyi başlat". */
    MONITORING_OFF,

    /** İzleme açık ama alarmın ulaşmasını engelleyebilecek sorun var (masaüstünde Windows ayarları): turuncu uyarı. */
    WARNING,

    /** "Alarm hazır". */
    READY,
}

/** [issues]: alarmın ulaşmasını engelleyen sorunların metin anahtarları (bkz. `settings.windowsIssues`). */
fun alertReadiness(monitoring: Boolean, issues: List<String>): AlertReadiness = when {
    !monitoring -> AlertReadiness.MONITORING_OFF
    issues.isNotEmpty() -> AlertReadiness.WARNING
    else -> AlertReadiness.READY
}
