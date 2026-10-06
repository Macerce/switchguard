package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinReg

/**
 * "Windows açılınca başlat": `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` altındaki [valueName] değeri
 * (yönetici hakkı gerekmez, yalnızca bu kullanıcı için). Yazılan komut `"<SwitchGuard.exe>" --tray` biçimindedir:
 * oturum açılışında pencere gizli, tepsi simgesiyle başlar.
 */
class Autostart(private val valueName: String = "SwitchGuard") {
    /**
     * Gerçek bir kurulumdan (jpackage) çalışıyorsak true. `./gradlew run` ya da Windows dışında yol yoktur
     * ([defaultCommand] null): bu durumda uygulama kayıt defterine hiç dokunmamalıdır, geliştirme çalıştırması
     * kurulu uygulamanın kaydını bozmasın. Açık bir komut verilen [setEnabled] bundan bağımsız çalışır (testler).
     */
    val available: Boolean
        get() = IS_WINDOWS && defaultCommand() != null

    /** Run değeri var ve boş değil mi. (Görev Yöneticisi'nin "devre dışı" işareti ayrı bir anahtardır, okunmaz.) */
    fun isEnabled(): Boolean = IS_WINDOWS && !current().isNullOrBlank()

    /**
     * [on] ise Run değerini [command] ile eşitler (yoksa yazar, farklıysa düzeltir: yeniden kurulumla yol değişmiş
     * olabilir); değilse siler. [command] null ise (kurulu uygulama yok) açma isteği yok sayılır. Kayıt defteri
     * hatası (ör. grup ilkesi) uygulamayı düşürmez, günlüğe yazılır; sonucu [isEnabled] ile okuyun.
     */
    fun setEnabled(on: Boolean, command: String? = defaultCommand()) {
        if (!IS_WINDOWS) return
        try {
            if (on) {
                if (command == null) {
                    Log.w(TAG, "no packaged launcher path, not enabling autostart")
                    return
                }
                if (!Advapi32Util.registryKeyExists(HKCU, RUN_KEY)) Advapi32Util.registryCreateKey(HKCU, RUN_KEY)
                if (current() != command) Advapi32Util.registrySetStringValue(HKCU, RUN_KEY, valueName, command)
            } else if (Advapi32Util.registryValueExists(HKCU, RUN_KEY, valueName)) {
                Advapi32Util.registryDeleteValue(HKCU, RUN_KEY, valueName)
            }
        } catch (e: Win32Exception) {
            Log.w(TAG, "could not ${if (on) "write" else "remove"} the Run value $valueName", e)
        }
    }

    private fun current(): String? = try {
        if (Advapi32Util.registryValueExists(HKCU, RUN_KEY, valueName)) {
            Advapi32Util.registryGetStringValue(HKCU, RUN_KEY, valueName)
        } else {
            null
        }
    } catch (e: Win32Exception) {
        Log.w(TAG, "could not read the Run value $valueName", e)
        null
    }

    companion object {
        private const val TAG = "Autostart"
        private const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        private val HKCU = WinReg.HKEY_CURRENT_USER
        private val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")

        /**
         * jpackage başlatıcısının yolu (`jpackage.app-path`, kurulu uygulamada JVM'e verilir) tırnaklanıp `--tray`
         * eklenir; kurulu değilse null.
         */
        fun defaultCommand(): String? =
            System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }?.let { "\"$it\" --tray" }
    }
}
