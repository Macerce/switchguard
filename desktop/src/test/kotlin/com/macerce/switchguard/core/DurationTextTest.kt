package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTextTest {
    private val s = DurationStrings(
        seconds = { "$it sn" },
        minSec = { m, sec -> "$m dk $sec sn" },
        minutes = { "$it dk" },
        hourMin = { h, m -> "$h sa $m dk" },
        dayHour = { d, h -> "$d g $h sa" },
    )

    @Test fun `bir dakikanin alti saniye`() = assertEquals("9 sn", DurationText.format(9_400, s))
    @Test fun `on dakikanin alti dakika ve saniye`() = assertEquals("2 dk 10 sn", DurationText.format(130_000, s))
    @Test fun `tam dakika saniyesiz`() = assertEquals("3 dk", DurationText.format(180_000, s))
    @Test fun `on dakikadan sonra yalniz dakika`() = assertEquals("35 dk", DurationText.format(35 * 60_000L + 20_000, s))
    @Test fun `saat ve dakika`() = assertEquals("2 sa 10 dk", DurationText.format(130 * 60_000L, s))
    @Test fun `gun ve saat`() = assertEquals("1 g 3 sa", DurationText.format(27 * 3_600_000L, s))
    @Test fun `negatif sure sifir saniye`() = assertEquals("0 sn", DurationText.format(-5, s))
}
