package com.macerce.switchguard.desktop.report

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.io.OutputStream

// Telefondaki raporlar android.graphics (Canvas, Paint, PdfDocument) ile çizilir. Bu dosya o API'nin raporların
// kullandığı kadarını Graphics2D + PDFBox üzerinde verir: ReportPdf / EnergyExport çizim kodu telefondakiyle aynı kalsın.

/** `android.graphics.Color`: ARGB tamsayı. */
object Color {
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    fun rgb(r: Int, g: Int, b: Int) = argb(255, r, g, b)
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
}

class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** `android.graphics.Paint`'in raporlarda kullanılan kısmı. */
class Paint(
    var textSize: Float = 12f,
    var color: Int = Color.BLACK,
    var bold: Boolean = false,
    var textAlign: Align = Align.LEFT,
    var style: Style = Style.FILL,
    /** Kesikli çizgi: çizgi ve boşluk uzunlukları; null = düz. */
    var dash: FloatArray? = null,
    var strokeWidth: Float = 0.5f,
) {
    enum class Align { LEFT, CENTER, RIGHT }
    enum class Style { FILL, STROKE }

    constructor(other: Paint) : this(other.textSize, other.color, other.bold, other.textAlign, other.style, other.dash, other.strokeWidth)

    val font: Font get() = Font(FAMILY, if (bold) Font.BOLD else Font.PLAIN, 1).deriveFont(textSize)

    fun measureText(text: String): Float = font.getStringBounds(text, FRC).width.toFloat()
    fun measureText(text: String, start: Int, end: Int): Float = measureText(text.substring(start, end))

    companion object {
        /** Türkçe harfleri olan, Windows'ta her zaman bulunan yazı tipi. */
        const val FAMILY = "Segoe UI"
        private val FRC = FontRenderContext(null, true, true)
    }
}

/** `android.graphics.Canvas`'ın raporlarda kullanılan kısmı. */
class Canvas(private val g: Graphics2D) {
    init {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    }

    private fun apply(p: Paint) {
        g.color = java.awt.Color(p.color, true)
        g.stroke = if (p.dash != null) BasicStroke(p.strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, p.dash, 0f)
            else BasicStroke(p.strokeWidth)
    }

    fun drawText(text: String, x: Float, y: Float, p: Paint) {
        apply(p)
        g.font = p.font
        val w = p.measureText(text)
        val left = when (p.textAlign) {
            Paint.Align.LEFT -> x
            Paint.Align.CENTER -> x - w / 2
            Paint.Align.RIGHT -> x - w
        }
        g.drawString(text, left, y)
    }

    fun drawRoundRect(r: RectF, rx: Float, ry: Float, p: Paint) = shape(RoundRectangle2D.Float(r.left, r.top, r.right - r.left, r.bottom - r.top, rx * 2, ry * 2), p)
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, p: Paint) = shape(Rectangle2D.Float(left, top, right - left, bottom - top), p)
    fun drawCircle(cx: Float, cy: Float, radius: Float, p: Paint) = shape(Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2), p)

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, p: Paint) {
        apply(p)
        g.draw(Line2D.Float(x1, y1, x2, y2))
    }

    private fun shape(s: java.awt.Shape, p: Paint) {
        apply(p)
        if (p.style == Paint.Style.FILL) g.fill(s) else g.draw(s)
    }
}

/** `android.graphics.pdf.PdfDocument`: sayfa sayfa çizilir, sonra [writeTo]. */
class PdfDocument {
    private val doc = PDDocument()
    private val fonts = PdfBoxGraphics2DFontTextDrawer().apply {
        // Yazı tipi gömülürse metin PDF'te seçilip aranabilir; dosya yoksa metin çizgi olarak çizilir.
        FONT_FILES.map(::File).filter { it.isFile }.forEach { runCatching { registerFont(it) } }
    }

    class PageInfo(val width: Int, val height: Int, val number: Int)

    inner class Page(val info: PageInfo) {
        internal val graphics = PdfBoxGraphics2D(doc, info.width.toFloat(), info.height.toFloat()).apply { setFontTextDrawer(fonts) }
        val canvas = Canvas(graphics)
    }

    fun startPage(info: PageInfo) = Page(info)

    fun finishPage(page: Page) {
        page.graphics.dispose()
        val p = PDPage(PDRectangle(page.info.width.toFloat(), page.info.height.toFloat()))
        doc.addPage(p)
        PDPageContentStream(doc, p).use { it.drawForm(page.graphics.xFormObject) }
    }

    fun writeTo(out: OutputStream) = doc.save(out)
    fun close() = doc.close()

    private companion object {
        val FONT_FILES = listOf("segoeui.ttf", "segoeuib.ttf").map { (System.getenv("WINDIR") ?: "C:\\Windows") + "\\Fonts\\" + it }
    }
}
