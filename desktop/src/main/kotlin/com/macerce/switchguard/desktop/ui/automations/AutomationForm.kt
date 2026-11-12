package com.macerce.switchguard.desktop.ui.automations

import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.Condition
import com.macerce.switchguard.core.ConditionKind
import com.macerce.switchguard.core.ConditionMatch
import com.macerce.switchguard.core.PresenceRule
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.Cycle
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.core.SensorKind

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
    /** Ve şu koşullarda (telefon 2.30.5): boş = koşul yok; hepsi mi biri mi. */
    val conds: List<CondDraft> = emptyList(),
    val condMatch: ConditionMatch = ConditionMatch.ALL,
    /** [ActionKind.RUN_SCENE]: çalıştırılacak sahne. */
    val sceneId: String = "",
    /** Döngü tetikleyicisi / eylemi: seçili döngü. */
    val triggerCycle: String = "",
    val actionCycle: String = "",
    /** Sensör tetikleyicisinde hangi değer ve eşik (metin). */
    val sensor: SensorKind? = null,
    val sensorValue: String = "",
) {
    /** Seçili cihaz bu olayı bildiremiyorsa (ör. güç ölçmüyor) olay "açıldı"ya döner. */
    fun normalized(devices: List<DeviceSnapshot>): AutomationForm {
        val src = devices.firstOrNull { it.id == triggerDevice }
        val kinds = kindsFor(src)
        var f = if (triggerKind in kinds) this else copy(triggerKind = kinds.firstOrNull() ?: TriggerKind.TURNED_ON)
        // Sensör olayında seçili değer bu cihazda yoksa ilkine geçilir; eşik boşsa şu anki değerle (yuvarlanmış) dolar.
        if (f.triggerKind.usesSensor) {
            val choices = sensorChoices(src, f.triggerKind)
            if (f.sensor !in choices) f = f.copy(sensor = choices.firstOrNull(), sensorValue = "")
            if (f.triggerKind.usesSensorValue && f.sensorValue.isBlank()) {
                f.sensor?.let { src?.sensors?.get(it) }?.let { f = f.copy(sensorValue = Math.round(it).toString()) }
            }
        }
        // Eylem hedefi açılıp kapatılabilen cihaz olmalı.
        f = f.copy(conds = f.conds.map { it.normalized(devices) })
        if (f.actionKind.usesDevice && devices.firstOrNull { it.id == f.actionDevice }?.sensorOnly == true) {
            f = f.copy(actionDevice = devices.firstOrNull { !it.sensorOnly && !DeviceCategory.isCategory(it.id) }?.id.orEmpty())
        }
        return f
    }

    fun isValid(devices: List<DeviceSnapshot>, scenes: List<Scene> = emptyList(), cycles: List<Cycle> = emptyList()): Boolean {
        val src = devices.firstOrNull { it.id == triggerDevice }
        val minutesValue = minutes.toIntOrNull()
        val wattsValue = wattsValue()
        val delayValue = delay.toIntOrNull()
        val lowValue = lowMinutes.toIntOrNull()
        val graceValue = grace.toIntOrNull()
        return name.isNotBlank() && (!triggerKind.usesDevice || (src != null && triggerKind in kindsFor(src))) &&
            (!triggerKind.usesTemp || tempValue() != null) &&
            (!triggerKind.usesSensor || (sensor != null && src?.sensors?.containsKey(sensor) == true)) &&
            (!triggerKind.usesSensorValue || sensorThreshold() != null) &&
            conds.all { it.valid(devices) } &&
            (actionKind != ActionKind.RUN_SCENE || scenes.any { it.id == sceneId }) &&
            (!triggerKind.usesCycle || cycles.any { it.id == triggerCycle }) &&
            (!actionKind.usesCycle || cycles.any { it.id == actionCycle }) &&
            (!triggerKind.usesChannelPower || (lowValue != null && lowValue >= 0 && graceValue != null && graceValue >= 0)) &&
            (!triggerKind.usesMinutes || (minutesValue != null && minutesValue > 0)) &&
            (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
            (!actionKind.usesDevice || (devices.any { it.id == actionDevice && !it.sensorOnly } && !DeviceCategory.isCategory(actionDevice))) &&
            delayValue != null && delayValue >= 0
    }

    /** Kaydedilecek otomasyon ([isValid] doğruyken çağrılır); kullanılmayan alanlar sıfırlanır. */
    fun build(id: String, enabled: Boolean): Automation = Automation(
        id = id,
        name = name.trim(),
        enabled = enabled,
        trigger = Trigger(
            triggerKind, when {
                triggerKind.usesDevice -> triggerDevice
                triggerKind.usesCycle -> triggerCycle
                else -> ""
            },
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
                triggerKind.usesSensorValue -> sensorThreshold() ?: 0.0
                else -> 0.0
            },
            graceMinutes = if (triggerKind.usesChannelPower) grace.toIntOrNull() ?: 0 else 0,
            sensor = if (triggerKind.usesSensor) sensor else null,
        ),
        action = when {
            actionKind.usesDevice -> AutoAction(actionKind, actionDevice, actionChannel)
            actionKind == ActionKind.RUN_SCENE -> AutoAction(actionKind, sceneId)
            actionKind.usesCycle -> AutoAction(actionKind, actionCycle)
            else -> AutoAction(actionKind)
        },
        delayMinutes = delay.toIntOrNull() ?: 0,
        window = if (windowOn) ActiveWindow(windowStart, windowEnd, windowDays) else null,
        conditions = conds.map { it.toCondition() },
        conditionMatch = condMatch,
        presence = presence,
    )

    /** Sıcaklık eşiği (eksi olabilir). */
    private fun sensorThreshold(): Double? = sensorValue.trim().replace(',', '.').toDoubleOrNull()

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
            if (initial == null) return AutomationForm(triggerDevice = first, actionKind = mode.defaultAction, actionDevice = first)
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
                conds = initial.conditions.map(CondDraft::of),
                condMatch = initial.conditionMatch,
                sceneId = if (initial.action.kind == ActionKind.RUN_SCENE) initial.action.deviceId else "",
                triggerCycle = if (t.kind.usesCycle) t.deviceId else "",
                actionCycle = if (initial.action.kind.usesCycle) initial.action.deviceId else "",
                sensor = t.sensor,
                sensorValue = if (t.kind.usesSensorValue) formatNumber(t.watts) else "",
            )
        }

        /** Güç olayları yalnızca enerji ölçen, kanal gücü olayı kanal başına ölçen cihazlarda sunulur. */
        fun kindsFor(device: DeviceSnapshot?): List<TriggerKind> = TriggerKind.entries.filter {
            when {
                // Kategori: yalnız anahtar ve bağlantı olayları (gün doğumu/sıcaklık cihazsız, yine sunulur).
                device != null && DeviceCategory.isCategory(device.id) -> !it.usesDevice || it in DeviceCategory.TRIGGER_KINDS
                it.usesChannelPower -> device?.hasChannelPower == true
                it.usesWatts -> device?.hasEnergy == true
                it.usesSensorValue -> device?.sensors?.keys?.any { k -> !k.binary } == true
                it.usesSensorState -> device?.sensors?.keys?.any { k -> k.binary } == true
                // Yalnız sensör olan cihazın rölesi yok: aç/kapa olayları sunulmaz.
                it.usesChannel || it.usesMinutes -> device?.sensorOnly != true
                else -> true
            }
        }

        /** Seçili olaya uyan sensörler (sayısal ya da ikili). */
        fun sensorChoices(device: DeviceSnapshot?, kind: TriggerKind): List<SensorKind> =
            if (!kind.usesSensor) emptyList() else device?.sensors?.keys?.filter { it.binary == kind.usesSensorState }.orEmpty()

        internal fun formatNumber(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
    }
}

/** Düzenleyicideki bir koşul (telefon `CondDraft` ile aynı); eşik yazıldığı gibi tutulur. */
data class CondDraft(
    val kind: ConditionKind,
    val deviceId: String,
    val channel: Int = 0,
    val value: String = "",
    val sensor: SensorKind? = null,
) {
    fun number(): Double? = value.trim().replace(',', '.').toDoubleOrNull()

    fun toCondition() = Condition(
        kind, if (kind.usesDevice) deviceId else "", if (kind.usesChannel) channel else 0,
        if (kind.usesValue) number() ?: 0.0 else 0.0, if (kind.usesSensor) sensor else null,
    )

    fun valid(devices: List<DeviceSnapshot>): Boolean =
        (!kind.usesDevice || devices.any { it.id == deviceId && !DeviceCategory.isCategory(it.id) }) &&
            (!kind.usesValue || number() != null) && (!kind.usesSensor || sensor != null)

    /**
     * Cihaza uymayan tür düzeltilir (sensör cihazında önce sensör eşiği), sensör uyacak şekilde seçilir, boş eşik
     * şu anki değerle (dış sıcaklıkta 20) dolar.
     */
    fun normalized(devices: List<DeviceSnapshot>): CondDraft {
        val d = devices.firstOrNull { it.id == deviceId }
        val kinds = kindsFor(d)
        var c = this
        if (c.kind !in kinds) {
            c = c.copy(kind = kinds.firstOrNull { it.usesSensorValue } ?: kinds.firstOrNull { it.usesSensorState } ?: kinds.first(), value = "", sensor = null)
        }
        val choices = sensorChoices(d, c.kind)
        if (c.kind.usesSensor && c.sensor !in choices) c = c.copy(sensor = choices.firstOrNull(), value = "")
        if (c.kind.usesValue && c.value.isBlank()) {
            val now = when {
                c.kind.usesPower -> d?.power
                c.kind.usesSensorValue -> c.sensor?.let { d?.sensors?.get(it) }
                else -> null
            }
            (now?.let { Math.round(it).toString() } ?: if (c.kind.usesTemp) "20" else null)?.let { c = c.copy(value = it) }
        }
        return c
    }

    companion object {
        fun of(c: Condition) = CondDraft(c.kind, c.deviceId, c.channel, if (c.kind.usesValue) AutomationForm.formatNumber(c.value) else "", c.sensor)

        /** Yeni koşul: ilk açılıp kapatılabilen cihazın "açık" durumu. */
        fun new(devices: List<DeviceSnapshot>): CondDraft {
            val targets = devices.filterNot { DeviceCategory.isCategory(it.id) }
            return CondDraft(ConditionKind.IS_ON, (targets.firstOrNull { !it.sensorOnly } ?: targets.firstOrNull())?.id.orEmpty())
        }

        /** Seçili cihazda anlamlı koşul türleri (dış sıcaklık her zaman). */
        fun kindsFor(d: DeviceSnapshot?): List<ConditionKind> = ConditionKind.entries.filter {
            when {
                it.usesTemp -> true
                d == null -> !it.usesPower && !it.usesSensor
                it.usesChannel -> !d.sensorOnly && d.switches.isNotEmpty()
                it.usesPower -> d.hasEnergy
                it.usesSensorValue -> d.sensors.keys.any { k -> !k.binary }
                it.usesSensorState -> d.sensors.keys.any { k -> k.binary }
                else -> true
            }
        }

        fun sensorChoices(d: DeviceSnapshot?, kind: ConditionKind): List<SensorKind> =
            if (!kind.usesSensor) emptyList() else d?.sensors?.keys?.filter { it.binary == kind.usesSensorState }.orEmpty()
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
