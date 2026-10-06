package com.macerce.switchguard.desktop.sound

import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.platform.Sounds
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.min

sealed interface ImportResult {
    /** [id] `user:<dosya>`; [trimmed] ses [SoundImport.MAX_SECONDS]'tan uzundu, başı alındı. */
    data class Ok(val id: String, val trimmed: Boolean) : ImportResult

    /** [detail] yalnızca günlük ve hata metni için (dosya adı, istisna türü); içerik içermez. */
    data class Failed(val reason: Reason, val detail: String) : ImportResult
}

enum class Reason { TOO_BIG, NOT_AUDIO, IO }

/**
 * Kullanıcının seçtiği ses dosyasını (WAV ya da MP3) uygulamanın kendi klasörüne ([userDir],
 * `%APPDATA%\SwitchGuard\sounds`) **16 bit PCM WAV** olarak kopyalar. Böylece:
 * - çalma yolu (`SoundPlayer`, güvenlik açısından kritik) yalnızca WAV görür, değişmez;
 * - asıl dosya silinse/taşınsa da ses çalar, yedeğe gömülebilir;
 * - aynı içerik iki kez yüklenirse tek dosya olur (ad içerik özetinden).
 *
 * Dönüşüm: MP3 `mp3spi` ile (ServiceLoader üzerinden `AudioSystem`'e kayıtlı), örnekleme en çok [MAX_RATE] Hz, kanal
 * sayısı korunur (1–2), en çok [MAX_SECONDS] sn. Kaynak [MAX_SOURCE_BYTES]'tan büyükse hiç okunmaz.
 */
class SoundImport(private val userDir: Path) {

    fun import(source: Path): ImportResult {
        val name = source.fileName?.toString() ?: "?"
        val size = try {
            Files.size(source)
        } catch (e: IOException) {
            return failed(Reason.IO, name, e)
        }
        if (size > MAX_SOURCE_BYTES) return ImportResult.Failed(Reason.TOO_BIG, "$name: $size bytes")

        val decoded = try {
            decode(source)
        } catch (e: IOException) {
            return failed(Reason.IO, name, e)
        } catch (e: Exception) {
            // UnsupportedAudioFileException, IllegalArgumentException (dönüşüm yok), çözücü hataları...
            return failed(Reason.NOT_AUDIO, name, e)
        } ?: return ImportResult.Failed(Reason.NOT_AUDIO, "$name: no usable audio")

        val fileName = "${hashOf(decoded.pcm)}-${safeName(name)}.wav"
        val target = userDir.resolve(fileName)
        try {
            if (!Files.exists(target)) write(decoded, target)
        } catch (e: IOException) {
            return failed(Reason.IO, name, e)
        }
        return ImportResult.Ok(Sounds.USER_PREFIX + fileName, decoded.trimmed)
    }

    /** Yüklenmiş seslerin kimlikleri (`user:<dosya>`), görünen ada göre. */
    fun list(): List<String> {
        if (!Files.isDirectory(userDir)) return emptyList()
        return Files.list(userDir).use { files ->
            files.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".wav", ignoreCase = true) }
                .map { Sounds.USER_PREFIX + it.fileName.toString() }
                .toList()
        }.sortedBy { userLabel(it).lowercase() }
    }

    private class Decoded(val pcm: ByteArray, val format: AudioFormat, val frames: Long, val trimmed: Boolean)

    /** Kullanılabilir ses yoksa (kanal sayısı uygunsuz, 50 ms'den kısa) null. */
    private fun decode(source: Path): Decoded? {
        AudioSystem.getAudioInputStream(source.toFile()).use { raw ->
            val f = raw.format
            if (f.channels !in 1..2 || f.sampleRate <= 0f) return null
            val rate = min(f.sampleRate, MAX_RATE)
            val pcm16 = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.sampleRate, 16, f.channels, f.channels * 2, f.sampleRate, false)
            val target = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, f.channels, f.channels * 2, rate, false)
            var stream: AudioInputStream = if (f.matches(pcm16)) raw else AudioSystem.getAudioInputStream(pcm16, raw)
            if (rate != f.sampleRate) stream = AudioSystem.getAudioInputStream(target, stream)

            val frameSize = target.frameSize
            val maxBytes = (MAX_SECONDS * rate).toLong() * frameSize
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var trimmed = false
            while (true) {
                val room = maxBytes - out.size()
                if (room <= 0) {
                    trimmed = stream.read(ByteArray(frameSize)) > 0
                    break
                }
                val n = stream.read(buffer, 0, min(buffer.size.toLong(), room).toInt())
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            val bytes = out.toByteArray().let { it.copyOf(it.size - it.size % frameSize) }
            val frames = (bytes.size / frameSize).toLong()
            if (Sounds.isTooShort(frames, rate)) return null
            return Decoded(bytes, target, frames, trimmed)
        }
    }

    private fun write(decoded: Decoded, target: Path) {
        Files.createDirectories(userDir)
        val tmp = Files.createTempFile(userDir, "import-", ".tmp")
        try {
            AudioSystem.write(
                AudioInputStream(ByteArrayInputStream(decoded.pcm), decoded.format, decoded.frames),
                AudioFileFormat.Type.WAVE,
                tmp.toFile(),
            )
            try {
                Files.move(tmp, target, ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp, target, REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    private fun failed(reason: Reason, name: String, e: Exception): ImportResult.Failed {
        val detail = "$name: ${e.javaClass.simpleName}"
        Log.w(TAG, "sound import failed ($reason, $detail)")
        return ImportResult.Failed(reason, detail)
    }

    companion object {
        private const val TAG = "SoundImport"
        const val MAX_SOURCE_BYTES = 20L * 1024 * 1024
        const val MAX_SECONDS = 60
        const val MAX_RATE = 48_000f
        private const val HASH_CHARS = 12
        private const val NAME_CHARS = 40

        /** `user:<12 hex>-<ad>.wav` kimliğinin görünen adı (`<ad>`). */
        fun userLabel(id: String): String =
            id.removePrefix(Sounds.USER_PREFIX).removeSuffix(".wav").let { if (it.length > HASH_CHARS + 1 && it[HASH_CHARS] == '-') it.substring(HASH_CHARS + 1) else it }

        private fun hashOf(pcm: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(pcm).joinToString("") { "%02x".format(it) }.take(HASH_CHARS)

        /** Uzantısız kaynak adı; harf, rakam, boşluk, `-` ve `_` dışındakiler `_` olur (Türkçe harfler kalır). */
        internal fun safeName(fileName: String): String =
            fileName.substringBeforeLast('.').map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }
                .joinToString("").trim().take(NAME_CHARS).trim().ifEmpty { "ses" }
    }
}
