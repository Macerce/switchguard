package com.macerce.switchguard.core

/**
 * "Bu cihazı neler kontrol ediyor?": cihaza dokunan SwitchGuard kuralları, sahneleri, döngüleri ve grupları.
 * Cihaz doğrudan ya da içinde bulunduğu bir grup / alan üzerinden hedeflenebilir. Cihazın kendi zamanlayıcıları
 * buluttan ayrıca okunur; hangisinin hangi kurala ait olduğu [timerRule] ile bulunur.
 */
object DeviceUsage {
    /** [switches]: kural cihazı açar/kapatır (doğrudan, grup, sahne ya da döngü ile). [watches]: tetikleyici ya da koşul. */
    data class RuleUse(val rule: Automation, val switches: Boolean, val watches: Boolean)

    data class Report(
        val rules: List<RuleUse>,
        val scenes: List<Pair<Scene, List<SceneAction>>>,
        val cycles: List<Cycle>,
        val groups: List<DeviceGroup>,
    ) {
        val isEmpty get() = rules.isEmpty() && scenes.isEmpty() && cycles.isEmpty() && groups.isEmpty()
    }

    fun find(
        deviceId: String,
        automations: List<Automation>,
        scenes: List<Scene>,
        cycles: List<Cycle>,
        groups: List<DeviceGroup>,
    ): Report {
        val inGroups = groups.filter { g -> g.members.any { it.deviceId == deviceId } }
        val targets = setOf(deviceId) + inGroups.map { it.deviceId }
        val sceneHits = scenes.mapNotNull { s -> s.actions.filter { it.deviceId in targets }.takeIf { it.isNotEmpty() }?.let { s to it } }
        val sceneIds = sceneHits.map { it.first.id }.toSet()
        val cycleHits = cycles.filter { c -> c.targets.any { it.deviceId in targets } }
        val cycleIds = cycleHits.map { it.id }.toSet()
        val rules = automations.mapNotNull { a ->
            val steps = a.allActions.flatMap { it.flatten() }
            val switches = steps.any { s ->
                (s.kind.usesDevice && s.deviceId in targets) ||
                    (s.kind == ActionKind.RUN_SCENE && s.deviceId in sceneIds) ||
                    (s.kind.usesCycle && s.deviceId in cycleIds)
            }
            val conds = a.conditions + steps.flatMap { it.cond?.items.orEmpty() }
            val watches = a.allTriggers.any { it.kind.usesDevice && it.deviceId in targets } || conds.any { it.deviceId in targets }
            if (switches || watches) RuleUse(a, switches, watches) else null
        }.sortedWith(compareByDescending<RuleUse> { it.switches }.thenBy { it.rule.name.lowercase() })
        return Report(rules, sceneHits, cycleHits, inGroups)
    }

    /** Cihazdaki zamanlayıcının sahibi olan kural ("Cihaza yaz" ile yazılmışsa); bulunamazsa null. */
    fun timerRule(timerId: String, automations: List<Automation>): Automation? =
        automations.firstOrNull { RuleToDevice.owns(timerId, it.id) }

    /** SwitchGuard'ın yazdığı bir zamanlayıcı mı (kuralı silinmiş olsa da). */
    fun isAppTimer(timerId: String) = timerId.startsWith("sg-")

    /** SwitchGuard'ın cihaz sayfasından elle eklenen zamanlayıcıların kimlik öneki (eWeLink uygulamasındakilerden ayırmak için). */
    const val MANUAL_PREFIX = "sgm-"

}
