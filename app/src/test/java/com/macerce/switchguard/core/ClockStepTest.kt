package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockStepTest {
    @Test fun `saat bir ileri dakika korunur`() = assertEquals(8 * 60 + 15, ClockStep.hour(7 * 60 + 15, +1))
    @Test fun `saat 23ten 0a doner`() = assertEquals(0 * 60 + 30, ClockStep.hour(23 * 60 + 30, +1))
    @Test fun `saat 0dan 23e doner`() = assertEquals(23 * 60 + 5, ClockStep.hour(5, -1))
    @Test fun `dakika bir ileri saat korunur`() = assertEquals(7 * 60 + 16, ClockStep.minute(7 * 60 + 15, +1))
    @Test fun `dakika 59dan 0a doner saat degismez`() = assertEquals(7 * 60, ClockStep.minute(7 * 60 + 59, +1))
    @Test fun `dakika 0dan 59a doner saat degismez`() = assertEquals(7 * 60 + 59, ClockStep.minute(7 * 60, -1))
    @Test fun `aralik disi deger gun icine alinir`() = assertEquals(1 * 60 + 10, ClockStep.hour(24 * 60 + 10, +1))
    @Test fun `on iki saat duzeni gece yarisi 12`() = assertEquals(12, ClockStep.hour12(0))
    @Test fun `on iki saat duzeni ogle 12`() = assertEquals(12, ClockStep.hour12(12))
    @Test fun `on iki saat duzeni aksam 11`() = assertEquals(11, ClockStep.hour12(23))
}
