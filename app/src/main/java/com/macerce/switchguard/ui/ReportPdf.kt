package com.macerce.switchguard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Tone

/**
 * [ReportDoc]'u A4 PDF olarak çizer: üst bant (başlık, dönem), "bu rapor neyi gösterir", açıklamalı gösterge kartları,
 * bölümler (ne inceleniyor, bulgular, tablolar), yapılacaklar, terimler ve sayfa altı. Enerji raporu grafiğini ve
 * dilim tablosunu [middle] / [end] kancalarıyla araya ekler.
 */
object ReportPdf {
    const val W = 595f // A4, 1/72 inç
    const val H = 842f
    const val M = 40f
    val ACCENT = Color.rgb(0x00, 0x79, 0x6B)
    val ACCENT_SOFT = Color.rgb(0xE0, 0xF2, 0xEF)
    val MUTED = Color.rgb(0x5F, 0x63, 0x68)
    val LINE = Color.rgb(0xDA, 0xDC, 0xE0)
    val ZEBRA = Color.rgb(0xF4, 0xF6, 0xF8)
    val CARD = Color.rgb(0xF7, 0xF8, 0xFA)
    private val GOOD = Color.rgb(0x2E, 0x7D, 0x32)
    private val WARN = Color.rgb(0xB2, 0x6A, 0x00)
    private val BAD = Color.rgb(0xC6, 0x28, 0x28)

    fun paint(size: Float, color: Int = Color.BLACK, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = align
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    /** Sayfaları sırayla açar; [doc] null iken çizmeden yalnızca sayfa sayar (alt bilgideki "Sayfa x / y" için). */
    class Pages(val doc: PdfDocument?, val footer: (Canvas, Int) -> Unit) {
        var count = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = M

        fun next() {
            finish()
            count++
            y = M
            if (doc != null) {
                page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), count).create())
                canvas = page!!.canvas
            }
        }

        fun finish() {
            val p = page ?: return
            footer(canvas!!, count)
            doc!!.finishPage(p)
            page = null
            canvas = null
        }

        /** [height] sığmıyorsa yeni sayfa; true döndürürse yeni sayfa açıldı. */
        fun ensure(height: Float): Boolean {
            if (y + height <= H - M - 20) return false
            next()
            return true
        }
    }

    /** İki geçişte çizer (sayfa sayısı, sonra asıl). */
    fun render(
        context: Context,
        doc: ReportDoc,
        footerLeft: String,
        middle: (Pages) -> Unit = {},
        end: (Pages) -> Unit = {},
    ): PdfDocument {
        val total = layout(context, doc, Pages(null) { _, _ -> }, middle, end).count
        val pdf = PdfDocument()
        val foot = paint(7.5f, MUTED)
        val footRight = paint(7.5f, MUTED, align = Paint.Align.RIGHT)
        layout(context, doc, Pages(pdf) { c, n ->
            c.drawLine(M, H - M - 4, W - M, H - M - 4, paint(0.5f, LINE))
            c.drawText(footerLeft, M, H - M + 8, foot)
            c.drawText(context.getString(R.string.energy_report_page, n, total), W - M, H - M + 8, footRight)
        }, middle, end).finish()
        return pdf
    }

    private fun layout(context: Context, doc: ReportDoc, pages: Pages, middle: (Pages) -> Unit, end: (Pages) -> Unit): Pages {
        pages.next()
        header(pages, doc)
        paragraphBox(pages, context.getString(R.string.report_what_title), doc.intro)
        kpis(pages, doc.kpis)
        middle(pages)
        doc.sections.forEach { section(pages, it) }
        if (doc.actions.isNotEmpty()) actions(pages, context.getString(R.string.report_actions_title), doc.actions)
        end(pages)
        if (doc.glossary.isNotEmpty()) glossary(pages, context.getString(R.string.report_glossary_title), doc.glossary)
        if (doc.footnote.isNotEmpty()) {
            pages.y += 6
            val note = paint(7.5f, MUTED)
            val lines = wrap(doc.footnote, note, W - 2 * M)
            // Dipnot bölünmesin: tek satırı sonraki sayfaya kalmasın.
            pages.ensure(lines.size * 10f)
            lines.forEach {
                pages.canvas?.drawText(it, M, pages.y + 8, note)
                pages.y += 10
            }
        }
        return pages
    }

    /** Renkli üst bant: başlık ve dönem. */
    private fun header(pages: Pages, doc: ReportDoc) {
        val c = pages.canvas
        c?.drawRoundRect(RectF(M, pages.y, W - M, pages.y + 58), 10f, 10f, fill(ACCENT))
        c?.drawText(doc.title, M + 16, pages.y + 26, paint(18f, Color.WHITE, bold = true))
        c?.drawText(doc.subtitle, M + 16, pages.y + 44, paint(9.5f, Color.argb(230, 255, 255, 255)))
        c?.drawText("SwitchGuard", W - M - 16, pages.y + 26, paint(10f, Color.WHITE, bold = true, align = Paint.Align.RIGHT))
        pages.y += 72
    }

    /** Açık zeminli kutu içinde başlık + paragraf ("Bu rapor neyi gösterir"). */
    fun paragraphBox(pages: Pages, title: String, text: String) {
        val body = paint(9f, Color.rgb(0x30, 0x33, 0x36))
        val lines = wrap(text, body, W - 2 * M - 24)
        val h = 24f + lines.size * 12f + 8
        pages.ensure(h)
        val c = pages.canvas
        c?.drawRoundRect(RectF(M, pages.y, W - M, pages.y + h), 8f, 8f, fill(ACCENT_SOFT))
        c?.drawText(title, M + 12, pages.y + 16, paint(9.5f, ACCENT, bold = true))
        lines.forEachIndexed { i, l -> c?.drawText(l, M + 12, pages.y + 30 + i * 12f, body) }
        pages.y += h + 14
    }

    /** Gösterge kartları: üç sütun; her kartta ad, büyük değer, değişim ve ne anlama geldiği. */
    fun kpis(pages: Pages, kpis: List<Kpi>) {
        if (kpis.isEmpty()) return
        val cols = 3
        val gap = 8f
        val w = (W - 2 * M - gap * (cols - 1)) / cols
        val label = paint(7.5f, MUTED, bold = true)
        val value = paint(15f, Color.BLACK, bold = true)
        val caption = paint(7f, MUTED)
        kpis.chunked(cols).forEach { row ->
            val captions = row.map { wrap(it.caption, caption, w - 16).take(4) }
            val h = 58f + (captions.maxOf { it.size }) * 9f + 6
            pages.ensure(h)
            row.forEachIndexed { i, k ->
                val x = M + i * (w + gap)
                val c = pages.canvas ?: return@forEachIndexed
                c.drawRoundRect(RectF(x, pages.y, x + w, pages.y + h), 8f, 8f, fill(CARD))
                c.drawRoundRect(RectF(x, pages.y, x + 3, pages.y + h), 1.5f, 1.5f, fill(toneColor(k.tone)))
                c.drawText(fit(k.label.uppercase(), label, w - 16), x + 10, pages.y + 14, label)
                c.drawText(fit(k.value, value, w - 16), x + 10, pages.y + 34, value)
                k.trend?.let { t -> c.drawText(fit(t.text, paint(7.5f, toneColor(t.tone), bold = true), w - 16), x + 10, pages.y + 46, paint(7.5f, toneColor(t.tone), bold = true)) }
                captions[i].forEachIndexed { j, l -> c.drawText(l, x + 10, pages.y + 58 + j * 9f, caption) }
            }
            pages.y += h + gap
        }
        pages.y += 6
    }

    fun sectionTitle(pages: Pages, title: String, explain: String?) {
        val ex = paint(8.5f, MUTED)
        val lines = explain?.let { wrap(it, ex, W - 2 * M) }.orEmpty()
        pages.ensure(26f + lines.size * 11f + 20)
        pages.canvas?.drawText(title, M, pages.y + 12, paint(12f, Color.BLACK, bold = true))
        pages.canvas?.drawRect(M, pages.y + 16, M + 28, pages.y + 18, fill(ACCENT))
        pages.y += 24
        lines.forEach { pages.canvas?.drawText(it, M, pages.y + 8, ex); pages.y += 11 }
        pages.y += 4
    }

    private fun section(pages: Pages, s: com.macerce.switchguard.core.ReportSection) {
        sectionTitle(pages, s.title, s.explain)
        s.findings.forEach { finding(pages, it) }
        s.table?.let { table(pages, it) }
        pages.y += 8
    }

    /** Bulgu: solda renkli işaret, metin; öneri varsa altında "→ öneri". */
    fun finding(pages: Pages, f: Finding) {
        val text = paint(9f)
        val act = paint(8.5f, ACCENT, bold = true)
        val lines = wrap(f.text, text, W - 2 * M - 22)
        val actLines = f.action?.let { wrap("→ $it", act, W - 2 * M - 22) }.orEmpty()
        val h = lines.size * 12f + actLines.size * 11f + 6
        pages.ensure(h)
        val c = pages.canvas
        c?.drawCircle(M + 6, pages.y + 6, 3.5f, fill(severityColor(f.severity)))
        lines.forEachIndexed { i, l -> c?.drawText(l, M + 16, pages.y + 9 + i * 12f, text) }
        actLines.forEachIndexed { i, l -> c?.drawText(l, M + 16, pages.y + 9 + lines.size * 12f + i * 11f, act) }
        pages.y += h + 2
    }

    /** Tablo: başlık satırı, zebra satırlar, isteğe bağlı açıklama notu. İlk sütun sola, diğerleri sağa yaslı. */
    fun table(pages: Pages, t: ReportTable) {
        val row = 15f
        val n = t.headers.size
        val first = (W - 2 * M) * if (n > 3) 0.34f else 0.5f
        val rest = (W - 2 * M - first) / (n - 1).coerceAtLeast(1)
        val head = paint(7.5f, MUTED, bold = true)
        val cell = paint(8.5f)
        fun line(values: List<String>, p: Paint, zebra: Boolean) {
            val c = pages.canvas
            if (c != null) {
                if (zebra) c.drawRect(M, pages.y, W - M, pages.y + row, fill(ZEBRA))
                values.forEachIndexed { i, v ->
                    val w = if (i == 0) first else rest
                    val x = if (i == 0) M else M + first + (i - 1) * rest
                    val pp = Paint(p).apply { textAlign = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT }
                    c.drawText(fit(v, pp, w - 8), if (i == 0) x + 4 else x + w - 4, pages.y + 10.5f, pp)
                }
            }
            pages.y += row
        }
        // Başlıklar dar sütunda kesilmesin: en çok iki satıra kaydırılır.
        val headLines = t.headers.mapIndexed { i, h -> wrap(h, head, (if (i == 0) first else rest) - 8).take(2) }
        val headHeight = 6f + 9f * (headLines.maxOfOrNull { it.size } ?: 1)
        fun headerRow() {
            pages.canvas?.let { c ->
                headLines.forEachIndexed { i, lines ->
                    val w = if (i == 0) first else rest
                    val x = if (i == 0) M else M + first + (i - 1) * rest
                    val pp = Paint(head).apply { textAlign = if (i == 0) Paint.Align.LEFT else Paint.Align.RIGHT }
                    lines.forEachIndexed { k, l -> c.drawText(fit(l, pp, w - 8), if (i == 0) x + 4 else x + w - 4, pages.y + 9f + k * 9f, pp) }
                }
            }
            pages.y += headHeight
            pages.canvas?.drawLine(M, pages.y, W - M, pages.y, paint(1f, LINE))
        }
        pages.ensure(headHeight + row * 2)
        headerRow()
        t.rows.forEachIndexed { i, r ->
            if (pages.ensure(row)) headerRow()
            line(r, cell, i % 2 == 1)
        }
        t.note?.let { note ->
            val p = paint(7.5f, MUTED)
            wrap(note, p, W - 2 * M).forEach { pages.ensure(10f); pages.canvas?.drawText(it, M, pages.y + 9, p); pages.y += 10 }
        }
        pages.y += 6
    }

    private fun actions(pages: Pages, title: String, actions: List<String>) {
        sectionTitle(pages, title, null)
        val p = paint(9f)
        actions.forEachIndexed { i, a ->
            val lines = wrap(a, p, W - 2 * M - 22)
            pages.ensure(lines.size * 12f + 4)
            pages.canvas?.drawText("${i + 1}.", M + 2, pages.y + 9, paint(9f, ACCENT, bold = true))
            lines.forEachIndexed { j, l -> pages.canvas?.drawText(l, M + 16, pages.y + 9 + j * 12f, p) }
            pages.y += lines.size * 12f + 4
        }
        pages.y += 8
    }

    private fun glossary(pages: Pages, title: String, items: List<Pair<String, String>>) {
        sectionTitle(pages, title, null)
        val term = paint(8.5f, Color.BLACK, bold = true)
        val def = paint(8.5f, MUTED)
        items.forEach { (k, v) ->
            val lines = wrap(v, def, W - 2 * M - 110)
            pages.ensure(lines.size * 11f + 4)
            pages.canvas?.drawText(fit(k, term, 100f), M, pages.y + 9, term)
            lines.forEachIndexed { j, l -> pages.canvas?.drawText(l, M + 110, pages.y + 9 + j * 11f, def) }
            pages.y += lines.size * 11f + 4
        }
    }

    private fun toneColor(t: Tone) = when (t) {
        Tone.GOOD -> GOOD
        Tone.NEUTRAL -> ACCENT
        Tone.WARN -> WARN
        Tone.BAD -> BAD
    }

    private fun severityColor(s: Severity) = when (s) {
        Severity.ALERT -> BAD
        Severity.WARN -> WARN
        Severity.INFO -> ACCENT
        Severity.GOOD -> GOOD
    }

    /** Sığmayan metni "…" ile kısaltır. */
    fun fit(text: String, p: Paint, width: Float): String {
        if (p.measureText(text) <= width) return text
        var end = text.length
        while (end > 0 && p.measureText(text, 0, end) + p.measureText("…") > width) end--
        return text.substring(0, end) + "…"
    }

    fun wrap(text: String, p: Paint, width: Float): List<String> {
        val lines = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            var line = ""
            paragraph.split(' ').forEach { word ->
                val next = if (line.isEmpty()) word else "$line $word"
                if (p.measureText(next) > width && line.isNotEmpty()) { lines += line; line = word } else line = next
            }
            lines += line
        }
        return lines.filterIndexed { i, l -> l.isNotEmpty() || i < lines.size - 1 }
    }
}
