package com.macerce.switchguard.core

/**
 * Otomasyonların ekrandaki yeri. Kayıtlar tek listede durur (JSON, motor ve yedek aynı); ekranda eyleme göre ayrılır:
 * alarm çalan ya da bildirim gönderenler Uyarılar sekmesinde "Koşullu uyarılar", cihaz açıp kapatanlar Otomasyon sekmesinde.
 */
enum class AutomationTab(
    /** Bu sekmenin düzenleyicisinde sunulan işlemler (sırasıyla). */
    val actions: List<ActionKind>,
) {
    ALERTS(listOf(ActionKind.ALARM, ActionKind.NOTIFY)),
    AUTOMATIONS(listOf(ActionKind.TURN_ON, ActionKind.TURN_OFF, ActionKind.RUN_SCENE));

    /** Bu sekmedeki kayıtlar, listedeki sırayla. */
    fun of(list: List<Automation>): List<Automation> = list.filter { tabOf(it) == this }

    companion object {
        fun tabOf(a: Automation): AutomationTab = tabOf(a.action.kind)

        fun tabOf(kind: ActionKind): AutomationTab = when (kind) {
            ActionKind.ALARM, ActionKind.NOTIFY -> ALERTS
            ActionKind.TURN_ON, ActionKind.TURN_OFF, ActionKind.RUN_SCENE -> AUTOMATIONS
        }
    }
}

/**
 * Uyarılar sekmesindeki cihaz satırının özeti: izleniyor mu, hangi olaylarda alarm, hangilerinde bildirim.
 * Olaylar [EventType] sırasıyla (kapandı, açıldı, çevrimdışı, çevrimiçi) listelenir.
 */
data class DeviceAlertSummary(
    /** Gerçekten izleniyor mu (ücretsiz sürüm sınırı dahil, bkz. Store.monitoredIds). */
    val monitored: Boolean,
    val alarm: List<EventType>,
    val notify: List<EventType>,
    val category: DeviceCategory,
    /** Cihaza özel ses seçilmiş. */
    val customSound: Boolean,
) {
    /** Satırın metinleri: izlenmiyorsa ya da hiçbir olay uyarı vermiyorsa tek satır, yoksa alarm ve bildirim ayrı satırda. */
    fun lines(t: Texts): List<String> = when {
        !monitored -> listOf(t.notMonitored)
        alarm.isEmpty() && notify.isEmpty() -> listOf(t.none)
        else -> buildList {
            if (alarm.isNotEmpty()) add(t.alarm(alarm.joinToString(", ") { t.event(it) }))
            if (notify.isNotEmpty()) add(t.notify(notify.joinToString(", ") { t.event(it) }))
        }
    }

    /** Dile bağlı metinler (arayüz kaynaklardan doldurur; testler düz metinle). */
    class Texts(
        val notMonitored: String,
        val none: String,
        /** "Alarm: %s" */
        val alarm: (String) -> String,
        /** "Bildirim: %s" */
        val notify: (String) -> String,
        /** Olayın kısa adı ("Kapanınca"). */
        val event: (EventType) -> String,
    )

    companion object {
        fun of(rules: DeviceRules, monitored: Boolean): DeviceAlertSummary = DeviceAlertSummary(
            monitored = monitored,
            alarm = EventType.entries.filter { rules.actionFor(it) == AlertAction.ALARM },
            notify = EventType.entries.filter { rules.actionFor(it) == AlertAction.NOTIFY },
            category = rules.effectiveCategory,
            customSound = rules.soundUri != null,
        )
    }
}
