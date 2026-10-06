package com.macerce.switchguard.desktop.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class SoundsTest {
    @get:Rule val tmp = TemporaryFolder()

    private val siren = SoundSource.Builtin("alarm_siren")
    private val chime = SoundSource.Builtin("notify_chime")

    @Test
    fun nullUsesDefault() {
        assertEquals(siren, Sounds.resolve(null, Sounds.DEFAULT_ALARM))
        assertEquals(siren, Sounds.resolve("", Sounds.DEFAULT_ALARM))
        assertEquals(siren, Sounds.resolve("   ", Sounds.DEFAULT_ALARM))
        assertEquals(chime, Sounds.resolve(null, Sounds.DEFAULT_NOTIFY))
    }

    @Test
    fun knownBuiltinIsResolved() {
        for (name in Sounds.BUILTIN) {
            assertEquals(SoundSource.Builtin(name), Sounds.resolve("builtin:$name", Sounds.DEFAULT_ALARM))
        }
    }

    @Test
    fun unknownBuiltinUsesDefault() {
        assertEquals(siren, Sounds.resolve("builtin:nope", Sounds.DEFAULT_ALARM))
        assertEquals(siren, Sounds.resolve("builtin:", Sounds.DEFAULT_ALARM))
        assertEquals(siren, Sounds.resolve("builtin:../alarm_siren", Sounds.DEFAULT_ALARM))
        assertEquals(chime, Sounds.resolve("builtin:nope", Sounds.DEFAULT_NOTIFY))
    }

    @Test
    fun missingCustomFileFallsBackToDefault() {
        val missing = tmp.root.toPath().resolve("yok.wav")
        assertEquals(siren, Sounds.resolve(missing.toString(), Sounds.DEFAULT_ALARM))
        assertEquals(chime, Sounds.resolve(missing.toString(), Sounds.DEFAULT_NOTIFY))
    }

    @Test
    fun nonAudioFileFallsBackToDefault() {
        val fake = tmp.root.toPath().resolve("sahte.wav")
        Files.write(fake, "bu bir ses dosyasi degil".toByteArray())
        assertEquals(siren, Sounds.resolve(fake.toString(), Sounds.DEFAULT_ALARM))
    }

    @Test
    fun directoryFallsBackToDefault() {
        assertEquals(siren, Sounds.resolve(tmp.root.toString(), Sounds.DEFAULT_ALARM))
    }

    @Test
    fun phoneSoundIdsFallBackToDefault() {
        assertEquals(siren, Sounds.resolve("content://settings/system/alarm_alert", Sounds.DEFAULT_ALARM))
        assertEquals(chime, Sounds.resolve("content://media/internal/audio/media/42", Sounds.DEFAULT_NOTIFY))
    }

    @Test
    fun existingWavFileIsUsed() {
        val wav: Path = tmp.root.toPath().resolve("benim.wav")
        Sounds.openResource("alarm_beep")!!.use { Files.copy(it, wav) }
        assertEquals(SoundSource.File(wav), Sounds.resolve(wav.toString(), Sounds.DEFAULT_ALARM))
    }

    @Test
    fun everyBuiltinResourceIsReadable() {
        assertEquals(listOf("alarm_siren", "alarm_beep", "alarm_pulse", "notify_chime"), Sounds.BUILTIN)
        for (name in Sounds.BUILTIN) {
            val stream = Sounds.openResource(name)
            assertNotNull("kaynak yok: $name", stream)
            AudioSystem.getAudioInputStream(stream!!).use { audio ->
                val f = audio.format
                assertEquals(name, AudioFormat.Encoding.PCM_SIGNED, f.encoding)
                assertEquals(name, 16, f.sampleSizeInBits)
                assertEquals(name, 1, f.channels)
                assertEquals(name, 44_100f, f.sampleRate, 0f)
                assertTrue("$name bos", audio.frameLength > 0)
            }
        }
    }

    @Test
    fun defaultsAreKnownBuiltins() {
        assertTrue(Sounds.DEFAULT_ALARM.removePrefix("builtin:") in Sounds.BUILTIN)
        assertTrue(Sounds.DEFAULT_NOTIFY.removePrefix("builtin:") in Sounds.BUILTIN)
    }

    @Test
    fun stopWithNothingPlayingIsHarmless() {
        val player = SoundPlayer()
        player.stop()
        player.stop()
    }

    @Test
    fun gainFollowsVolumeAndClampsToControlRange() {
        assertEquals(0f, SoundPlayer.gainDb(1f, -80f, 6f), 1e-4f)
        assertEquals(-6.0206f, SoundPlayer.gainDb(0.5f, -80f, 6f), 1e-3f)
        assertEquals(-20f, SoundPlayer.gainDb(0.1f, -80f, 6f), 1e-3f)
        assertEquals(-80f, SoundPlayer.gainDb(0f, -80f, 6f), 0f)
        assertEquals(-80f, SoundPlayer.gainDb(-1f, -80f, 6f), 0f)
        assertEquals(-30f, SoundPlayer.gainDb(0.0001f, -30f, 6f), 0f)
        // tam sesin ustu ya da gecersiz deger asla susturmaz / yukseltmez
        assertEquals(0f, SoundPlayer.gainDb(3f, -80f, 6f), 1e-4f)
        assertEquals(0f, SoundPlayer.gainDb(Float.NaN, -80f, 6f), 1e-4f)
    }
}
