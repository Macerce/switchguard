package com.macerce.switchguard.core

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCommandTest {
    private val salon = DeviceSnapshot("s", "Salon Işık", online = true, switches = mapOf(0 to false))
    private val mutfak = DeviceSnapshot("m", "Mutfak Işık", online = true, switches = mapOf(0 to true))
    private val pompa = DeviceSnapshot("p", "Pompa", online = true, switches = mapOf(0 to true, 1 to false))
    private val klima = DeviceSnapshot("k", "Klima", online = true, switches = mapOf(0 to true), power = 900.0)
    private val devices = listOf(salon, mutfak, pompa, klima)

    @Test
    fun freeTextSwitchWithSeveralTargets() {
        assertEquals(ChatCommand.Intent.Switch(false, listOf("salon", "mutfak isigini")), ChatCommand.parse("Salon ve mutfak ışığını kapat"))
        assertEquals(ChatCommand.Intent.Switch(true, listOf("salonu")), ChatCommand.parse("salonu aç"))
        assertEquals(ChatCommand.Intent.Switch(true, listOf("kitchen", "garden")), ChatCommand.parse("turn on kitchen, garden"))
        assertEquals(ChatCommand.Intent.Switch(false, listOf("bahçe", "havuz")), ChatCommand.parse("/kapat bahçe, havuz"))
    }

    @Test
    fun questionsAreNotCommands() {
        assertEquals(ChatCommand.Intent.Status("salon"), ChatCommand.parse("salon açık mı"))
        assertEquals(ChatCommand.Intent.Status(""), ChatCommand.parse("durum"))
        assertEquals(ChatCommand.Intent.Status("kitchen"), ChatCommand.parse("is the kitchen on?"))
        assertEquals(ChatCommand.Intent.Power("klima"), ChatCommand.parse("klima kaç watt çekiyor"))
        assertEquals(ChatCommand.Intent.Power(""), ChatCommand.parse("güç"))
        assertEquals(ChatCommand.Intent.Power("klima"), ChatCommand.parse("/watt klima"))
        assertEquals(ChatCommand.Intent.Temperature, ChatCommand.parse("kaç derece"))
        assertEquals(ChatCommand.Intent.SetPresence(true), ChatCommand.parse("dışarıdayım"))
        assertEquals(ChatCommand.Intent.RunScene("aksam"), ChatCommand.parse("akşam sahnesini çalıştır"))
        assertEquals(ChatCommand.Intent.Help, ChatCommand.parse("yardım"))
        assertNull(ChatCommand.parse("merhaba, yarın görüşürüz"))
    }

    @Test
    fun resolvesInflectedNames() {
        assertEquals(ChatCommand.Target.One(salon, 0), ChatCommand.resolve("salon isigini", devices))
        assertEquals(ChatCommand.Target.One(pompa, 1), ChatCommand.resolve("pompayı 2", devices))
        assertEquals(ChatCommand.Target.NeedChannel(pompa), ChatCommand.resolve("pompayı", devices))
        assertEquals(ChatCommand.Target.Many(listOf(salon, mutfak)), ChatCommand.resolve("tüm ışıkları", devices))
        assertTrue(ChatCommand.resolve("ışık", devices) is ChatCommand.Target.Ambiguous)
        assertEquals(ChatCommand.Target.NotFound, ChatCommand.resolve("garaj", devices))
    }

    @Test
    fun typicalWattsIgnoresStandby() {
        val samples = listOf(0.5, 1.0, 850.0, 900.0, 950.0, 2.0, 880.0, 920.0).mapIndexed { i, w -> PowerSample(i.toLong(), w) }
        assertEquals(900.0, ChatCommand.typicalWatts(samples)!!, 0.01)
        assertNull(ChatCommand.typicalWatts(samples.take(3)))
    }

    private fun at(h: Int, m: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 28, h, m, 0); set(Calendar.MILLISECOND, 0) // Çarşamba
    }.timeInMillis

    @Test
    fun timedTemplateRunsOncePerDay() {
        val t = ChatTemplate("a", "Akşam", "bahçe aç", atMinute = 19 * 60)
        assertTrue(ChatTemplate.due(listOf(t), emptyMap(), at(18, 59)).first.isEmpty())
        val (due, runs) = ChatTemplate.due(listOf(t), emptyMap(), at(19, 0))
        assertEquals(listOf(t), due)
        assertTrue(ChatTemplate.due(listOf(t), runs, at(19, 5)).first.isEmpty())
        // Telefon kapalıydı: çok geç fark edilince çalışmaz ama harcanır.
        val (late, lateRuns) = ChatTemplate.due(listOf(t), emptyMap(), at(23, 0))
        assertTrue(late.isEmpty())
        assertTrue("a" in lateRuns)
        // Çarşamba seçili değil.
        assertTrue(ChatTemplate.due(listOf(t.copy(days = 0b0000011)), emptyMap(), at(19, 0)).first.isEmpty())
        assertEquals(at(19, 0), ChatTemplate.nextAt(listOf(t), emptyMap(), at(10, 0)))
    }

    @Test
    fun templateJsonAndAutomations() {
        val list = listOf(
            ChatTemplate("a", "Akşam", "bahçe aç", atMinute = 1140, days = 0b0011111),
            ChatTemplate("b", "Klima", "salon kapat", enabled = false, trigger = Trigger(TriggerKind.POWER_ABOVE, "k", watts = 500.0)),
        )
        assertEquals(list, ChatTemplate.listFromJson(ChatTemplate.listToJson(list)))
        val autos = ChatTemplate.automations(list)
        assertEquals(1, autos.size)
        assertEquals("tpl:b", autos[0].id)
        assertEquals(AutoAction(ActionKind.RUN_SCENE, "tpl:b"), autos[0].action)
        assertNull(ChatTemplate.listFromJson(null))
    }
}
