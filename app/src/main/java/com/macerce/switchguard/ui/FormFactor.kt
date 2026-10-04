package com.macerce.switchguard.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ekran türüne göre düzen kararları: telefon, geniş ekran (tablet/Chromebook) ve Android TV. */
object FormFactor {
    /** Android TV / Google TV: dokunmatik yok, kumandayla gezilir, bildirimler ekranda görünmez. */
    fun isTv(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    /** Material'ın "orta" genişlik eşiği; altı telefon düzenidir. */
    const val WIDE_DP = 600

    /** TV'lerin ekran kenarını kesebildiği pay (Android TV tasarım kılavuzu: yatay %5, dikey %5). */
    val TV_OVERSCAN_H = 48.dp
    val TV_OVERSCAN_V = 27.dp
}

/** Uygulama ağacına bir kez verilir; bileşenler TV'ye özel davranışı buradan okur. */
val LocalIsTv = staticCompositionLocalOf { false }

/** Genişlik tablet eşiğini geçti mi (yan menü, iki bölme vb. için). */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= FormFactor.WIDE_DP

/** Geniş ekranda içeriği okunabilir genişlikte ortalar; telefonda etkisizdir. */
fun Modifier.readableWidth(max: Dp = 840.dp): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

/**
 * TV'de kumandayla seçili öğeyi belli eder (Google TV dili): ince açık kenar, arkasında yumuşak
 * vurgu rengi parıltısı ve hafif büyüme. Çizim öğenin dışına taşar, yerleşim değişmez.
 * Dokunmatik cihazlarda etkisizdir.
 * Odaklanabilir öğeden ÖNCE (zincirde dışta) ya da onu saran ebeveyne verilmeli; çocuğun odağı da sayılır.
 * Kart/menü içindeki öğelerde kenar dışarıyı keser: [inside] ile zemin hafifçe aydınlanır, kenar içe çizilir.
 */
fun Modifier.tvFocusRing(shape: Shape = CircleShape, inside: Boolean = false, color: Color = Color.Unspecified): Modifier = composed {
    if (!LocalIsTv.current) return@composed this
    var focused by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (focused) 1f else 0f, label = "focusRing")
    val edge = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    val glow = MaterialTheme.colorScheme.primary
    this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer {
            val scale = if (inside) 1f else 1f + 0.05f * progress
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            if (inside && progress > 0f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), edge.copy(alpha = 0.10f * progress))
            }
            drawContent()
            if (progress <= 0f) return@drawWithContent
            if (inside) {
                val inset = 2.dp.toPx()
                val outline = shape.createOutline(Size(size.width - 2 * inset, size.height - 2 * inset), layoutDirection, this)
                translate(inset, inset) {
                    drawOutline(outline, edge.copy(alpha = 0.85f * progress), style = Stroke(width = 2.dp.toPx()))
                }
            } else {
                // Dışta yumuşak parıltı, içte ince net kenar.
                for ((gapDp, widthDp, alpha) in listOf(Triple(6f, 6f, 0.22f), Triple(3f, 2f, 0.95f))) {
                    val gap = gapDp.dp.toPx()
                    val outline = shape.createOutline(Size(size.width + 2 * gap, size.height + 2 * gap), layoutDirection, this)
                    val c = if (alpha < 0.5f) glow else edge
                    translate(-gap, -gap) {
                        drawOutline(outline, c.copy(alpha = alpha * progress), style = Stroke(width = widthDp.dp.toPx()))
                    }
                }
            }
        }
}

/**
 * Kaydırıcıda (Slider) kumanda: Material Slider yukarı/aşağı tuşlarını da değer değiştirmek için kullanır,
 * odak çubukta sıkışır. TV'de yukarı/aşağı odağı komşu öğeye taşır; sol/sağ değeri değiştirmeye devam eder.
 */
fun Modifier.tvSliderKeys(): Modifier = composed {
    if (!LocalIsTv.current) return@composed this
    val focusManager = LocalFocusManager.current
    this.onPreviewKeyEvent { e ->
        val dir = when (e.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            else -> return@onPreviewKeyEvent false
        }
        if (e.type == KeyEventType.KeyDown) focusManager.moveFocus(dir)
        true
    }
}
