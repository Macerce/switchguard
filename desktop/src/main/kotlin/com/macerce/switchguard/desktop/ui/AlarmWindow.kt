package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings

// Telefondaki alarm ekranının (AlarmActivity, 2.12) renkleri: koyu kırmızı dikey geçiş, beyaz düğmede kırmızı yazı.
private val AlarmTop = Color(0xFF7A1010)
private val AlarmBottom = Color(0xFF2A0606)
private val DismissText = Color(0xFF8B1A1A)

/** Alarm penceresi hata verince en çok bu kadar kez baştan kurulur (her seferinde aynı hata veren içerik döngüye girmesin). */
private const val MAX_ALARM_REBUILDS = 3

/**
 * Alarm penceresi: çerçevesiz, her zaman üstte, ana monitörü kaplayan pencere; içinde telefondaki alarm ekranı
 * ([AlarmScreen]). [AppState.alarmLines] doluyken vardır, boşalınca (alarm kapatılınca) kapanır. Pencere kapatma
 * isteği yok sayılır: alarm yalnızca düğmeyle kapanır. Klavye odağı düğmeye verilmez: kullanıcı o an yazı yazıyorsa
 * Enter/Boşluk alarmı yanlışlıkla kapatmasın.
 *
 * Penceredeki bir hata yeniden fırlatılmaz (günlüğe yazılır) ve pencere baştan kurulur ([MAX_ALARM_REBUILDS] kez);
 * ses ve izleme pencereden bağımsız sürer.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlarmWindow(graph: AppGraph) {
    val lines by graph.appState.alarmLines.collectAsState()
    if (lines.isEmpty()) return

    val strings = graph.strings()
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized, position = WindowPosition(0.dp, 0.dp))
    var generation by remember { mutableIntStateOf(0) }
    val errors = remember {
        var rebuilds = 0
        windowErrorFactory("alarm") {
            if (rebuilds < MAX_ALARM_REBUILDS) {
                rebuilds++
                generation++
            }
        }
    }
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides errors) {
        key(generation) { AlarmWindowContent(graph, lines, strings, windowState) }
    }
}

@Composable
private fun AlarmWindowContent(
    graph: AppGraph,
    lines: List<String>,
    strings: Strings,
    windowState: WindowState,
) {
    Window(
        onCloseRequest = {},
        state = windowState,
        title = strings["app_name"],
        undecorated = true,
        alwaysOnTop = true,
    ) {
        // Motor her tick'te (ve yeni satırda) sayacı artırır: küçültülmüş ya da arkada kalmış pencere öne gelir.
        val raise by graph.appState.alarmRaise.collectAsState()
        LaunchedEffect(raise) {
            if (windowState.isMinimized) windowState.isMinimized = false
            window.toFront()
        }
        AlarmScreen(lines, strings, onDismiss = graph::dismissAlarm)
    }
}

/**
 * Telefondaki alarm ekranı: koyu kırmızı zemin, zilin çevresinden dışa yayılan halkalar (logodaki ses dalgaları gibi)
 * ve nabız gibi atan zil, başlık, satırlar (en yeni en üstte ve büyük), büyük "Kapat". Telefon ilk dört satırı
 * gösterir; masaüstünde hepsi kaydırılabilir listede. Sessiz test alarmı satırının önünde "ses kapalı" simgesi durur.
 * Telefondaki "Uygulamayı aç" yok: pencere her zaman üstte olduğundan açılan ana pencere bunun arkasında kalırdı.
 */
@Composable
internal fun AlarmScreen(lines: List<String>, strings: Strings, onDismiss: () -> Unit) {
    // Animasyon değerleri yalnızca çizim aşamasında okunur: her karede yeniden düzenleme (recomposition) olmaz.
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    val wave = rememberInfiniteTransition(label = "waves").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave",
    )
    val silentLine = strings["test_alarm_line_silent"]

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AlarmTop, AlarmBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 900.dp).padding(horizontal = 48.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(240.dp).testTag("alarm-waves").drawBehind {
                    val base = 66.dp.toPx()
                    for (k in 0..2) {
                        val t = (wave.value + k / 3f) % 1f
                        drawCircle(
                            Color.White.copy(alpha = 0.28f * (1f - t)),
                            radius = base + (size.minDimension / 2 - base) * t,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(132.dp)
                        .graphicsLayer {
                            scaleX = pulse.value
                            scaleY = pulse.value
                        }
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(70.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            // Uzun başlık iki satıra bölünebilir: satır yüksekliği yazı boyutuna göre verilmezse satırlar üst üste biner.
            Text(
                strings.plural("alarm_banner_title", lines.size, lines.size),
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 1.2.em,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            // En yeni olay en üstte ve büyük; çok satır varsa liste kayar, düğme hep görünür kalır.
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("alarm-lines"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                lines.reversed().forEachIndexed { i, line ->
                    AlarmLine(line, newest = i == 0, silent = line.endsWith(silentLine))
                }
            }
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(72.dp).widthIn(min = 320.dp).testTag("alarm-dismiss"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DismissText),
            ) {
                Text(strings["alarm_dismiss"], fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Alarm satırı: en yeni olan tam beyaz ve büyük, öncekiler soluk. Sessiz test satırının önünde "ses kapalı" simgesi. */
@Composable
private fun AlarmLine(line: String, newest: Boolean, silent: Boolean) {
    val color = Color.White.copy(alpha = if (newest) 1f else 0.7f)
    val size = if (newest) 26.sp else 19.sp
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (silent) {
            Icon(Icons.AutoMirrored.Rounded.VolumeOff, null, tint = color, modifier = Modifier.size(if (newest) 26.dp else 20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(line, color = color, fontSize = size, lineHeight = 1.25.em, textAlign = TextAlign.Center)
    }
}
