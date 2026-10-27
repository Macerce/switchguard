package com.macerce.switchguard.core

/**
 * Cihazın önem kategorisi. Kullanıcı seçmezse ([DeviceRules.category] null) kurallardan çıkar: bir olayında alarm
 * seçilmiş izlenen cihaz kritik, yalnız bildirim seçilmiş olan önemli, gerisi normal. Kategoriler Uyarılar'da
 * süzgeç ve sessiz saat ayarı ([quietDefault]), otomasyonlarda tetikleyici olur: "kritik bir cihaz çevrimdışı olunca".
 * Otomasyonda kimliği [PREFIX] ile başlayan sanal cihaz gibi seçilir; motor kuralı [expand] ile cihazlara açar.
 */
enum class DeviceCategory {
    CRITICAL, IMPORTANT, NORMAL;

    /** Otomasyon düzenleyicisindeki sanal cihazın kimliği. */
    val deviceId: String get() = PREFIX + name

    companion object {
        const val PREFIX = "category:"

        /** Kategoriye bağlanabilen olaylar: açılma/kapanma, açık/kapalı kalma, bağlantı. Güç olayları cihaza özel. */
        val TRIGGER_KINDS = setOf(
            TriggerKind.TURNED_ON, TriggerKind.TURNED_OFF, TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF,
            TriggerKind.WENT_OFFLINE, TriggerKind.CAME_ONLINE,
        )

        fun isCategory(deviceId: String): Boolean = deviceId.startsWith(PREFIX)

        fun fromId(deviceId: String): DeviceCategory? =
            if (isCategory(deviceId)) entries.firstOrNull { it.name == deviceId.removePrefix(PREFIX) } else null

        /** Kullanıcının seçtiği, yoksa kurallardan çıkan kategori. */
        fun of(rules: DeviceRules): DeviceCategory = rules.category ?: auto(rules)

        /** Alarm seçilmiş olay → kritik, bildirim → önemli; izlenmeyen cihaz uyarı vermez, normaldir. */
        fun auto(rules: DeviceRules): DeviceCategory {
            if (!rules.monitored) return NORMAL
            val actions = EventType.entries.map { rules.actionFor(it) }
            return when {
                AlertAction.ALARM in actions -> CRITICAL
                AlertAction.NOTIFY in actions -> IMPORTANT
                else -> NORMAL
            }
        }

        /**
         * Sessiz saatlerde de alarm çalan kategoriler, kullanıcı henüz seçmemişse. Eskiden yalnız "kritik" işaretli
         * cihazlar sessiz saati delerdi; artık alarm seçilen her cihaz kritik sayıldığından, sessiz saatleri açık olup
         * hiç kritik cihazı olmayan kurulumda kritik de sessize alınır (gece birden alarm çalmaya başlamasın).
         */
        fun quietDefault(quietEnabled: Boolean, anyMarkedCritical: Boolean): Set<DeviceCategory> =
            if (!quietEnabled || anyMarkedCritical) setOf(CRITICAL) else emptySet()

        fun setToString(set: Set<DeviceCategory>): String = entries.filter { it in set }.joinToString(",") { it.name }

        fun setFromString(s: String): Set<DeviceCategory> =
            s.split(',').mapNotNull { n -> entries.firstOrNull { it.name == n.trim() } }.toSet()

        /** Düzenleyicide cihazların yanında gösterilen sanal kategori cihazları ([name] dile göre ad). */
        fun snapshots(name: (DeviceCategory) -> String): List<DeviceSnapshot> = entries.map {
            DeviceSnapshot(id = it.deviceId, name = name(it), online = true, switches = mapOf(0 to false), cloud = Cloud.DEMO)
        }

        /**
         * Kategori tetikleyicili otomasyonu kategorideki her cihaza (anahtar olaylarında her kanala) ayrı bir kopya
         * olarak açar; motor kopyaları sıradan otomasyon gibi değerlendirir, uyarı metni de tetikleyen cihazın adını
         * taşır. Kopyanın kimliği "<kimlik>@<cihaz>[/<kanal>]": durum ve gecikmeli eylemler kopya başına tutulur.
         * Kategoriye uymayan olay türleri hiç tetiklenmez. Gruplar kategoriye girmez.
         */
        fun expand(
            automations: List<Automation>,
            devices: List<DeviceSnapshot>,
            categoryOf: (String) -> DeviceCategory,
        ): List<Automation> {
            if (automations.none { isCategory(it.trigger.deviceId) }) return automations
            val real = devices.filterNot { DeviceGroup.isGroup(it.id) || isCategory(it.id) }
            return automations.flatMap { a ->
                val t = a.trigger
                val category = fromId(t.deviceId) ?: return@flatMap listOf(a)
                if (t.kind !in TRIGGER_KINDS) return@flatMap emptyList()
                real.filter { categoryOf(it.id) == category }.flatMap { d ->
                    if (t.kind.usesChannel) {
                        d.switches.keys.sorted().map { ch -> a.copy(id = "${a.id}@${d.id}/$ch", trigger = t.copy(deviceId = d.id, channel = ch)) }
                    } else {
                        listOf(a.copy(id = "${a.id}@${d.id}", trigger = t.copy(deviceId = d.id, channel = 0)))
                    }
                }
            }
        }
    }
}
