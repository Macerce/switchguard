package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmVolumeTest {
    @Test fun `yukseltme aciksa en yuksege cikarir`() = assertEquals(7, AlarmVolume.target(current = 2, max = 7, boost = true))
    @Test fun `zaten en yuksekse dokunmaz`() = assertNull(AlarmVolume.target(current = 7, max = 7, boost = true))
    @Test fun `yukseltme kapaliysa dokunmaz`() = assertNull(AlarmVolume.target(current = 0, max = 7, boost = false))
    @Test fun `gecersiz azami deger dokunmaz`() = assertNull(AlarmVolume.target(current = 0, max = 0, boost = true))
}
