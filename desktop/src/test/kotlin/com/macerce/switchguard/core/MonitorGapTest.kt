package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorGapTest {
    private val min = 60_000L

    @Test
    fun `hic calismamissa kesinti yok`() {
        assertNull(MonitorGap.detect(lastAliveAt = 0, cleanStop = false, now = 100 * min))
    }

    @Test
    fun `kullanici kapattiysa kesinti sayilmaz`() {
        assertNull(MonitorGap.detect(lastAliveAt = 10 * min, cleanStop = true, now = 100 * min))
    }

    @Test
    fun `kisa yeniden baslatma kesinti sayilmaz`() {
        assertNull(MonitorGap.detect(lastAliveAt = 10 * min, cleanStop = false, now = 14 * min))
    }

    @Test
    fun `beklenmedik kapanma ve uzun ara kesinti araligi verir`() {
        assertEquals(10 * min to 45 * min, MonitorGap.detect(lastAliveAt = 10 * min, cleanStop = false, now = 45 * min))
    }

    @Test
    fun `saat geri alinmissa kesinti uydurulmaz`() {
        assertNull(MonitorGap.detect(lastAliveAt = 50 * min, cleanStop = false, now = 45 * min))
    }
}
