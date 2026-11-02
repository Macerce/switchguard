package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.core.UpdateCheck
import com.macerce.switchguard.desktop.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * GitHub'daki masaüstü yayınlarını (`desktop-v*`) denetler; yenisi varsa kurulum dosyasını indirip çalıştırır.
 * jpackage'ın kullanıcıya özel MSI'ı aynı yükseltme kimliğindeki (upgradeUuid) eski sürümü kaldırmıyor, iki kayıt
 * kalıyor: bu yüzden önce eski kurulumlar sessizce kaldırılır, sonra yenisi kurulur. Ayarlar %APPDATA%'da kalır.
 * Kurulum bitince uygulama yeniden açılır.
 */
class Updater(private val currentVersion: String?, private val tempDir: Path) {
    enum class Phase { IDLE, DOWNLOADING, FAILED }

    private val _available = MutableStateFlow<UpdateCheck.Release?>(null)
    /** Kurulabilecek yeni sürüm (indirme dosyası olan); yoksa null. */
    val available: StateFlow<UpdateCheck.Release?> = _available.asStateFlow()

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    /** İndirme durumu: [bytes] inen, [total] toplam (sunucu bildirmezse null), [bytesPerSec] anlık hız. */
    data class Progress(val bytes: Long, val total: Long?, val bytesPerSec: Double)

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = _progress.asStateFlow()

    /** Ağdan denetler (çağıran iş parçacığında). Geliştirme çalıştırmasında (sürüm yok) hiçbir şey yapmaz. */
    fun check() {
        val current = currentVersion ?: return
        val body = runCatching { get(UpdateCheck.RELEASES_URL) }
            .onFailure { Log.w(TAG, "update check failed: ${it.javaClass.simpleName}") }.getOrNull() ?: return
        val release = UpdateCheck.newer(body, UpdateCheck.DESKTOP_TAG, current, ".exe")?.takeIf { it.assetUrl != null }
        if (release != null && release != _available.value) Log.i(TAG, "update available: $current -> ${release.version}")
        _available.value = release
    }

    fun dismiss() { _available.value = null }

    /**
     * Kurulum dosyasını indirir ve uygulama kapandıktan sonra çalışacak şekilde başlatır. Başarılıysa true döner;
     * çağıran uygulamayı hemen kapatmalı (açık dosyalar kurulumu engeller).
     */
    fun downloadAndLaunch(): Boolean {
        val release = _available.value ?: return false
        _progress.value = null
        _phase.value = Phase.DOWNLOADING
        return runCatching {
            val exe = tempDir.resolve("SwitchGuard-${release.version}-Kurulum.exe")
            val conn = open(release.assetUrl!!)
            download(conn, exe)
            val app = Path.of(System.getenv("LOCALAPPDATA") ?: "", "SwitchGuard", "SwitchGuard.exe")
            val script = tempDir.resolve("switchguard-update.cmd")
            // Uygulamanın kapanmasını bekle, eski kurulumları kaldır, kur, kurulum başarılıysa yeniden aç.
            val uninstall = "\$i = New-Object -ComObject WindowsInstaller.Installer; " +
                "\$i.RelatedProducts('{$UPGRADE_CODE}') | ForEach-Object { Start-Process msiexec -ArgumentList '/x',\$_,'/qn' -Wait }"
            Files.writeString(
                script,
                "@echo off\r\ntimeout /t 3 /nobreak >nul\r\n" +
                    "powershell -NoProfile -ExecutionPolicy Bypass -Command \"$uninstall\"\r\n" +
                    "\"$exe\"\r\nif %errorlevel%==0 start \"\" \"$app\"\r\n",
            )
            ProcessBuilder("cmd", "/c", "start", "\"\"", "/min", script.toString()).start()
            Log.i(TAG, "update ${release.version} downloaded, installer started")
            true
        }.getOrElse {
            Log.w(TAG, "update download failed: ${it.javaClass.simpleName}: ${it.message}")
            _phase.value = Phase.FAILED
            false
        }
    }

    /** [conn]'u [target]'a yazar; yarım saniyede bir [progress]'i tazeler (hız son aralıktan, yumuşatılmış). */
    private fun download(conn: HttpURLConnection, target: Path) {
        val total = conn.contentLengthLong.takeIf { it > 0 }
        val part = target.resolveSibling(target.fileName.toString() + ".part")
        var bytes = 0L
        var speed = 0.0
        var lastAt = System.nanoTime()
        var lastBytes = 0L
        _progress.value = Progress(0, total, 0.0)
        conn.inputStream.use { input ->
            Files.newOutputStream(part).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    bytes += n
                    val now = System.nanoTime()
                    if (now - lastAt >= 500_000_000L) {
                        val current = (bytes - lastBytes) * 1e9 / (now - lastAt)
                        speed = if (speed == 0.0) current else speed * 0.6 + current * 0.4
                        lastAt = now
                        lastBytes = bytes
                        _progress.value = Progress(bytes, total, speed)
                    }
                }
            }
        }
        if (total != null && bytes != total) throw java.io.IOException("download incomplete: $bytes of $total bytes")
        _progress.value = Progress(bytes, total, speed)
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "SwitchGuard-Desktop")
    }

    private companion object {
        const val TAG = "Updater"
        /** build.gradle.kts'teki `upgradeUuid` ile aynı olmalı. */
        const val UPGRADE_CODE = "0ADB2F78-FB6A-4DF0-9106-404091D283C8"
    }

    private fun get(url: String): String = open(url).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
}
