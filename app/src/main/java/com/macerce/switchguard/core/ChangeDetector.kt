package com.macerce.switchguard.core

/** Değişiklik metinlerinde kullanılan, dile göre değişen etiketler. */
class ChangeLabels(
    val on: String,
    val off: String,
    val online: String,
    val offline: String,
    /** (cihaz adı, 1'den başlayan kanal no) → "Salon (K2)" gibi. */
    val channel: (String, Int) -> String,
)

sealed class Change {
    abstract val deviceId: String
    abstract val name: String
    abstract val eventType: EventType
    abstract fun describe(labels: ChangeLabels): String

    data class Switch(
        override val deviceId: String,
        override val name: String,
        val channel: Int,
        val multiChannel: Boolean,
        val from: Boolean,
        val to: Boolean,
    ) : Change() {
        override val eventType get() = if (to) EventType.TURNED_ON else EventType.TURNED_OFF

        override fun describe(labels: ChangeLabels): String {
            val label = if (multiChannel) labels.channel(name, channel + 1) else name
            return "$label: ${labels.onOff(from)} → ${labels.onOff(to)}"
        }
    }

    data class Connection(
        override val deviceId: String,
        override val name: String,
        val from: Boolean,
        val to: Boolean,
    ) : Change() {
        override val eventType get() = if (to) EventType.CAME_ONLINE else EventType.WENT_OFFLINE

        override fun describe(labels: ChangeLabels) =
            "$name: ${labels.onlineText(from)} → ${labels.onlineText(to)}"
    }
}

private fun ChangeLabels.onOff(isOn: Boolean) = if (isOn) on else off
private fun ChangeLabels.onlineText(isOnline: Boolean) = if (isOnline) online else offline

object ChangeDetector {
    /**
     * [previous] ile [current] arasındaki farkları döndürür.
     * Sadece [monitored] içindeki ve her iki durumda da bulunan cihazlar karşılaştırılır;
     * böylece ilk çalıştırma ya da yeni eklenen cihaz yanlış alarm üretmez.
     */
    fun detect(
        previous: Map<String, DeviceSnapshot>,
        current: Map<String, DeviceSnapshot>,
        monitored: Set<String>,
    ): List<Change> {
        val changes = mutableListOf<Change>()
        for ((id, now) in current) {
            if (id !in monitored) continue
            val before = previous[id] ?: continue

            if (before.online != now.online) {
                changes += Change.Connection(id, now.name, before.online, now.online)
            }
            for ((channel, on) in now.switches.toSortedMap()) {
                val was = before.switches[channel] ?: continue
                if (was != on) {
                    changes += Change.Switch(id, now.name, channel, now.isMultiChannel, was, on)
                }
            }
        }
        return changes
    }
}
