package com.macerce.switchguard.desktop.report

import com.macerce.switchguard.core.Compare
import com.macerce.switchguard.core.Csv
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.EnergySummary
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Tariff
import com.macerce.switchguard.core.Tone
import com.macerce.switchguard.core.Trend
import com.macerce.switchguard.core.Usage
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.devices.formatWatts as formatWattsLocale
import com.macerce.switchguard.desktop.ui.energy.EnergyFormat
import java.text.DateFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enerji raporu: seçili cihazın seçili günü/ayı/yılı; özet, grafik, dilim dilim tablo, maliyet ve anahtar kullanımı.
 * PDF (yazdırmak, paylaşmak) ya da CSV (Excel) olarak kaydedilir.
 */
object EnergyExport {

    enum class Format(val mime: String, val ext: String) { PDF("application/pdf", "pdf"), CSV("text/csv", "csv") }

    /** "switchguard-enerji-salon-2026-10.pdf" gibi önerilen ad. */
    fun fileName(s: Strings, device: DeviceSnapshot, period: EnergyPeriod, start: Long, format: Format): String {
        val stamp = SimpleDateFormat(
            when (period) { EnergyPeriod.DAY -> "yyyy-MM-dd"; EnergyPeriod.MONTH -> "yyyy-MM"; EnergyPeriod.YEAR -> "yyyy" },
            Locale.ROOT,
        ).format(Date(start))
        val name = device.name.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]+"), "-").trim('-').take(30)
        return listOf("switchguard", s["energy_report_file_word"], name, stamp)
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

    /** Raporu [out]'a yazar (engelleyici; arayüz arka planda çağırır). */
    fun write(s: Strings, store: Store, metrics: MetricsLog, out: java.io.OutputStream, device: DeviceSnapshot, period: EnergyPeriod, anchor: Long, format: Format) {
            val now = System.currentTimeMillis()
            val tariff = store.tariffFor(device.id)
            val summary = metrics.summary(device.id, period, anchor, now, tariff)
            val until = minOf(summary.end, now)
            val usages = device.switches.keys.sorted().map { ch ->
                val label = if (device.isMultiChannel) s.format("channel_n", ch + 1) else device.name
                label to metrics.usage(device.id, ch, summary.start, until)
            }
            val previous = runCatching { metrics.summary(device.id, period, EnergyReport.shift(period, anchor, -1), now, tariff) }.getOrNull()
                ?.takeIf { it.coveredMs > 0 }
            val standby = runCatching { metrics.standby(device.id, now) }.getOrNull()
            val report = Report(device, summary, usages, tariff, now, previous, standby)
            when (format) {
                Format.CSV -> out.write(csv(s, report).toByteArray(Charsets.UTF_8))
                Format.PDF -> pdf(s, report).apply { writeTo(out); close() }
            }
    }

    // ------------------------------------------------------------ ortak satırlar

    private fun locale(s: Strings) = s.locale

    private fun sliceColumn(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> "energy_report_col_hour"
        EnergyPeriod.MONTH -> "energy_report_col_day"
        EnergyPeriod.YEAR -> "energy_report_col_month"
    }

    /** Özet: (başlık, değer) çiftleri. */
    private fun summaryLines(s: Strings, r: Report): List<Pair<String, String>> {
        val locale = locale(s)
        val sm = r.summary
        val durations = s.durations()
        return buildList {
            add(s["energy_total"] to s.formatKwh(sm.kwh))
            if (r.withCost) add(s["energy_cost"] to EnergyFormat.money(sm.cost, locale))
            EnergyFormat.averageLabel(r.period)?.let { label ->
                sm.avgPerActiveBucket?.let { add(s.str(label) to s.formatKwh(it)) }
            }
            sm.avgWatts?.let { add(s["energy_avg_power"] to formatWattsLocale(it, locale(s))) }
            sm.peak?.let { p ->
                add(s["energy_peak"] to formatWattsLocale(p.peakWatts!!, locale(s)) + " (" + peakAt(s, r, p.peakAt!!) + ")")
            }
            sm.busiest?.let { b ->
                add(s.str(EnergyFormat.busiestLabel(r.period)) to
                    EnergyFormat.sliceLabel(r.period, b, locale) + " — " + s.formatKwh(b.kwh))
            }
            val pct = if (r.elapsed > 0) (sm.coveredMs * 100 / r.elapsed).toInt() else 0
            add(s["energy_coverage"] to s.format("energy_report_coverage_of",
                DurationText.format(sm.coveredMs, durations), DurationText.format(r.elapsed, durations), pct,
            ))
        }
    }

    private fun peakAt(s: Strings, r: Report, at: Long): String {
        val locale = locale(s)
        return if (r.period == EnergyPeriod.DAY) EnergyFormat.peakTime(r.period, at, locale)
        else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(at))
    }

    private fun usageLines(s: Strings, r: Report): List<String> {
        val durations = s.durations()
        return r.usages.map { (label, u) ->
            s.format("energy_report_usage_line", label, u.onCount, DurationText.format(u.onMillis, durations))
        }
    }

    // ------------------------------------------------------------ CSV

    private fun csv(s: Strings, r: Report): String {
        val locale = locale(s)
        val kwh = EnergyFormat.decimal(locale, 3)
        val one = EnergyFormat.decimal(locale, 1)
        val money = EnergyFormat.decimal(locale, 2)
        val durations = s.durations()
        val created = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(r.now))
        val rows = mutableListOf<List<String>>()
        rows += listOf(s["energy_report_title"])
        rows += listOf(s["energy_report_device"], r.device.name)
        rows += listOf(s["energy_report_period"], EnergyFormat.periodLabel(r.period, r.summary.start, locale))
        rows += listOf(s["energy_report_created"], created)
        if (r.withCost) rows += listOf(s["energy_report_price"], EnergyFormat.tariffText(s, r.tariff, locale))
        rows += emptyList<String>()
        rows += listOf(s["energy_report_summary"])
        summaryLines(s, r).forEach { (k, v) -> rows += listOf(k, v) }
        rows += emptyList<String>()
        rows += listOf(s["energy_report_usage"])
        usageLines(s, r).forEach { rows += listOf(it) }
        rows += emptyList<String>()

        val header = listOf(
            sliceColumn(r.period), "energy_report_col_kwh", "energy_report_col_share",
            "energy_report_col_avg", "energy_report_col_peak", "energy_report_col_peak_at",
            "energy_report_col_covered",
        ).map({ s[it] }) + listOfNotNull(s["energy_report_col_cost"].takeIf { r.withCost })
        rows += header
        val total = r.summary.kwh
        r.summary.buckets.forEach { b ->
            rows += listOf(
                EnergyFormat.sliceLabel(r.period, b, locale),
                kwh.format(b.kwh),
                if (total > 0) one.format(b.kwh * 100 / total) else "",
                b.avgWatts?.let(one::format).orEmpty(),
                b.peakWatts?.let(one::format).orEmpty(),
                b.peakAt?.let { peakAt(s, r, it) }.orEmpty(),
                if (b.coveredMs > 0) DurationText.format(b.coveredMs, durations) else "",
            ) + listOfNotNull(money.format(b.cost).takeIf { r.withCost })
        }
        rows += listOf(s["energy_total"], kwh.format(total), if (total > 0) one.format(100.0) else "") +
            listOf("", "", "", DurationText.format(r.summary.coveredMs, durations)) +
            listOfNotNull(money.format(r.summary.cost).takeIf { r.withCost })
        rows += emptyList<String>()
        rows += listOf(s["energy_hint"])
        val separator = Csv.separatorFor(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        return Csv.write(rows, separator)
    }

    // ------------------------------------------------------------ PDF

    /** PDF'in metin kısmı: göstergeler (açıklamalı), öne çıkanlar, anahtar kullanımı, terimler. */
    private fun doc(s: Strings, r: Report): ReportDoc {
        val locale = locale(s)
        val sm = r.summary
        val durations = s.durations()
        val created = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(r.now))
        val periodText = EnergyFormat.periodLabel(r.period, sm.start, locale)
        val prevLabel = s.str(when (r.period) {
            EnergyPeriod.DAY -> "report_vs_yesterday"
            EnergyPeriod.MONTH -> "report_vs_last_month"
            EnergyPeriod.YEAR -> "report_vs_last_year"
        })
        fun trend(now: Double, before: Double?): Trend? {
            if (before == null || (now == 0.0 && before == 0.0)) return null
            val pct = Compare.change(now, before)
            val text = when {
                pct == null -> s.format("report_trend_new", prevLabel, s.formatKwh(before))
                kotlin.math.abs(pct) < Compare.SAME_PERCENT -> s.format("report_trend_same", prevLabel)
                pct > 0 -> s.format("report_trend_up", prevLabel, pct)
                else -> s.format("report_trend_down", prevLabel, -pct)
            }
            return Trend(text, Compare.tone(pct, lowerIsBetter = true))
        }
        val pct = if (r.elapsed > 0) (sm.coveredMs * 100 / r.elapsed).toInt().coerceIn(0, 100) else 0

        val kpis = buildList {
            add(Kpi(s["energy_total"], s.formatKwh(sm.kwh), s["kpi_total_caption"], trend(sm.kwh, r.previous?.kwh)))
            if (r.withCost) add(Kpi(s["energy_cost"], EnergyFormat.money(sm.cost, locale), EnergyFormat.tariffText(s, r.tariff, locale)))
            sm.avgWatts?.let { add(Kpi(s["energy_avg_power"], formatWattsLocale(it, locale(s)), s["kpi_avg_power_caption"])) }
            sm.peak?.let { p -> add(Kpi(s["energy_peak"], formatWattsLocale(p.peakWatts!!, locale(s)), s.format("kpi_peak_caption", peakAt(s, r, p.peakAt!!)))) }
            EnergyFormat.averageLabel(r.period)?.let { label ->
                sm.avgPerActiveBucket?.let { add(Kpi(s.str(label), s.formatKwh(it), s["kpi_bucket_avg_caption"])) }
            }
            add(Kpi(s["energy_coverage"], s.format("volume_pct", pct), s.format("kpi_coverage_caption",
                DurationText.format(sm.coveredMs, durations), DurationText.format(r.elapsed, durations)), tone = if (pct < 90) Tone.WARN else Tone.GOOD))
        }

        val findings = mutableListOf<Finding>()
        sm.busiest?.takeIf { sm.kwh > 0 }?.let { b ->
            findings += Finding(Severity.INFO, s.format("ins_busiest", EnergyFormat.sliceLabel(r.period, b, locale), (b.kwh * 100 / sm.kwh).toInt()))
        }
        r.previous?.let { prev ->
            val change = Compare.change(sm.kwh, prev.kwh)
            if (change != null && kotlin.math.abs(change) >= 20 && r.elapsed >= (sm.end - sm.start)) {
                findings += Finding(if (change > 0) Severity.WARN else Severity.GOOD,
                    s.str(if (change > 0) "ins_more" else "ins_less", kotlin.math.abs(change), prevLabel))
            }
        }
        if (pct < 90) findings += Finding(Severity.WARN, s.format("ins_coverage", pct), s["act_battery"])
        // İçinde bulunulan ay: bu hızla ay sonu tahmini.
        if (r.period == EnergyPeriod.MONTH && r.now < sm.end && sm.coveredMs > 3 * 86_400_000L) {
            val factor = (sm.end - sm.start).toDouble() / sm.coveredMs
            val kwh = sm.kwh * factor
            findings += Finding(Severity.INFO, s.format("ins_projection", s.formatKwh(kwh)) +
                (if (r.withCost) " (≈ ${EnergyFormat.money(sm.cost * factor, locale)})" else ""))
        }
        r.standby?.let { st ->
            findings += Finding(Severity.INFO, s.format("ins_standby", formatWattsLocale(st.watts, locale(s)), s.formatKwh(st.monthlyKwh)), s["act_standby"])
        }
        if (findings.isEmpty()) findings += Finding(Severity.GOOD, s["ins_nothing"])
        val usage = usageLines(s, r).map { Finding(Severity.INFO, it) }

        return ReportDoc(
            title = s["energy_report_title"],
            subtitle = s.format("energy_report_subtitle", r.device.name, periodText, created),
            intro = s.format("energy_report_intro", r.device.name, periodText),
            kpis = kpis,
            sections = listOfNotNull(
                ReportSection(s["ins_title"], s["ins_explain"], findings),
                usage.takeIf { it.isNotEmpty() }?.let { ReportSection(s["energy_report_usage"], s["usage_explain"], it) },
            ),
            glossary = listOf(
                "gl_kwh" to "gl_kwh_def", "gl_w" to "gl_w_def",
                "gl_peak" to "gl_peak_def", "gl_coverage" to "gl_coverage_def",
                "gl_standby" to "gl_standby_def",
            ).map { (k, v) -> s.str(k) to s.str(v) } +
                listOfNotNull((s["gl_tariff"] to s["gl_tariff_def"]).takeIf { r.withCost }),
            footnote = s["energy_hint"],
        )
    }

    private fun pdf(s: Strings, r: Report): PdfDocument {
        val locale = locale(s)
        return ReportPdf.render(
            s, doc(s, r), "SwitchGuard · " + r.device.name,
            middle = { pages ->
                ReportPdf.sectionTitle(pages, s.format("chart_title", s.str(sliceColumn(r.period)).lowercase(locale)), s["chart_explain"])
                val chartH = 150f
                pages.ensure(chartH + 30)
                pages.canvas?.let { chart(s, it, r, locale, ReportPdf.M, pages.y, ReportPdf.W - 2 * ReportPdf.M, chartH) }
                pages.y += chartH + 30
            },
            end = { pages ->
                ReportPdf.sectionTitle(pages, s["energy_report_details"], s["table_explain"])
                table(s, r, pages)
            },
        )
    }

    /** Sütun grafik: y ekseni (kWh, üç ızgara çizgisi), dilim etiketleri, ortalama çizgisi ve açıklaması. */
    private fun chart(s: Strings, c: Canvas, r: Report, locale: Locale, x: Float, y: Float, w: Float, h: Float) {
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
            val paint = if (b == busiest) ReportPdf.fill(Color.rgb(0x00, 0x4D, 0x40)) else bar
            if (bh > 0) c.drawRoundRect(RectF(bx, plotTop + plotH - bh, bx + slot * 0.7f, plotTop + plotH), 1.5f, 1.5f, paint)
            if (i % every == 0) c.drawText(EnergyFormat.axisLabel(r.period, b.start, locale), left + i * slot + slot / 2, plotTop + plotH + 10, axis)
        }
        // Ortalama: ölçülen dilimlerin ortalaması, kesikli çizgi.
        r.summary.avgPerActiveBucket?.let { avg ->
            val ay = plotTop + plotH - (avg / max * plotH).toFloat()
            val dash = ReportPdf.paint(0.8f, Color.rgb(0xB2, 0x6A, 0x00)).apply {
                style = Paint.Style.STROKE; dash = floatArrayOf(4f, 3f); strokeWidth = 0.8f
            }
            c.drawLine(left, ay, left + plotW, ay, dash)
        }
        // Açıklama (lejant)
        val legendY = plotTop + plotH + 22
        val lp = ReportPdf.paint(7f, muted)
        c.drawRect(left, legendY - 6, left + 8, legendY + 1, bar)
        c.drawText(s["chart_legend_bar"], left + 12, legendY, lp)
        val l2 = left + 12 + lp.measureText(s["chart_legend_bar"]) + 16
        c.drawRect(l2, legendY - 6, l2 + 8, legendY + 1, ReportPdf.fill(Color.rgb(0x00, 0x4D, 0x40)))
        c.drawText(s["chart_legend_busiest"], l2 + 12, legendY, lp)
        val l3 = l2 + 12 + lp.measureText(s["chart_legend_busiest"]) + 16
        c.drawLine(l3, legendY - 2.5f, l3 + 12, legendY - 2.5f, ReportPdf.paint(1f, Color.rgb(0xB2, 0x6A, 0x00)))
        c.drawText(s["chart_legend_avg"], l3 + 16, legendY, lp)
    }

    private fun table(s: Strings, r: Report, pages: ReportPdf.Pages) {
        val locale = locale(s)
        val kwh = EnergyFormat.decimal(locale, 3)
        val one = EnergyFormat.decimal(locale, 1)
        val durations = s.durations()
        val withCost = r.withCost
        val total = r.summary.kwh
        val headers = listOf(
            sliceColumn(r.period), "energy_report_col_kwh", "energy_report_col_share",
            "energy_report_col_avg", "energy_report_col_peak", "energy_report_col_peak_at",
            "energy_report_col_covered",
        ).map({ s[it] }) + listOfNotNull(s["energy_report_col_cost"].takeIf { withCost })
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
                s["energy_total"], kwh.format(total), if (total > 0) one.format(100.0) else "–",
                r.summary.avgWatts?.let(one::format) ?: "–", r.summary.peak?.peakWatts?.let(one::format) ?: "–", "",
                DurationText.format(r.summary.coveredMs, durations),
            ) + listOfNotNull(EnergyFormat.money(r.summary.cost, locale).takeIf { withCost }),
        )
        ReportPdf.table(pages, ReportTable(headers, rows, s["table_note"]))
    }
}
