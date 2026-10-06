package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.data.PlainCodec
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopHostTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Ses cihazının yerine: [loopResults] sıradaki `loop` sonuçları (boşsa başarılı), [looping] o anki durum. */
    private class FakeSound : AlarmSound {
        val loopResults = ArrayDeque<Boolean>()
        var looping = false
        val loops = mutableListOf<Pair<String?, Float>>()
        var stops = 0
        val onces = mutableListOf<String?>()

        override fun loop(id: String?, volume: Float): Boolean {
            loops += id to volume
            looping = loopResults.removeFirstOrNull() ?: true
            return looping
        }

        override fun isLooping() = looping

        override fun once(id: String?, volume: Float, maxMs: Long) {
            onces += id
        }

        override fun stop() {
            stops++
            looping = false
        }
    }

    private class FakeDisplay : DisplayWaker {
        var wakes = 0
        var keepOns = 0
        override fun wake() {
            wakes++
        }

        override fun keepOn() {
            keepOns++
        }
    }

    private val sound = FakeSound()
    private val display = FakeDisplay()
    private val state = AppState()
    private lateinit var store: Store
    private lateinit var host: DesktopHost

    @Before
    fun setUp() {
        store = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)
        val strings = Strings.load("en")
        host = DesktopHost(store, sound, state, NotificationQueue(), { strings }, display)
    }

    /** Motorun yaptığı gibi: satır kalıcı listeye eklenir, sonra pencere/ses istenir. */
    private fun alarm(vararg lines: String, sound: String? = null) {
        store.pendingAlarm = lines.toList()
        host.showAlarm(lines.toList(), sound)
    }

    @Test
    fun failedLoopIsRetriedOnTheNextTick() {
        sound.loopResults += false // o an ses cihazı yok
        alarm("12:00  Pump: ON → OFF")
        assertEquals(1, sound.loops.size)
        assertFalse(sound.isLooping())

        host.ensureAlarmVisible() // sonraki tick: cihaz geri geldi

        assertEquals(2, sound.loops.size)
        assertTrue(sound.isLooping())
        host.ensureAlarmVisible()
        assertEquals("çalan ses yeniden başlatılmaz", 2, sound.loops.size)
    }

    @Test
    fun sameSoundAlreadyLoopingIsNotRestarted() {
        alarm("12:00  a")
        alarm("12:00  a", "12:01  b")
        host.ensureAlarmVisible()

        assertEquals(1, sound.loops.size)
        assertEquals(listOf("12:00  a", "12:01  b"), state.alarmLines.value)
    }

    @Test
    fun stoppedLoopIsRestartedWithTheAlarmsOwnSound() {
        alarm("12:00  a", sound = "builtin:alarm_beep")
        sound.looping = false // ses cihazı kayboldu (kulaklık çıkarıldı, monitör hoparlörü uyudu)

        host.ensureAlarmVisible()

        assertEquals(listOf("builtin:alarm_beep", "builtin:alarm_beep"), sound.loops.map { it.first })
        assertTrue(sound.isLooping())
    }

    @Test
    fun differentSoundReplacesTheLoop() {
        alarm("12:00  a")
        alarm("12:00  a", "12:01  b", sound = "builtin:alarm_beep")

        assertEquals(2, sound.loops.size)
        assertEquals("builtin:alarm_beep", sound.loops.last().first)
    }

    @Test
    fun dismissStopsTheSoundAndTheNextAlarmRingsAgain() {
        alarm("12:00  a")

        store.pendingAlarm = emptyList()
        host.stopAlarm()

        assertEquals(1, sound.stops)
        assertTrue(state.alarmLines.value.isEmpty())
        alarm("12:05  b")
        assertEquals(2, sound.loops.size)
    }

    @Test
    fun alarmWakesTheDisplayAndEveryTickKeepsItOn() {
        alarm("12:00  a")
        assertEquals(1, display.wakes)

        host.ensureAlarmVisible()
        host.ensureAlarmVisible()
        assertEquals(2, display.keepOns)
        assertEquals(1, display.wakes)

        alarm("12:00  a", "12:01  b")
        assertEquals(2, display.wakes)
    }
}
