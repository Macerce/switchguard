package com.macerce.switchguard.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.NotificationQueue
import com.macerce.switchguard.desktop.platform.TrayIcons
import com.macerce.switchguard.desktop.ui.AlarmWindow
import com.macerce.switchguard.desktop.ui.ExitConfirmDialog
import com.macerce.switchguard.desktop.ui.MainWindow
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** Ayarlar, günlükler ve veritabanları: `%APPDATA%\SwitchGuard`. */
private fun dataDir(): Path =
    Paths.get(System.getenv("APPDATA") ?: Paths.get(System.getProperty("user.home"), "AppData", "Roaming").toString(), "SwitchGuard")

fun main() {
    val dataDir = dataDir()
    // Ayarlar okunamıyorsa (Store IOException atar) ya da veritabanı açılamıyorsa asla başlanmaz: boş ayarla
    // başlamak asıl dosyanın üzerine yazılmasına yol açabilir.
    val graph = try {
        AppGraph(dataDir)
    } catch (e: Exception) {
        failStartup(dataDir, e)
    }
    // Çıkışta monitoringEnabled değişmez: bir sonraki açılışta izleme (ve kapatılmamış alarm) geri gelir.
    if (graph.store.monitoringEnabled) graph.engine.start()
    application { SwitchGuardApp(graph) }
}

private fun failStartup(dataDir: Path, e: Exception): Nothing {
    Log.w("Main", "startup failed", e)
    val strings = Strings.load("")
    JOptionPane.showMessageDialog(
        null,
        strings.format("startup_failed", dataDir.toString(), e.message ?: e.javaClass.simpleName),
        strings["app_name"],
        JOptionPane.ERROR_MESSAGE,
    )
    exitProcess(1)
}

/** Windows tepsi ipucu 127 karakterle sınırlıdır. */
private fun trayTooltip(appName: String, status: String): String = "$appName — $status".take(120)

@Composable
private fun ApplicationScope.SwitchGuardApp(graph: AppGraph) {
    val appState = graph.appState
    val version by graph.store.version.collectAsState()
    val strings = remember(version) { graph.strings() }
    val monitoring = remember(version) { graph.store.monitoringEnabled }
    val canMonitor = remember(version) { graph.store.hasAnyAccount }
    val status by appState.status.collectAsState()
    var confirmExit by remember { mutableStateOf(false) }

    fun exit() {
        graph.shutdown()
        exitApplication()
    }

    // İzleme açıkken çıkmak alarmların bu bilgisayarda çalmamasına yol açar: onay istenir.
    fun requestExit() {
        if (graph.store.monitoringEnabled) confirmExit = true else exit()
    }

    if (isTraySupported) {
        Tray(
            icon = TrayIcons.painter(status.first),
            state = graph.tray,
            tooltip = trayTooltip(strings["app_name"], status.second),
            onAction = appState::showMainWindow,
            menu = {
                Item(strings["tray_open"], onClick = appState::showMainWindow)
                Item(
                    strings[if (monitoring) "tray_stop" else "tray_start"],
                    enabled = monitoring || canMonitor,
                    onClick = { if (graph.store.monitoringEnabled) graph.engine.stop() else graph.engine.start() },
                )
                Item(strings["test_alarm"], onClick = graph.engine::testAlarm)
                Separator()
                Item(strings["tray_exit"], onClick = ::requestExit)
            },
        )
    }

    // Tek bildirim pompası, Tray'den sonra başlar (Tray'in toplayıcısı hazır olsun). Bildirimler önce kuyruğa girer
    // çünkü TrayState.sendNotification tamponsuz bir kanala yazar: alıcı o an beklemiyorsa bildirim düşer.
    LaunchedEffect(Unit) {
        if (isTraySupported) {
            graph.notifications.pump(NotificationQueue.INITIAL_DELAY_MS, NotificationQueue.SPACING_MS) {
                Log.i("Notifications", "to tray: ${it.message}")
                graph.tray.sendNotification(it)
                true
            }
        } else {
            graph.notifications.pump(0, 0) {
                Log.w("Notifications", "no system tray, notification dropped: ${it.title}: ${it.message}")
                false
            }
        }
    }

    // Tepsi yoksa pencereyi gizlemenin geri dönüşü olmaz: kapatma çıkış sayılır.
    MainWindow(graph, onCloseRequest = { if (isTraySupported) graph.hideMainWindow() else requestExit() })
    AlarmWindow(graph)
    if (confirmExit) {
        ExitConfirmDialog(graph, onConfirm = { confirmExit = false; exit() }, onCancel = { confirmExit = false })
    }
}
