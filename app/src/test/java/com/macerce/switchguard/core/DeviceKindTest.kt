package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceKindTest {
    private fun d(name: String, channels: Int = 1, power: Double? = null, cloud: Cloud = Cloud.EWELINK) =
        DeviceSnapshot(name, name, true, (0 until channels).associateWith { false }, power = power, cloud = cloud)

    @Test fun `addan tahmin, Turkce ekler ve harfler`() {
        assertEquals(DeviceKind.LIGHT, DeviceKinds.of(d("Teknik Önü Aydınlatma")))
        assertEquals(DeviceKind.LIGHT, DeviceKinds.of(d("Teknik Onu Aydinlatma")))
        assertEquals(DeviceKind.LIGHT, DeviceKinds.of(d("Demo · Bahçe ışığı")))
        assertEquals(DeviceKind.WATER, DeviceKinds.of(d("Rögar Su Seviye Bildirimi", channels = 4)))
        assertEquals(DeviceKind.WATER, DeviceKinds.of(d("Demo · Pompa paneli", channels = 4)))
        assertEquals(DeviceKind.WATER, DeviceKinds.of(d("Ana su deposu su seviyesi")))
        assertEquals(DeviceKind.COLD, DeviceKinds.of(d("Demo · Dondurucu")))
        assertEquals(DeviceKind.PLUG, DeviceKinds.of(d("akıllı priz")))
        assertEquals(DeviceKind.AIR, DeviceKinds.of(d("Lobi klimaları")))
        assertEquals(DeviceKind.HEAT, DeviceKinds.of(d("KOMBİ")))
        assertEquals(DeviceKind.DOOR, DeviceKinds.of(d("Garage door")))
    }

    @Test fun `kisa kelime yalniz tam eslesir`() {
        assertEquals(DeviceKind.SWITCH, DeviceKinds.of(d("Sunucu odası")))
        assertEquals(DeviceKind.WATER, DeviceKinds.of(d("Su vanası")))
    }

    @Test fun `ad bir sey soylemezse ozelliklerden`() {
        assertEquals(DeviceKind.RELAYS, DeviceKinds.of(d("Pano 3", channels = 4)))
        assertEquals(DeviceKind.PLUG, DeviceKinds.of(d("POW", power = 12.0)))
        assertEquals(DeviceKind.PLUG, DeviceKinds.of(d("Cihaz", cloud = Cloud.TUYA)))
        assertEquals(DeviceKind.SWITCH, DeviceKinds.of(d("Röle 1")))
    }
}
