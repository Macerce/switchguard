package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.ActionKind
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
) {
    /** Seçili cihaz bu olayı bildiremiyorsa (ör. güç ölçmüyor) olay "açıldı"ya döner. */
    fun normalized(devices: List<DeviceSnapshot>): AutomationForm =
        if (triggerKind in kindsFor(devices.firstOrNull { it.id == triggerDevice })) this else copy(triggerKind = TriggerKind.TURNED_ON)

    fun isValid(devices: List<DeviceSnapshot>): Boolean {
        val src = devices.firstOrNull { it.id == triggerDevice }
        val minutesValue = minutes.toIntOrNull()
        val wattsValue = wattsValue()
        val delayValue = delay.toIntOrNull()
        val lowValue = lowMinutes.toIntOrNull()
        val graceValue = grace.toIntOrNull()
        return name.isNotBlank() && src != null && triggerKind in kindsFor(src) &&
            (!triggerKind.usesChannelPower || (lowValue != null && lowValue >= 0 && graceValue != null && graceValue >= 0)) &&
            (!triggerKind.usesMinutes || (minutesValue != null && minutesValue > 0)) &&
            (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
            (!actionKind.usesDevice || devices.any { it.id == actionDevice }) &&
            delayValue != null && delayValue >= 0
    }

    /** Kaydedilecek otomasyon ([isValid] doğruyken çağrılır); kullanılmayan alanlar sıfırlanır. */
    fun build(id: String, enabled: Boolean): Automation = Automation(
        id = id,
        name = name.trim(),
        enabled = enabled,
        trigger = Trigger(
            triggerKind, triggerDevice,
            channel = if (triggerKind.usesChannel) triggerChannel else 0,
            minutes = when {
                triggerKind.usesMinutes -> minutes.toIntOrNull() ?: 0
                triggerKind.usesChannelPower -> lowMinutes.toIntOrNull() ?: 0
                else -> 0
            },
            watts = if (triggerKind.usesWatts) wattsValue() ?: 0.0 else 0.0,
            graceMinutes = if (triggerKind.usesChannelPower) grace.toIntOrNull() ?: 0 else 0,
        ),
        action = if (actionKind.usesDevice) AutoAction(actionKind, actionDevice, actionChannel) else AutoAction(actionKind),
        delayMinutes = delay.toIntOrNull() ?: 0,
    )

    /** Türkçe klavyedeki virgül de kabul edilir ("1,5"). */
    private fun wattsValue(): Double? = watts.trim().replace(',', '.').toDoubleOrNull()

    companion object {
        fun from(initial: Automation?, devices: List<DeviceSnapshot>): AutomationForm {
            val first = devices.firstOrNull()?.id.orEmpty()
            if (initial == null) return AutomationForm(triggerDevice = first, actionDevice = first)
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
            )
        }

        /** Güç olayları yalnızca enerji ölçen, kanal gücü olayı kanal başına ölçen cihazlarda sunulur. */
        fun kindsFor(device: DeviceSnapshot?): List<TriggerKind> = TriggerKind.entries.filter {
            when {
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
    /** Kanal gücü düşük kalma süresi ve açılıştan sonra güce bakılmayan süre. */
    val LOW = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
    val GRACE = LOW

    /** − düğmesinin gideceği değer; daha küçük adım yoksa null (düğme pasif). */
    fun lower(value: Int, steps: List<Int>): Int? = steps.lastOrNull { it < value }

    /** + düğmesinin gideceği değer; daha büyük adım yoksa null (düğme pasif). */
    fun higher(value: Int, steps: List<Int>): Int? = steps.firstOrNull { it > value }
}
