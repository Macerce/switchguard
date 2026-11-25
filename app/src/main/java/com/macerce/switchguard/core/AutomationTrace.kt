package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Kural izi ("neden çalıştı / neden çalışmadı"): her tetiklenişte saat aralığı, koşullar ve evde/dışarıda sınaması,
 * sonra her eylemin sonucu. Art arda aynı nedenle engellenen tetiklenişler tek satırda sayılır ([TraceEntry.count]).
 * Saf; kayıt dışarıda (RuntimeState) tutulur.
 */
enum class TraceOutcome {
    RAN,
    /** Gecikmeli: eylem [Automation.delayMinutes] sonra; vakti gelince ayrı bir [RAN] satırı yazılır. */
    DELAYED,
    BLOCKED_WINDOW, BLOCKED_CONDITION, BLOCKED_PRESENCE,
    /** 10 sn içinde ikinci kez tetiklendi (birbirini tetikleyen kurallar döngüye girmesin). */
    THROTTLED,
    /** Döngü koruması: bir saatte çok kez çalıştı; kuralın cihazları kapatıldı, alarm verildi, kural durduruldu. */
    LOCKED_OUT,
    /** Önceki çalışması sürüyordu (tek kip) ya da sıra/aynı anda sınırı doluydu: yeni tetikleniş yok sayıldı. */
    BUSY,
    /** Elle çalıştırıldı ("Şimdi çalıştır", bekleme ekranı kartı, başka kuraldan): tetikleyici ve koşullar atlandı. */
    MANUAL;

    val blocked get() = this != RAN && this != DELAYED && this != MANUAL
    /** Eylemleri çalıştı (adım sonuçları bu satıra yazılır). */
    val ran get() = this == RAN || this == MANUAL
}

/** Bir kuralın şu anki sınaması: saat aralığı, her koşulun tek tek sonucu, evde/dışarıda. */
data class Verdict(val windowOk: Boolean, val conds: List<Boolean>, val condsOk: Boolean, val presenceOk: Boolean) {
    val ok get() = windowOk && condsOk && presenceOk
    val outcome: TraceOutcome get() = when {
        !windowOk -> TraceOutcome.BLOCKED_WINDOW
        !condsOk -> TraceOutcome.BLOCKED_CONDITION
        !presenceOk -> TraceOutcome.BLOCKED_PRESENCE
        else -> TraceOutcome.RAN
    }
}

enum class StepResult {
    /** Komut gitti / bildirim verildi / sahne başlatıldı. */
    DONE,
    FAILED,
    /** Hedef (cihaz, sahne, döngü) artık yok. */
    MISSING,
    /** Sessiz saatlerde bildirim yok sayıldı. */
    SILENCED,
    /** "Şu olana kadar bekle" süresi doldu. */
    TIMEOUT,
    /** Güvenlik sınırı: çok fazla adım/tekrar ya da iç içe kural çalıştırma; kural yarıda kesildi. */
    LIMIT,
}

/** [step]: [Automation.flatActions] sırası (0 = ilk eylem; iç içe adımlar dahil). */
data class TraceStep(val step: Int, val result: StepResult, val at: Long, val error: String = "")

data class TraceEntry(
    val automationId: String,
    val at: Long,
    val outcome: TraceOutcome,
    /** Koşulların tek tek sonucu (kuraldaki sırayla). */
    val conds: List<Boolean> = emptyList(),
    /** Tetikleyicinin o anki ölçümü (güç W, sensör, dış sıcaklık); yoksa null. */
    val value: Double? = null,
    val away: Boolean? = null,
    val steps: List<TraceStep> = emptyList(),
    /** Aynı nedenle art arda kaç kez engellendi; [lastAt] sonuncusu. */
    val count: Int = 1,
    val lastAt: Long = at,
    /** Hangi tetikleyici (0 = ilki; ek tetikleyiciler 1..). */
    val trigger: Int = 0,
)

object AutomationTraces {
    const val PER_AUTOMATION = 15
    const val MAX_TOTAL = 400

    /**
     * Yeni satır ekler (en yeni sonda). Engellenen tetikleniş, aynı kuralın son satırıyla aynı neden ve koşul
     * sonuçlarındaysa yeni satır açılmaz: o satırın sayısı artar.
     */
    fun add(list: List<TraceEntry>, e: TraceEntry): List<TraceEntry> {
        val lastIdx = list.indexOfLast { it.automationId == e.automationId }
        val last = list.getOrNull(lastIdx)
        if (last != null && e.outcome.blocked && last.outcome == e.outcome && last.conds == e.conds) {
            return list.toMutableList().also { it[lastIdx] = last.copy(count = last.count + 1, lastAt = e.at, value = e.value ?: last.value) }
        }
        val out = (list + e).toMutableList()
        // Kural başına sınır: o kuralın en eskileri atılır; sonra toplam sınır.
        val mine = out.filter { it.automationId == e.automationId }
        if (mine.size > PER_AUTOMATION) out.removeAll(mine.take(mine.size - PER_AUTOMATION).toSet())
        return if (out.size > MAX_TOTAL) out.takeLast(MAX_TOTAL) else out
    }

    /** Eylem sonucunu o kuralın en son çalışan satırına ekler; aynı adımın eski sonucu (yeniden deneme) yenilenir. */
    fun step(list: List<TraceEntry>, automationId: String, s: TraceStep): List<TraceEntry> {
        val idx = list.indexOfLast { it.automationId == automationId && it.outcome.ran }
        if (idx < 0) return list
        val e = list[idx]
        return list.toMutableList().also { it[idx] = e.copy(steps = e.steps.filterNot { x -> x.step == s.step } + s) }
    }

    /** Silinen kuralların izleri atılır. */
    fun prune(list: List<TraceEntry>, ids: Set<String>): List<TraceEntry> = list.filter { it.automationId in ids }

    /** Bir kuralın izleri, en yeni önce. */
    fun of(list: List<TraceEntry>, automationId: String): List<TraceEntry> =
        list.filter { it.automationId == automationId }.sortedByDescending { it.lastAt }

    fun toJson(list: List<TraceEntry>): String = JSONArray().apply {
        list.forEach { e ->
            put(JSONObject().put("a", e.automationId).put("t", e.at).put("o", e.outcome.name).apply {
                if (e.conds.isNotEmpty()) put("c", JSONArray(e.conds))
                e.value?.let { put("v", it) }
                e.away?.let { put("w", it) }
                if (e.count > 1) put("n", e.count).put("l", e.lastAt)
                if (e.trigger > 0) put("g", e.trigger)
                if (e.steps.isNotEmpty()) put("s", JSONArray().apply {
                    e.steps.forEach { s -> put(JSONObject().put("i", s.step).put("r", s.result.name).put("t", s.at).apply { if (s.error.isNotEmpty()) put("e", s.error) }) }
                })
            })
        }
    }.toString()

    /** Bozuk kayıtlar atlanır. */
    fun fromJson(json: String?): List<TraceEntry> {
        val arr = runCatching { JSONArray(json ?: return emptyList()) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                val at = o.getLong("t")
                TraceEntry(
                    automationId = o.getString("a"),
                    at = at,
                    outcome = TraceOutcome.valueOf(o.getString("o")),
                    conds = o.optJSONArray("c")?.let { c -> (0 until c.length()).map { c.getBoolean(it) } }.orEmpty(),
                    value = if (o.has("v")) o.getDouble("v") else null,
                    away = if (o.has("w")) o.getBoolean("w") else null,
                    steps = o.optJSONArray("s")?.let { s ->
                        (0 until s.length()).mapNotNull { j ->
                            runCatching {
                                val x = s.getJSONObject(j)
                                TraceStep(x.getInt("i"), StepResult.valueOf(x.getString("r")), x.getLong("t"), x.optString("e"))
                            }.getOrNull()
                        }
                    }.orEmpty(),
                    count = o.optInt("n", 1),
                    lastAt = o.optLong("l", at),
                    trigger = o.optInt("g", 0),
                )
            }.getOrNull()
        }
    }
}
