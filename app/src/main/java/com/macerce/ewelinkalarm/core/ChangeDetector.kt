package com.macerce.ewelinkalarm.core

sealed class Change {
    abstract val deviceId: String
    abstract fun describe(): String

    data class Switch(
        override val deviceId: String,
        val name: String,
        val channel: Int,
        val multiChannel: Boolean,
        val from: Boolean,
        val to: Boolean,
    ) : Change() {
        override fun describe(): String {
            val label = if (multiChannel) "$name (K${channel + 1})" else name
            return "$label: ${onOff(from)} → ${onOff(to)}"
        }
    }

    data class Connection(
        override val deviceId: String,
        val name: String,
        val from: Boolean,
        val to: Boolean,
    ) : Change() {
        override fun describe() = "$name: ${onlineText(from)} → ${onlineText(to)}"
    }

    companion object {
        private fun onOff(on: Boolean) = if (on) "AÇIK" else "KAPALI"
        private fun onlineText(online: Boolean) = if (online) "ONLINE" else "OFFLINE"
    }
}

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
