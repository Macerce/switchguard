package com.macerce.switchguard.core

/**
 * Döngü korumasının hangi cihazları kapatacağı. Yalnız kuralın KENDİSİNİN açıp kapattığı cihazlar (aç/kapat hedefi,
 * grup/alan üyeleri, sahnenin cihazları, durumu hatırlanan cihaz) kapatılır; kuralı BAŞLATAN cihaza (ör. dondurucu
 * "güç düştü" tetikleyicisi) dokunulmaz. Kapatırken onay istenen ve kritik kategorideki cihazlar hiç kapatılmaz,
 * yalnız alarm metninde adı geçer. Kural hiçbir cihazı açıp kapatmıyorsa (alarm/bildirim) hiçbir şey kapatılmaz.
 * Saf; veriler dışarıdan verilir.
 */
object LoopGuard {
    /** [turnOff]: kapatılacak (cihaz, kanal) çiftleri (yalnız şu an açık olanlar). [kept]: korunduğu için dokunulmayanlar. */
    data class Plan(val turnOff: List<Pair<DeviceSnapshot, Int>>, val kept: List<DeviceSnapshot>) {
        /** Kapatılan cihazlar (adı bir kez). */
        val offDevices: List<DeviceSnapshot> get() = turnOff.map { it.first }.distinctBy { it.id }
    }

    /**
     * [actions]: kuralın bütün eylemleri (iç içe adımlar düzleştirilmiş). [protected]: kapatırken onay istenen ya da
     * kritik cihaz mı.
     */
    fun plan(
        actions: List<AutoAction>,
        byId: Map<String, DeviceSnapshot>,
        groups: List<DeviceGroup>,
        scenes: List<Scene>,
        protected: (String) -> Boolean,
    ): Plan {
        // Cihaz → kanallar (null = açık olan bütün kanallar). Sıra korunur: alarm metni kural sırasıyla okunur.
        val wanted = LinkedHashMap<String, MutableSet<Int>?>()
        fun add(id: String, channel: Int?) {
            if (id.isEmpty() || DeviceCategory.isCategory(id)) return
            if (DeviceGroup.isGroup(id)) {
                groups.firstOrNull { it.deviceId == id }?.members?.forEach { add(it.deviceId, it.channel) }
                return
            }
            if (!wanted.containsKey(id)) wanted[id] = if (channel == null) null else mutableSetOf(channel)
            else if (channel == null) wanted[id] = null
            else wanted[id]?.add(channel)
        }
        fun addScene(id: String): Boolean {
            val scene = scenes.firstOrNull { it.id == id } ?: return false
            scene.actions.forEach { add(it.deviceId, it.channel) }
            return true
        }
        val savedTarget = actions.firstOrNull { it.kind == ActionKind.SAVE_STATE }?.deviceId.orEmpty()
        for (x in actions) when (x.kind) {
            ActionKind.TURN_ON, ActionKind.TURN_OFF, ActionKind.TOGGLE -> add(x.deviceId, x.channel)
            ActionKind.RUN_SCENE -> addScene(x.deviceId)
            ActionKind.SAVE_STATE, ActionKind.RESTORE_STATE -> {
                val target = if (x.kind == ActionKind.SAVE_STATE) x.deviceId else savedTarget
                if (!addScene(target)) add(target, null)
            }
            else -> Unit
        }
        val off = mutableListOf<Pair<DeviceSnapshot, Int>>()
        val kept = mutableListOf<DeviceSnapshot>()
        for ((id, channels) in wanted) {
            val d = byId[id] ?: continue
            if (d.sensorOnly) continue
            val on = (channels ?: d.switches.keys).filter { d.switches[it] == true }.sorted()
            if (protected(id)) { kept += d; continue }
            if (d.online) on.forEach { off += d to it }
        }
        return Plan(off, kept)
    }
}
