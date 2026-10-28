package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneTest {

    private val home = Zone("zone:home", "Ev", 41.0082, 28.9784, 200)

    @Test
    fun containsUsesRadiusInMeters() {
        assertTrue(home.contains(41.0082, 28.9784))
        // ≈111 m kuzeyde: içeride; ≈333 m kuzeyde: dışarıda.
        assertTrue(home.contains(41.0092, 28.9784))
        assertFalse(home.contains(41.0112, 28.9784))
        assertEquals(111.2, Zone.distance(0.0, 0.0, 0.001, 0.0), 0.5)
    }

    @Test
    fun jsonRoundTripAndBadRecords() {
        val list = listOf(home, Zone("zone:work", "İş", -33.5, 151.2, 1_000))
        assertEquals(list, Zone.listFromJson(Zone.listToJson(list)))
        // Bozuk kayıt atlanır, yarıçap sınırlara çekilir.
        val parsed = Zone.listFromJson("""[{"id":"zone:a","name":"A","lat":1.0,"lon":2.0,"r":5},{"name":"no id"}]""")
        assertEquals(1, parsed.size)
        assertEquals(Zone.MIN_RADIUS, parsed[0].radius)
        assertEquals(emptyList<Zone>(), Zone.listFromJson(null))
        assertEquals(emptyList<Zone>(), Zone.listFromJson("not json"))
    }

    @Test
    fun zoneTriggersAreNotDeviceTriggers() {
        assertTrue(TriggerKind.ZONE_ENTER.usesZone && TriggerKind.ZONE_EXIT.usesZone)
        assertFalse(TriggerKind.ZONE_ENTER.usesDevice)
        assertFalse(TriggerKind.ZONE_EXIT.usesChannel)
    }

    @Test
    fun engineFiresOnlyMatchingZoneAndDirection() {
        val arrive = Automation("a1", "Eve gelince", true, Trigger(TriggerKind.ZONE_ENTER, home.id), AutoAction(ActionKind.TURN_ON, "lamp"))
        val leave = Automation("a2", "Evden çıkınca", true, Trigger(TriggerKind.ZONE_EXIT, home.id), AutoAction(ActionKind.TURN_OFF, "lamp"))
        val other = Automation("a3", "İşe gelince", true, Trigger(TriggerKind.ZONE_ENTER, "zone:work"), AutoAction(ActionKind.NOTIFY))
        val off = Automation("a4", "Kapalı", false, Trigger(TriggerKind.ZONE_ENTER, home.id), AutoAction(ActionKind.NOTIFY))
        val all = listOf(arrive, leave, other, off)
        val engine = AutomationEngine()
        val now = 1_000_000L
        assertEquals(listOf(arrive), engine.onZone(home.id, true, emptyMap(), all, now))
        assertEquals(listOf(leave), engine.onZone(home.id, false, emptyMap(), all, now + 60_000))
        // Aynı geçiş hemen tekrar bildirilirse ikinci kez çalışmaz.
        assertEquals(emptyList<Automation>(), engine.onZone(home.id, false, emptyMap(), all, now + 61_000))
        // Cihaz durumları değişince bölge kuralları kendiliğinden tetiklenmez.
        assertEquals(emptyList<Automation>(), engine.onStates(emptyMap(), emptyMap(), all, now + 120_000))
    }

    @Test
    fun zoneRuleRespectsPresenceAndJson() {
        val rule = Automation(
            "a1", "Eve gelince", true, Trigger(TriggerKind.ZONE_ENTER, home.id), AutoAction(ActionKind.NOTIFY),
            presence = PresenceRule.AWAY,
        )
        val engine = AutomationEngine()
        assertEquals(emptyList<Automation>(), engine.onZone(home.id, true, emptyMap(), listOf(rule), 0L, Environment(away = false)))
        assertEquals(listOf(rule), engine.onZone(home.id, true, emptyMap(), listOf(rule), 20_000L, Environment(away = true)))
        assertEquals(listOf(rule), Automation.listFromJson(Automation.listToJson(listOf(rule))))
    }

    @Test
    fun capitalizedTemplateTextsStillParse() {
        assertEquals(ChatCommand.Intent.Temperature, ChatCommand.parse("Kaç derece?"))
        assertEquals(ChatCommand.Intent.Temperature, ChatCommand.parse("Temperature?"))
        assertEquals(ChatCommand.Intent.Status(""), ChatCommand.parse("Durum"))
        assertEquals(ChatCommand.Intent.Power(""), ChatCommand.parse("Güç"))
        assertEquals(ChatCommand.Intent.SetPresence(false), ChatCommand.parse("Evdeyim"))
        assertEquals(ChatCommand.Intent.SetPresence(true), ChatCommand.parse("Dışarıdayım"))
        assertEquals(ChatCommand.Intent.SetPresence(false), ChatCommand.parse("Home"))
        assertEquals(ChatCommand.Intent.SetPresence(true), ChatCommand.parse("Away"))
        assertEquals(ChatCommand.Intent.Help, ChatCommand.parse("Yardım"))
        assertEquals(ChatCommand.Intent.Help, ChatCommand.parse("Help"))
    }
}
