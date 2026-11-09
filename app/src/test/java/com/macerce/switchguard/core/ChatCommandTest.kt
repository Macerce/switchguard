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
    fun spokenSentences() {
        fun on(vararg t: String) = ChatCommand.Intent.Switch(true, t.toList())
        fun off(vararg t: String) = ChatCommand.Intent.Switch(false, t.toList())
        assertEquals(off("mutfagin isigini"), ChatCommand.parse("Mutfağın ışığını kapat"))
        assertEquals(off("mutfaktaki isigi"), ChatCommand.parse("mutfaktaki ışığı kapatır mısın"))
        assertEquals(on("pompayi"), ChatCommand.parse("pompayı çalıştır"))
        assertEquals(off("pompayi"), ChatCommand.parse("lütfen pompayı durdur"))
        assertEquals(off("kitchen light"), ChatCommand.parse("Turn off the kitchen light"))
        assertEquals(off("kitchen light"), ChatCommand.parse("turn the kitchen light off"))
        assertEquals(on("pompa bir"), ChatCommand.parse("pompa bir aç"))
        assertEquals(on("salonu bir"), ChatCommand.parse("şu salonu bir aç"))
        // Tek cümlede aç ve kapat.
        assertEquals(ChatCommand.Intent.Batch(listOf(on("mutfagi"), off("salonu", "klimayi"))), ChatCommand.parse("mutfağı aç, salonu ve klimayı kapat"))
        assertEquals(ChatCommand.Intent.Batch(listOf(off("salon"), on("mutfak"))), ChatCommand.parse("salon kapat mutfak aç"))
        assertEquals(
            ChatCommand.Intent.Batch(listOf(on("garden"), off("garage"))),
            ChatCommand.parse("Turn on the garden and turn off the garage"),
        )
        assertEquals(ChatCommand.Intent.Batch(listOf(on("kitchen"), off("garden"))), ChatCommand.parse("turn the kitchen on and the garden off"))
        // Aynı yöndeki adımlar birleşir.
        assertEquals(on("salonu", "mutfagi"), ChatCommand.parse("salonu aç mutfağı aç"))
        assertNull(ChatCommand.parse("aç kapat"))
    }

    @Test
    fun resolvesSpokenTargets() {
        val oda = DeviceSnapshot("o", "Yatak Odası", online = true, switches = mapOf(0 to false))
        val isitici = DeviceSnapshot("i", "Isıtıcı", online = true, switches = mapOf(0 to false))
        val all = devices + oda + isitici
        assertEquals(ChatCommand.Target.One(mutfak, 0), ChatCommand.resolve("mutfagin isigini", all))
        assertEquals(ChatCommand.Target.One(mutfak, 0), ChatCommand.resolve("mutfaktaki isigi", all))
        // Adında "ışık" geçmeyen cihaz: genel söz atılır.
        assertEquals(ChatCommand.Target.One(oda, 0), ChatCommand.resolve("yatak odasinin isigini", all))
        assertEquals(ChatCommand.Target.One(klima, 0), ChatCommand.resolve("klimanin lambasini", all))
        // "ısıtıcı" ile "ışık" aynı kök değil.
        assertEquals(ChatCommand.Target.One(isitici, 0), ChatCommand.resolve("isiticiyi", all))
        assertEquals(ChatCommand.Target.NotFound, ChatCommand.resolve("isiticiyi", devices))
        // Kanal: sözle, sıra sayısıyla, "kanal" sözüyle.
        assertEquals(ChatCommand.Target.One(pompa, 1), ChatCommand.resolve("pompa iki", all))
        assertEquals(ChatCommand.Target.One(pompa, 0), ChatCommand.resolve("pompa bir", all))
        assertEquals(ChatCommand.Target.One(pompa, 1), ChatCommand.resolve("pompanin ikinci kanalini", all))
        assertEquals(ChatCommand.Target.One(pompa, 1), ChatCommand.resolve("pump second channel".replace("pump", "pompa"), all))
        assertEquals(ChatCommand.Target.One(salon, 0), ChatCommand.resolve("salonu bir", all))
        assertEquals(ChatCommand.Target.NotFound, ChatCommand.resolve("kanal", all))
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
