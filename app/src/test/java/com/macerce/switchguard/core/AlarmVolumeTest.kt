package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmVolumeTest {
    @Test fun `yukseltme aciksa en yuksege cikarir`() = assertEquals(7, AlarmVolume.target(current = 2, max = 7, boost = true))
    @Test fun `zaten en yuksekse dokunmaz`() = assertNull(AlarmVolume.target(current = 7, max = 7, boost = true))
    @Test fun `yukseltme kapaliysa dokunmaz`() = assertNull(AlarmVolume.target(current = 0, max = 7, boost = false))
    @Test fun `gecersiz azami deger dokunmaz`() = assertNull(AlarmVolume.target(current = 0, max = 0, boost = true))

    // Alarm başlarken eski seviye her zaman saklanır: çalarken kullanıcı sesi kısarsa, kapatınca geri döner
    // (yoksa telefonun saat alarmı da kısık kalır).
    @Test fun `zaten en yuksekse de eski seviye saklanir`() =
        assertEquals(AlarmVolume.Start(remember = 7, set = null), AlarmVolume.start(current = 7, max = 7, boost = true, saved = false))

    @Test fun `kisiksa saklanir ve en yuksege cikarilir`() =
        assertEquals(AlarmVolume.Start(remember = 2, set = 7), AlarmVolume.start(current = 2, max = 7, boost = true, saved = false))

    @Test fun `alarm surerken yeni olayda saklanan seviyenin ustune yazilmaz ama ses yine yukseltilir`() =
        assertEquals(AlarmVolume.Start(remember = null, set = 7), AlarmVolume.start(current = 1, max = 7, boost = true, saved = true))

    @Test fun `yukseltme kapaliysa sese hic dokunulmaz`() =
        assertEquals(AlarmVolume.Start(remember = null, set = null), AlarmVolume.start(current = 0, max = 7, boost = false, saved = false))
}
