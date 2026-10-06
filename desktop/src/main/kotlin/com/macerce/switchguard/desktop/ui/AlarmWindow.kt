package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.macerce.switchguard.desktop.AppGraph

private val AlarmRed = Color(0xFFB71C1C)

/**
 * Alarm penceresi: çerçevesiz, her zaman üstte, ana monitörü kaplayan kırmızı pencere; olay satırları ve büyük
 * "Kapat" düğmesi. [AppState.alarmLines] doluyken vardır, boşalınca (alarm kapatılınca) kapanır. Pencere
 * kapatma isteği yok sayılır: alarm yalnızca düğmeyle kapanır. Klavye odağı düğmeye verilmez: kullanıcı o an
 * yazı yazıyorsa Enter/Boşluk alarmı yanlışlıkla kapatmasın.
 */
@Composable
fun AlarmWindow(graph: AppGraph) {
    val lines by graph.appState.alarmLines.collectAsState()
    if (lines.isEmpty()) return

    val strings = graph.strings()
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized, position = WindowPosition(0.dp, 0.dp))
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

        Box(Modifier.fillMaxSize().background(AlarmRed)) {
            Column(
                Modifier.fillMaxSize().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.format("alarm_banner_title", lines.size),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(24.dp))
                Column(
                    Modifier.weight(1f, fill = false).widthIn(max = 900.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lines.forEach { line ->
                        Text(line, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    }
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { graph.engine.dismissAlarm() },
                    modifier = Modifier.widthIn(min = 360.dp).height(88.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AlarmRed),
                ) {
                    Text(
                        strings["alarm_dismiss"],
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
