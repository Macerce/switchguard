package com.macerce.switchguard.core

/**
 * Kullanıcının uygulamadan yaptığı aç/kapa işlemlerini hatırlar ki
 * aynı değişiklik birkaç saniye sonra eWeLink'ten geri geldiğinde alarm çalmasın.
 */
class ExpectedChanges {
    private data class Key(val deviceId: String, val channel: Int, val to: Boolean)

    private val expected = mutableMapOf<Key, Long>()

    @Synchronized
    fun expect(deviceId: String, channel: Int, to: Boolean, now: Long) {
        expected[Key(deviceId, channel, to)] = now + WINDOW_MS
    }

    /** Değişiklik beklenen bir kullanıcı işlemiyse true döner ve beklentiyi tüketir. */
    @Synchronized
    fun consume(change: Change, now: Long): Boolean {
        if (change !is Change.Switch) return false
        expected.values.removeAll { it < now }
        return expected.remove(Key(change.deviceId, change.channel, change.to)) != null
    }

    companion object {
        const val WINDOW_MS = 30_000L
    }
}
