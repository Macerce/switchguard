package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

/**
 * Pencerenin Windows başlık çubuğu uygulamanın temasına uysun: koyu temada koyu (Windows 10 2004+ ve 11), Windows 11'de
 * başlık zemini ve yazısı da uygulamanın kenar çubuğu rengiyle aynı (beyaz başlık çubuğu altında koyu uygulama durmasın).
 * Desteklenmeyen sürümde çağrı sessizce yok sayılır.
 */
object TitleBar {
    private interface Dwm : StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: WinDef.HWND, attribute: Int, value: Pointer, size: Int): Int
    }

    private val dwm: Dwm? by lazy { runCatching { Native.load("dwmapi", Dwm::class.java) }.getOrNull() }

    private const val USE_IMMERSIVE_DARK_MODE = 20
    private const val BORDER_COLOR = 34
    private const val CAPTION_COLOR = 35
    private const val TEXT_COLOR = 36

    /** [caption] ve [text]: 0xRRGGBB. */
    fun apply(window: java.awt.Window, dark: Boolean, caption: Int, text: Int) {
        val api = dwm ?: return
        runCatching {
            val hwnd = WinDef.HWND(Native.getWindowPointer(window) ?: return)
            fun set(attr: Int, v: Int) {
                val ref = IntByReference(v)
                api.DwmSetWindowAttribute(hwnd, attr, ref.pointer, 4)
            }
            set(USE_IMMERSIVE_DARK_MODE, if (dark) 1 else 0)
            set(CAPTION_COLOR, colorRef(caption))
            set(TEXT_COLOR, colorRef(text))
            set(BORDER_COLOR, colorRef(caption))
        }.onFailure { Log.w("TitleBar", "DwmSetWindowAttribute failed: $it") }
    }

    /** 0xRRGGBB → Windows COLORREF (0x00BBGGRR). */
    private fun colorRef(rgb: Int): Int = ((rgb and 0xFF) shl 16) or (rgb and 0xFF00) or ((rgb shr 16) and 0xFF)

    /** Kenar çubuğu rengiyle aynı (bkz. `sidebarColor`). */
    fun forTheme(window: java.awt.Window, dark: Boolean) =
        if (dark) apply(window, true, 0x1C1C1C, 0xE6E6E6) else apply(window, false, 0xEBEBEB, 0x1B1B1B)
}
