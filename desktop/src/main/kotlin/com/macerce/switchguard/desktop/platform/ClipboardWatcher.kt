package com.macerce.switchguard.desktop.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * Panoyu aralıklı okur ([poll], çağıran zamanlar) ve **değişen** metni [onText]'e verir. İzleme başladığında panoda
 * duran metin (ör. eski bir giriş adresi) bildirilmez: yalnızca sonradan kopyalanan. Pano başka bir programca kilitli
 * ya da metin değilse o deneme sessizce atlanır.
 */
class ClipboardWatcher(private val read: () -> String? = ::readClipboardText, private val onText: (String) -> Unit) {
    private var started = false
    private var last: String? = null

    fun poll() {
        val text = try {
            read()
        } catch (_: Exception) {
            return
        }
        if (!started) {
            started = true
            last = text
            return
        }
        if (text == null || text == last) return
        last = text
        onText(text)
    }
}

/** Panodaki düz metin; yoksa, kilitliyse ya da metin değilse null. */
fun readClipboardText(): String? = try {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) clipboard.getData(DataFlavor.stringFlavor) as? String else null
} catch (_: Exception) {
    null // IllegalStateException (pano meşgul), UnsupportedFlavorException, IOException
}
