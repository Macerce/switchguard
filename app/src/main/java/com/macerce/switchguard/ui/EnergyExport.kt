package com.macerce.switchguard.ui

import com.macerce.switchguard.core.Compare
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Tone
import com.macerce.switchguard.core.Trend
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Csv
import com.macerce.switchguard.core.Tariff
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

    /** "2,50 ₺ / kWh" ya da "Gündüz 2,50 · Puant 3,80 · Gece 1,40 ₺ / kWh". */
    fun tariffText(context: Context, tariff: Tariff, locale: Locale): String = when (tariff) {
        is Tariff.Flat -> money(tariff.price, locale) + " / kWh"
        is Tariff.ThreeZone -> context.getString(
            R.string.tariff_three_text, money(tariff.day, locale), money(tariff.peak, locale), money(tariff.night, locale),
        )
    }

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
        val tariff: Tariff,
        val now: Long,
        /** Önceki dönem (karşılaştırma için) ve bekleme tüketimi; yoksa null. */
        val previous: EnergySummary? = null,
        val standby: com.macerce.switchguard.core.Standby.Result? = null,
    ) {
        val withCost get() = tariff.hasPrice
        val period get() = summary.period
        val elapsed get() = (minOf(summary.end, now) - summary.start).coerceAtLeast(0)
    }

    suspend fun write(context: Context, uri: Uri, device: DeviceSnapshot, period: EnergyPeriod, anchor: Long, format: Format) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val metrics = MetricsLog.get(context)
            val tariff = Store.get(context).tariffFor(device.id)
            val summary = metrics.summary(device.id, period, anchor, now, tariff)
            val until = minOf(summary.end, now)
            val usages = device.switches.keys.sorted().map { ch ->
                val label = if (device.isMultiChannel) context.getString(R.string.channel_n, ch + 1) else device.name
                label to metrics.usage(device.id, ch, summary.start, until)
            }
            val previous = runCatching { metrics.summary(device.id, period, EnergyReport.shift(period, anchor, -1), now, tariff) }.getOrNull()
                ?.takeIf { it.coveredMs > 0 }
            val standby = runCatching { metrics.standby(device.id, now) }.getOrNull()
            val report = Report(device, summary, usages, tariff, now, previous, standby)
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
            if (r.withCost) add(context.getString(R.string.energy_cost) to EnergyFormat.money(s.cost, locale))
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
        if (r.withCost) rows += listOf(context.getString(R.string.energy_report_price), EnergyFormat.tariffText(context, r.tariff, locale))
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
        ).map(context::getString) + listOfNotNull(context.getString(R.string.energy_report_col_cost).takeIf { r.withCost })
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
            ) + listOfNotNull(money.format(b.cost).takeIf { r.withCost })
        }
        rows += listOf(context.getString(R.string.energy_total), kwh.format(total), if (total > 0) one.format(100.0) else "") +
            listOf("", "", "", DurationText.format(r.summary.coveredMs, durations)) +
            listOfNotNull(money.format(r.summary.cost).takeIf { r.withCost })
        rows += emptyList<String>()
        rows += listOf(context.getString(R.string.energy_hint))
        val separator = Csv.separatorFor(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        return Csv.write(rows, separator)
    }

    // ------------------------------------------------------------ PDF

    /** PDF'in metin kısmı: göstergeler (açıklamalı), öne çıkanlar, anahtar kullanımı, terimler. */
    private fun doc(context: Context, r: Report): ReportDoc {
        val locale = locale(context)
        val s = r.summary
        val durations = EventLog.durations(context)
        val created = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(r.now))
        val periodText = EnergyFormat.periodLabel(r.period, s.start, locale)
        val prevLabel = context.getString(when (r.period) {
            EnergyPeriod.DAY -> R.string.report_vs_yesterday
            EnergyPeriod.MONTH -> R.string.report_vs_last_month
            EnergyPeriod.YEAR -> R.string.report_vs_last_year
        })
        fun trend(now: Double, before: Double?): Trend? {
            if (before == null || (now == 0.0 && before == 0.0)) return null
            val pct = Compare.change(now, before)
            val text = when {
                pct == null -> context.getString(R.string.report_trend_new, prevLabel, DailySummary.formatKwh(before))
                kotlin.math.abs(pct) < Compare.SAME_PERCENT -> context.getString(R.string.report_trend_same, prevLabel)
                pct > 0 -> context.getString(R.string.report_trend_up, prevLabel, pct)
                else -> context.getString(R.string.report_trend_down, prevLabel, -pct)
            }
            return Trend(text, Compare.tone(pct, lowerIsBetter = true))
        }
        val pct = if (r.elapsed > 0) (s.coveredMs * 100 / r.elapsed).toInt().coerceIn(0, 100) else 0

        val kpis = buildList {
            add(Kpi(context.getString(R.string.energy_total), DailySummary.formatKwh(s.kwh), context.getString(R.string.kpi_total_caption), trend(s.kwh, r.previous?.kwh)))
            if (r.withCost) add(Kpi(context.getString(R.string.energy_cost), EnergyFormat.money(s.cost, locale), EnergyFormat.tariffText(context, r.tariff, locale)))
            s.avgWatts?.let { add(Kpi(context.getString(R.string.energy_avg_power), formatWatts(it), context.getString(R.string.kpi_avg_power_caption))) }
            s.peak?.let { p -> add(Kpi(context.getString(R.string.energy_peak), formatWatts(p.peakWatts!!), context.getString(R.string.kpi_peak_caption, peakAt(context, r, p.peakAt!!)))) }
            EnergyFormat.averageLabel(r.period)?.let { label ->
                s.avgPerActiveBucket?.let { add(Kpi(context.getString(label), DailySummary.formatKwh(it), context.getString(R.string.kpi_bucket_avg_caption))) }
            }
            add(Kpi(context.getString(R.string.energy_coverage), context.getString(R.string.volume_pct, pct), context.getString(R.string.kpi_coverage_caption,
                DurationText.format(s.coveredMs, durations), DurationText.format(r.elapsed, durations)), tone = if (pct < 90) Tone.WARN else Tone.GOOD))
        }

        val findings = mutableListOf<Finding>()
        s.busiest?.takeIf { s.kwh > 0 }?.let { b ->
            findings += Finding(Severity.INFO, context.getString(R.string.ins_busiest, EnergyFormat.sliceLabel(r.period, b, locale), (b.kwh * 100 / s.kwh).toInt()))
        }
        r.previous?.let { prev ->
            val change = Compare.change(s.kwh, prev.kwh)
            if (change != null && kotlin.math.abs(change) >= 20 && r.elapsed >= (s.end - s.start)) {
                findings += Finding(if (change > 0) Severity.WARN else Severity.GOOD,
                    context.getString(if (change > 0) R.string.ins_more else R.string.ins_less, kotlin.math.abs(change), prevLabel))
            }
        }
        if (pct < 90) findings += Finding(Severity.WARN, context.getString(R.string.ins_coverage, pct), context.getString(R.string.act_battery))
        // İçinde bulunulan ay: bu hızla ay sonu tahmini.
        if (r.period == EnergyPeriod.MONTH && r.now < s.end && s.coveredMs > 3 * 86_400_000L) {
            val factor = (s.end - s.start).toDouble() / s.coveredMs
            val kwh = s.kwh * factor
            findings += Finding(Severity.INFO, context.getString(R.string.ins_projection, DailySummary.formatKwh(kwh)) +
                (if (r.withCost) " (≈ ${EnergyFormat.money(s.cost * factor, locale)})" else ""))
        }
        r.standby?.let { st ->
            findings += Finding(Severity.INFO, context.getString(R.string.ins_standby, formatWatts(st.watts), DailySummary.formatKwh(st.monthlyKwh)), context.getString(R.string.act_standby))
        }
        if (findings.isEmpty()) findings += Finding(Severity.GOOD, context.getString(R.string.ins_nothing))
        val usage = usageLines(context, r).map { Finding(Severity.INFO, it) }

        return ReportDoc(
            title = context.getString(R.string.energy_report_title),
            subtitle = context.getString(R.string.energy_report_subtitle, r.device.name, periodText, created),
            intro = context.getString(R.string.energy_report_intro, r.device.name, periodText),
            kpis = kpis,
            sections = listOfNotNull(
                ReportSection(context.getString(R.string.ins_title), context.getString(R.string.ins_explain), findings),
                usage.takeIf { it.isNotEmpty() }?.let { ReportSection(context.getString(R.string.energy_report_usage), context.getString(R.string.usage_explain), it) },
            ),
            glossary = listOf(
                R.string.gl_kwh to R.string.gl_kwh_def, R.string.gl_w to R.string.gl_w_def,
                R.string.gl_peak to R.string.gl_peak_def, R.string.gl_coverage to R.string.gl_coverage_def,
                R.string.gl_standby to R.string.gl_standby_def,
            ).map { (k, v) -> context.getString(k) to context.getString(v) } +
                listOfNotNull((context.getString(R.string.gl_tariff) to context.getString(R.string.gl_tariff_def)).takeIf { r.withCost }),
            footnote = context.getString(R.string.energy_hint),
        )
    }

    private fun pdf(context: Context, r: Report): PdfDocument {
        val locale = locale(context)
        return ReportPdf.render(
            context, doc(context, r), "SwitchGuard · " + r.device.name,
            middle = { pages ->
                ReportPdf.sectionTitle(pages, context.getString(R.string.chart_title, context.getString(sliceColumn(r.period)).lowercase(locale)), context.getString(R.string.chart_explain))
                val chartH = 150f
                pages.ensure(chartH + 30)
                pages.canvas?.let { chart(context, it, r, locale, ReportPdf.M, pages.y, ReportPdf.W - 2 * ReportPdf.M, chartH) }
                pages.y += chartH + 30
            },
            end = { pages ->
                ReportPdf.sectionTitle(pages, context.getString(R.string.energy_report_details), context.getString(R.string.table_explain))
                table(context, r, pages)
            },
        )
    }

    /** Sütun grafik: y ekseni (kWh, üç ızgara çizgisi), dilim etiketleri, ortalama çizgisi ve açıklaması. */
    private fun chart(context: Context, c: Canvas, r: Report, locale: Locale, x: Float, y: Float, w: Float, h: Float) {
        val buckets = r.summary.buckets
        val muted = ReportPdf.MUTED
        val axis = ReportPdf.paint(7f, muted, align = Paint.Align.CENTER)
        val axisRight = ReportPdf.paint(7f, muted, align = Paint.Align.RIGHT)
        val max = buckets.maxOfOrNull { it.kwh }?.takeIf { it > 0 } ?: 1.0
        val left = x + 38
        val plotW = w - 38
        val plotTop = y + 14
        val plotH = h - 28
        c.drawText("kWh", x, y + 8, ReportPdf.paint(7f, muted, bold = true))
        for (i in 0..3) {
            val v = max * i / 3
            val gy = plotTop + plotH - (plotH * i / 3)
            c.drawLine(left, gy, left + plotW, gy, ReportPdf.paint(if (i == 0) 1f else 0.4f, ReportPdf.LINE))
            c.drawText(EnergyFormat.decimal(locale, if (max < 1) 3 else 2).format(v), left - 4, gy + 2.5f, axisRight)
        }
        val slot = plotW / buckets.size
        val bar = ReportPdf.fill(ReportPdf.ACCENT)
        val busiest = r.summary.busiest
        val every = when (r.period) { EnergyPeriod.DAY -> 3; EnergyPeriod.MONTH -> 5; EnergyPeriod.YEAR -> 1 }
        buckets.forEachIndexed { i, b ->
            val bh = (b.kwh / max * plotH).toFloat()
            val bx = left + i * slot + slot * 0.15f
            val paint = if (b == busiest) ReportPdf.fill(android.graphics.Color.rgb(0x00, 0x4D, 0x40)) else bar
            if (bh > 0) c.drawRoundRect(RectF(bx, plotTop + plotH - bh, bx + slot * 0.7f, plotTop + plotH), 1.5f, 1.5f, paint)
            if (i % every == 0) c.drawText(EnergyFormat.axisLabel(r.period, b.start, locale), left + i * slot + slot / 2, plotTop + plotH + 10, axis)
        }
        // Ortalama: ölçülen dilimlerin ortalaması, kesikli çizgi.
        r.summary.avgPerActiveBucket?.let { avg ->
            val ay = plotTop + plotH - (avg / max * plotH).toFloat()
            val dash = ReportPdf.paint(0.8f, android.graphics.Color.rgb(0xB2, 0x6A, 0x00)).apply {
                style = Paint.Style.STROKE; pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 3f), 0f)
            }
            c.drawLine(left, ay, left + plotW, ay, dash)
        }
        // Açıklama (lejant)
        val legendY = plotTop + plotH + 22
        val lp = ReportPdf.paint(7f, muted)
        c.drawRect(left, legendY - 6, left + 8, legendY + 1, bar)
        c.drawText(context.getString(R.string.chart_legend_bar), left + 12, legendY, lp)
        val l2 = left + 12 + lp.measureText(context.getString(R.string.chart_legend_bar)) + 16
        c.drawRect(l2, legendY - 6, l2 + 8, legendY + 1, ReportPdf.fill(android.graphics.Color.rgb(0x00, 0x4D, 0x40)))
        c.drawText(context.getString(R.string.chart_legend_busiest), l2 + 12, legendY, lp)
        val l3 = l2 + 12 + lp.measureText(context.getString(R.string.chart_legend_busiest)) + 16
        c.drawLine(l3, legendY - 2.5f, l3 + 12, legendY - 2.5f, ReportPdf.paint(1f, android.graphics.Color.rgb(0xB2, 0x6A, 0x00)))
        c.drawText(context.getString(R.string.chart_legend_avg), l3 + 16, legendY, lp)
    }

    private fun table(context: Context, r: Report, pages: ReportPdf.Pages) {
        val locale = locale(context)
        val kwh = EnergyFormat.decimal(locale, 3)
        val one = EnergyFormat.decimal(locale, 1)
        val durations = EventLog.durations(context)
        val withCost = r.withCost
        val total = r.summary.kwh
        val headers = listOf(
            sliceColumn(r.period), R.string.energy_report_col_kwh, R.string.energy_report_col_share,
            R.string.energy_report_col_avg, R.string.energy_report_col_peak, R.string.energy_report_col_peak_at,
            R.string.energy_report_col_covered,
        ).map(context::getString) + listOfNotNull(context.getString(R.string.energy_report_col_cost).takeIf { withCost })
        val rows = r.summary.buckets.map { b ->
            listOf(
                EnergyFormat.sliceLabel(r.period, b, locale),
                kwh.format(b.kwh),
                if (total > 0) one.format(b.kwh * 100 / total) else "–",
                b.avgWatts?.let(one::format) ?: "–",
                b.peakWatts?.let(one::format) ?: "–",
                b.peakAt?.let { EnergyFormat.peakTime(r.period, it, locale) } ?: "–",
                if (b.coveredMs > 0) DurationText.format(b.coveredMs, durations) else "–",
            ) + listOfNotNull(EnergyFormat.money(b.cost, locale).takeIf { withCost })
        } + listOf(
            listOf(
                context.getString(R.string.energy_total), kwh.format(total), if (total > 0) one.format(100.0) else "–",
                r.summary.avgWatts?.let(one::format) ?: "–", r.summary.peak?.peakWatts?.let(one::format) ?: "–", "",
                DurationText.format(r.summary.coveredMs, durations),
            ) + listOfNotNull(EnergyFormat.money(r.summary.cost, locale).takeIf { withCost }),
        )
        ReportPdf.table(pages, ReportTable(headers, rows, context.getString(R.string.table_note)))
    }
}
