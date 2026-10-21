package com.macerce.switchguard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Csv
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EnergyBucket
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergySummary
import com.macerce.switchguard.core.Usage
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.DailySummary
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dönem ve dilim adları, sayı ve para biçimleri: ekran ve rapor aynı metni kullanır. */
object EnergyFormat {

    fun periodLabel(period: EnergyPeriod, start: Long, locale: Locale): String = when (period) {
        EnergyPeriod.DAY -> DateFormat.getDateInstance(DateFormat.FULL, locale).format(Date(start))
        EnergyPeriod.MONTH -> pattern(locale, "LLLLyyyy", start)
        EnergyPeriod.YEAR -> pattern(locale, "yyyy", start)
    }

    fun sliceLabel(period: EnergyPeriod, b: EnergyBucket, locale: Locale): String = when (period) {
        EnergyPeriod.DAY -> pattern(locale, "HH:mm", b.start) + "–" + pattern(locale, "HH:mm", b.end)
        EnergyPeriod.MONTH -> pattern(locale, "EEEdMMM", b.start)
        EnergyPeriod.YEAR -> pattern(locale, "LLLL", b.start)
    }

    /** Grafiğin altındaki kısa etiket. */
    fun axisLabel(period: EnergyPeriod, start: Long, locale: Locale): String = when (period) {
        EnergyPeriod.DAY -> pattern(locale, "HH", start)
        EnergyPeriod.MONTH -> pattern(locale, "d", start)
        EnergyPeriod.YEAR -> pattern(locale, "LLL", start)
    }

    fun peakTime(period: EnergyPeriod, at: Long, locale: Locale): String = when (period) {
        EnergyPeriod.DAY, EnergyPeriod.MONTH -> pattern(locale, "HH:mm", at)
        EnergyPeriod.YEAR -> pattern(locale, "dMMMHHmm", at)
    }

    fun busiestLabel(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> R.string.energy_busiest_hour
        EnergyPeriod.MONTH -> R.string.energy_busiest_day
        EnergyPeriod.YEAR -> R.string.energy_busiest_month
    }

    /** Dilim başına ortalamanın adı; günlük raporda (saatlik ortalama) gösterilmez. */
    fun averageLabel(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> null
        EnergyPeriod.MONTH -> R.string.energy_avg_per_day
        EnergyPeriod.YEAR -> R.string.energy_avg_per_month
    }

    /** Fiyatın para birimi (telefonun bölgesinden); bilinmiyorsa null. */
    fun currency(): Currency? = runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()

    fun money(amount: Double, locale: Locale): String {
        val c = currency() ?: return decimal(locale, 2).format(amount)
        return NumberFormat.getCurrencyInstance(locale).apply { currency = c }.format(amount)
    }

    fun decimal(locale: Locale, digits: Int) = (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
        minimumFractionDigits = digits; maximumFractionDigits = digits; isGroupingUsed = false
    }

    private fun pattern(locale: Locale, skeleton: String, time: Long): String =
        SimpleDateFormat(AndroidDateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(time))
}

/**
 * Enerji raporu: seçili cihazın seçili günü/ayı/yılı; özet, grafik, dilim dilim tablo, maliyet ve anahtar kullanımı.
 * PDF (yazdırmak, paylaşmak) ya da CSV (Excel) olarak kaydedilir.
 */
object EnergyExport {

    enum class Format(val mime: String, val ext: String) { PDF("application/pdf", "pdf"), CSV("text/csv", "csv") }

    /** "switchguard-enerji-salon-2026-10.pdf" gibi önerilen ad. */
    fun fileName(context: Context, device: DeviceSnapshot, period: EnergyPeriod, start: Long, format: Format): String {
        val stamp = SimpleDateFormat(
            when (period) { EnergyPeriod.DAY -> "yyyy-MM-dd"; EnergyPeriod.MONTH -> "yyyy-MM"; EnergyPeriod.YEAR -> "yyyy" },
            Locale.ROOT,
        ).format(Date(start))
        val name = device.name.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]+"), "-").trim('-').take(30)
        return listOf("switchguard", context.getString(R.string.energy_report_file_word), name, stamp)
            .filter { it.isNotEmpty() }.joinToString("-") + "." + format.ext
    }

    /** Raporun içeriği: biçimden bağımsız, önce hesaplanır. */
    private class Report(
        val device: DeviceSnapshot,
        val summary: EnergySummary,
        val usages: List<Pair<String, Usage>>,
        val price: Double,
        val now: Long,
    ) {
        val period get() = summary.period
        val elapsed get() = (minOf(summary.end, now) - summary.start).coerceAtLeast(0)
    }

    suspend fun write(context: Context, uri: Uri, device: DeviceSnapshot, period: EnergyPeriod, anchor: Long, format: Format) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val metrics = MetricsLog.get(context)
            val summary = metrics.summary(device.id, period, anchor, now)
            val until = minOf(summary.end, now)
            val usages = device.switches.keys.sorted().map { ch ->
                val label = if (device.isMultiChannel) context.getString(R.string.channel_n, ch + 1) else device.name
                label to metrics.usage(device.id, ch, summary.start, until)
            }
            val report = Report(device, summary, usages, Store.get(context).energyPrice, now)
            val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("no output stream")
            out.use {
                when (format) {
                    Format.CSV -> it.write(csv(context, report).toByteArray(Charsets.UTF_8))
                    Format.PDF -> pdf(context, report).apply { writeTo(it); close() }
                }
            }
        }

    // ------------------------------------------------------------ ortak satırlar

    private fun locale(context: Context) = context.resources.configuration.locales[0]

    private fun sliceColumn(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> R.string.energy_report_col_hour
        EnergyPeriod.MONTH -> R.string.energy_report_col_day
        EnergyPeriod.YEAR -> R.string.energy_report_col_month
    }

    /** Özet: (başlık, değer) çiftleri. */
    private fun summaryLines(context: Context, r: Report): List<Pair<String, String>> {
        val locale = locale(context)
        val s = r.summary
        val durations = EventLog.durations(context)
        return buildList {
            add(context.getString(R.string.energy_total) to DailySummary.formatKwh(s.kwh))
            if (r.price > 0) add(context.getString(R.string.energy_cost) to EnergyFormat.money(s.kwh * r.price, locale))
            EnergyFormat.averageLabel(r.period)?.let { label ->
                s.avgPerActiveBucket?.let { add(context.getString(label) to DailySummary.formatKwh(it)) }
            }
            s.avgWatts?.let { add(context.getString(R.string.energy_avg_power) to formatWatts(it)) }
            s.peak?.let { p ->
                add(context.getString(R.string.energy_peak) to formatWatts(p.peakWatts!!) + " (" + peakAt(context, r, p.peakAt!!) + ")")
            }
            s.busiest?.let { b ->
                add(context.getString(EnergyFormat.busiestLabel(r.period)) to
                    EnergyFormat.sliceLabel(r.period, b, locale) + " — " + DailySummary.formatKwh(b.kwh))
            }
            val pct = if (r.elapsed > 0) (s.coveredMs * 100 / r.elapsed).toInt() else 0
            add(context.getString(R.string.energy_coverage) to context.getString(
                R.string.energy_report_coverage_of,
                DurationText.format(s.coveredMs, durations), DurationText.format(r.elapsed, durations), pct,
            ))
        }
    }

    private fun peakAt(context: Context, r: Report, at: Long): String {
        val locale = locale(context)
        return if (r.period == EnergyPeriod.DAY) EnergyFormat.peakTime(r.period, at, locale)
        else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(at))
    }

    private fun usageLines(context: Context, r: Report): List<String> {
        val durations = EventLog.durations(context)
        return r.usages.map { (label, u) ->
            context.getString(R.string.energy_report_usage_line, label, u.onCount, DurationText.format(u.onMillis, durations))
        }
    }

    // ------------------------------------------------------------ CSV

    private fun csv(context: Context, r: Report): String {
        val locale = locale(context)
        val kwh = EnergyFormat.decimal(locale, 3)
        val one = EnergyFormat.decimal(locale, 1)
        val money = EnergyFormat.decimal(locale, 2)
        val durations = EventLog.durations(context)
        val created = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(r.now))
        val rows = mutableListOf<List<String>>()
        rows += listOf(context.getString(R.string.energy_report_title))
        rows += listOf(context.getString(R.string.energy_report_device), r.device.name)
        rows += listOf(context.getString(R.string.energy_report_period), EnergyFormat.periodLabel(r.period, r.summary.start, locale))
        rows += listOf(context.getString(R.string.energy_report_created), created)
        if (r.price > 0) rows += listOf(context.getString(R.string.energy_report_price), EnergyFormat.money(r.price, locale))
        rows += emptyList<String>()
        rows += listOf(context.getString(R.string.energy_report_summary))
        summaryLines(context, r).forEach { (k, v) -> rows += listOf(k, v) }
        rows += emptyList<String>()
        rows += listOf(context.getString(R.string.energy_report_usage))
        usageLines(context, r).forEach { rows += listOf(it) }
        rows += emptyList<String>()

        val header = listOf(
            sliceColumn(r.period), R.string.energy_report_col_kwh, R.string.energy_report_col_share,
            R.string.energy_report_col_avg, R.string.energy_report_col_peak, R.string.energy_report_col_peak_at,
            R.string.energy_report_col_covered,
        ).map(context::getString) + listOfNotNull(context.getString(R.string.energy_report_col_cost).takeIf { r.price > 0 })
        rows += header
        val total = r.summary.kwh
        r.summary.buckets.forEach { b ->
            rows += listOf(
                EnergyFormat.sliceLabel(r.period, b, locale),
                kwh.format(b.kwh),
                if (total > 0) one.format(b.kwh * 100 / total) else "",
                b.avgWatts?.let(one::format).orEmpty(),
                b.peakWatts?.let(one::format).orEmpty(),
                b.peakAt?.let { peakAt(context, r, it) }.orEmpty(),
                if (b.coveredMs > 0) DurationText.format(b.coveredMs, durations) else "",
            ) + listOfNotNull(money.format(b.kwh * r.price).takeIf { r.price > 0 })
        }
        rows += listOf(context.getString(R.string.energy_total), kwh.format(total), if (total > 0) one.format(100.0) else "") +
            listOf("", "", "", DurationText.format(r.summary.coveredMs, durations)) +
            listOfNotNull(money.format(total * r.price).takeIf { r.price > 0 })
        rows += emptyList<String>()
        rows += listOf(context.getString(R.string.energy_hint))
        val separator = Csv.separatorFor(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        return Csv.write(rows, separator)
    }

    // ------------------------------------------------------------ PDF

    private const val W = 595f // A4, 1/72 inç
    private const val H = 842f
    private const val M = 40f
    private const val ROW = 15f
    private val ACCENT = Color.rgb(0x1F, 0x6F, 0xEB)
    private val MUTED = Color.rgb(0x5F, 0x63, 0x68)
    private val LINE = Color.rgb(0xDA, 0xDC, 0xE0)
    private val ZEBRA = Color.rgb(0xF4, 0xF6, 0xF8)

    private fun paint(size: Float, color: Int = Color.BLACK, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = align
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    /** Sayfaları sırayla açar; [dry] iken çizmeden yalnızca sayfa sayar (alt bilgideki "Sayfa x / y" için). */
    private class Pages(val doc: PdfDocument?, val footer: (Canvas, Int) -> Unit) {
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

    private fun pdf(context: Context, r: Report): PdfDocument {
        // Önce sayfa sayısı (kuru geçiş), sonra asıl çizim.
        val total = layout(context, r, Pages(null) { _, _ -> }).count
        val doc = PdfDocument()
        val foot = paint(8f, MUTED)
        val footRight = paint(8f, MUTED, align = Paint.Align.RIGHT)
        layout(context, r, Pages(doc) { c, n ->
            c.drawText("SwitchGuard · " + r.device.name, M, H - M + 8, foot)
            c.drawText(context.getString(R.string.energy_report_page, n, total), W - M, H - M + 8, footRight)
        }).finish()
        return doc
    }

    private fun layout(context: Context, r: Report, pages: Pages): Pages {
        val locale = locale(context)
        val title = paint(20f, ACCENT, bold = true)
        val h2 = paint(12f, bold = true)
        val label = paint(9f, MUTED)
        val text = paint(9.5f)
        val bold = paint(9.5f, bold = true)
        pages.next()
        fun c() = pages.canvas

        // Başlık
        c()?.drawText(context.getString(R.string.energy_report_title), M, pages.y + 18, title)
        pages.y += 30
        val created = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(r.now))
        val head = listOfNotNull(
            context.getString(R.string.energy_report_device) to r.device.name,
            context.getString(R.string.energy_report_period) to EnergyFormat.periodLabel(r.period, r.summary.start, locale),
            context.getString(R.string.energy_report_created) to created,
            (context.getString(R.string.energy_report_price) to EnergyFormat.money(r.price, locale)).takeIf { r.price > 0 },
        )
        head.forEach { (k, v) ->
            c()?.drawText(k, M, pages.y + 10, label)
            c()?.drawText(v, M + 90, pages.y + 10, bold)
            pages.y += ROW
        }
        pages.y += 6
        c()?.drawLine(M, pages.y, W - M, pages.y, paint(1f, LINE))
        pages.y += 16

        // Özet: iki sütunlu kutucuklar
        c()?.drawText(context.getString(R.string.energy_report_summary), M, pages.y + 10, h2)
        pages.y += 20
        val lines = summaryLines(context, r)
        val colW = (W - 2 * M) / 2
        lines.chunked(2).forEach { pair ->
            pair.forEachIndexed { i, (k, v) ->
                val x = M + i * colW
                c()?.drawText(k, x, pages.y + 9, label)
                c()?.drawText(fit(v, text, colW - 10), x, pages.y + 22, text)
            }
            pages.y += 30
        }

        // Anahtar kullanımı
        val usage = usageLines(context, r)
        if (usage.isNotEmpty()) {
            pages.y += 4
            c()?.drawText(context.getString(R.string.energy_report_usage), M, pages.y + 10, h2)
            pages.y += 18
            usage.forEach {
                pages.ensure(ROW)
                c()?.drawText(fit(it, text, W - 2 * M), M, pages.y + 10, text)
                pages.y += ROW
            }
        }

        // Grafik
        pages.y += 10
        val chartH = 130f
        pages.ensure(chartH + 30)
        c()?.let { chart(it, r, locale, M, pages.y, W - 2 * M, chartH) }
        pages.y += chartH + 26

        // Tablo
        c()?.drawText(context.getString(R.string.energy_report_details), M, pages.y + 10, h2)
        pages.y += 18
        table(context, r, pages)

        // Not
        pages.y += 10
        val note = paint(8f, MUTED)
        wrap(context.getString(R.string.energy_hint), note, W - 2 * M).forEach {
            pages.ensure(11f)
            c()?.drawText(it, M, pages.y + 8, note)
            pages.y += 11
        }
        return pages
    }

    private fun chart(c: Canvas, r: Report, locale: Locale, x: Float, y: Float, w: Float, h: Float) {
        val buckets = r.summary.buckets
        val axis = paint(7.5f, MUTED, align = Paint.Align.CENTER)
        val axisLeft = paint(7.5f, MUTED)
        val max = buckets.maxOfOrNull { it.kwh }?.takeIf { it > 0 } ?: 1.0
        val plotTop = y + 12
        val plotH = h - 12
        c.drawText(DailySummary.formatKwh(max), x, y + 7, axisLeft)
        c.drawLine(x, plotTop, x + w, plotTop, paint(0.5f, LINE))
        c.drawLine(x, plotTop + plotH, x + w, plotTop + plotH, paint(1f, LINE))
        val slot = w / buckets.size
        val bar = paint(1f, ACCENT).apply { style = Paint.Style.FILL }
        val every = when (r.period) { EnergyPeriod.DAY -> 3; EnergyPeriod.MONTH -> 5; EnergyPeriod.YEAR -> 1 }
        buckets.forEachIndexed { i, b ->
            val bh = (b.kwh / max * plotH).toFloat()
            val left = x + i * slot + slot * 0.15f
            if (bh > 0) c.drawRoundRect(RectF(left, plotTop + plotH - bh, left + slot * 0.7f, plotTop + plotH), 1.5f, 1.5f, bar)
            if (i % every == 0) c.drawText(EnergyFormat.axisLabel(r.period, b.start, locale), x + i * slot + slot / 2, plotTop + plotH + 10, axis)
        }
    }

    private fun table(context: Context, r: Report, pages: Pages) {
        val locale = locale(context)
        val kwh = EnergyFormat.decimal(locale, 3)
        val one = EnergyFormat.decimal(locale, 1)
        val durations = EventLog.durations(context)
        val withCost = r.price > 0
        // Sütun genişlikleri (toplam 515): ad, kWh, pay, ort, en yüksek, zamanı, ölçülen, [maliyet]
        val widths = if (withCost) floatArrayOf(98f, 62f, 42f, 58f, 58f, 62f, 70f, 65f)
        else floatArrayOf(118f, 72f, 50f, 66f, 66f, 70f, 73f)
        val headers = listOf(
            sliceColumn(r.period), R.string.energy_report_col_kwh, R.string.energy_report_col_share,
            R.string.energy_report_col_avg, R.string.energy_report_col_peak, R.string.energy_report_col_peak_at,
            R.string.energy_report_col_covered,
        ).map(context::getString) + listOfNotNull(context.getString(R.string.energy_report_col_cost).takeIf { withCost })
        val headPaint = paint(7.5f, MUTED, bold = true)
        val cell = paint(8f)
        val cellBold = paint(8f, bold = true)

        fun row(values: List<String>, p: Paint, fill: Int?) {
            val c = pages.canvas
            if (c != null) {
                if (fill != null) c.drawRect(M, pages.y, W - M, pages.y + ROW, paint(1f, fill).apply { style = Paint.Style.FILL })
                var x = M
                values.forEachIndexed { i, v ->
                    val w = widths[i]
                    val right = i > 0
                    val pp = Paint(p).apply { textAlign = if (right) Paint.Align.RIGHT else Paint.Align.LEFT }
                    c.drawText(fit(v, pp, w - 6), if (right) x + w - 3 else x + 3, pages.y + 10.5f, pp)
                    x += w
                }
            }
            pages.y += ROW
        }

        fun header() {
            row(headers, headPaint, null)
            pages.canvas?.drawLine(M, pages.y, W - M, pages.y, paint(1f, LINE))
        }

        pages.ensure(ROW * 3)
        header()
        val total = r.summary.kwh
        r.summary.buckets.forEachIndexed { i, b ->
            if (pages.ensure(ROW)) header()
            row(
                listOf(
                    EnergyFormat.sliceLabel(r.period, b, locale),
                    kwh.format(b.kwh),
                    if (total > 0) one.format(b.kwh * 100 / total) else "–",
                    b.avgWatts?.let(one::format) ?: "–",
                    b.peakWatts?.let(one::format) ?: "–",
                    b.peakAt?.let { EnergyFormat.peakTime(r.period, it, locale) } ?: "–",
                    if (b.coveredMs > 0) DurationText.format(b.coveredMs, durations) else "–",
                ) + listOfNotNull(EnergyFormat.money(b.kwh * r.price, locale).takeIf { withCost }),
                cell, if (i % 2 == 1) ZEBRA else null,
            )
        }
        if (pages.ensure(ROW)) header()
        pages.canvas?.drawLine(M, pages.y, W - M, pages.y, paint(1f, LINE))
        row(
            listOf(
                context.getString(R.string.energy_total), kwh.format(total), if (total > 0) one.format(100.0) else "–",
                r.summary.avgWatts?.let(one::format) ?: "–", r.summary.peak?.peakWatts?.let(one::format) ?: "–", "",
                DurationText.format(r.summary.coveredMs, durations),
            ) + listOfNotNull(EnergyFormat.money(total * r.price, locale).takeIf { withCost }),
            cellBold, null,
        )
    }

    /** Sığmayan metni "…" ile kısaltır. */
    private fun fit(text: String, p: Paint, width: Float): String {
        if (p.measureText(text) <= width) return text
        var end = text.length
        while (end > 0 && p.measureText(text, 0, end) + p.measureText("…") > width) end--
        return text.substring(0, end) + "…"
    }

    private fun wrap(text: String, p: Paint, width: Float): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        text.split(' ').forEach { word ->
            val next = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(next) > width && line.isNotEmpty()) { lines += line; line = word } else line = next
        }
        if (line.isNotEmpty()) lines += line
        return lines
    }
}
