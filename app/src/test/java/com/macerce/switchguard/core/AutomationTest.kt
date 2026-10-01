package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationTest {

    private val min = 60_000L
    private fun dev(vararg sw: Boolean, online: Boolean = true, power: Double? = null) =
        DeviceSnapshot("d", "Rögar", online, sw.withIndex().associate { it.index to it.value }, power = power)
    private fun map(d: DeviceSnapshot) = mapOf(d.id to d)
    private fun auto(kind: TriggerKind, channel: Int = 0, minutes: Int = 0, watts: Double = 0.0, delay: Int = 0) =
        Automation("a1", "test", true, Trigger(kind, "d", channel, minutes, watts), AutoAction(ActionKind.TURN_ON, "lamp"), delay)

    // ---- kanal açıkken güç düşmesi (SPM-4Relay gibi kanal başına ölçüm yapan cihazlar)

    private fun spm(k1On: Boolean, k1Watts: Double?) = DeviceSnapshot(
        "d", "SPM", true, mapOf(0 to false, 1 to k1On),
        channelPower = listOfNotNull(0 to 0.0, k1Watts?.let { 1 to it }).toMap(),
    )
    private val lowPower = listOf(
        Automation("p1", "pompa", true, Trigger(TriggerKind.CHANNEL_POWER_LOW, "d", channel = 1, minutes = 1, watts = 20.0, graceMinutes = 2),
            AutoAction(ActionKind.ALARM)),
    )

    @Test
    fun `kanal acikken guc esigin altinda kalinca bir kez tetiklenir, toparlaninca yeniden kurulur`() {
        val e = AutomationEngine()
        val a = lowPower
        e.onStates(map(spm(false, 0.0)), map(spm(true, 0.0)), a, 0)
        // Açılış payı (2 dk) içinde güç 0 olsa da tetiklenmez.
        assertTrue(e.onTick(map(spm(true, 0.0)), a, 1 * min).isEmpty())
        e.onStates(map(spm(true, 0.0)), map(spm(true, 100.0)), a, 3 * min)
        assertTrue(e.onTick(map(spm(true, 100.0)), a, 5 * min).isEmpty())

        e.onStates(map(spm(true, 100.0)), map(spm(true, 5.0)), a, 10 * min)
        assertTrue(e.onTick(map(spm(true, 5.0)), a, 10 * min + 30_000).isEmpty())
        assertEquals(11 * min, e.nextDueAt(map(spm(true, 5.0)), a))
        assertEquals(a, e.onTick(map(spm(true, 5.0)), a, 11 * min))
        assertTrue(e.onTick(map(spm(true, 5.0)), a, 15 * min).isEmpty())

        e.onStates(map(spm(true, 5.0)), map(spm(true, 100.0)), a, 20 * min)
        e.onStates(map(spm(true, 100.0)), map(spm(true, 5.0)), a, 30 * min)
        assertEquals(a, e.onTick(map(spm(true, 5.0)), a, 31 * min))
    }

    @Test
    fun `kanal kapaliyken guc sifir olsa da tetiklenmez`() {
        val e = AutomationEngine()
        e.onStates(map(spm(true, 100.0)), map(spm(false, 0.0)), lowPower, 0)
        assertTrue(e.onTick(map(spm(false, 0.0)), lowPower, 60 * min).isEmpty())
        assertEquals(null, e.nextDueAt(map(spm(false, 0.0)), lowPower))
    }

    @Test
    fun `kisa dusus sure dolmadan toparlanirsa tetiklenmez`() {
        val e = AutomationEngine()
        e.onStates(map(spm(false, 0.0)), map(spm(true, 100.0)), lowPower, 0)
        e.onStates(map(spm(true, 100.0)), map(spm(true, 5.0)), lowPower, 10 * min)
        e.onStates(map(spm(true, 5.0)), map(spm(true, 100.0)), lowPower, 10 * min + 40_000)
        assertTrue(e.onTick(map(spm(true, 100.0)), lowPower, 12 * min).isEmpty())
    }

    @Test
    fun `guc verisi yoksa tetiklenmez`() {
        val e = AutomationEngine()
        e.onStates(map(spm(false, null)), map(spm(true, null)), lowPower, 0)
        assertTrue(e.onTick(map(spm(true, null)), lowPower, 30 * min).isEmpty())
    }

    @Test
    fun `kanal guc tetikleyicisi json gidis donus`() {
        assertEquals(lowPower, Automation.listFromJson(Automation.listToJson(lowPower)))
    }

    @Test
    fun `kanal acilinca tetiklenir, digeri etkilemez`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.TURNED_ON, channel = 1))
        assertEquals(emptyList<Automation>(), e.onStates(map(dev(false, false)), map(dev(true, false)), a, 0))
        assertEquals(a, e.onStates(map(dev(true, false)), map(dev(true, true)), a, 20_000))
    }

    @Test
    fun `ilk durum tetiklemez`() {
        val e = AutomationEngine()
        assertTrue(e.onStates(emptyMap(), map(dev(true)), listOf(auto(TriggerKind.TURNED_ON)), 0).isEmpty())
    }

    @Test
    fun `pasif otomasyon calismaz`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.TURNED_ON).copy(enabled = false))
        assertTrue(e.onStates(map(dev(false)), map(dev(true)), a, 0).isEmpty())
    }

    @Test
    fun `sure dolunca bir kez tetiklenir, durum degisince yeniden kurulur`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.STAYED_ON, minutes = 30))
        e.onStates(map(dev(false)), map(dev(true)), a, 0)
        assertEquals(30 * min, e.nextDueAt(map(dev(true)), a))
        assertTrue(e.onTick(map(dev(true)), a, 29 * min).isEmpty())
        assertEquals(a, e.onTick(map(dev(true)), a, 30 * min))
        assertTrue(e.onTick(map(dev(true)), a, 45 * min).isEmpty())

        e.onStates(map(dev(true)), map(dev(false)), a, 50 * min)
        e.onStates(map(dev(false)), map(dev(true)), a, 60 * min)
        assertEquals(a, e.onTick(map(dev(true)), a, 90 * min))
    }

    @Test
    fun `sure dolmadan kapanirsa tetiklenmez`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.STAYED_ON, minutes = 30))
        e.onStates(map(dev(false)), map(dev(true)), a, 0)
        e.onStates(map(dev(true)), map(dev(false)), a, 10 * min)
        assertTrue(e.onTick(map(dev(false)), a, 40 * min).isEmpty())
    }

    @Test
    fun `guc esigi histerezisle bir kez tetiklenir`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.POWER_ABOVE, watts = 1000.0))
        val t = { p: Double, time: Long -> e.onStates(map(dev(true, power = 0.0)), map(dev(true, power = p)), a, time) }
        assertTrue(t(500.0, 0).isEmpty())
        assertEquals(a, t(1200.0, 20_000))
        assertTrue(t(1300.0, 40_000).isEmpty())
        assertTrue(t(950.0, 60_000).isEmpty()) // eşiğin %10 gerisine inmedi, kurulmadı
        assertTrue(t(1100.0, 80_000).isEmpty())
        assertTrue(t(800.0, 100_000).isEmpty()) // yeniden kuruldu
        assertEquals(a, t(1100.0, 120_000))
    }

    @Test
    fun `guc altina inme`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.POWER_BELOW, watts = 5.0))
        assertTrue(e.onStates(emptyMap(), map(dev(true, power = 100.0)), a, 0).isEmpty())
        assertEquals(a, e.onStates(emptyMap(), map(dev(true, power = 2.0)), a, 20_000))
    }

    @Test
    fun `cevrimdisi olma`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.WENT_OFFLINE))
        assertEquals(a, e.onStates(map(dev(true)), map(dev(true, online = false)), a, 0))
    }

    @Test
    fun `gecikmeli eylem vakti gelince doner`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.TURNED_ON, delay = 5))
        assertTrue(e.onStates(map(dev(false)), map(dev(true)), a, 0).isEmpty())
        assertEquals(5 * min, e.nextDueAt(map(dev(true)), a))
        assertTrue(e.onTick(map(dev(true)), a, 4 * min).isEmpty())
        assertEquals(a, e.onTick(map(dev(true)), a, 5 * min))
        assertTrue(e.onTick(map(dev(true)), a, 6 * min).isEmpty())
    }

    @Test
    fun `hizli tekrar dongu korumasiyla engellenir`() {
        val e = AutomationEngine()
        val a = listOf(auto(TriggerKind.TURNED_ON))
        assertEquals(a, e.onStates(map(dev(false)), map(dev(true)), a, 0))
        assertTrue(e.onStates(map(dev(false)), map(dev(true)), a, 5_000).isEmpty())
    }

    @Test
    fun `json gidis donus, bozuk kayit atlanir`() {
        val list = listOf(auto(TriggerKind.POWER_ABOVE, watts = 1500.5, delay = 2), auto(TriggerKind.STAYED_OFF, 3, 45).copy(id = "b"))
        assertEquals(list, Automation.listFromJson(Automation.listToJson(list)))
        val broken = Automation.listToJson(list).replace("STAYED_OFF", "YOK")
        assertEquals(list.take(1), Automation.listFromJson(broken))
    }

    /** Tek kanallı güç ölçerli priz (Tuya cz, eWeLink POW): kanal gücü = cihazın toplam gücü. */
    private fun plug(on: Boolean, watts: Double) =
        DeviceSnapshot("p", "priz", true, mapOf(0 to on), power = watts, cloud = Cloud.TUYA, switchCodes = mapOf(0 to "switch_1"))

    @Test
    fun `tek kanalli prizde guc dususu kanal gucu gibi calisir`() {
        assertTrue(plug(true, 24.0).hasChannelPower)
        assertEquals(24.0, plug(true, 24.0).channelPowerOf(0)!!, 1e-9)
        val a = listOf(
            Automation("q", "pompa", true, Trigger(TriggerKind.CHANNEL_POWER_LOW, "p", channel = 0, minutes = 1, watts = 10.0, graceMinutes = 1),
                AutoAction(ActionKind.ALARM)),
        )
        val e = AutomationEngine()
        val pm = { d: DeviceSnapshot -> mapOf(d.id to d) }
        e.onStates(pm(plug(false, 0.0)), pm(plug(true, 24.0)), a, 0)
        assertTrue(e.onTick(pm(plug(true, 24.0)), a, 2 * min).isEmpty())
        e.onStates(pm(plug(true, 24.0)), pm(plug(true, 2.0)), a, 3 * min)
        assertTrue(e.onTick(pm(plug(true, 2.0)), a, 3 * min + 30_000).isEmpty())
        assertEquals(a, e.onTick(pm(plug(true, 2.0)), a, 4 * min))
        // Priz kapalıyken (bekleme dönemi) güç 0 olsa da tetiklenmez.
        val e2 = AutomationEngine()
        e2.onStates(pm(plug(true, 24.0)), pm(plug(false, 0.0)), a, 0)
        assertTrue(e2.onTick(pm(plug(false, 0.0)), a, 10 * min).isEmpty())
    }

    @Test
    fun `cok kanalli cihazda toplam guc kanala atanmaz`() {
        val d = DeviceSnapshot("m", "4ch", true, mapOf(0 to true, 1 to true), power = 50.0)
        assertEquals(null, d.channelPowerOf(0))
        assertEquals(false, d.hasChannelPower)
    }
}
