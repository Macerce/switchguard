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
        monitorGap = { from, to -> "Monitoring was down $from–$to" },
        monitorBlind = { "Monitoring not working since $it" },
        monitorRestored = { from, to -> "Monitoring working again after ${to - from}" },
        alarmDismissed = { n, ms -> "Alarm dismissed: $n events, $ms ms" },
        reminder = { label, state, ms -> "Reminder: $label is still $state ($ms ms)" },
        escalation = { n, min, err -> if (err == null) "Not dismissed for $min min: told $n" else "Not dismissed for $min min, couldn't tell: $err" },
    )
    private val tr = EventStrings(
        ChangeLabels("AÇIK", "KAPALI", "ONLINE", "OFFLINE") { n, c -> "$n (K$c)" },
        shortDrop = { "$it: kısa bağlantı kopması yok sayıldı" },
        test = "Test alarmı",
        automationFired = { "Otomasyon “$it”" },
        deviceMissing = "Bu cihaz artık hesabınızda değil.",
        automationFailed = { t, e -> "$t — başarısız: $e" },
        powerStale = { "$it: kanal gücü verisi 5 dakikadır alınamıyor." },
        monitorGap = { from, to -> "İzleme durdu: $from–$to" },
        monitorBlind = { "İzleme çalışmıyor: $it" },
        monitorRestored = { from, to -> "İzleme yeniden çalışıyor: ${to - from}" },
        alarmDismissed = { n, ms -> "Alarm kapatıldı: $n olay, $ms ms" },
        reminder = { label, state, ms -> "Hatırlatma: $label hâlâ $state ($ms ms)" },
        escalation = { n, min, err -> if (err == null) "$min dk kapatılmadı: $n kişi" else "$min dk kapatılmadı, haber verilemedi: $err" },
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
            EventArgs.MonitorGap(1_000, 61_000), EventArgs.MonitorBlind(5_000), EventArgs.MonitorRestored(5_000, 9_000),
            EventArgs.AlarmDismissed(3, 130_000),
            EventArgs.Reminder(1, multi = true, connection = false, to = false, durationMs = 60_000),
            EventArgs.Reminder(0, multi = false, connection = true, to = false, durationMs = 90_000),
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

    @Test fun monitorGapRendersInEachLanguage() {
        val a = EventArgs.MonitorGap(1_000, 61_000)
        assertEquals("İzleme durdu: 1000–61000", a.render("SwitchGuard", tr))
        assertEquals("Monitoring was down 1000–61000", a.render("SwitchGuard", en))
    }

    @Test fun blindAndRestoredRender() {
        assertEquals("İzleme çalışmıyor: 5000", EventArgs.MonitorBlind(5_000).render("SwitchGuard", tr))
        assertEquals("Monitoring working again after 4000", EventArgs.MonitorRestored(5_000, 9_000).render("SwitchGuard", en))
    }

    @Test fun alarmDismissedRendersCountAndResponseTime() {
        assertEquals("Alarm kapatıldı: 3 olay, 130000 ms", EventArgs.AlarmDismissed(3, 130_000).render("SwitchGuard", tr))
        assertEquals("Alarm dismissed: 1 events, 9000 ms", EventArgs.AlarmDismissed(1, 9_000).render("SwitchGuard", en))
    }

    @Test fun reminderRendersChannelAndState() {
        assertEquals("Hatırlatma: $rogar (K2) hâlâ KAPALI (60000 ms)",
            EventArgs.Reminder(1, multi = true, connection = false, to = false, durationMs = 60_000).render(rogar, tr))
        assertEquals("Reminder: Pump is still OFFLINE (90000 ms)",
            EventArgs.Reminder(0, multi = false, connection = true, to = false, durationMs = 90_000).render("Pump", en))
    }

    @Test fun reminderArgsFromChange() {
        val r = AlarmReminders.Reminder(Change.Switch("r", rogar, 1, true, from = true, to = false), 60_000)
        assertEquals(EventArgs.Reminder(1, multi = true, connection = false, to = false, durationMs = 60_000), EventArgs.Reminder.of(r))
    }
}
