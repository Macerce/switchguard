package com.macerce.switchguard.desktop.ui.energy

import com.macerce.switchguard.core.EnergyBucket
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.Tariff
import com.macerce.switchguard.desktop.i18n.Strings
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

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
        EnergyPeriod.DAY -> "energy_busiest_hour"
        EnergyPeriod.MONTH -> "energy_busiest_day"
        EnergyPeriod.YEAR -> "energy_busiest_month"
    }

    /** Dilim başına ortalamanın adı; günlük raporda (saatlik ortalama) gösterilmez. */
    fun averageLabel(period: EnergyPeriod) = when (period) {
        EnergyPeriod.DAY -> null
        EnergyPeriod.MONTH -> "energy_avg_per_day"
        EnergyPeriod.YEAR -> "energy_avg_per_month"
    }

    /** Fiyatın para birimi (bilgisayarın bölgesinden); bilinmiyorsa null. */
    fun currency(): Currency? = runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()

    /** "2,50 ₺ / kWh" ya da "Gündüz 2,50 · Puant 3,80 · Gece 1,40 ₺ / kWh". */
    fun tariffText(s: Strings, tariff: Tariff, locale: Locale): String = when (tariff) {
        is Tariff.Flat -> money(tariff.price, locale) + " / kWh"
        is Tariff.ThreeZone -> s.format("tariff_three_text", money(tariff.day, locale), money(tariff.peak, locale), money(tariff.night, locale),
        )
    }

    fun money(amount: Double, locale: Locale): String {
        val c = currency() ?: return decimal(locale, 2).format(amount)
        return NumberFormat.getCurrencyInstance(locale).apply { currency = c }.format(amount)
    }

    fun decimal(locale: Locale, digits: Int) = (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
        minimumFractionDigits = digits; maximumFractionDigits = digits; isGroupingUsed = false
    }

    /** Telefondaki `DateFormat.getBestDateTimePattern` karşılığı: raporların kullandığı iskeletler, TR/EN. */
    private fun pattern(locale: Locale, skeleton: String, time: Long): String {
        val tr = locale.language == "tr"
        val p = when (skeleton) {
            "LLLLyyyy" -> "LLLL yyyy"
            "EEEdMMM" -> if (tr) "d MMM EEE" else "EEE, MMM d"
            "dMMMHHmm" -> if (tr) "d MMM HH:mm" else "MMM d, HH:mm"
            else -> skeleton
        }
        return SimpleDateFormat(p, locale).format(Date(time))
    }
}

