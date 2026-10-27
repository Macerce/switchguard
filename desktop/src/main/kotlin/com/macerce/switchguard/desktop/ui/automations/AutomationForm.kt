package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.Condition
import com.macerce.switchguard.core.ConditionKind
import com.macerce.switchguard.core.PresenceRule
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind

/**
 * Otomasyon düzenleyicisinin durumu ve kuralları; telefondaki `AutomationEditor` ile aynı varsayılanlar, olay süzme,
 * doğrulama ve kaydedilecek [Automation]'ı kurma. Sayı alanları kullanıcının yazdığı metin olarak tutulur.
 * Arayüzden bağımsızdır (test edilir); ekran yalnızca alanları değiştirir.
 */
data class AutomationForm(
    val name: String = "",
    val triggerDevice: String = "",
    val triggerKind: TriggerKind = TriggerKind.TURNED_ON,
    val triggerChannel: Int = 0,
    val minutes: String = "30",
    val watts: String = "1000",
    /** [TriggerKind.CHANNEL_POWER_LOW]: eşiğin altında kalma süresi (dk). */
    val lowMinutes: String = "1",
    /** [TriggerKind.CHANNEL_POWER_LOW]: açılıştan sonra güce bakılmayan süre (dk). */
    val grace: String = "2",
    val actionKind: ActionKind = ActionKind.NOTIFY,
    val actionDevice: String = "",
    val actionChannel: Int = 0,
    val delay: String = "0",
    /** Telefon 2.15: gün doğumu/batımı kayması (dk, eksi = önce). */
    val offset: Int = 0,
    /** Dış sıcaklık eşiği (°C, metin). */
    val temp: String = "5",
    /** Süreli güç tetikleyicisi: eşiğin üstünde/altında en az bu kadar dakika; 0 = hemen. */
    val holdMinutes: Int = 0,
    /** Yalnızca şu saatlerde / günlerde. */
    val windowOn: Boolean = false,
    val windowStart: Int = 22 * 60,
    val windowEnd: Int = 6 * 60,
    val windowDays: Int = ActiveWindow.ALL_DAYS,
    /** Yalnızca evdeyken / dışarıdayken; null = her zaman. */
    val presence: PresenceRule? = null,
    /** Ve başka bir cihaz şu durumdaysa. */
    val condOn: Boolean = false,
    val condKind: ConditionKind = ConditionKind.IS_ON,
    val condDevice: String = "",
    val condChannel: Int = 0,
    /** [ActionKind.RUN_SCENE]: çalıştırılacak sahne. */
    val sceneId: String = "",
) {
    /** Seçili cihaz bu olayı bildiremiyorsa (ör. güç ölçmüyor) olay "açıldı"ya döner. */
    fun normalized(devices: List<DeviceSnapshot>): AutomationForm =
        if (triggerKind in kindsFor(devices.firstOrNull { it.id == triggerDevice })) this else copy(triggerKind = TriggerKind.TURNED_ON)

    fun isValid(devices: List<DeviceSnapshot>, scenes: List<Scene> = emptyList()): Boolean {
        val src = devices.firstOrNull { it.id == triggerDevice }
        val minutesValue = minutes.toIntOrNull()
        val wattsValue = wattsValue()
        val delayValue = delay.toIntOrNull()
        val lowValue = lowMinutes.toIntOrNull()
        val graceValue = grace.toIntOrNull()
        return name.isNotBlank() && (!triggerKind.usesDevice || (src != null && triggerKind in kindsFor(src))) &&
            (!triggerKind.usesTemp || tempValue() != null) &&
            (!condOn || (devices.any { it.id == condDevice } && !DeviceCategory.isCategory(condDevice))) &&
            (actionKind != ActionKind.RUN_SCENE || scenes.any { it.id == sceneId }) &&
            (!triggerKind.usesChannelPower || (lowValue != null && lowValue >= 0 && graceValue != null && graceValue >= 0)) &&
            (!triggerKind.usesMinutes || (minutesValue != null && minutesValue > 0)) &&
            (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
            (!actionKind.usesDevice || (devices.any { it.id == actionDevice } && !DeviceCategory.isCategory(actionDevice))) &&
            delayValue != null && delayValue >= 0
    }

    /** Kaydedilecek otomasyon ([isValid] doğruyken çağrılır); kullanılmayan alanlar sıfırlanır. */
    fun build(id: String, enabled: Boolean): Automation = Automation(
        id = id,
        name = name.trim(),
        enabled = enabled,
        trigger = Trigger(
            triggerKind, if (triggerKind.usesDevice) triggerDevice else "",
            channel = if (triggerKind.usesChannel) triggerChannel else 0,
            minutes = when {
                triggerKind.usesMinutes -> minutes.toIntOrNull() ?: 0
                triggerKind.usesChannelPower -> lowMinutes.toIntOrNull() ?: 0
                triggerKind.allowsHold -> holdMinutes
                triggerKind.isSun -> offset
                else -> 0
            },
            watts = when {
                triggerKind.usesWatts -> wattsValue() ?: 0.0
                triggerKind.usesTemp -> tempValue() ?: 0.0
                else -> 0.0
            },
            graceMinutes = if (triggerKind.usesChannelPower) grace.toIntOrNull() ?: 0 else 0,
        ),
        action = when {
            actionKind.usesDevice -> AutoAction(actionKind, actionDevice, actionChannel)
            actionKind == ActionKind.RUN_SCENE -> AutoAction(actionKind, sceneId)
            else -> AutoAction(actionKind)
        },
        delayMinutes = delay.toIntOrNull() ?: 0,
        window = if (windowOn) ActiveWindow(windowStart, windowEnd, windowDays) else null,
        condition = if (condOn) Condition(condKind, condDevice, if (condKind.usesChannel) condChannel else 0) else null,
        presence = presence,
    )

    /** Sıcaklık eşiği (eksi olabilir). */
    private fun tempValue(): Double? = temp.trim().replace(',', '.').toDoubleOrNull()

    /** Türkçe klavyedeki virgül de kabul edilir ("1,5"). */
    private fun wattsValue(): Double? = watts.trim().replace(',', '.').toDoubleOrNull()

    /** İşlem bu modda sunulmuyorsa modun varsayılan işlemine döner (düzenleyici yalnızca modun işlemlerini gösterir). */
    fun inMode(mode: AutomationMode): AutomationForm = if (actionKind in mode.actions) this else copy(actionKind = mode.defaultAction)

    companion object {
        /**
         * Düzenleyicinin ilk durumu. Var olan kayıt olduğu gibi gelir (modu işleminden belli); yeni kayıt [mode]'un
         * varsayılan işlemiyle başlar.
         */
        fun from(initial: Automation?, devices: List<DeviceSnapshot>, mode: AutomationMode = AutomationMode.ALERT): AutomationForm {
            val first = devices.firstOrNull()?.id.orEmpty()
            if (initial == null) return AutomationForm(triggerDevice = first, actionKind = mode.defaultAction, actionDevice = first, condDevice = first)
            val t = initial.trigger
            return AutomationForm(
                name = initial.name,
                triggerDevice = t.deviceId,
                triggerKind = t.kind,
                triggerChannel = t.channel,
                minutes = if (t.kind.usesMinutes && t.minutes > 0) t.minutes.toString() else "30",
                watts = t.watts.takeIf { it > 0 }?.let(::formatNumber) ?: "1000",
                lowMinutes = if (t.kind.usesChannelPower) t.minutes.toString() else "1",
                // Kayıtlı kanal gücü uyarısının bekleme süresi olduğu gibi gelir: 0 (beklemeden bak) geçerli bir değerdir,
                // varsayılana (2) çevrilirse açıp kaydetmek uyarının davranışını sessizce değiştirirdi.
                grace = if (t.kind.usesChannelPower) t.graceMinutes.toString() else "2",
                actionKind = initial.action.kind,
                actionDevice = initial.action.deviceId.ifEmpty { first },
                actionChannel = initial.action.channel,
                delay = initial.delayMinutes.toString(),
                offset = if (t.kind.isSun) t.minutes else 0,
                temp = if (t.kind.usesTemp) formatNumber(t.watts) else "5",
                holdMinutes = if (t.kind.allowsHold) t.minutes else 0,
                windowOn = initial.window != null,
                windowStart = initial.window?.startMinute ?: 22 * 60,
                windowEnd = initial.window?.endMinute ?: 6 * 60,
                windowDays = initial.window?.days ?: ActiveWindow.ALL_DAYS,
                presence = initial.presence,
                condOn = initial.condition != null,
                condKind = initial.condition?.kind ?: ConditionKind.IS_ON,
                condDevice = initial.condition?.deviceId ?: first,
                condChannel = initial.condition?.channel ?: 0,
                sceneId = if (initial.action.kind == ActionKind.RUN_SCENE) initial.action.deviceId else "",
            )
        }

        /** Güç olayları yalnızca enerji ölçen, kanal gücü olayı kanal başına ölçen cihazlarda sunulur. */
        fun kindsFor(device: DeviceSnapshot?): List<TriggerKind> = TriggerKind.entries.filter {
            when {
                // Kategori: yalnız anahtar ve bağlantı olayları (gün doğumu/sıcaklık cihazsız, yine sunulur).
                device != null && DeviceCategory.isCategory(device.id) -> !it.usesDevice || it in DeviceCategory.TRIGGER_KINDS
                it.usesChannelPower -> device?.hasChannelPower == true
                it.usesWatts -> device?.hasEnergy == true
                else -> true
            }
        }

        private fun formatNumber(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
    }
}

/**
 * Düzenleyicideki − / + adımlayıcılarının değerleri (telefondaki 2.12 ile aynı): düğme bir sonraki anlamlı değere
 * atlar (30 → 45 → 60 …). Listede olmayan bir değer (eski bir uyarıdan, ör. 7 dk) olduğu gibi gösterilir; düğmeler
 * ondan en yakın alt/üst adıma gider.
 */
internal object Steps {
    /** "Açık/kapalı kalınca" süresi: 0 olamaz. */
    val MINUTES = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440)
    /** İşlemden önce bekleme: 0 = hemen. */
    val DELAY = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120)
    /** Süreli güç tetikleyicisi: 0 = hemen. */
    val HOLD = listOf(0) + MINUTES
    /** Gün doğumu/batımı kayması: eksi önce, artı sonra. */
    val OFFSET = listOf(-240, -180, -120, -90, -60, -45, -30, -20, -15, -10, -5, 0, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240)
    /** Kanal gücü düşük kalma süresi ve açılıştan sonra güce bakılmayan süre. */
    val LOW = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
    val GRACE = LOW

    /** − düğmesinin gideceği değer; daha küçük adım yoksa null (düğme pasif). */
    fun lower(value: Int, steps: List<Int>): Int? = steps.lastOrNull { it < value }

    /** + düğmesinin gideceği değer; daha büyük adım yoksa null (düğme pasif). */
    fun higher(value: Int, steps: List<Int>): Int? = steps.firstOrNull { it > value }
}

/** Koşulun kanalı var mı (açık/kapalı); bağlantı koşulları cihazın tamamına bakar. */
internal val ConditionKind.usesChannel: Boolean get() = this == ConditionKind.IS_ON || this == ConditionKind.IS_OFF
