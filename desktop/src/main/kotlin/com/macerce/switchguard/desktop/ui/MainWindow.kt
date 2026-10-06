package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.TrayIcons
import java.awt.Dimension

/**
 * Ana pencere. Kapatma düğmesi uygulamadan çıkmaz, pencereyi gizler ([onCloseRequest]); [AppState.windowVisible]
 * ve [AppState.mainRaise] ile tepsiden yeniden gösterilir/öne getirilir.
 *
 * İçindeki bir hata (ör. Cihazlar ekranında) süreci, alarm penceresini ya da izlemeyi düşürmez: pencere yeniden kurulur
 * ve ekranın yerinde hata açıklaması gösterilir ([WindowErrorPanel]); "Cihazları yeniden göster" ekranı geri getirir.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainWindow(graph: AppGraph, onCloseRequest: () -> Unit) {
    val appState = graph.appState
    val visible by appState.windowVisible.collectAsState()
    val raise by appState.mainRaise.collectAsState()
    val version by graph.store.version.collectAsState()
    val strings = remember(version) { graph.strings() }
    val theme = remember(version) { graph.store.theme }
    val windowState = rememberWindowState(size = DpSize(1000.dp, 700.dp))
    // Hata olunca pencere yeni kuşakla baştan kurulur (eski pencerenin bileşimi bozulmuş olabilir).
    var failed by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }
    val errors = remember {
        windowErrorFactory("main") {
            // Hata panelinin kendisi de hata verirse döngüye girilmez: yalnızca ilk hata pencereyi yeniden kurar.
            if (!failed) {
                failed = true
                generation++
            }
        }
    }

    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides errors) {
        key(generation) {
            Window(
                onCloseRequest = onCloseRequest,
                state = windowState,
                visible = visible,
                title = strings["app_name"],
                icon = TrayIcons.app,
            ) {
                LaunchedEffect(Unit) { window.minimumSize = Dimension(900, 600) }
                LaunchedEffect(raise) {
                    if (raise == 0) return@LaunchedEffect
                    if (windowState.isMinimized) windowState.isMinimized = false
                    window.toFront()
                }
                SwitchGuardTheme(theme) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (failed) {
                            WindowErrorPanel(strings, graph.dataDir.resolve("logs").toString()) {
                                failed = false
                                generation++
                            }
                        } else {
                            DevicesScreen(graph, strings)
                        }
                    }
                }
            }
        }
    }
}

/** Ana penceredeki ekran hata verdi: açıklama ve ekranı yeniden deneme düğmesi. */
@Composable
private fun WindowErrorPanel(strings: Strings, logFolder: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(strings.format("window_error", logFolder), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(strings["window_error_retry"]) }
    }
}

/**
 * Çıkış onayı (izleme açıkken): ana pencere gizliyken de görünsün diye ayrı, her zaman üstte bir pencere.
 * [onConfirm] çıkar, [onCancel] vazgeçer.
 */
@Composable
fun ExitConfirmDialog(graph: AppGraph, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val strings = graph.strings()
    val theme = graph.store.theme
    DialogWindow(
        onCloseRequest = onCancel,
        state = rememberDialogState(position = WindowPosition(Alignment.Center), size = DpSize(460.dp, 240.dp)),
        title = strings["app_name"],
        resizable = false,
        alwaysOnTop = true,
    ) {
        SwitchGuardTheme(theme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(strings["tray_exit_confirm"], style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                        OutlinedButton(onClick = onCancel) { Text(strings["action_cancel"]) }
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) { Text(strings["tray_exit_confirm_yes"]) }
                    }
                }
            }
        }
    }
}
