package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import java.awt.Toolkit
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import kotlin.math.log10

/** Çalınacak ses: uygulamayla gelen bir kaynak ya da kullanıcının seçtiği, doğrulanmış bir ses dosyası. */
sealed interface SoundSource {
    /** `sounds/<name>.wav` kaynağı (id olarak `builtin:<name>`). */
    data class Builtin(val name: String) : SoundSource

    /** Var olan, okunabilen ve JDK'nın ses dosyası olarak açabildiği dosya. */
    data class File(val path: Path) : SoundSource
}

object Sounds {
    const val BUILTIN_PREFIX = "builtin:"
    const val DEFAULT_ALARM = "builtin:alarm_siren"
    const val DEFAULT_NOTIFY = "builtin:notify_chime"

    private const val TAG = "Sounds"
    private const val LAST_RESORT = "alarm_siren"

    /** Yerleşik ses adları (`builtin:` öneki olmadan); dosyaları `desktop/tools/gen_sounds.py` üretir. */
    val BUILTIN: List<String> = listOf("alarm_siren", "alarm_beep", "alarm_pulse", "notify_chime")

    /**
     * Ayarlardaki ses kimliğini çalınabilir bir kaynağa çevirir; **hiçbir zaman sessizlik döndürmez**.
     * Boş/null, bilinmeyen `builtin:` adı, olmayan/okunamayan/ses olmayan dosya ve telefondan gelebilecek
     * `content://...` gibi dosya yolu olmayan kimlikler [default] değerine düşer (boş/null dışındakilerde
     * Log.w yazılır). Saf fonksiyondur: ses cihazı gerekmez.
     */
    fun resolve(id: String?, default: String): SoundSource {
        val fallback = builtinOrNull(default) ?: SoundSource.Builtin(LAST_RESORT)
        val wanted = id?.trim().orEmpty()
        if (wanted.isEmpty()) return fallback

        val reason: String
        if (wanted.startsWith(BUILTIN_PREFIX)) {
            builtinOrNull(wanted)?.let { return it }
            reason = "unknown built-in sound"
        } else {
            val path = try {
                Paths.get(wanted)
            } catch (e: InvalidPathException) {
                null
            }
            if (path == null) {
                reason = "not a file path"
            } else {
                val rejected = rejectionOf(path)
                if (rejected == null) return SoundSource.File(path)
                reason = rejected
            }
        }
        Log.w(TAG, "configured sound '$wanted' rejected ($reason); using $fallback")
        return fallback
    }

    /** `builtin:<ad>` biçimini ve adın bilinmesini birlikte doğrular. */
    private fun builtinOrNull(id: String): SoundSource.Builtin? =
        id.takeIf { it.startsWith(BUILTIN_PREFIX) }
            ?.removePrefix(BUILTIN_PREFIX)
            ?.takeIf { it in BUILTIN }
            ?.let { SoundSource.Builtin(it) }

    /** Dosya kullanılamıyorsa nedeni, kullanılabiliyorsa null. */
    private fun rejectionOf(path: Path): String? = try {
        when {
            !Files.isRegularFile(path) -> "not an existing file"
            !Files.isReadable(path) -> "not readable"
            else -> {
                AudioSystem.getAudioFileFormat(path.toFile())
                null
            }
        }
    } catch (e: Exception) {
        // UnsupportedAudioFileException (ses değil), IOException, SecurityException...
        "not a playable audio file (${e.javaClass.simpleName})"
    }

    /** Yerleşik sesin ham akışı (mark/reset destekli, [AudioSystem.getAudioInputStream] için); yoksa null. */
    internal fun openResource(name: String): InputStream? =
        Sounds::class.java.getResourceAsStream("/sounds/$name.wav")?.let { BufferedInputStream(it) }
}

/**
 * Alarm döngüsü ve tek seferlik bildirim sesi. İzleme iş parçacığından ve arayüzden çağrılabilir.
 *
 * Temel garanti: ayarlı ama bozuk bir ses (dosya silinmiş, ses değil, cihaz biçimi çalamıyor) asla sessizlik
 * yaratmaz; varsayılana düşülür. Hiçbir çağrı çağırana istisna fırlatmaz; hata Log.w'ye yazılır. Varsayılan
 * da çalınamazsa (ses cihazı yok) son çare olarak sistem bip'i denenir.
 */
class SoundPlayer {
    private val lock = Any()
    private var loopClip: Playing? = null
    private var onceClip: Playing? = null
    private var cutoff: ScheduledFuture<*>? = null

    private val scheduler by lazy {
        ScheduledThreadPoolExecutor(1) { r -> Thread(r, "SoundPlayer-cutoff").apply { isDaemon = true } }
            .apply { removeOnCancelPolicy = true }
    }

    /**
     * [id]'yi ([Sounds.DEFAULT_ALARM] varsayılanıyla) kesintisiz döngüde çalar; o an çalan her sesi
     * (döngü ve tek seferlik) durdurup yerine geçer. [stop] çağrılana dek sürer. [volume] 0..1.
     */
    fun loop(id: String?, volume: Float) {
        synchronized(lock) {
            stopLocked()
            loopClip = play(id, Sounds.DEFAULT_ALARM, volume, loop = true)
        }
    }

    /**
     * [id]'yi ([Sounds.DEFAULT_NOTIFY] varsayılanıyla) bir kez çalar; kayıt [maxMs]'ten uzunsa o noktada keser
     * (en az 1 ms). Önceki tek seferlik sesin yerine geçer; çalmakta olan alarm döngüsüne dokunmaz.
     */
    fun once(id: String?, volume: Float, maxMs: Long = 6_000) {
        synchronized(lock) {
            closeOnceLocked()
            val playing = play(id, Sounds.DEFAULT_NOTIFY, volume, loop = false) ?: return
            onceClip = playing
            cutoff = attempt("schedule one-shot cutoff") {
                scheduler.schedule(Runnable {
                    synchronized(lock) {
                        release(playing)
                        if (onceClip === playing) onceClip = null
                    }
                }, maxMs.coerceAtLeast(1), TimeUnit.MILLISECONDS)
            }
        }
    }

    /** Hem döngüyü hem tek seferlik sesi durdurur; hiçbir şey çalmıyorsa zararsızdır. */
    fun stop() {
        synchronized(lock) { stopLocked() }
    }

    private fun stopLocked() {
        closeOnceLocked()
        loopClip?.let(::release)
        loopClip = null
    }

    private fun closeOnceLocked() {
        cutoff?.cancel(false)
        cutoff = null
        onceClip?.let(::release)
        onceClip = null
    }

    /** İstenen sesi, olmazsa varsayılanı, o da olmazsa sistem bip'ini dener. Çalan sesi ya da null döndürür. */
    private fun play(id: String?, default: String, volume: Float, loop: Boolean): Playing? {
        val wanted = Sounds.resolve(id, default)
        attempt("play $wanted") { start(wanted, volume, loop) }?.let { return it }

        val fallback = Sounds.resolve(null, default)
        if (fallback != wanted) {
            attempt("play default $fallback") { start(fallback, volume, loop) }?.let { return it }
        }
        attempt("system beep") { Toolkit.getDefaultToolkit().beep() }
        return null
    }

    private fun start(source: SoundSource, volume: Float, loop: Boolean): Playing {
        openAudio(source).use { audio ->
            val playing = Playing(AudioSystem.getClip())
            val clip = playing.clip
            try {
                clip.open(audio) // tüm veriyi belleğe alır; akış/dosya hemen kapanabilir
                applyGain(clip, volume)
                if (loop) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY)
                } else {
                    // Doğal bitişte (ya da durdurulunca) hat serbest bırakılır; release() tek seferliktir.
                    clip.addLineListener { e -> if (e.type == LineEvent.Type.STOP) release(playing) }
                    clip.start()
                }
                return playing
            } catch (t: Throwable) {
                release(playing)
                throw t
            }
        }
    }

    private fun openAudio(source: SoundSource): AudioInputStream = when (source) {
        is SoundSource.Builtin -> {
            val raw = Sounds.openResource(source.name)
                ?: throw FileNotFoundException("built-in sound resource missing: ${source.name}")
            try {
                AudioSystem.getAudioInputStream(raw)
            } catch (t: Throwable) {
                raw.close()
                throw t
            }
        }
        is SoundSource.File -> AudioSystem.getAudioInputStream(source.path.toFile())
    }

    private fun applyGain(clip: Clip, volume: Float) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            Log.w(TAG, "MASTER_GAIN not supported; playing at full volume")
            return
        }
        val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        gain.value = gainDb(volume, gain.minimum, gain.maximum)
    }

    /**
     * Sesi hemen susturur, hattı biraz sonra kapatır; kim önce gelirse tek seferlik, sonraki çağrılar boştur.
     *
     * Kapatma bilerek ertelenir: JDK'nın `DirectClip`'i, çalma iş parçacığı son verisini boşaltırken (kısa klipler,
     * örn. 0,6 sn'lik bildirim sesi) `close()` çağrılırsa mixer kilidinde kendi kendini kilitler ve `close()`
     * 2 sn bekler (JDK 21, Windows'ta ölçüldü). `stop()` ile `close()` arasına 50 ms koymak bunu ortadan kaldırıyor;
     * erteleme ayrıca çağıranı (arayüz/izleme iş parçacığı) bloklamaz.
     */
    private fun release(playing: Playing) {
        if (!playing.released.compareAndSet(false, true)) return
        attempt("stop clip") { playing.clip.stop() }
        val closeIt = Runnable { attempt("close clip") { playing.clip.close() } }
        if (attempt("schedule clip close") { scheduler.schedule(closeIt, CLOSE_DELAY_MS, TimeUnit.MILLISECONDS) } == null) {
            closeIt.run()
        }
    }

    private class Playing(val clip: Clip) {
        val released = AtomicBoolean(false)
    }

    /** Çalma hatalarını çağırana sızdırmaz: günlüğe yazar, null döner. */
    private inline fun <T> attempt(what: String, block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        Log.w(TAG, "$what failed", e)
        null
    } catch (e: LinkageError) {
        Log.w(TAG, "$what failed", e)
        null
    }

    internal companion object {
        private const val TAG = "SoundPlayer"
        private const val CLOSE_DELAY_MS = 100L

        /**
         * `MASTER_GAIN = 20·log10(volume)` dB; denetimin [minDb]/[maxDb] aralığına kırpılır (volume 0 → alt sınır).
         * Hacim 0..1'e sıkıştırılır (tam sesin üstüne çıkılmaz); NaN tam ses sayılır: alarm yanlışlıkla susmasın.
         */
        fun gainDb(volume: Float, minDb: Float, maxDb: Float): Float {
            val v = if (volume.isNaN()) 1f else volume.coerceIn(0f, 1f)
            val db = if (v == 0f) minDb else (20.0 * log10(v.toDouble())).toFloat()
            return db.coerceIn(minDb, maxDb)
        }
    }
}
