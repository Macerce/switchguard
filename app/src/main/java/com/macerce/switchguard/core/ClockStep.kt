package com.macerce.switchguard.core

/**
 * Gün içindeki dakikayı (0..1439) kumandayla adım adım değiştirme: saat ve dakika ayrı ayrı
 * başa döner, biri değişirken öteki korunur (23 → 0 olunca dakika aynı kalır).
 */
object ClockStep {
    private const val DAY = 24 * 60

    fun hour(minuteOfDay: Int, delta: Int): Int {
        val m = minuteOfDay.mod(DAY)
        return ((m / 60 + delta).mod(24)) * 60 + m % 60
    }

    fun minute(minuteOfDay: Int, delta: Int): Int {
        val m = minuteOfDay.mod(DAY)
        return (m / 60) * 60 + (m % 60 + delta).mod(60)
    }

    /** 12 saat düzeninde gösterilecek saat: 0 ve 12 → 12. */
    fun hour12(hour: Int): Int = (hour + 11) % 12 + 1
}
