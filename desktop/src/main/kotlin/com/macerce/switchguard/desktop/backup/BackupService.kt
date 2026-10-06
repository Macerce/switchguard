package com.macerce.switchguard.desktop.backup

import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.platform.Sounds
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

/**
 * Ayarlar → Yedekleme (`spec-2-ayarlar.md` §6): şifreli yedek dosyası yazar, okur ve geri yükler. Dosya işleri yapar:
 * arayüz iş parçacığından çağırmayın. Motoru durdurup başlatmak çağıranın işidir (bu sınıf yalnızca dosyalara ve
 * [Store]'a dokunur). Şifre ve gizli alanlar günlüğe yazılmaz.
 */
class BackupService(
    private val store: Store,
    private val userDir: Path,
    private val settingsFile: Path,
    private val appVersion: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val iterations: Int = 600_000,
) {
    /** Açılmış yedek: içerik ve geri yüklemeden önce gösterilecek özet. */
    data class Loaded(val content: JSONObject, val summary: Summary)

    /** Tüm ayarları [target]'a yazar (önce geçici dosyaya, sonra yerine taşıyarak). */
    fun write(target: Path, password: CharArray) {
        val created = Instant.ofEpochMilli(clock())
        val content = BackupContent.build(store, userDir, appVersion, created.toString())
        val sealed = BackupCrypto.seal(content.toString().toByteArray(UTF_8), password, created, iterations)
        try {
            val dir = target.toAbsolutePath().parent
            Files.createDirectories(dir)
            val tmp = Files.createTempFile(dir, ".switchguard-", ".tmp")
            try {
                Files.write(tmp, sealed)
                move(tmp, target)
            } finally {
                Files.deleteIfExists(tmp)
            }
        } catch (e: IOException) {
            Log.w(TAG, "backup could not be written: ${e.javaClass.simpleName}")
            throw BackupException(BackupException.Kind.IO, e)
        }
        Log.i(TAG, "backup written (${sealed.size} bytes)")
    }

    /** Yedeği okuyup açar; [Store]'a dokunmaz. */
    fun read(source: Path, password: CharArray): Loaded {
        val bytes = try {
            if (Files.size(source) > MAX_FILE_BYTES) throw BackupException(BackupException.Kind.TOO_BIG)
            Files.readAllBytes(source)
        } catch (e: IOException) {
            throw BackupException(BackupException.Kind.IO, e)
        }
        val plain = BackupCrypto.open(bytes, password)
        val content = try {
            JSONObject(String(plain, UTF_8))
        } catch (e: JSONException) {
            throw BackupException(BackupException.Kind.NOT_BACKUP, e)
        }
        if (content.optJSONObject("settings") == null) throw BackupException(BackupException.Kind.NOT_BACKUP)
        return Loaded(content, BackupContent.summary(content))
    }

    /**
     * Geri yükler: mevcut `settings.json`'ın kopyası alınır (`settings.json.before-restore-<ms>`), gömülü sesler ses
     * klasörüne yazılır, sonra ayarlar tek seferde yedektekiyle **değiştirilir** ([BackupContent.KEYS], oturum ve
     * anlık durum [BackupContent.CLEARED] temizlenir). Bir dosya adımı başarısız olursa [Store]'a dokunulmadan
     * [BackupException] atılır.
     */
    fun apply(loaded: Loaded) {
        try {
            if (Files.exists(settingsFile)) {
                Files.copy(settingsFile, settingsFile.resolveSibling("${settingsFile.fileName}.before-restore-${clock()}"), REPLACE_EXISTING)
            }
            writeSounds(loaded.content.optJSONObject("sounds") ?: JSONObject())
        } catch (e: IOException) {
            Log.w(TAG, "restore stopped before changing settings: ${e.javaClass.simpleName}")
            throw BackupException(BackupException.Kind.IO, e)
        }
        store.replaceSettings(loaded.content.getJSONObject("settings"), BackupContent.KEYS, BackupContent.CLEARED)
        Log.i(TAG, "settings restored from a backup made with ${loaded.summary.appVersion}")
    }

    private fun writeSounds(sounds: JSONObject) {
        if (sounds.length() == 0) return
        Files.createDirectories(userDir)
        val dec = Base64.getDecoder()
        for (id in sounds.keySet()) {
            val name = id.removePrefix(Sounds.USER_PREFIX)
            if (!id.startsWith(Sounds.USER_PREFIX) || !BackupContent.isPlainName(name)) {
                Log.w(TAG, "skipped a sound with an unsafe name in the backup")
                continue
            }
            val bytes = try {
                dec.decode(sounds.getString(id))
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "skipped a damaged sound in the backup: $name")
                continue
            }
            val target = userDir.resolve(name)
            if (Files.exists(target)) continue // aynı ad = aynı içerik (ad içerik özetinden)
            val tmp = Files.createTempFile(userDir, "restore-", ".tmp")
            try {
                Files.write(tmp, bytes)
                move(tmp, target)
            } finally {
                Files.deleteIfExists(tmp)
            }
        }
    }

    private fun move(from: Path, to: Path) {
        try {
            Files.move(from, to, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from, to, REPLACE_EXISTING)
        }
    }

    companion object {
        private const val TAG = "Backup"
        const val MAX_FILE_BYTES = 100L * 1024 * 1024
        const val EXTENSION = "sgbackup"

        fun defaultFileName(date: LocalDate) = "SwitchGuard-yedek-$date.$EXTENSION"
    }
}
