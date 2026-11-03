package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/**
 * Döngü / zamanlayıcı: seçilen cihaz-kanallarda sırayla adımlar uygular (her adım bir süre sürer),
 * istenen kadar tekrarlar, bitince son bir durum verir. Geri sayım ("30 dk sonra kapat"),
 * "Her gün 07:00'de aç, 20 dk sonra kapat", aç/kapa döngüsü ve "sırayla yak" hep bununla kurulur.
 * Telefonda (izleme servisinde) çalışır; eWeLink ve Tuya cihazlarında, gruplarda aynı.
 */
data class Cycle(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val targets: List<CycleTarget>,
    val steps: List<CycleStep>,
    /** Kaç tur; 0 = durdurulana (ya da saat aralığı bitene) kadar. */
    val repeat: Int = 1,
    val start: CycleStart = CycleStart.MANUAL,
    /** [CycleStart.AT_TIME] başlama saati / [CycleStart.WINDOW] aralık başı (günün dakikası). */
    val startMinute: Int = 7 * 60,
    /** [CycleStart.WINDOW] aralık sonu. */
    val endMinute: Int = 22 * 60,
    /** Pazartesi = bit 0 ([ActiveWindow] ile aynı). */
    val days: Int = ActiveWindow.ALL_DAYS,
    val end: CycleEnd = CycleEnd.OFF,
    /** Adım sürelerine ± bu kadar yüzde rastgele sapma (evde biri var gibi görünsün). */
    val jitterPct: Int = 0,
    /** Kendiliğinden başlayan döngü yalnızca evdeyken / dışarıdayken; null = her zaman. */
    val presence: PresenceRule? = null,
    /** Döngünün bir cihazı elle (uygulama dışından ya da uygulamadan) değiştirilince döngü durur. */
    val stopOnManual: Boolean = false,
    /** Düzenleyicide hangi kalıpla kurulduğu (adımlar ondan üretilir). */
    val preset: CyclePreset = CyclePreset.CUSTOM,
) {
    val valid get() = name.isNotBlank() && targets.isNotEmpty() && steps.isNotEmpty() && steps.all { it.seconds >= MIN_STEP_SECONDS }

    /** Bir turun süresi (sapmasız). */
    val roundSeconds get() = steps.sumOf { it.seconds.toLong() }

    companion object {
        const val MIN_STEP_SECONDS = 5
        const val MAX_STEP_SECONDS = 7 * 24 * 3600
        const val MAX_STEPS = 20

        fun listToJson(list: List<Cycle>): String = JSONArray().apply { list.forEach { put(toJson(it)) } }.toString()

        fun toJson(c: Cycle): JSONObject = JSONObject()
            .put("id", c.id).put("name", c.name).put("enabled", c.enabled)
            .put("targets", JSONArray().apply { c.targets.forEach { put(JSONObject().put("device", it.deviceId).put("channel", it.channel)) } })
            .put("steps", JSONArray().apply {
                c.steps.forEach { s ->
                    put(JSONObject().put("sec", s.seconds).put("set", JSONObject().apply { s.states.forEach { (k, v) -> put(k, v) } }))
                }
            })
            .put("repeat", c.repeat).put("start", c.start.name).put("from", c.startMinute).put("to", c.endMinute)
            .put("days", c.days).put("end", c.end.name).put("jitter", c.jitterPct).put("manualStop", c.stopOnManual).put("preset", c.preset.name)
            .apply { c.presence?.let { put("presence", it.name) } }

        /** Bozuk / tanınmayan kayıtlar atlanır. */
        fun listFromJson(json: String?): List<Cycle> {
            if (json.isNullOrEmpty()) return emptyList()
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i -> runCatching { fromJson(arr.getJSONObject(i)) }.getOrNull() }
        }

        fun fromJson(o: JSONObject): Cycle {
            val t = o.getJSONArray("targets")
            val s = o.getJSONArray("steps")
            return Cycle(
                id = o.getString("id"),
                name = o.optString("name"),
                enabled = o.optBoolean("enabled", true),
                targets = (0 until t.length()).map { t.getJSONObject(it).let { x -> CycleTarget(x.getString("device"), x.optInt("channel")) } },
                steps = (0 until s.length()).map { j ->
                    val x = s.getJSONObject(j)
                    val set = x.optJSONObject("set") ?: JSONObject()
                    CycleStep(x.getInt("sec"), set.keys().asSequence().associateWith { set.getBoolean(it) })
                },
                repeat = o.optInt("repeat", 1).coerceAtLeast(0),
                start = runCatching { CycleStart.valueOf(o.optString("start")) }.getOrDefault(CycleStart.MANUAL),
                startMinute = o.optInt("from", 7 * 60),
                endMinute = o.optInt("to", 22 * 60),
                days = o.optInt("days", ActiveWindow.ALL_DAYS).takeIf { it != 0 } ?: ActiveWindow.ALL_DAYS,
                end = runCatching { CycleEnd.valueOf(o.optString("end")) }.getOrDefault(CycleEnd.OFF),
                jitterPct = o.optInt("jitter").coerceIn(0, 50),
                presence = o.optString("presence").takeIf { it.isNotEmpty() }?.let { runCatching { PresenceRule.valueOf(it) }.getOrNull() },
                stopOnManual = o.optBoolean("manualStop"),
                preset = runCatching { CyclePreset.valueOf(o.optString("preset")) }.getOrDefault(CyclePreset.CUSTOM),
            )
        }
    }
}

data class CycleTarget(val deviceId: String, val channel: Int) {
    val key get() = "$deviceId/$channel"
}

/** Bir adım: [states] = hedef anahtarı ([CycleTarget.key]) → aç/kapat; adımda olmayan hedefe dokunulmaz. */
data class CycleStep(val seconds: Int, val states: Map<String, Boolean> = emptyMap())

/** Düzenleyicinin hazır kalıpları; [CycleEngine.Presets] adımları üretir. */
enum class CyclePreset { COUNTDOWN, ON_FOR, BLINK, CHASE, CUSTOM }

enum class CycleStart {
    /** Düğmeyle (uygulama, Telegram) başlar. */
    MANUAL,
    /** Seçili günlerde belirli saatte kendiliğinden başlar. */
    AT_TIME,
    /** Seçili günlerde saat aralığı boyunca çalışır; aralık bitince durur. */
    WINDOW,
}

enum class CycleEnd {
    /** Bitince hepsi kapanır. */
    OFF,
    /** Bitince hepsi açılır ("1 saat sonra aç" geri sayımı). */
    ON,
    /** Son adımdaki gibi kalır. */
    KEEP,
    /** Döngü başlamadan önceki durumlarına döner. */
    RESTORE,
}

/** Süren bir döngü. Zamanlar duvar saatidir (servis yeniden başlasa da kalır). */
data class CycleRun(
    val cycleId: String,
    val startedAt: Long,
    /** Düğmeyle başlatıldı (saat aralığı / evde-dışarıda kuralı onu durdurmaz). */
    val manual: Boolean,
    val step: Int,
    /** Tamamlanan tur sayısı. */
    val round: Int,
    val stepEndsAt: Long,
    /** [CycleEnd.RESTORE] için başlamadan önceki durumlar. */
    val before: Map<String, Boolean> = emptyMap(),
    /** Saat aralığıyla başladıysa aralığın bittiği an; 0 = yok. */
    val windowEnd: Long = 0,
) {
    companion object {
        fun mapToJson(runs: Map<String, CycleRun>): String = JSONObject().apply {
            runs.forEach { (id, r) ->
                put(id, JSONObject().put("at", r.startedAt).put("manual", r.manual).put("step", r.step).put("round", r.round)
                    .put("ends", r.stepEndsAt).put("winEnd", r.windowEnd)
                    .put("before", JSONObject().apply { r.before.forEach { (k, v) -> put(k, v) } }))
            }
        }.toString()

        fun mapFromJson(json: String?): Map<String, CycleRun> {
            if (json.isNullOrEmpty()) return emptyMap()
            val o = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
            return o.keys().asSequence().mapNotNull { id ->
                runCatching {
                    val r = o.getJSONObject(id)
                    val b = r.optJSONObject("before") ?: JSONObject()
                    id to CycleRun(
                        id, r.getLong("at"), r.optBoolean("manual"), r.getInt("step"), r.getInt("round"), r.getLong("ends"),
                        b.keys().asSequence().associateWith { b.getBoolean(it) }, r.optLong("winEnd"),
                    )
                }.getOrNull()
            }.toMap()
        }
    }
}

/** Gönderilecek tek aç/kapa komutu (hedef grup da olabilir). */
data class CycleCommand(val cycle: Cycle, val target: CycleTarget, val on: Boolean)

enum class CycleEventKind { STARTED, FINISHED, STOPPED, OVERRIDDEN }

data class CycleEvent(val cycle: Cycle, val kind: CycleEventKind)

/** Döngü motorunun bir adımının sonucu: yeni durum, gönderilecek komutlar, geçmişe yazılacak olaylar. */
data class CycleResult(
    val runs: Map<String, CycleRun>,
    /** Döngü kimliği → kendiliğinden başladığı son fırsatın başlangıcı (aynı fırsatta yeniden başlamasın). */
    val done: Map<String, Long>,
    val commands: List<CycleCommand> = emptyList(),
    val events: List<CycleEvent> = emptyList(),
)

/** Saf döngü motoru: zaman, rastgelelik ve cihaz durumları dışarıdan verilir. */
object CycleEngine {

    /** Kendiliğinden başlama saati bu kadar geç yakalanırsa (telefon uyuyordu) yine başlar; daha geçse o gün atlanır. */
    const val LATE_START_MS = 15 * 60_000L

    /** Düğmeyle başlatır; süren varsa baştan başlar. */
    fun start(
        cycle: Cycle, runs: Map<String, CycleRun>, done: Map<String, Long>, states: Map<String, Boolean>,
        now: Long, random: () -> Double = Math::random,
    ): CycleResult {
        val (run, cmds) = begin(cycle, now, manual = true, windowEnd = 0, states = states, random = random)
        return CycleResult(runs + (cycle.id to run), done, cmds, listOf(CycleEvent(cycle, CycleEventKind.STARTED)))
    }

    /** Düğmeyle durdurur; bitiş eylemi uygulanır. */
    fun stop(cycle: Cycle, runs: Map<String, CycleRun>, done: Map<String, Long>): CycleResult {
        val run = runs[cycle.id] ?: return CycleResult(runs, done)
        return CycleResult(runs - cycle.id, done, finish(cycle, run), listOf(CycleEvent(cycle, CycleEventKind.STOPPED)))
    }

    /**
     * Süren döngünün bir hedefi döngünün o adımda istediğinin dışına elle değiştirildi ([target] = değişen
     * cihaz/kanal, [groupIds] = onu içeren gruplar, [to] = yeni durum): "elle değiştirilince dur" seçili döngüler
     * bitiş eylemi uygulanmadan durur (kontrol kullanıcıda).
     */
    fun manualChange(
        cycles: List<Cycle>, runs: Map<String, CycleRun>, done: Map<String, Long>,
        target: CycleTarget, groupIds: Set<String>, to: Boolean,
    ): CycleResult {
        val stopped = cycles.filter { c ->
            val run = runs[c.id] ?: return@filter false
            val wanted = c.steps.getOrNull(run.step)?.states ?: return@filter false
            c.stopOnManual && c.targets.any { t -> (t == target || t.deviceId in groupIds) && wanted[t.key] != to }
        }
        if (stopped.isEmpty()) return CycleResult(runs, done)
        return CycleResult(runs - stopped.map { it.id }.toSet(), done, events = stopped.map { CycleEvent(it, CycleEventKind.OVERRIDDEN) })
    }

    fun tick(
        cycles: List<Cycle>, runs: Map<String, CycleRun>, done: Map<String, Long>, states: Map<String, Boolean>,
        now: Long, away: Boolean, zone: TimeZone = TimeZone.getDefault(), random: () -> Double = Math::random,
    ): CycleResult {
        val byId = cycles.associateBy { it.id }
        val newRuns = runs.filterKeys { it in byId }.toMutableMap()
        val newDone = done.filterKeys { it in byId }.toMutableMap()
        val commands = mutableListOf<CycleCommand>()
        val events = mutableListOf<CycleEvent>()

        // 1) Sürenleri ilerlet / durdur.
        for ((id, run) in newRuns.toMap()) {
            val c = byId.getValue(id)
            val stopNow = !c.valid || !c.enabled ||
                (!run.manual && run.windowEnd > 0 && now >= run.windowEnd) ||
                (!run.manual && !presenceOk(c, away))
            if (stopNow) {
                newRuns.remove(id)
                commands += finish(c, run)
                events += CycleEvent(c, if (run.windowEnd in 1..now) CycleEventKind.FINISHED else CycleEventKind.STOPPED)
                continue
            }
            if (now < run.stepEndsAt) continue
            val (next, cmds, finished) = advance(c, run, now, random)
            commands += cmds
            if (finished) {
                newRuns.remove(id)
                events += CycleEvent(c, CycleEventKind.FINISHED)
            } else {
                newRuns[id] = next!!
            }
        }

        // 2) Kendiliğinden başlayanlar.
        for (c in cycles) {
            if (!c.enabled || !c.valid || c.start == CycleStart.MANUAL || c.id in newRuns || !presenceOk(c, away)) continue
            val chance = opportunity(c, now, zone) ?: continue
            if (newDone[c.id] == chance.first) continue
            newDone[c.id] = chance.first
            val (run, cmds) = begin(c, now, manual = false, windowEnd = chance.second, states = states, random = random)
            newRuns[c.id] = run
            commands += cmds
            events += CycleEvent(c, CycleEventKind.STARTED)
        }
        return CycleResult(newRuns, newDone, dedupe(commands), events)
    }

    /** Sıradaki uyanma anı (adım sonu, aralık sonu ya da sıradaki kendiliğinden başlama). */
    fun nextDueAt(cycles: List<Cycle>, runs: Map<String, CycleRun>, now: Long, zone: TimeZone = TimeZone.getDefault()): Long? {
        val times = mutableListOf<Long>()
        for (r in runs.values) {
            times += r.stepEndsAt
            if (r.windowEnd > 0) times += r.windowEnd
        }
        for (c in cycles) if (c.enabled && c.valid && c.start != CycleStart.MANUAL && c.id !in runs) {
            nextStart(c, now, zone)?.let { times += it }
        }
        return times.minOrNull()
    }

    /** Kendiliğinden başlayan döngünün şimdiden sonraki ilk başlama anı. */
    fun nextStart(c: Cycle, now: Long, zone: TimeZone = TimeZone.getDefault()): Long? {
        if (c.start == CycleStart.MANUAL) return null
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        for (d in 0..7) {
            val day = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, d)
                set(Calendar.HOUR_OF_DAY, c.startMinute / 60); set(Calendar.MINUTE, c.startMinute % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (day.timeInMillis > now && c.days and (1 shl ActiveWindow.dayIndex(day)) != 0) return day.timeInMillis
        }
        return null
    }

    /**
     * Şu an bir başlama fırsatı içinde miyiz: (fırsatın başlangıcı, aralık sonu ya da 0).
     * [CycleStart.AT_TIME]: başlama saatinden sonraki [LATE_START_MS] içinde. [CycleStart.WINDOW]: aralığın içinde.
     */
    fun opportunity(c: Cycle, now: Long, zone: TimeZone = TimeZone.getDefault()): Pair<Long, Long>? {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = now }
        // Bugün ve dün başlayan fırsatlar (gece yarısını geçen aralık).
        for (back in 0..1) {
            val s = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -back)
                set(Calendar.HOUR_OF_DAY, c.startMinute / 60); set(Calendar.MINUTE, c.startMinute % 60)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (c.days and (1 shl ActiveWindow.dayIndex(s)) == 0) continue
            val startAt = s.timeInMillis
            if (now < startAt) continue
            when (c.start) {
                CycleStart.AT_TIME -> if (now < startAt + LATE_START_MS) return startAt to 0L
                CycleStart.WINDOW -> {
                    val length = Math.floorMod(c.endMinute - c.startMinute, 1440).let { if (it == 0) 1440 else it }
                    val e = (s.clone() as Calendar).apply { add(Calendar.MINUTE, length) }.timeInMillis
                    if (now < e) return startAt to e
                }
                CycleStart.MANUAL -> return null
            }
        }
        return null
    }

    private fun presenceOk(c: Cycle, away: Boolean) = c.presence == null || (c.presence == PresenceRule.AWAY) == away

    private fun begin(
        c: Cycle, now: Long, manual: Boolean, windowEnd: Long, states: Map<String, Boolean>, random: () -> Double,
    ): Pair<CycleRun, List<CycleCommand>> {
        val before = c.targets.mapNotNull { t -> states[t.key]?.let { t.key to it } }.toMap()
        val run = CycleRun(c.id, now, manual, 0, 0, now + duration(c, 0, random), before, windowEnd)
        return run to stepCommands(c, 0)
    }

    /** Vakti gelen adımları geçer (telefon uyuduysa birkaçını birden); yalnızca son durum gönderilir. */
    private fun advance(c: Cycle, run: CycleRun, now: Long, random: () -> Double): Triple<CycleRun?, List<CycleCommand>, Boolean> {
        var step = run.step
        var round = run.round
        var endsAt = run.stepEndsAt
        val cmds = mutableListOf<CycleCommand>()
        var guard = 0
        while (endsAt <= now) {
            step++
            if (step >= c.steps.size) {
                step = 0
                round++
                if (c.repeat in 1..round) return Triple(null, dedupe(cmds + finish(c, run)), true)
            }
            cmds += stepCommands(c, step)
            // Çok geride kaldıysa (telefon saatlerce kapalıydı) ritmi şimdiden yeniden kur.
            endsAt = (if (++guard > 1000) now else endsAt) + duration(c, step, random)
        }
        return Triple(run.copy(step = step, round = round, stepEndsAt = endsAt), dedupe(cmds), false)
    }

    private fun stepCommands(c: Cycle, step: Int): List<CycleCommand> {
        val states = c.steps[step].states
        return c.targets.mapNotNull { t -> states[t.key]?.let { CycleCommand(c, t, it) } }
    }

    private fun finish(c: Cycle, run: CycleRun): List<CycleCommand> = when (c.end) {
        CycleEnd.OFF -> c.targets.map { CycleCommand(c, it, false) }
        CycleEnd.ON -> c.targets.map { CycleCommand(c, it, true) }
        CycleEnd.KEEP -> emptyList()
        CycleEnd.RESTORE -> c.targets.mapNotNull { t -> run.before[t.key]?.let { CycleCommand(c, t, it) } }
    }

    private fun duration(c: Cycle, step: Int, random: () -> Double): Long {
        val base = c.steps[step].seconds * 1000L
        if (c.jitterPct <= 0) return base
        val factor = 1 + (random() * 2 - 1) * c.jitterPct / 100.0
        return (base * factor).toLong().coerceAtLeast(Cycle.MIN_STEP_SECONDS * 1000L)
    }

    /** Aynı hedefe birden çok komut çıktıysa yalnızca sonuncusu. */
    private fun dedupe(cmds: List<CycleCommand>): List<CycleCommand> =
        cmds.asReversed().distinctBy { it.cycle.id to it.target.key }.asReversed()

    /** Hazır kalıplar (düzenleyicideki "Hazır" seçenekleri). Süreler saniye. */
    object Presets {
        /** [seconds] sonra hepsini kapat ([on] ise aç). */
        fun countdown(targets: List<CycleTarget>, seconds: Int, on: Boolean) =
            Triple(listOf(CycleStep(seconds)), 1, if (on) CycleEnd.ON else CycleEnd.OFF)

        /** Aç, [seconds] açık kalsın, sonra kapat. */
        fun onFor(targets: List<CycleTarget>, seconds: Int) =
            Triple(listOf(CycleStep(seconds, targets.associate { it.key to true })), 1, CycleEnd.OFF)

        /** [onSec] açık, [offSec] kapalı, [repeat] tur. */
        fun blink(targets: List<CycleTarget>, onSec: Int, offSec: Int, repeat: Int) = Triple(
            listOf(CycleStep(onSec, targets.associate { it.key to true }), CycleStep(offSec, targets.associate { it.key to false })),
            repeat, CycleEnd.OFF,
        )

        /** Her hedef sırayla tek başına açık ([seconds] süreyle). */
        fun chase(targets: List<CycleTarget>, seconds: Int, repeat: Int) = Triple(
            targets.map { on -> CycleStep(seconds, targets.associate { it.key to (it == on) }) },
            repeat, CycleEnd.OFF,
        )
    }
}
