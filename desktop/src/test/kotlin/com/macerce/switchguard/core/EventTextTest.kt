package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTextTest {
    private val en = EventStrings(
        ChangeLabels("ON", "OFF", "ONLINE", "OFFLINE") { n, c -> "$n (Ch $c)" },
        shortDrop = { "$it: brief disconnection ignored" },
        test = "Test alarm",
        automationFired = { "Automation “$it”" },
        deviceMissing = "This device is no longer in your account.",
        automationFailed = { t, e -> "$t — failed: $e" },
        powerStale = { "$it: no channel power data for 5 minutes." },
    )
    private val tr = EventStrings(
        ChangeLabels("AÇIK", "KAPALI", "ONLINE", "OFFLINE") { n, c -> "$n (K$c)" },
        shortDrop = { "$it: kısa bağlantı kopması yok sayıldı" },
        test = "Test alarmı",
        automationFired = { "Otomasyon “$it”" },
        deviceMissing = "Bu cihaz artık hesabınızda değil.",
        automationFailed = { t, e -> "$t — başarısız: $e" },
        powerStale = { "$it: kanal gücü verisi 5 dakikadır alınamıyor." },
    )
    private val both = listOf(en, tr)
    private val rogar = "Rögar Su Seviye Bildirimi"

    @Test fun switchRendersInEachLanguage() {
        val a = EventArgs.Switch(0, multi = true, from = true, to = false)
        assertEquals("$rogar (K1): AÇIK → KAPALI", a.render(rogar, tr))
        assertEquals("$rogar (Ch 1): ON → OFF", a.render(rogar, en))
    }

    @Test fun jsonRoundTrip() {
        val all = listOf(
            EventArgs.Switch(3, true, false, true), EventArgs.Connection(true, false),
            EventArgs.ShortDrop, EventArgs.Test, EventArgs.PowerStale,
            EventArgs.AutoMissing("x"), EventArgs.AutoSwitch("a", 1, true, false, "boom"),
            EventArgs.AutoSwitch("a", 0, false, true), EventArgs.AutoAlert("a", "src"), EventArgs.AutoAlert("a", null),
        )
        all.forEach { assertEquals(it, EventArgs.parse(it.toJson())) }
        assertNull(EventArgs.parse(null)); assertNull(EventArgs.parse("{bad"))
    }

    @Test fun legacyTurkishSwitchAndConnection() {
        assertEquals(
            EventArgs.Switch(0, true, false, true),
            EventArgs.fromLegacy("TURNED_ON", rogar, "$rogar (K1): KAPALI → AÇIK", both),
        )
        assertEquals(
            EventArgs.Connection(true, false),
            EventArgs.fromLegacy("WENT_OFFLINE", rogar, "$rogar: ONLINE → OFFLINE", both),
        )
        assertEquals(
            EventArgs.Switch(0, false, true, false),
            EventArgs.fromLegacy("TURNED_OFF", "Lamp", "Lamp: ON → OFF", both),
        )
    }

    @Test fun legacySimpleKinds() {
        assertEquals(EventArgs.ShortDrop, EventArgs.fromLegacy("SHORT_DROP", rogar, "$rogar: kısa bağlantı kopması yok sayıldı", both))
        assertEquals(EventArgs.Test, EventArgs.fromLegacy("TEST", "SwitchGuard", "Test alarmı", both))
    }

    @Test fun legacyAutomations() {
        assertEquals(
            EventArgs.AutoAlert("Rogar acik kaldi", rogar),
            EventArgs.fromLegacy("AUTOMATION", rogar, "Otomasyon “Rogar acik kaldi” ($rogar)", both),
        )
        assertEquals(
            EventArgs.AutoSwitch("Pompa", 1, true, false),
            EventArgs.fromLegacy("AUTOMATION", "Pump", "Automation “Pompa”: Pump (Ch 2) → OFF", both),
        )
        assertEquals(
            EventArgs.AutoSwitch("x “y”", 0, false, true, "timeout: 10 s"),
            EventArgs.fromLegacy("AUTOMATION", "Lamp", "Otomasyon “x “y””: Lamp → AÇIK — başarısız: timeout: 10 s", both),
        )
        assertEquals(
            EventArgs.AutoMissing("Gone"),
            EventArgs.fromLegacy("AUTOMATION", "Gone", "Automation “Gone”: This device is no longer in your account.", both),
        )
    }

    @Test fun unknownTextStaysUnparsed() {
        assertNull(EventArgs.fromLegacy("TURNED_ON", rogar, "something else", both))
    }
}
