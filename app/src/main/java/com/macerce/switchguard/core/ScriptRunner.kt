package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Kuralın adımlarını yürütür (Home Assistant "script" mantığı): sırayla eylemler, bekle, şu olana kadar bekle, eğer/
 * değilse, tekrarla, dur, başka kuralı çalıştır. Saf ve tek thread'den çağrılır: cihaz komutlarını kendisi göndermez,
 * [Out] olarak döndürür; servis uygular. Bekleyen çalışma ([Run]) kalıcıdır: servis yeniden başlasa da kaldığı
 * adımdan sürer. Kural bu arada düzenlendiyse ve konum artık yoksa çalışma sessizce biter.
 */
object ScriptRunner {
    /** Bir tek [advance] çağrısında en çok bu kadar adım işlenir (beklemesiz sonsuz tekrar telefonu kilitlemesin). */
    const val STEP_BUDGET = 500
    /** Bir tekrar bloğunun üst sınırı. */
    const val MAX_REPEAT = 100
    /** Kural içinden kural çalıştırma derinliği. */
    const val MAX_DEPTH = 3

    /**
     * Adım listesindeki konum. [branch]: 'r' = kuralın üst listesi, 't' = üstteki bloğun "ise"/tekrar gövdesi,
     * 'e' = "değilse". [index]: sıradaki adım. [iter]: tekrar gövdesinde kaçıncı tur. [waited]: bu adımın ön beklemesi
     * ([AutoAction.waitMinutes]) yapıldı.
     */
    data class Frame(val branch: Char, val index: Int, val iter: Int = 0, val waited: Boolean = false)

    /**
     * Süren bir çalışma. [resumeAt]: zamanlı bekleme bitişi (0 = beklemiyor). [untilDeadline]: "şu olana kadar bekle"
     * sürüyor (Long.MAX = süresiz; 0 = yok). [queued]: sırada, henüz başlamadı.
     */
    data class Run(
        val id: String,
        val automationId: String,
        val frames: List<Frame>,
        val startedAt: Long,
        val resumeAt: Long = 0,
        val untilDeadline: Long = 0,
        val triggerIndex: Int? = 0,
        val depth: Int = 0,
        val queued: Boolean = false,
    ) {
        val waitingForCondition get() = untilDeadline > 0
    }

    sealed interface Out {
        /** Bu eylemi uygula; [step] = [Automation.flatActions] sırası. */
        data class Do(val action: AutoAction, val step: Int) : Out
        /** Başka kuralı çalıştır (derinlik [depth]). */
        data class Start(val automationId: String, val depth: Int, val step: Int) : Out
        /** Akış adımının sonucu (iz için): bekleme süresi doldu, sınır aşıldı. */
        data class Note(val step: Int, val result: StepResult) : Out
    }

    /** [run] null = çalışma bitti. */
    data class Result(val run: Run?, val out: List<Out>)

    fun start(a: Automation, now: Long, runId: String, triggerIndex: Int? = 0, depth: Int = 0, queued: Boolean = false): Run =
        Run(runId, a.id, listOf(Frame('r', 0)), now, triggerIndex = triggerIndex, depth = depth, queued = queued)

    /** Çerçevelerin gösterdiği liste; konum artık yoksa (kural düzenlendi) null. */
    private fun listAt(a: Automation, frames: List<Frame>, upTo: Int): List<AutoAction>? {
        var list = a.allActions
        for (k in 1..upTo) {
            val parent = list.getOrNull(frames[k - 1].index) ?: return null
            list = when (frames[k].branch) {
                't' -> parent.then
                'e' -> parent.otherwise
                else -> return null
            }
        }
        return list
    }

    /** Konumdaki adımın [Automation.flatActions] sırası. */
    fun stepNumber(a: Automation, frames: List<Frame>): Int {
        var n = 0
        var list = a.allActions
        for ((k, f) in frames.withIndex()) {
            // Önceki kardeşler (ve içleri).
            for (i in 0 until f.index.coerceAtMost(list.size)) n += list[i].flatten().size
            val step = list.getOrNull(f.index) ?: return n
            val next = frames.getOrNull(k + 1) ?: return n
            n += 1 // adımın kendisi
            if (next.branch == 'e') n += step.then.sumOf { it.flatten().size }
            list = if (next.branch == 't') step.then else step.otherwise
        }
        return n
    }

    /**
     * Çalışmayı ilerletir: bekleme bitmediyse (zaman ya da koşul) dokunmaz; bitti ise sıradaki beklemeye ya da sona
     * kadar yürür. [test]: koşul kümesi şu an sağlanıyor mu.
     */
    fun advance(a: Automation, run: Run, now: Long, test: (CondSet) -> Boolean): Result {
        if (run.queued) return Result(run, emptyList())
        if (run.resumeAt > 0 && now < run.resumeAt && !run.waitingForCondition) return Result(run, emptyList())
        val out = mutableListOf<Out>()
        val frames = run.frames.toMutableList()
        var untilDeadline = run.untilDeadline
        var budget = STEP_BUDGET
        fun finish() = Result(null, out)
        fun waitFor(at: Long) = Result(run.copy(frames = frames.toList(), resumeAt = at, untilDeadline = 0), out)

        while (true) {
            if (--budget < 0) {
                out += Out.Note(stepNumber(a, frames), StepResult.LIMIT)
                return finish()
            }
            if (frames.isEmpty()) return finish()
            val top = frames.size - 1
            val list = listAt(a, frames, top) ?: return finish()
            val f = frames[top]
            if (f.index >= list.size) {
                // Liste bitti: üst bloğa dön.
                frames.removeAt(top)
                if (frames.isEmpty()) return finish()
                val ptop = frames.size - 1
                val pf = frames[ptop]
                val parent = listAt(a, frames, ptop)?.getOrNull(pf.index) ?: return finish()
                if (parent.kind == ActionKind.REPEAT && f.branch == 't') {
                    val nextIter = f.iter + 1
                    val again = nextIter < MAX_REPEAT && if (parent.count > 0) nextIter < parent.count else parent.cond?.let(test) == true
                    if (nextIter >= MAX_REPEAT) out += Out.Note(stepNumber(a, frames), StepResult.LIMIT)
                    if (again) { frames += Frame('t', 0, nextIter); continue }
                }
                frames[ptop] = pf.copy(index = pf.index + 1, waited = false)
                continue
            }
            val step = list[f.index]
            val no = stepNumber(a, frames)
            fun next() { frames[top] = f.copy(index = f.index + 1, waited = false) }

            // 2.55 öncesi biçim: ek eylemin kendi ön beklemesi (yalnız üst listede).
            if (step.waitMinutes > 0 && !f.waited && top == 0) {
                frames[top] = f.copy(waited = true)
                val at = now + step.waitMinutes * 60_000L
                return waitFor(at)
            }
            when (step.kind) {
                ActionKind.DELAY -> {
                    if (step.seconds > 0 && !f.waited) {
                        frames[top] = f.copy(waited = true)
                        return waitFor(now + step.seconds * 1000L)
                    }
                    next()
                }
                ActionKind.WAIT_UNTIL -> {
                    val c = step.cond
                    when {
                        c == null || test(c) -> { untilDeadline = 0; next() }
                        untilDeadline in 1..now -> {
                            // Süre doldu.
                            untilDeadline = 0
                            out += Out.Note(no, StepResult.TIMEOUT)
                            if (step.stopOnTimeout) return finish()
                            next()
                        }
                        else -> {
                            val deadline = if (untilDeadline > 0) untilDeadline
                            else if (step.seconds > 0) now + step.seconds * 1000L else Long.MAX_VALUE
                            return Result(run.copy(frames = frames.toList(), resumeAt = if (deadline == Long.MAX_VALUE) 0 else deadline, untilDeadline = deadline), out)
                        }
                    }
                }
                ActionKind.IF -> {
                    val yes = step.cond?.let(test) ?: true
                    val branch = if (yes) step.then else step.otherwise
                    if (branch.isEmpty()) next() else frames += Frame(if (yes) 't' else 'e', 0)
                }
                ActionKind.REPEAT -> {
                    val enter = step.then.isNotEmpty() && if (step.count > 0) true else step.cond?.let(test) == true
                    if (enter) frames += Frame('t', 0, 0) else next()
                }
                ActionKind.STOP -> return finish()
                ActionKind.RUN_AUTOMATION -> {
                    if (run.depth + 1 >= MAX_DEPTH) out += Out.Note(no, StepResult.LIMIT)
                    else out += Out.Start(step.deviceId, run.depth + 1, no)
                    next()
                }
                else -> {
                    out += Out.Do(step, no)
                    next()
                }
            }
        }
    }

    fun toJson(list: List<Run>): String = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().put("id", r.id).put("a", r.automationId).put("s", r.startedAt).put("d", r.depth).apply {
                if (r.resumeAt > 0) put("r", r.resumeAt)
                if (r.untilDeadline > 0) put("u", r.untilDeadline)
                r.triggerIndex?.let { put("t", it) }
                if (r.queued) put("q", true)
                put("f", JSONArray().apply {
                    r.frames.forEach { f -> put(JSONObject().put("b", f.branch.toString()).put("i", f.index).put("n", f.iter).put("w", f.waited)) }
                })
            })
        }
    }.toString()

    fun fromJson(json: String?): List<Run> = runCatching {
        val arr = JSONArray(json ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val f = o.getJSONArray("f")
                Run(
                    id = o.getString("id"), automationId = o.getString("a"),
                    frames = (0 until f.length()).map { j ->
                        f.getJSONObject(j).let { Frame(it.getString("b").first(), it.getInt("i"), it.optInt("n"), it.optBoolean("w")) }
                    },
                    startedAt = o.getLong("s"),
                    resumeAt = o.optLong("r", 0), untilDeadline = o.optLong("u", 0),
                    triggerIndex = if (o.has("t")) o.getInt("t") else null,
                    depth = o.optInt("d", 0), queued = o.optBoolean("q", false),
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    /**
     * 2.54 kaydı ([ActionSteps.Due]: kuralın n. ek eylemi şu vakitte) yeni biçime: o adımın ön beklemesi yapılmış olarak
     * vaktinde sürer. Aynı kuralın birden çok bekleyen adımı varsa en erkeni yeter (sonrakiler sırayla gelir).
     */
    fun fromLegacy(due: List<ActionSteps.Due>): List<Run> = due.groupBy { it.automationId }.map { (id, list) ->
        val d = list.minBy { it.at }
        Run("legacy-$id", id, listOf(Frame('r', d.step + 1, waited = true)), d.at, resumeAt = d.at)
    }
}

/**
 * Çalışma kipine göre yeni tetiklenişin ne olacağı (saf). [active]: kuralın şu an süren (bekleyen) çalışmaları,
 * [queued]: sıradakiler.
 */
object RunPolicy {
    enum class Decision { START, START_CANCEL_OTHERS, QUEUE, SKIP }

    fun decide(mode: RunMode, maxRuns: Int, active: Int, queued: Int): Decision = when {
        active == 0 && queued == 0 -> if (mode == RunMode.RESTART) Decision.START_CANCEL_OTHERS else Decision.START
        mode == RunMode.RESTART -> Decision.START_CANCEL_OTHERS
        mode == RunMode.SINGLE -> Decision.SKIP
        mode == RunMode.QUEUED -> if (active + queued < maxRuns) Decision.QUEUE else Decision.SKIP
        else -> if (active < maxRuns) Decision.START else Decision.SKIP
    }
}
