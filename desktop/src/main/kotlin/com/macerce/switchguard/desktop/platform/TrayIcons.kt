package com.macerce.switchguard.desktop.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.desktop.ui.StatusColors
import java.awt.BasicStroke
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/** Tepsi simgeleri: durum renginde yuvarlak; kod ile çizilir, dosya gerekmez. */
object TrayIcons {
    private const val SIZE = 64

    private val cache = ConcurrentHashMap<ConnState, Painter>()

    /** [state]'in rengindeki simge ([StatusColors.of]). */
    fun painter(state: ConnState): Painter = cache.getOrPut(state) { BitmapPainter(disc(StatusColors.of(state))) }

    /** Pencere (görev çubuğu) simgesi: uygulamanın ana rengi. */
    val app: Painter by lazy { BitmapPainter(disc(Color(0xFF006A6A))) }

    private fun disc(color: Color): ImageBitmap {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val pad = 4
            val d = SIZE - 2 * pad
            g.color = java.awt.Color(color.red, color.green, color.blue)
            g.fillOval(pad, pad, d, d)
            // İnce açık halka: hem koyu hem açık görev çubuğunda seçilsin.
            g.stroke = BasicStroke(4f)
            g.color = java.awt.Color(255, 255, 255, 230)
            g.drawOval(pad + 2, pad + 2, d - 4, d - 4)
        } finally {
            g.dispose()
        }
        return image.toComposeImageBitmap()
    }
}
