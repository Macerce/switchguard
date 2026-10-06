package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.Native
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

/** Alarmın ekranı uyandırma kancası; [DesktopHost] testlerinde sahtesi verilir. */
interface DisplayWaker {
    /** Yeni alarm: ekran kapalıysa açar ve ekranın boşta kapanma sayacını sıfırlar. */
    fun wake()

    /** Alarm sürüyor (her tick): ekranın boşta kapanma sayacını sıfırlar. */
    fun keepOn()
}

/**
 * Alarm ekranı uyandırır. [KeepAwake] yalnızca sistemi uyanık tutar (`ES_SYSTEM_REQUIRED`); boşta duran
 * bilgisayarda ekran yine kapanır, tam ekran alarm görünmez ve monitör hoparlörü de uykuda kalır.
 *
 * - [keepOn]: `SetThreadExecutionState(ES_DISPLAY_REQUIRED)`, `ES_CONTINUOUS` **olmadan**: tek seferliktir, yalnızca
 *   ekranın boşta sayacını sıfırlar; iş parçacığının sürekli durumuna ([KeepAwake]) dokunmaz, bu yüzden hangi iş
 *   parçacığından çağrıldığı önemsizdir.
 * - [wake]: buna ek olarak sıfır mesafeli bir fare hareketi (`SendInput`, `MOUSEEVENTF_MOVE`, dx = dy = 0) gönderir:
 *   zaten kapanmış ekranı kullanıcı girdisi gibi açar (`ES_DISPLAY_REQUIRED` kapalı ekranı her sistemde açmaz);
 *   imleç yerinden oynamaz.
 *
 * Hatalar çağırana sızmaz, yalnızca günlüğe yazılır. Windows dışında hiçbir şey yapmaz.
 */
object DisplayWake : DisplayWaker {
    private const val TAG = "DisplayWake"
    private const val MOUSEEVENTF_MOVE = 0x0001
    private val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")

    override fun wake() {
        keepOn()
        if (!IS_WINDOWS) return
        try {
            @Suppress("UNCHECKED_CAST")
            val inputs = WinUser.INPUT().toArray(1) as Array<WinUser.INPUT>
            val input = inputs[0]
            input.type = WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE.toLong())
            input.input.setType("mi")
            input.input.mi.apply {
                dx = WinDef.LONG(0)
                dy = WinDef.LONG(0)
                mouseData = WinDef.DWORD(0)
                dwFlags = WinDef.DWORD(MOUSEEVENTF_MOVE.toLong())
                time = WinDef.DWORD(0)
                dwExtraInfo = BaseTSD.ULONG_PTR(0)
            }
            val sent = User32.INSTANCE.SendInput(WinDef.DWORD(1), inputs, input.size())
            if (sent.toInt() == 1) {
                Log.i(TAG, "alarm: display wake input sent")
            } else {
                Log.w(TAG, "SendInput (display wake) failed, error ${Native.getLastError()}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SendInput (display wake) failed", t)
        }
    }

    override fun keepOn() {
        if (!IS_WINDOWS) return
        try {
            // Dönüş: önceki durum; başarısızlıkta 0.
            if (Kernel32.INSTANCE.SetThreadExecutionState(WinBase.ES_DISPLAY_REQUIRED) == 0) {
                Log.w(TAG, "SetThreadExecutionState(ES_DISPLAY_REQUIRED) failed")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "SetThreadExecutionState(ES_DISPLAY_REQUIRED) failed", t)
        }
    }
}
