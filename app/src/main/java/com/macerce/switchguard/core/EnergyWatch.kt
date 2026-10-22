package com.macerce.switchguard.core

import java.util.Calendar

/** Tüketim uyarıları: günlük anormal tüketim ve aylık bütçe. Saf; ölçümler dışarıdan verilir. */
object EnergyWatch {

    /** Karşılaştırma için en az bu kadar geçerli gün gerekir. */
    const val MIN_DAYS = 7
    /** Geriye bakılan gün sayısı. */
    const val HISTORY_DAYS = 14
    /** Ortalamadan bu oran kadar sapma anormal sayılır (%50). */
    const val RATIO = 0.5
    /** Çok küçük tüketimlerde oran yanıltır: fark en az bu kadar olmalı. */
    const val MIN_DIFF_KWH = 0.3
    /** Günün "ölçülmüş" sayılması için en az ölçüm sayısı (izleme günün çoğunda açıkken 5 dk'da bir yazılır). */
    const val MIN_SAMPLES = 200

    data class Anomaly(val high: Boolean, val kwh: Double, val average: Double)

    /** [day]: dünkü tüketim; [history]: önceki geçerli günlerin tüketimleri. */
    fun anomaly(day: Double, history: List<Double>): Anomaly? {
        if (history.size < MIN_DAYS) return null
        val avg = history.average()
        if (avg <= 0) return null
        return when {
            day >= avg * (1 + RATIO) && day - avg >= MIN_DIFF_KWH -> Anomaly(true, day, avg)
            day <= avg * (1 - RATIO) && avg - day >= MIN_DIFF_KWH -> Anomaly(false, day, avg)
            else -> null
        }
    }

    /** Ortalamaya göre fark yüzdesi (her zaman pozitif). */
    fun percent(a: Anomaly): Int = kotlin.math.abs(((a.kwh / a.average - 1) * 100).toInt())

    fun budgetExceeded(cost: Double, budget: Double): Boolean = budget > 0 && cost >= budget

    /** "2026-10": bütçe uyarısı ayda bir kez. */
    fun monthKey(time: Long): String = Calendar.getInstance().run {
        timeInMillis = time
        "%04d-%02d".format(get(Calendar.YEAR), get(Calendar.MONTH) + 1)
    }

    fun startOfMonth(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    /** [time]'ın gününün başlangıcından [days] gün önce/sonra (yaz saati değişiminde de doğru). */
    fun dayStart(time: Long, days: Int = 0): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, days)
        timeInMillis
    }
}

/**
 * Bekçi: iki cihaz (telefon + TV/tablet/ikinci telefon) birbirini Telegram üzerinden izler. Gönderen, kişiyle olan
 * sohbete sabitlenmiş bir mesajı düzenli aralıklarla günceller; denetleyen o sabit mesajın son düzenlenme zamanına bakar.
 * Botlar birbirinin mesajını okuyamadığı için sabit mesaj kullanılır (getChat herkese açık bilgiyi döner).
 */
object Watchdog {
    /** Gönderen mesajı bu aralıkla günceller. */
    const val BEAT_MS = 10 * 60_000L
    /** Denetleyen bu aralıkla bakar. */
    const val CHECK_MS = 5 * 60_000L
    val WATCH_MINUTES = listOf(30, 60, 120)

    /** Sabit mesajın ilk karakterleri: çalışıyor / kullanıcı izlemeyi kapattı. */
    const val RUNNING = "🟢"
    const val STOPPED = "⏸"

    sealed class Seen {
        data class Beat(val name: String, val at: Long) : Seen()
        data class Stopped(val name: String) : Seen()
        data object None : Seen()
    }

    /** getChat yanıtındaki sabit mesajı okur. İkinci satır gönderen cihazın adıdır. */
    fun read(chatJson: String): Seen {
        val pinned = org.json.JSONObject(chatJson).optJSONObject("result")?.optJSONObject("pinned_message") ?: return Seen.None
        val text = pinned.optString("text")
        val name = text.lines().getOrNull(1)?.trim().orEmpty()
        val at = (pinned.optLong("edit_date").takeIf { it > 0 } ?: pinned.optLong("date")) * 1000
        return when {
            text.startsWith(RUNNING) -> Seen.Beat(name, at)
            text.startsWith(STOPPED) -> Seen.Stopped(name)
            else -> Seen.None
        }
    }

    /** Sessizlik [limitMs]'yi aştı mı. */
    fun silent(lastBeat: Long, now: Long, limitMs: Long): Boolean = lastBeat > 0 && now - lastBeat >= limitMs
}
