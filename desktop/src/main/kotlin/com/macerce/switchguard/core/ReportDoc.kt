package com.macerce.switchguard.core

/**
 * Raporların ortak yapısı: uygulama ekranında, PDF'te ve Telegram'da aynı içerik farklı biçimde gösterilir.
 * Okuyan kişi her şeyin ne anlama geldiğini bilsin diye her göstergenin altında açıklaması, sonunda terimler var.
 */
data class ReportDoc(
    val title: String,
    /** Dönem ("13–19 Ekim 2026") ve kapsam ("4 cihaz"). */
    val subtitle: String,
    /** "Bu rapor neyi gösterir": iki üç cümle. */
    val intro: String,
    val kpis: List<Kpi>,
    val sections: List<ReportSection>,
    /** Terim → açıklama. */
    val glossary: List<Pair<String, String>> = emptyList(),
    /** Altta küçük not (tahmini değerler, kaynak). */
    val footnote: String = "",
) {
    /** Yapılacaklar: bulgulardaki öneriler, en önemliden. */
    val actions: List<String>
        get() = sections.flatMap { it.findings }.filter { it.action != null }
            .sortedBy { it.severity.ordinal }.mapNotNull { it.action }.distinct()
}

/** Ana gösterge: ad, değer, ne anlama geldiği; önceki döneme göre değişim. */
data class Kpi(
    val label: String,
    val value: String,
    val caption: String,
    val trend: Trend? = null,
    val tone: Tone = Tone.NEUTRAL,
)

/** "Geçen haftaya göre %12 az" ve bunun iyi mi kötü mü olduğu. */
data class Trend(val text: String, val tone: Tone)

enum class Tone { GOOD, NEUTRAL, WARN, BAD }

data class ReportSection(
    val title: String,
    /** Bölümde neye bakıldığı (bir cümle). */
    val explain: String? = null,
    val findings: List<Finding> = emptyList(),
    /** İsteğe bağlı tablo: başlıklar ve satırlar (ilk sütun ad, diğerleri sağa yaslı). */
    val table: ReportTable? = null,
)

/** Bulgu: önem derecesi, düz cümle ve varsa yapılması önerilen. */
data class Finding(val severity: Severity, val text: String, val action: String? = null)

/** Sıra önem sırasıdır (en önemli önce). */
enum class Severity { ALERT, WARN, INFO, GOOD }

data class ReportTable(val headers: List<String>, val rows: List<List<String>>, val note: String? = null)

/** Önceki döneme göre değişimin yönü ve tonu. Saf; metinleri çağıran kurar. */
object Compare {
    /**
     * [now] ile [before] karşılaştırması: yüzde değişim (önceki 0 ise null). [lowerIsBetter]: kesinti, tepki süresi,
     * tüketim gibi azalması iyi olan değerler. %5'ten küçük değişim "aynı" sayılır.
     */
    fun change(now: Double, before: Double): Int? = if (before <= 0) null else (((now - before) / before) * 100).toInt()

    fun tone(percent: Int?, lowerIsBetter: Boolean): Tone = when {
        percent == null || kotlin.math.abs(percent) < SAME_PERCENT -> Tone.NEUTRAL
        (percent < 0) == lowerIsBetter -> Tone.GOOD
        else -> Tone.WARN
    }

    const val SAME_PERCENT = 5
}
