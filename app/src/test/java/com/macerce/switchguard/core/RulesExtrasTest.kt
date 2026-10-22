package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SunTest {
    private val istanbul = TimeZone.getTimeZone("Europe/Istanbul")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0, zone: TimeZone = istanbul) =
        Calendar.getInstance(zone).apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    private fun hm(t: Long, zone: TimeZone = istanbul) = Calendar.getInstance(zone).run { timeInMillis = t; get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE) }

    @Test fun istanbulSummerAndWinter() {
        // İstanbul 21 Haziran: gün doğumu ≈05:32, batımı ≈20:39 (UTC+3).
        val rise = Sun.time(at(2026, 6, 21), 41.01, 28.98, sunrise = true, zone = istanbul)!!
        val set = Sun.time(at(2026, 6, 21), 41.01, 28.98, sunrise = false, zone = istanbul)!!
        assertEquals(5 * 60 + 32.0, hm(rise).toDouble(), 4.0)
        assertEquals(20 * 60 + 39.0, hm(set).toDouble(), 4.0)
        // 21 Aralık: ≈08:24 ve ≈17:39 (Türkiye 2016'dan beri yıl boyu UTC+3).
        assertEquals(8 * 60 + 24.0, hm(Sun.time(at(2026, 12, 21), 41.01, 28.98, true, istanbul)!!).toDouble(), 4.0)
        assertEquals(17 * 60 + 39.0, hm(Sun.time(at(2026, 12, 21), 41.01, 28.98, false, istanbul)!!).toDouble(), 4.0)
    }

    @Test fun resultIsOnTheSameLocalDay() {
        val day = at(2026, 3, 1, 0, 30)
        val rise = Sun.time(day, 41.01, 28.98, true, istanbul)!!
        assertEquals(Calendar.getInstance(istanbul).run { timeInMillis = day; get(Calendar.DAY_OF_YEAR) },
            Calendar.getInstance(istanbul).run { timeInMillis = rise; get(Calendar.DAY_OF_YEAR) })
    }

    @Test fun polarNightHasNoSunrise() {
        assertNull(Sun.time(at(2026, 12, 21, zone = TimeZone.getTimeZone("UTC")), 78.2, 15.6, true, TimeZone.getTimeZone("UTC")))
    }
}

class ActiveWindowTest {
    private val utc = TimeZone.getTimeZone("UTC")
    // 2026-10-19 pazartesi.
    private fun at(day: Int, h: Int, m: Int = 0) = Calendar.getInstance(utc).apply { clear(); set(2026, 9, day, h, m) }.timeInMillis

    @Test fun sameDayRange() {
        val w = ActiveWindow(9 * 60, 17 * 60)
        assertTrue(w.contains(at(19, 9), utc))
        assertFalse(w.contains(at(19, 17), utc))
        assertFalse(w.contains(at(19, 8, 59), utc))
    }

    @Test fun overnightRangeUsesStartDay() {
        // Yalnızca cuma gecesi 22:00–06:00: cumartesi 03:00 içinde, pazartesi 03:00 dışında.
        val fridayNight = ActiveWindow(22 * 60, 6 * 60, days = 1 shl 4)
        assertTrue(fridayNight.contains(at(23, 23), utc))
        assertTrue(fridayNight.contains(at(24, 3), utc))
        assertFalse(fridayNight.contains(at(19, 3), utc))
        assertFalse(fridayNight.contains(at(24, 7), utc))
    }

    @Test fun allDayOnSelectedDays() {
        val weekend = ActiveWindow(0, 0, days = 0b1100000)
        assertTrue(weekend.contains(at(25, 15), utc))
        assertFalse(weekend.contains(at(20, 15), utc))
    }
}

class ConditionAndGateTest {
    private val devices = mapOf(
        "p" to DeviceSnapshot("p", "Pompa", true, mapOf(0 to true, 1 to false)),
        "x" to DeviceSnapshot("x", "Kombi", false, mapOf(0 to true)),
    )

    @Test fun conditions() {
        assertTrue(Condition(ConditionKind.IS_ON, "p", 0).holds(devices))
        assertTrue(Condition(ConditionKind.IS_OFF, "p", 1).holds(devices))
        assertTrue(Condition(ConditionKind.IS_OFFLINE, "x").holds(devices))
        // Çevrimdışı cihazın son anahtar durumu bilinmez: "açık" sayılmaz.
        assertFalse(Condition(ConditionKind.IS_ON, "x").holds(devices))
        assertFalse(Condition(ConditionKind.IS_ONLINE, "yok").holds(devices))
    }

    @Test fun conditionBlocksInstantTrigger() {
        val a = Automation(
            "a", "Pompa açılınca", true, Trigger(TriggerKind.TURNED_ON, "p", channel = 1), AutoAction(ActionKind.NOTIFY),
            condition = Condition(ConditionKind.IS_ONLINE, "x"),
        )
        val e = AutomationEngine()
        val off = devices
        val on = devices + ("p" to devices["p"]!!.copy(switches = mapOf(0 to true, 1 to true)))
        assertTrue(e.onStates(off, on, listOf(a), 0).isEmpty())
        val online = on + ("x" to devices["x"]!!.copy(online = true))
        assertEquals(listOf(a), e.onStates(off, online, listOf(a), 20_000))
    }

    @Test fun durationTriggerWaitsForWindow() {
        val utc = TimeZone.getDefault()
        val base = Calendar.getInstance(utc).apply { set(2026, 9, 19, 20, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val a = Automation(
            "a", "Gece açık kalırsa", true, Trigger(TriggerKind.STAYED_ON, "p", minutes = 30), AutoAction(ActionKind.ALARM),
            window = ActiveWindow(22 * 60, 6 * 60),
        )
        val e = AutomationEngine()
        e.onStates(emptyMap(), devices, listOf(a), base)
        // 20:30'da süre doldu ama aralık dışında: harcanmaz, 22:00'de tetiklenir.
        assertTrue(e.onTick(devices, listOf(a), base + 30 * 60_000L).isEmpty())
        assertEquals(listOf(a), e.onTick(devices, listOf(a), base + 2 * 3_600_000L))
        assertTrue(e.onTick(devices, listOf(a), base + 2 * 3_600_000L + 60_000).isEmpty())
    }

    @Test fun jsonRoundTripKeepsWindowAndCondition() {
        val a = Automation(
            "a", "x", true, Trigger(TriggerKind.SUNSET, "", minutes = -15), AutoAction(ActionKind.TURN_ON, "p", 1),
            window = ActiveWindow(60, 120, 0b0011111), condition = Condition(ConditionKind.IS_OFF, "p", 1),
        )
        assertEquals(listOf(a), Automation.listFromJson(Automation.listToJson(listOf(a))))
        // Eski kayıt (alanlar yok) olduğu gibi okunur.
        val old = Automation("b", "y", true, Trigger(TriggerKind.TURNED_ON, "p"), AutoAction(ActionKind.ALARM))
        assertEquals(listOf(old), Automation.listFromJson(Automation.listToJson(listOf(old))))
    }
}

class SunAndTempTriggerTest {
    private val env = Environment(41.01, 28.98, null)

    @Test fun sunsetWithOffsetFiresOncePerDay() {
        val a = Automation("s", "Akşam", true, Trigger(TriggerKind.SUNSET, "", minutes = -10), AutoAction(ActionKind.TURN_ON, "p"))
        val e = AutomationEngine()
        val noon = Calendar.getInstance().apply { set(2026, 5, 21, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val due = e.nextDueAt(emptyMap(), listOf(a), env, noon)
        assertNotNull(due)
        val sunset = Sun.time(noon, 41.01, 28.98, false)!!
        assertEquals(sunset - 600_000L, due)
        assertTrue(e.onTick(emptyMap(), listOf(a), due!! - 1, env).isEmpty())
        assertEquals(listOf(a), e.onTick(emptyMap(), listOf(a), due, env))
        assertTrue(e.onTick(emptyMap(), listOf(a), due + 60_000, env).isEmpty())
    }

    @Test fun lateSunTriggerIsSkipped() {
        val a = Automation("s", "Sabah", true, Trigger(TriggerKind.SUNRISE, ""), AutoAction(ActionKind.TURN_OFF, "p"))
        val rise = Sun.time(System.currentTimeMillis(), 41.01, 28.98, true)!!
        assertTrue(AutomationEngine().onTick(emptyMap(), listOf(a), rise + AutomationEngine.SUN_LATE_MS + 1, env).isEmpty())
    }

    @Test fun noLocationNoSun() {
        val a = Automation("s", "x", true, Trigger(TriggerKind.SUNRISE, ""), AutoAction(ActionKind.NOTIFY))
        assertNull(AutomationEngine().nextDueAt(emptyMap(), listOf(a), Environment(), 0))
    }

    @Test fun temperatureWithHysteresis() {
        val a = Automation("t", "Don", true, Trigger(TriggerKind.TEMP_BELOW, "", watts = 2.0), AutoAction(ActionKind.ALARM))
        val e = AutomationEngine()
        fun tick(t: Double, now: Long) = e.onTick(emptyMap(), listOf(a), now, env.copy(outdoorTemp = t))
        assertTrue(tick(5.0, 0).isEmpty())
        assertEquals(listOf(a), tick(1.5, 20_000))
        assertTrue(tick(0.0, 40_000).isEmpty())
        assertTrue(tick(2.5, 60_000).isEmpty()) // 1 °C geri dönmedi
        tick(3.5, 80_000)
        assertEquals(listOf(a), tick(1.0, 100_000))
    }
}

class TariffTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(h: Int, m: Int = 0) = Calendar.getInstance(utc).apply { clear(); set(2026, 9, 19, h, m) }.timeInMillis

    @Test fun flatEqualsKwhTimesPrice() {
        val samples = listOf(PowerSample(at(0), 1000.0), PowerSample(at(1), 1000.0))
        assertEquals(2.0, Tariff.Flat(2.0).cost(samples, at(0), at(1), utc, maxGapMs = 2 * 3_600_000L), 1e-9)
    }

    @Test fun threeZoneSplitsAtBoundaries() {
        val t = Tariff.ThreeZone(day = 2.0, peak = 3.0, night = 1.0)
        // 1 kW sürekli, 16:00–23:00 → gündüz 1 sa (2), puant 5 sa (15), gece 1 sa (1) = 18.
        val samples = (16..23).map { PowerSample(at(it), 1000.0) }
        assertEquals(18.0, t.cost(samples, at(16), at(23), utc, maxGapMs = 2 * 3_600_000L), 1e-6)
        assertEquals(3.0, t.priceAt(17 * 60), 0.0)
        assertEquals(1.0, t.priceAt(5 * 60 + 59), 0.0)
    }

    @Test fun noPriceNoCost() {
        assertFalse(Tariff.ThreeZone(0.0, 0.0, 0.0).hasPrice)
        assertEquals(0.0, Tariff.Flat(0.0).cost(listOf(PowerSample(0, 5000.0)), 0, 3_600_000), 0.0)
    }

    @Test fun summaryCarriesCost() {
        val samples = (0..24).map { PowerSample(at(0) + it * 3_600_000L, 500.0) }
        val s = EnergyReport.summarize(EnergyPeriod.DAY, at(12), samples, at(23, 59) + 60_000, utc, maxGapMs = 2 * 3_600_000L, tariff = Tariff.Flat(4.0))
        assertEquals(s.kwh * 4.0, s.cost, 1e-6)
    }
}
