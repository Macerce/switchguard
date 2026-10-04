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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
 * TV'de kumandayla seçili öğeyi belli eder: dışına yüksek kontrastlı bir çerçeve çizer ve hafifçe büyütür.
 * Çerçeve öğenin dışına çizilir (yerleşim değişmez). Dokunmatik cihazlarda etkisizdir.
 * Odaklanabilir öğeden ÖNCE (zincirde dışta) ya da onu saran ebeveyne verilmeli; çocuğun odağı da sayılır.
 * Kart içindeki satırlarda kart kenarı dışarıyı keser: [inside] ile çerçeve içe çizilir ve büyütme yapılmaz.
 */
fun Modifier.tvFocusRing(shape: Shape = CircleShape, inside: Boolean = false): Modifier = composed {
    if (!LocalIsTv.current) return@composed this
    var focused by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (focused) 1f else 0f, label = "focusRing")
    val color = MaterialTheme.colorScheme.onBackground
    this
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer {
            val scale = if (inside) 1f else 1f + 0.04f * progress
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (progress > 0f) {
                val gap = (if (inside) (-3).dp else 4.dp).toPx()
                val outline = shape.createOutline(Size(size.width + 2 * gap, size.height + 2 * gap), layoutDirection, this)
                translate(-gap, -gap) {
                    drawOutline(outline, color.copy(alpha = progress), style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
}
