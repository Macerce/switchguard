package com.macerce.switchguard.core

/**
 * Basit saatli kuralı cihazın kendi zamanlayıcısına çevirme: telefon kapalıyken de çalışsın.
 * Uygun kural: yalnız "Saatte" tetikleyicisi, koşulsuz (saat aralığı, evde/dışarıda, ek tetikleyici yok), gecikmesiz;
 * adımlar yalnız eWeLink cihazlarını aç/kapat ve aralarında tam dakikalık bekleme. Her aç/kapa adımı bir haftalık
 * zamanlayıcı olur (bekleme saate eklenir; gece yarısını geçerse günler bir kayar).
 */
object RuleToDevice {
    data class Planned(val deviceId: String, val timer: RepeatTimer)

    /** Kuralın zamanlayıcıları; uygun değilse null. [devices]: kimlik → cihaz (yalnız eWeLink cihazlarına yazılır). */
    fun plan(a: Automation, devices: Map<String, DeviceSnapshot>): List<Planned>? {
        val t = a.trigger
        if (t.kind != TriggerKind.TIME || a.extraTriggers.isNotEmpty() || a.conditions.isNotEmpty() || a.window != null ||
            a.presence != null || a.delayMinutes != 0 || t.days == 0
        ) return null
        var offset = 0
        val out = mutableListOf<Planned>()
        for ((i, x) in a.allActions.withIndex()) {
            if (x.cond != null || x.then.isNotEmpty() || x.otherwise.isNotEmpty()) return null
            offset += x.waitMinutes
            when (x.kind) {
                ActionKind.DELAY -> { if (x.seconds % 60 != 0) return null; offset += x.seconds / 60 }
                ActionKind.TURN_ON, ActionKind.TURN_OFF -> {
                    val d = devices[x.deviceId] ?: return null
                    if (d.cloud != Cloud.EWELINK || DeviceGroup.isGroup(d.id) || d.switches.isEmpty()) return null
                    val at = t.minutes + offset
                    val shift = at / MINUTES_PER_DAY
                    val local = at % MINUTES_PER_DAY
                    out += Planned(
                        d.id,
                        RepeatTimer(
                            id = timerId(a.id, i), enabled = true, hour = local / 60, minute = local % 60,
                            days = cronDays(t.days, shift), channel = x.channel, on = x.kind == ActionKind.TURN_ON,
                        ),
                    )
                }
                else -> return null
            }
        }
        if (out.isEmpty()) return null
        // Cihazda en çok 8 zamanlayıcı olabilir.
        if (out.groupBy { it.deviceId }.any { it.value.size > DeviceTimers.MAX_TIMERS }) return null
        return out
    }

    /** Aynı kural yeniden yazılınca eski kayıtların üstüne gelsin: kimlik kurala ve adıma bağlı. */
    fun timerId(ruleId: String, step: Int) = prefix(ruleId) + step

    /** Bu kuralın cihaza yazdığı zamanlayıcı mı (adım sayısı değişse de eskileri bulunur). */
    fun owns(timerId: String, ruleId: String) = timerId.startsWith(prefix(ruleId))

    private fun prefix(ruleId: String) = "sg-" + ruleId.filter { it.isLetterOrDigit() }.take(12) + "-"

    /** Kural günleri (bit 0 = pazartesi … bit 6 = pazar) → cron günleri (0 = pazar … 6 = cumartesi), [shift] gün ileri. */
    fun cronDays(ruleDays: Int, shift: Int): Set<Int> =
        (0 until 7).filter { ruleDays and (1 shl it) != 0 }.map { (it + 1 + shift) % 7 }.toSet()

    private const val MINUTES_PER_DAY = 24 * 60
}
