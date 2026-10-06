package com.macerce.switchguard.desktop.sound

import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Sounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.sin

class SoundImportTest {
    @get:Rule val tmp = TemporaryFolder()

    private val userDir: Path by lazy { tmp.root.toPath().resolve("sounds") }
    private val source: Path by lazy { tmp.newFolder("Masaüstü").toPath() }
    private val import by lazy { SoundImport(userDir) }

    /** [seconds] uzunluğunda, gerçekten ses taşıyan (sinüs) PCM WAV. */
    private fun wav(name: String, seconds: Double, rate: Float = 44_100f, bits: Int = 16, channels: Int = 1): Path {
        val format = AudioFormat(rate, bits, channels, true, false)
        val frames = (seconds * rate).toLong()
        val bytesPerSample = bits / 8
        val data = ByteArray((frames * format.frameSize).toInt())
        for (f in 0 until frames) {
            val v = (sin(2 * Math.PI * 440 * f / rate) * 0.5 * ((1L shl (bits - 1)) - 1)).toLong()
            for (c in 0 until channels) for (b in 0 until bytesPerSample) {
                data[((f * channels + c) * bytesPerSample + b).toInt()] = (v shr (8 * b)).toByte()
            }
        }
        val path = source.resolve(name)
        AudioSystem.write(AudioInputStream(ByteArrayInputStream(data), format, frames), AudioFileFormat.Type.WAVE, path.toFile())
        return path
    }

    private fun fileOf(id: String): Path = userDir.resolve(id.removePrefix(Sounds.USER_PREFIX))

    private fun ok(result: ImportResult): ImportResult.Ok {
        assertTrue(result.toString(), result is ImportResult.Ok)
        return result as ImportResult.Ok
    }

    @Test
    fun importsAWavAs16BitPcm() {
        val r = ok(import.import(wav("siren.wav", 2.0)))

        assertTrue(r.id, r.id.startsWith("user:"))
        val format = AudioSystem.getAudioFileFormat(fileOf(r.id).toFile())
        assertEquals(16, format.format.sampleSizeInBits)
        assertEquals(AudioFileFormat.Type.WAVE, format.type)
        assertEquals(false, r.trimmed)
    }

    @Test
    fun convertsHighResWav() {
        val r = ok(import.import(wav("studio.wav", 1.0, rate = 96_000f, bits = 24, channels = 2)))

        val f = AudioSystem.getAudioFileFormat(fileOf(r.id).toFile()).format
        assertEquals(16, f.sampleSizeInBits)
        assertTrue("rate ${f.sampleRate}", f.sampleRate <= 48_000f)
        assertEquals(2, f.channels)
    }

    @Test
    fun importsMp3() {
        val mp3 = source.resolve("beep.mp3")
        Files.copy(checkNotNull(javaClass.getResourceAsStream("/sounds/beep-1s.mp3")), mp3)

        val r = ok(import.import(mp3))

        val f = AudioSystem.getAudioFileFormat(fileOf(r.id).toFile())
        val seconds = f.frameLength / f.format.sampleRate
        assertTrue("seconds $seconds", seconds in 0.9f..1.2f)
        assertEquals(16, f.format.sampleSizeInBits)
    }

    @Test
    fun trimsTo60Seconds() {
        val r = ok(import.import(wav("long.wav", 61.0, rate = 8_000f)))

        assertEquals(true, r.trimmed)
        assertEquals(60L * 8_000, AudioSystem.getAudioFileFormat(fileOf(r.id).toFile()).frameLength.toLong())
    }

    @Test
    fun rejectsNonAudio() {
        val fake = source.resolve("not-a-song.mp3")
        Files.writeString(fake, "bu bir ses değil")

        val r = import.import(fake)

        assertTrue(r.toString(), r is ImportResult.Failed && r.reason == Reason.NOT_AUDIO)
        assertTrue(!Files.exists(userDir) || Files.list(userDir).use { it.count() } == 0L)
    }

    @Test
    fun rejectsOver20Mb() {
        val big = source.resolve("big.wav")
        RandomAccessFile(big.toFile(), "rw").use { it.setLength(20L * 1024 * 1024 + 1) }

        val r = import.import(big)

        assertTrue(r.toString(), r is ImportResult.Failed && r.reason == Reason.TOO_BIG)
    }

    @Test
    fun sameSoundTwiceIsOneFile() {
        val w = wav("same.wav", 1.0)

        val a = ok(import.import(w))
        val b = ok(import.import(w))

        assertEquals(a.id, b.id)
        assertEquals(1L, Files.list(userDir).use { it.count() })
        assertEquals(listOf(a.id), import.list())
    }

    @Test
    fun turkishFileNameKeepsItsName() {
        val r = ok(import.import(wav("alarm sesi ğş.wav", 1.0)))

        assertTrue(r.id, r.id.endsWith("-alarm sesi ğş.wav"))
        assertEquals("alarm sesi ğş", Sounds.displayName(r.id, Strings.load("tr")))
    }
}
