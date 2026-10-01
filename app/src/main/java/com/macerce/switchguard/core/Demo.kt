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

    fun isDemo(id: String) = id.startsWith("demo-")

    /** Cihaz adları uygulamanın o anki dilinde verilir; sonradan dil değişirse adlar aynı kalır. */
    data class Names(val freezer: String, val pumps: String, val light: String, val stalledPump: String, val lightLeftOn: String)

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

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
