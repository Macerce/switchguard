package com.macerce.switchguard.core

/**
 * Uyarılar › cihaz sayfasındaki sensör uyarıları (kapı açılınca alarm, sıcaklık sınır dışına çıkınca bildirim ...).
 * Bunlar ayrı bir kural türü değil, sabit kimlikli koşullu uyarılardır ([Automation], işlem ALARM/NOTIFY): motor,
 * yedek ve "Koşullu uyarılar" listesi onları olduğu gibi görür; oradan ayrıntılı düzenlenebilir.
 */
object SensorAlerts {
    const val PREFIX = "salert:"
    /** Pil uyarısının eşiği (%). */
    const val LOW_BATTERY = 15.0

    enum class Bound { MIN, MAX }

    /** Bu cihazda hızlı uyarısı sunulan sensörler: ikililer, sıcaklık/nem (sınır) ve pil. */
    fun kinds(device: DeviceSnapshot): List<SensorKind> = SensorKind.entries.filter { it in device.sensors && it != SensorKind.ILLUMINANCE }

    fun id(deviceId: String, kind: SensorKind, bound: Bound? = null) =
        PREFIX + deviceId + ":" + kind.name + (bound?.let { ":" + it.name } ?: "")

    /** İkili sensör (kapı açıldı, hareket, su) ya da pil azaldı uyarısı; [action] null = kapalı (silinir). */
    fun simple(deviceId: String, name: String, kind: SensorKind, action: ActionKind?): Automation? {
        action ?: return null
        val trigger = if (kind == SensorKind.BATTERY) Trigger(TriggerKind.SENSOR_BELOW, deviceId, watts = LOW_BATTERY, sensor = kind)
        else Trigger(TriggerKind.SENSOR_ACTIVE, deviceId, sensor = kind)
        return Automation(id(deviceId, kind), name, true, trigger, AutoAction(action))
    }

    /** Sıcaklık/nem sınırı: [MIN] altına inince, [MAX] üstüne çıkınca. [value] ya da [action] null = kapalı. */
    fun bound(deviceId: String, name: String, kind: SensorKind, bound: Bound, value: Double?, action: ActionKind?): Automation? {
        if (value == null || action == null) return null
        val tk = if (bound == Bound.MAX) TriggerKind.SENSOR_ABOVE else TriggerKind.SENSOR_BELOW
        return Automation(id(deviceId, kind, bound), name, true, Trigger(tk, deviceId, watts = value, sensor = kind), AutoAction(action))
    }

    /** Var olan kaydın işlemi (kapalı kayıt = null). */
    fun actionOf(list: List<Automation>, id: String): ActionKind? = list.firstOrNull { it.id == id && it.enabled }?.action?.kind

    fun valueOf(list: List<Automation>, id: String): Double? = list.firstOrNull { it.id == id }?.trigger?.watts
}
