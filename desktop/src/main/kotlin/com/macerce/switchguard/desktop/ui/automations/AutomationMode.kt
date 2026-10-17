package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.Automation

/**
 * Bir kaydın hangi sekmede durduğu (telefon 2.13 ile aynı ayrım): Alarm çal / Bildirim gönder işlemliler Uyarılar
 * sekmesinde "Koşullu uyarılar", Cihazı aç / kapat işlemliler Otomasyon sekmesinde. Kayıtlar yine tek listededir
 * (`Store.automations`; JSON, motor ve yedek değişmez); ayrım yalnızca ekrandadır. Düzenleyici de bu modla açılır:
 * işlem kutucukları, başlıklar ve silme sorusu moda göre.
 */
enum class AutomationMode(
    /** Düzenleyicide sunulan işlemler. */
    val actions: List<ActionKind>,
    /** Yeni kaydın işlemi: uyarıda bildirim (düzenleyicinin bugünkü varsayılanı), otomasyonda cihazı aç. */
    val defaultAction: ActionKind,
    val newTitle: String,
    val editTitle: String,
    val deleteTitle: String,
    /** Düzenleyicinin altındaki "bu bilgisayarda çalışır" notu. */
    val hint: String,
) {
    ALERT(
        listOf(ActionKind.ALARM, ActionKind.NOTIFY), ActionKind.NOTIFY,
        "alert_new", "alert_edit", "alert_delete_title", "desktop_alert_hint",
    ),
    AUTOMATION(
        listOf(ActionKind.TURN_ON, ActionKind.TURN_OFF), ActionKind.TURN_ON,
        "automation_new", "automation_edit", "automation_delete_title", "desktop_automation_hint",
    );

    companion object {
        /** İşlemin modu: her işlem tam olarak bir moddadır. */
        fun of(kind: ActionKind): AutomationMode = entries.first { kind in it.actions }

        fun of(a: Automation): AutomationMode = of(a.action.kind)
    }
}

/** Listenin [mode] sekmesine düşen kayıtları; sıra korunur. */
fun List<Automation>.inMode(mode: AutomationMode): List<Automation> = filter { AutomationMode.of(it) == mode }
