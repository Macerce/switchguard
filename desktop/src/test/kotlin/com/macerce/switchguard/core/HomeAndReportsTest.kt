package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalTest {
    @Test fun parsesAndRatesRssi() {
        assertEquals(-62, Signal.rssiOf(JSONObject("""{"rssi":-62}""")))
        assertNull(Signal.rssiOf(JSONObject("""{"rssi":0}""")))
        assertNull(Signal.rssiOf(JSONObject("""{"switch":"on"}""")))
        assertEquals(Signal.Quality.EXCELLENT, Signal.quality(-50))
        assertEquals(Signal.Quality.GOOD, Signal.quality(-67))
        assertEquals(Signal.Quality.FAIR, Signal.quality(-70))
        assertEquals(Signal.Quality.WEAK, Signal.quality(-82))
    }

    @Test fun dropCause() {
        assertNull(Signal.dropCause(2, -85))
        assertEquals(Signal.Cause.WEAK_SIGNAL, Signal.dropCause(3, -80))
        assertEquals(Signal.Cause.POWER_OR_ROUTER, Signal.dropCause(5, -55))
        assertEquals(Signal.Cause.UNKNOWN, Signal.dropCause(4, null))
    }

    @Test fun rssiSurvivesSnapshotJsonAndWsUpdate() {
        val d = DeviceSnapshot("a", "A", true, mapOf(0 to true), rssi = -71)
        assertEquals(-71, DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(d))).single().rssi)
        val after = WsMessages.apply(d, WsEvent.Update("a", online = null, switches = emptyMap(), params = JSONObject("""{"rssi":-60}""")))
        assertEquals(-60, after.rssi)
        val unchanged = WsMessages.apply(d, WsEvent.Update("a", online = null, switches = emptyMap(), params = JSONObject("""{"switch":"on"}""")))
        assertEquals(-71, unchanged.rssi)
    }
}

class StandbyTest {
    private fun hour(i: Int, w: Double?, covered: Long = 3_600_000) =
        EnergyBucket(i * 3_600_000L, (i + 1) * 3_600_000L, (w ?: 0.0) / 1000, w, w, null, if (w == null) 0 else covered)

    @Test fun lowestFullyMeasuredHour() {
        val hours = (0 until 30).map { hour(it, if (it == 5) 6.0 else 40.0) } + hour(31, 1.0, covered = 10 * 60_000L)
        val r = Standby.baseline(hours)!!
        assertEquals(6.0, r.watts, 0.0)
        assertEquals(6.0 * 24 * 30 / 1000, r.monthlyKwh, 1e-9)
    }

    @Test fun needsEnoughHoursAndAboveThreshold() {
        assertNull(Standby.baseline((0 until 10).map { hour(it, 50.0) }))
        assertNull(Standby.baseline((0 until 30).map { hour(it, 1.0) }))
    }

    @Test fun monthlyCost() {
        assertEquals(10.0 * 24 * 30 / 1000 * 2.0, Standby.monthlyCost(10.0, Tariff.Flat(2.0), 100L * 86_400_000L), 1e-6)
    }
}

class StagedEscalationTest {
    private val ahmet = Recipient(1, "Ahmet")
    private val ayse = Recipient(2, "Ayşe")
    private val grup = Recipient(-3, "Aile")

    @Test fun eachRecipientOnOwnDelay() {
        val stages = mapOf(1L to 5, -3L to 30)
        val all = listOf(ahmet, ayse, grup)
        fun due(min: Int, done: Set<Long>) = Escalation.dueRecipients(all, stages, 15, 1_000, done, 1_000 + min * 60_000L)
        assertEquals(emptyList<Recipient>(), due(4, emptySet()))
        assertEquals(listOf(ahmet), due(5, emptySet()))
        assertEquals(emptyList<Recipient>(), due(10, setOf(1)))
        assertEquals(listOf(ayse), due(15, setOf(1)))
        assertEquals(listOf(grup), due(30, setOf(1, 2)))
        assertEquals(emptyList<Recipient>(), Escalation.dueRecipients(all, stages, 15, 0, emptySet(), 99_999_999))
    }
}

class ScenePresenceTest {
    @Test fun sceneJsonAndFind() {
        val s = Scene("1", "Hepsini Kapat", listOf(SceneAction("a", 0, false), SceneAction("b", 2, true)))
        val t = Scene("2", "Tatil", emptyList())
        assertEquals(listOf(s, t), Scene.listFromJson(Scene.listToJson(listOf(s, t))))
        assertEquals(t, Scene.find("TATİL", listOf(s, t)))
        assertEquals(s, Scene.find("hepsini", listOf(s, t)))
        assertNull(Scene.find("yok", listOf(s, t)))
        assertEquals(emptyList<Scene>(), Scene.listFromJson("bozuk"))
    }

    @Test fun vacationCountsAsAway() {
        assertTrue(Presence.away(awayMode = false, vacationUntil = 2_000, now = 1_000))
        assertFalse(Presence.away(awayMode = false, vacationUntil = 500, now = 1_000))
        assertTrue(Presence.away(awayMode = true, vacationUntil = 0, now = 1_000))
    }

    @Test fun presenceRuleGatesAutomation() {
        val devices = mapOf("p" to DeviceSnapshot("p", "Kapı", true, mapOf(0 to false)))
        val opened = mapOf("p" to DeviceSnapshot("p", "Kapı", true, mapOf(0 to true)))
        val a = Automation("a", "Kapı açıldı", true, Trigger(TriggerKind.TURNED_ON, "p"), AutoAction(ActionKind.ALARM), presence = PresenceRule.AWAY)
        val e = AutomationEngine()
        assertTrue(e.onStates(devices, opened, listOf(a), 0, Environment(away = false)).isEmpty())
        assertEquals(listOf(a), e.onStates(devices, opened, listOf(a), 20_000, Environment(away = true)))
        // Bilinmiyorsa (eski çağrılar) sınır yok sayılır.
        assertEquals(listOf(a), AutomationEngine().onStates(devices, opened, listOf(a), 0))
    }

    @Test fun runSceneActionAndPresenceRoundTrip() {
        val a = Automation("a", "Akşam", true, Trigger(TriggerKind.SUNSET, ""), AutoAction(ActionKind.RUN_SCENE, "scene-1"), presence = PresenceRule.HOME)
        assertEquals(listOf(a), Automation.listFromJson(Automation.listToJson(listOf(a))))
        assertEquals(AutomationTab.AUTOMATIONS, AutomationTab.tabOf(a))
    }

    @Test fun telegramPresenceAndSceneCommands() {
        assertEquals(TelegramBot.Command.SetPresence(true), TelegramBot.parseCommand("/dışarıda"))
        assertEquals(TelegramBot.Command.SetPresence(false), TelegramBot.parseCommand("/evde"))
        assertEquals(TelegramBot.Command.RunScene("tatil"), TelegramBot.parseCommand("/sahne tatil"))
    }
}

class ReportTextTest {
    private val doc = ReportDoc(
        title = "Haftalık rapor", subtitle = "13–19 Eki · 3 cihaz", intro = "x",
        kpis = listOf(Kpi("Kesinti", "Yok", "c", Trend("▼ %50 az", Tone.GOOD), Tone.GOOD)),
        sections = listOf(
            ReportSection("Bağlantı", findings = listOf(
                Finding(Severity.GOOD, "iyi"),
                Finding(Severity.WARN, "Pompa <SPM> & röle koptu", action = "Yaklaştırın"),
            )),
            ReportSection("İzleme", findings = listOf(Finding(Severity.GOOD, "Kesintisiz"))),
        ),
    )

    @Test fun htmlIsEscapedAndCompact() {
        val t = ReportText.telegramHtml(doc, "Ne yapmalı", "Ne yapmalı:")
        assertTrue(t.contains("<b>Haftalık rapor</b>"))
        assertTrue(t.contains("Pompa &lt;SPM&gt; &amp; röle koptu"))
        // Bölümde başka bulgu varsa "iyi" bulguları Telegram'da gösterilmez; tek bulgu ise gösterilir.
        assertFalse(t.contains("✅ iyi"))
        assertTrue(t.contains("✅ Kesintisiz"))
        assertTrue(t.contains("1. Yaklaştırın"))
        assertEquals(listOf("Yaklaştırın"), doc.actions)
    }

    @Test fun compareTone() {
        assertEquals(-50, Compare.change(5.0, 10.0))
        assertNull(Compare.change(5.0, 0.0))
        assertEquals(Tone.GOOD, Compare.tone(-50, lowerIsBetter = true))
        assertEquals(Tone.WARN, Compare.tone(30, lowerIsBetter = true))
        assertEquals(Tone.NEUTRAL, Compare.tone(3, lowerIsBetter = true))
    }
}
