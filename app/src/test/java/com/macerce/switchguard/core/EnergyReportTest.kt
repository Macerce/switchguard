package com.macerce.switchguard.core

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnergyReportTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val istanbul = TimeZone.getTimeZone("Europe/Istanbul")
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")
    private val hour = 3_600_000L

    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, zone: TimeZone = utc) =
        Calendar.getInstance(zone).apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    @Test
    fun `donem araliklari`() {
        val anchor = at(2026, 10, 21, 15, 30)
        assertEquals(at(2026, 10, 21) to at(2026, 10, 22), EnergyReport.range(EnergyPeriod.DAY, anchor, utc))
        assertEquals(at(2026, 10, 1) to at(2026, 11, 1), EnergyReport.range(EnergyPeriod.MONTH, anchor, utc))
        assertEquals(at(2026, 1, 1) to at(2027, 1, 1), EnergyReport.range(EnergyPeriod.YEAR, anchor, utc))
    }

    @Test
    fun `dilim sayilari`() {
        val anchor = at(2024, 2, 10, 8)
        assertEquals(24, EnergyReport.slices(EnergyPeriod.DAY, anchor, utc).size)
        assertEquals(29, EnergyReport.slices(EnergyPeriod.MONTH, anchor, utc).size) // artık yıl
        assertEquals(12, EnergyReport.slices(EnergyPeriod.YEAR, anchor, utc).size)
        // Yaz saatine geçilen gün 23 saattir.
        assertEquals(23, EnergyReport.slices(EnergyPeriod.DAY, at(2026, 3, 29, 12, zone = berlin), berlin).size)
    }

    @Test
    fun `onceki ve sonraki donem`() {
        val anchor = at(2026, 1, 31, 10, zone = istanbul)
        assertEquals(at(2025, 12, 1, zone = istanbul), EnergyReport.shift(EnergyPeriod.MONTH, anchor, -1, istanbul))
        assertEquals(at(2026, 2, 1, zone = istanbul), EnergyReport.shift(EnergyPeriod.MONTH, anchor, 1, istanbul))
        assertEquals(at(2025, 1, 1, zone = istanbul), EnergyReport.shift(EnergyPeriod.YEAR, anchor, -1, istanbul))
        assertEquals(at(2026, 1, 30, zone = istanbul), EnergyReport.shift(EnergyPeriod.DAY, anchor, -1, istanbul))
    }

    @Test
    fun `gunluk rapor saatlere dagilir, donem oncesi olcum sayilir`() {
        val day = at(2026, 10, 21)
        // Gece yarısından önce 1000 W başlamış, 01:00'de 500 W'a inmiş; ölçümler 10 dk arayla sürüyor.
        val samples = buildList {
            add(PowerSample(day - 5 * 60_000, 1000.0))
            var t = day + 5 * 60_000
            while (t < day + hour) { add(PowerSample(t, 1000.0)); t += 10 * 60_000 }
            t = day + hour
            while (t < day + 2 * hour) { add(PowerSample(t, 500.0)); t += 10 * 60_000 }
        }
        val r = EnergyReport.summarize(EnergyPeriod.DAY, day + 3 * hour, samples, now = day + 10 * hour, zone = utc)
        assertEquals(24, r.buckets.size)
        assertEquals(1.0, r.buckets[0].kwh, 1e-9)
        assertEquals(0.5, r.buckets[1].kwh, 1e-9)
        // Son ölçüm 01:50, 15 dk boşluk sınırı: 02:00–02:05 arası sayılır.
        assertEquals(500.0 * 5 / 60 / 1000, r.buckets[2].kwh, 1e-9)
        assertEquals(0.0, r.buckets[5].kwh, 0.0)
        assertNull(r.buckets[5].avgWatts)
        assertEquals(1000.0, r.buckets[0].avgWatts!!, 1e-9)
        assertEquals(1000.0, r.peak!!.peakWatts!!, 0.0)
        assertEquals(day, r.peak!!.peakAt)
        assertEquals(r.buckets[0], r.busiest)
        assertEquals(1.5 + 500.0 * 5 / 60 / 1000, r.kwh, 1e-9)
        // Ortalama güç yalnızca ölçüm olan süreye bölünür.
        assertEquals(r.kwh * 1000 / (2 * hour + 5 * 60_000).toDouble() * hour, r.avgWatts!!, 1e-6)
    }

    @Test
    fun `gelecek hesaba katilmaz`() {
        val day = at(2026, 10, 21)
        val samples = listOf(PowerSample(day, 100.0))
        val r = EnergyReport.summarize(EnergyPeriod.DAY, day, samples, now = day + 5 * 60_000, zone = utc)
        assertEquals(100.0 * 5 / 60 / 1000, r.kwh, 1e-9)
        assertEquals(5 * 60_000L, r.coveredMs)
    }

    @Test
    fun `yillik rapor aylara dagilir`() {
        val samples = listOf(
            PowerSample(at(2026, 3, 10), 2000.0),
            PowerSample(at(2026, 3, 10, 0, 10), 0.0),
            PowerSample(at(2026, 7, 1), 600.0),
            PowerSample(at(2026, 7, 1, 0, 10), 0.0),
        )
        val r = EnergyReport.summarize(EnergyPeriod.YEAR, at(2026, 5, 5), samples, now = at(2027, 1, 5), zone = utc)
        assertEquals(12, r.buckets.size)
        assertEquals(2000.0 / 6 / 1000, r.buckets[2].kwh, 1e-9)
        assertEquals(600.0 / 6 / 1000, r.buckets[6].kwh, 1e-9)
        assertEquals(r.buckets[2], r.busiest)
    }

    @Test
    fun `olcum yoksa bos rapor`() {
        val r = EnergyReport.summarize(EnergyPeriod.MONTH, at(2026, 10, 5), emptyList(), now = at(2026, 10, 20), zone = utc)
        assertEquals(0.0, r.kwh, 0.0)
        assertNull(r.avgWatts)
        assertNull(r.peak)
        assertNull(r.busiest)
        assertNull(r.avgPerActiveBucket)
    }
}
