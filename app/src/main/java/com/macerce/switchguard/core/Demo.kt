package com.macerce.switchguard.core

import kotlin.random.Random

/**
 * Hesap gerektirmeyen sanal cihazlar. Uygulamayı denemek isteyenler (testçiler, inceleme ekibi)
 * gerçek akışı görsün diye bunlar da kurallardan, alarmdan, otomasyonlardan ve geçmişten geçer;
 * yalnızca durum bir buluttan değil, uygulamanın içindeki simülasyondan gelir.
 */
object Demo {
    const val FREEZER = "demo-freezer"
    const val PUMPS = "demo-pumps"
    const val LIGHT = "demo-light"
    /** SNZB-02D gibi sıcaklık/nem sensörü; değer yavaşça oynar (eşik otomasyonları denenebilsin). */
    const val SENSOR = "demo-sensor"
    /** SNZB-04P gibi kapı sensörü; ara sıra açılıp kapanır. */
    const val DOOR = "demo-door"

    fun isDemo(id: String) = id.startsWith("demo-")

    /** Cihaz adları uygulamanın o anki dilinde verilir; sonradan dil değişirse adlar aynı kalır. */
    data class Names(
        val freezer: String, val pumps: String, val light: String, val stalledPump: String, val lightLeftOn: String,
        val sensor: String = "Demo · Sensor", val door: String = "Demo · Door",
    )

    /** Açık kanalın normal gücü (W). */
    private val nominal = mapOf(
        FREEZER to mapOf(0 to 118.0),
        PUMPS to mapOf(0 to 740.0, 1 to 1100.0, 2 to 55.0, 3 to 35.0),
    )
    /** "Güç düştü" simülasyonunda kanalın çektiği güç: çalışıyor görünüp iş yapmayan pompa. */
    const val STALLED_WATTS = 3.0
    private const val VOLTAGE = 230.0

    fun devices(n: Names): List<DeviceSnapshot> = listOf(
        // POW R2 gibi tek kanallı güç ölçer.
        recompute(DeviceSnapshot(FREEZER, n.freezer, online = true, switches = mapOf(0 to true), uiid = 32, cloud = Cloud.DEMO)),
        // SPM-4Relay gibi kanal başına güç ölçen 4 kanallı.
        recompute(DeviceSnapshot(PUMPS, n.pumps, online = true, switches = mapOf(0 to true, 1 to false, 2 to true, 3 to false), uiid = 130, cloud = Cloud.DEMO)),
        DeviceSnapshot(LIGHT, n.light, online = true, switches = mapOf(0 to false), uiid = 1, cloud = Cloud.DEMO),
        DeviceSnapshot(
            SENSOR, n.sensor, online = true, switches = emptyMap(), uiid = 7014, cloud = Cloud.DEMO,
            sensors = mapOf(SensorKind.TEMPERATURE to 23.5, SensorKind.HUMIDITY to 52.0, SensorKind.BATTERY to 88.0),
        ),
        DeviceSnapshot(
            DOOR, n.door, online = true, switches = emptyMap(), uiid = 7003, cloud = Cloud.DEMO,
            sensors = mapOf(SensorKind.DOOR to 0.0, SensorKind.BATTERY to 95.0),
        ),
    )

    /** Örnek otomasyonlar: kanal gücü düşünce alarm, ışık fazla açık kalınca bildirim. */
    fun automations(n: Names): List<Automation> = listOf(
        Automation(
            id = "demo-auto-pump", name = n.stalledPump, enabled = true,
            trigger = Trigger(TriggerKind.CHANNEL_POWER_LOW, PUMPS, channel = 0, minutes = 1, watts = 50.0),
            action = AutoAction(ActionKind.ALARM),
        ),
        Automation(
            id = "demo-auto-light", name = n.lightLeftOn, enabled = true,
            trigger = Trigger(TriggerKind.STAYED_ON, LIGHT, channel = 0, minutes = 10),
            action = AutoAction(ActionKind.NOTIFY),
        ),
    )

    fun isDemoAutomation(a: Automation) = a.id.startsWith("demo-auto-")

    /**
     * Güç değerlerini anahtarlara göre yeniden hesaplar. [stalled]: gücü düşürülmüş kanallar.
     * [random] verilirse değerler gerçek ölçüm gibi biraz oynar.
     */
    fun recompute(d: DeviceSnapshot, stalled: Set<Int> = emptySet(), random: Random? = null): DeviceSnapshot {
        if (d.id == SENSOR || d.id == DOOR) return if (random == null) d else d.copy(sensors = simulateSensors(d, random))
        val table = nominal[d.id] ?: return d
        fun jitter(w: Double) = if (random == null || w == 0.0) w else w * (1 + (random.nextDouble() - 0.5) * 0.06)
        val perChannel = table.mapValues { (ch, w) ->
            when {
                d.switches[ch] != true -> 0.0
                ch in stalled -> STALLED_WATTS
                else -> round1(jitter(w))
            }
        }
        val total = round1(perChannel.values.sum())
        return d.copy(
            power = total,
            voltage = round1(jitter(VOLTAGE)),
            current = Math.round(total / VOLTAGE * 100) / 100.0,
            channelPower = if (d.isMultiChannel) perChannel else emptyMap(),
        )
    }

    /** Sıcaklık 19–28 °C arasında dakikada en çok ±0,6 °C gezer; kapı her adımda %25 olasılıkla durum değiştirir. */
    private fun simulateSensors(d: DeviceSnapshot, random: Random): Map<SensorKind, Double> {
        val s = d.sensors.toMutableMap()
        s[SensorKind.TEMPERATURE]?.let { s[SensorKind.TEMPERATURE] = round1((it + (random.nextDouble() - 0.5) * 1.2).coerceIn(19.0, 28.0)) }
        s[SensorKind.HUMIDITY]?.let { s[SensorKind.HUMIDITY] = round1((it + (random.nextDouble() - 0.5) * 3).coerceIn(35.0, 75.0)) }
        s[SensorKind.DOOR]?.let { if (random.nextDouble() < 0.25) s[SensorKind.DOOR] = 1.0 - it }
        return s
    }

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
