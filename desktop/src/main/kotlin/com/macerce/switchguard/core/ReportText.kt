package com.macerce.switchguard.core

/** [ReportDoc]'un Telegram metni (HTML biçimi: kalın başlıklar). Telegram sınırı 4096 karakter. */
object ReportText {
    private const val MAX = 3900

    fun telegramHtml(doc: ReportDoc, actionsTitle: String, whatToDo: String): String {
        val b = StringBuilder()
        b.append("📅 <b>").append(esc(doc.title)).append("</b>\n<i>").append(esc(doc.subtitle)).append("</i>\n\n")
        doc.kpis.forEach { k ->
            b.append(toneMark(k.tone)).append(" <b>").append(esc(k.label)).append(":</b> ").append(esc(k.value))
            k.trend?.let { b.append(" <i>(").append(esc(it.text)).append(")</i>") }
            b.append('\n')
        }
        doc.sections.forEach { s ->
            val shown = s.findings.filter { it.severity != Severity.GOOD || s.findings.size == 1 }
            if (shown.isEmpty()) return@forEach
            b.append("\n<b>").append(esc(s.title)).append("</b>\n")
            shown.forEach { f ->
                b.append(severityMark(f.severity)).append(' ').append(esc(f.text)).append('\n')
                f.action?.let { b.append("   <i>").append(esc(whatToDo)).append(' ').append(esc(it)).append("</i>\n") }
            }
        }
        if (doc.actions.isNotEmpty()) {
            b.append("\n<b>").append(esc(actionsTitle)).append("</b>\n")
            doc.actions.forEachIndexed { i, a -> b.append(i + 1).append(". ").append(esc(a)).append('\n') }
        }
        val text = b.toString().trimEnd()
        // Kesilirse açık etiket kalmasın diye satır sınırında kesilir.
        return if (text.length <= MAX) text else text.take(MAX).substringBeforeLast('\n') + "\n…"
    }

    fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun toneMark(t: Tone) = when (t) {
        Tone.GOOD -> "🟢"
        Tone.NEUTRAL -> "🔹"
        Tone.WARN -> "🟠"
        Tone.BAD -> "🔴"
    }

    private fun severityMark(s: Severity) = when (s) {
        Severity.ALERT -> "🔴"
        Severity.WARN -> "🟠"
        Severity.INFO -> "🔹"
        Severity.GOOD -> "✅"
    }
}
