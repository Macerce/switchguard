package com.macerce.switchguard.desktop.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    /** 16 bit mono 44.1 kHz WAV; başlık [declaredDataBytes] veri bildirir, dosyada [data] kadar bayt vardır. */
    private fun wavBytes(declaredDataBytes: Int, data: ByteArray): ByteArray {
        val b = ByteBuffer.allocate(44 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + declaredDataBytes).put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(44_100).putInt(88_200)
        b.putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(declaredDataBytes).put(data)
        return b.array()
    }

    @Test
    fun emptyWavFallsBackToDefault() {
        val empty = tmp.root.toPath().resolve("bos.wav")
        Files.write(empty, wavBytes(0, ByteArray(0))) // 44 bayt: yalnızca başlık, 0 kare
        assertEquals(44L, Files.size(empty))
        assertEquals(siren, Sounds.resolve(empty.toString(), Sounds.DEFAULT_ALARM))
        assertEquals(chime, Sounds.resolve(empty.toString(), Sounds.DEFAULT_NOTIFY))
    }

    @Test
    fun truncatedWavFallsBackToDefault() {
        // başlık 1 sn (88 200 bayt) bildiriyor ama dosyada yalnızca 10 kare var
        val cut = tmp.root.toPath().resolve("kesik.wav")
        Files.write(cut, wavBytes(88_200, ByteArray(20)))
        assertEquals(siren, Sounds.resolve(cut.toString(), Sounds.DEFAULT_ALARM))
    }

    @Test
    fun truncatedButLongEnoughWavIsStillUsed() {
        // başlık 1 sn bildiriyor, dosyada 0,5 sn var: oynatılabilir, reddedilmez
        val half = tmp.root.toPath().resolve("yarim.wav")
        Files.write(half, wavBytes(88_200, ByteArray(44_100)))
        assertEquals(SoundSource.File(half), Sounds.resolve(half.toString(), Sounds.DEFAULT_ALARM))
    }

    @Test
    fun tooShortRuleIsFiftyMilliseconds() {
        assertTrue(Sounds.isTooShort(0, 44_100f))
        assertTrue(Sounds.isTooShort(10, 44_100f))
        assertTrue(Sounds.isTooShort(2_204, 44_100f)) // 49,98 ms
        assertTrue(!Sounds.isTooShort(2_205, 44_100f)) // tam 50 ms
        assertTrue(!Sounds.isTooShort(26_460, 44_100f)) // 0,6 sn'lik bildirim sesi
        assertTrue(Sounds.isTooShort(2_399, 48_000f))
        assertTrue(!Sounds.isTooShort(2_400, 48_000f))
        // bilinmeyen (-1) uzunluk ya da örnekleme hızı: yine de 0 kare bozuk sayılır, 1+ kare değil
        assertTrue(Sounds.isTooShort(0, -1f))
        assertTrue(Sounds.isTooShort(-1, 44_100f))
        assertTrue(!Sounds.isTooShort(1, -1f))
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
    fun nothingPlayingIsNotLooping() {
        val player = SoundPlayer()
        assertFalse(player.isLooping())
        player.stop()
        assertFalse(player.isLooping())
    }

    @Test
    fun loopProgressDetectsAStalledClip() {
        val second = 1_000_000_000L
        val progress = SoundPlayer.LoopProgress(minIntervalNs = second)

        assertTrue("ilk ölçüm: yeni başladı", progress.advanced(position = 0, now = 0))
        assertTrue("aralık dolmadan karar verilmez", progress.advanced(position = 0, now = second / 2))
        assertTrue(progress.advanced(position = 44_100, now = second))
        assertFalse("konum durdu: cihaz kayboldu", progress.advanced(position = 44_100, now = 3 * second))
        assertTrue(progress.advanced(position = 50_000, now = 5 * second))
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

    @Test
    fun userIdResolvesInsideUserDir() {
        val userDir = tmp.newFolder("sounds").toPath()
        val wav = userDir.resolve("0123456789ab-zil.wav")
        javax.sound.sampled.AudioSystem.write(
            javax.sound.sampled.AudioInputStream(
                java.io.ByteArrayInputStream(ByteArray(8_000 * 2)),
                javax.sound.sampled.AudioFormat(8_000f, 16, 1, true, false),
                8_000,
            ),
            javax.sound.sampled.AudioFileFormat.Type.WAVE,
            wav.toFile(),
        )

        assertEquals(SoundSource.File(wav), Sounds.resolve("user:0123456789ab-zil.wav", Sounds.DEFAULT_ALARM, userDir))
        assertEquals(siren, Sounds.resolve("user:../0123456789ab-zil.wav", Sounds.DEFAULT_ALARM, userDir))
        assertEquals(siren, Sounds.resolve("user:sounds\\0123456789ab-zil.wav", Sounds.DEFAULT_ALARM, tmp.root.toPath()))
        assertEquals(siren, Sounds.resolve("user:0123456789ab-zil.wav", Sounds.DEFAULT_ALARM, null))
    }
}
