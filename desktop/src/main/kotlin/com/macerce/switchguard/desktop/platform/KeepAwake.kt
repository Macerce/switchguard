package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase

/**
 * Bilgisayarın uyumasını engeller ("Her zaman uyanık"): `SetThreadExecutionState(ES_CONTINUOUS or
 * ES_SYSTEM_REQUIRED)`. Yalnızca sistemi uyanık tutar, ekran yine kapanabilir. Windows bu durumu **iş
 * parçacığı başına** tutar ve iş parçacığı bitince bırakır: [set] her zaman aynı, uzun ömürlü iş parçacığından
 * (motorun tek iş parçacıklı zamanlayıcısı) çağrılmalıdır; aç ve kapat farklı iş parçacıklarından gelirse kapatma
 * açmayı geri almaz. Aktif istekler `powercfg /requests` çıktısında SYSTEM altında görünür.
 */
object KeepAwake {
    private const val TAG = "KeepAwake"
    private val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")

    fun set(on: Boolean) {
        if (!IS_WINDOWS) return
        val flags = if (on) WinBase.ES_CONTINUOUS or WinBase.ES_SYSTEM_REQUIRED else WinBase.ES_CONTINUOUS
        // Dönüş: önceki durum; başarısızlıkta 0.
        if (Kernel32.INSTANCE.SetThreadExecutionState(flags) == 0) {
            Log.w(TAG, "SetThreadExecutionState(${if (on) "on" else "off"}) failed")
        } else {
            Log.i(TAG, if (on) "system sleep blocked" else "system sleep allowed")
        }
    }
}
