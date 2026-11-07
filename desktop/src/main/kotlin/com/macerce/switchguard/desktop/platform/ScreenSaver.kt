package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.win32.StdCallLibrary
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * SwitchGuard'ı Windows ekran koruyucusu yapar (telefondaki DreamService'in karşılığı). Uygulamaya gömülü küçük
 * `SwitchGuard.scr` (packaging/saver) veri klasörüne çıkarılır ve kullanıcının ekran koruyucusu olarak kayıt
 * defterine yazılır (`HKCU\Control Panel\Desktop`: `SCRNSAVE.EXE`, `ScreenSaveActive`, `ScreenSaveTimeOut`;
 * yönetici izni gerekmez). Windows boşta kalınca `.scr /s` çalışır; o da uygulamayı `--saver` ile çağırır ve bekleme
 * ekranı açılır. `.scr` uygulamanın yolunu [APP_FILE]'dan okur; yol her açılışta tazelenir ([refresh]).
 * Kuruluma bağlı (jpackage); geliştirme çalıştırmasında [available] false.
 */
class ScreenSaver(private val dataDir: Path) {
    private val scr: Path = dataDir.resolve("SwitchGuard.scr")

    /** Kurulu uygulamanın başlatıcısı; geliştirme çalıştırmasında null. */
    private val appPath: String? = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }

    val available: Boolean get() = IS_WINDOWS && appPath != null

    /** Windows'un ekran koruyucusu şu an SwitchGuard mı (ve açık mı). */
    fun isInstalled(): Boolean = IS_WINDOWS && runCatching {
        val current = if (Advapi32Util.registryValueExists(HKCU, KEY, NAME)) Advapi32Util.registryGetStringValue(HKCU, KEY, NAME) else ""
        current.equals(scr.toString(), ignoreCase = true) && active()
    }.getOrDefault(false)

    /** Boşta bekleme süresi (dakika); okunamazsa null. */
    fun timeoutMinutes(): Int? = runCatching {
        Advapi32Util.registryGetStringValue(HKCU, KEY, TIMEOUT).trim().toInt() / 60
    }.getOrNull()

    /** Ekran koruyucu yapar; [minutes] boşta bekleme süresi. Başarısızsa false (günlüğe yazılır). */
    fun install(minutes: Int): Boolean {
        if (!available) return false
        return try {
            extract()
            Advapi32Util.registrySetStringValue(HKCU, KEY, NAME, scr.toString())
            Advapi32Util.registrySetStringValue(HKCU, KEY, TIMEOUT, (minutes * 60).toString())
            Advapi32Util.registrySetStringValue(HKCU, KEY, ACTIVE, "1")
            // Kayıt defteri değişikliği oturum yeniden açılmadan da geçerli olsun.
            spi(SPI_SETSCREENSAVETIMEOUT, minutes * 60)
            spi(SPI_SETSCREENSAVEACTIVE, 1)
            Log.i(TAG, "installed as screen saver ($minutes min)")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "could not install the screen saver", t)
            false
        }
    }

    /** SwitchGuard ekran koruyucusuysa kaldırır (Windows ekran koruyucusuz kalır). */
    fun uninstall(): Boolean = try {
        if (Advapi32Util.registryValueExists(HKCU, KEY, NAME) &&
            Advapi32Util.registryGetStringValue(HKCU, KEY, NAME).equals(scr.toString(), ignoreCase = true)
        ) {
            Advapi32Util.registryDeleteValue(HKCU, KEY, NAME)
            Advapi32Util.registrySetStringValue(HKCU, KEY, ACTIVE, "0")
            spi(SPI_SETSCREENSAVEACTIVE, 0)
        }
        Log.i(TAG, "screen saver removed")
        true
    } catch (t: Throwable) {
        Log.w(TAG, "could not remove the screen saver", t)
        false
    }

    /** Açılışta: ekran koruyucuysak `.scr` ve uygulama yolu güncellenir (güncellemeden sonra yeni .scr, taşınan kurulum). */
    fun refresh() {
        if (!available || !isInstalled()) return
        runCatching { extract() }.onFailure { Log.w(TAG, "could not refresh the screen saver", it) }
    }

    /** Windows'un ekran koruyucu penceresi (süre, "devam edince oturum açma ekranını göster"). */
    fun openWindowsSettings() {
        runCatching { ProcessBuilder("control.exe", "desk.cpl,,@screensaver").start() }
            .onFailure { Log.w(TAG, "could not open screen saver settings", it) }
    }

    private fun extract() {
        Files.createDirectories(dataDir)
        val tmp = dataDir.resolve("SwitchGuard.scr.tmp")
        (javaClass.getResourceAsStream("/saver/SwitchGuard.scr") ?: error("saver resource missing")).use {
            Files.copy(it, tmp, StandardCopyOption.REPLACE_EXISTING)
        }
        // Çalışan .scr varsa üzerine yazılamaz: o zaman eskisi kalır (aynı işi yapar).
        runCatching { Files.move(tmp, scr, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            .onFailure { Files.deleteIfExists(tmp); if (!Files.exists(scr)) throw it }
        Files.writeString(dataDir.resolve(APP_FILE), appPath!!)
    }

    private fun active(): Boolean = !Advapi32Util.registryValueExists(HKCU, KEY, ACTIVE) ||
        Advapi32Util.registryGetStringValue(HKCU, KEY, ACTIVE).trim() == "1"

    private fun spi(action: Int, value: Int) {
        runCatching { User32Spi.INSTANCE.SystemParametersInfoW(action, value, null, SPIF_UPDATEINIFILE or SPIF_SENDCHANGE) }
            .onFailure { Log.w(TAG, "SystemParametersInfo($action) failed", it) }
    }

    @Suppress("FunctionName")
    private interface User32Spi : StdCallLibrary {
        fun SystemParametersInfoW(uiAction: Int, uiParam: Int, pvParam: Pointer?, fWinIni: Int): Boolean

        companion object {
            val INSTANCE: User32Spi = Native.load("user32", User32Spi::class.java)
        }
    }

    companion object {
        private const val TAG = "ScreenSaver"
        private val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
        private val HKCU = WinReg.HKEY_CURRENT_USER
        private const val KEY = "Control Panel\\Desktop"
        private const val NAME = "SCRNSAVE.EXE"
        private const val ACTIVE = "ScreenSaveActive"
        private const val TIMEOUT = "ScreenSaveTimeOut"
        private const val SPI_SETSCREENSAVETIMEOUT = 0x000F
        private const val SPI_SETSCREENSAVEACTIVE = 0x0011
        private const val SPIF_UPDATEINIFILE = 0x01
        private const val SPIF_SENDCHANGE = 0x02

        /** `.scr`'nin uygulamayı bulduğu dosya (veri klasöründe). */
        const val APP_FILE = "saver-app.txt"

        /** Uygulamanın `.scr` tarafından çağrıldığı bayrak. */
        const val FLAG = "--saver"

        /** Ayarlar'daki süre seçenekleri (dakika). */
        val MINUTES = listOf(1, 3, 5, 10, 15, 20, 30, 45, 60)
    }
}
