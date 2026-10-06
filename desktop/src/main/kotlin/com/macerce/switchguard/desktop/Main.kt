package com.macerce.switchguard.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import com.macerce.switchguard.desktop.data.SqliteNative
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Autostart
import com.macerce.switchguard.desktop.platform.NotificationQueue
import com.macerce.switchguard.desktop.platform.SingleInstance
import com.macerce.switchguard.desktop.platform.TrayIcons
import com.macerce.switchguard.desktop.ui.AlarmWindow
import com.macerce.switchguard.desktop.ui.ExitConfirmDialog
import com.macerce.switchguard.desktop.ui.MainWindow
import com.macerce.switchguard.desktop.ui.windowErrorFactory
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** Ayarlar, günlükler ve veritabanları: `%APPDATA%\SwitchGuard`. */
private fun dataDir(): Path =
    Paths.get(System.getenv("APPDATA") ?: Paths.get(System.getProperty("user.home"), "AppData", "Roaming").toString(), "SwitchGuard")

/** Oturum açılışında Run değerinin eklediği bayrak: pencere gizli, yalnızca tepsi simgesiyle başlar. */
private const val TRAY_FLAG = "--tray"

fun main(args: Array<String>) {
    val dataDir = dataDir()
    Log.init(dataDir)
    // javaw'ın konsolu yok: hiçbir iş parçacığının hatası iz bırakmadan kaybolmasın.
    CrashLog.install()

    // Tek örnek kilidi AppGraph'tan ÖNCE alınır: ikinci örnek veritabanlarını açmaz, ikinci bir motor başlatmaz;
    // yalnızca ilk örneğe "göster" der ve çıkar. Kilit alınamıyorsa (dizin/dosya hatası) asla başlanmaz.
    val showRelay = ShowRelay()
    val instance = SingleInstance(dataDir)
    val owner = try {
        instance.acquire(showRelay::request)
    } catch (e: Throwable) {
        failStartup(dataDir, e)
    }
    if (!owner) {
        Log.i("Main", "another instance is already running, exiting")
        exitProcess(0)
    }
    // Süreç her zaman `exitProcess` ile biter; kilit ve port dosyası bu yüzden kapanış kancasında bırakılır. Kilit süreç
    // ölünce zaten işletim sistemince bırakılır, kanca port dosyasını siler.
    Runtime.getRuntime().addShutdownHook(Thread(instance::release, "single-instance-release"))

    // sqlite-jdbc'nin DLL'i %TEMP% yerine veri klasörüne çıkarılsın (ilk veritabanı bağlantısından önce); klasör
    // kurulamazsa sqlite-jdbc'nin varsayılanı (%TEMP%) kalır.
    runCatching { SqliteNative.useDataDir(dataDir) }
        .onFailure { Log.w("Main", "could not prepare the native library folder; sqlite-jdbc uses %TEMP%", it) }

    // Ayarlar okunamıyorsa (Store IOException atar) ya da veritabanı açılamıyorsa asla başlanmaz: boş ayarla
    // başlamak asıl dosyanın üzerine yazılmasına yol açabilir.
    val graph = try {
        AppGraph(dataDir)
    } catch (e: Throwable) {
        failStartup(dataDir, e)
    }
    // Tepsi yoksa gizli başlamanın geri dönüşü olmaz (bkz. SwitchGuardApp'teki kapatma kuralı): bayrak yok sayılır.
    if (TRAY_FLAG in args && isTraySupported) graph.appState.windowVisible.value = false
    // Gizli başlangıçtan sonra bağlanır: hazırlık sırasında gelen "göster" isteği pencereyi yine açar.
    showRelay.bind(graph.appState::showMainWindow)

    syncAutostart(Autostart(), graph.store.autostart)

    // Çıkışta monitoringEnabled değişmez: bir sonraki açılışta izleme (ve kapatılmamış alarm) geri gelir.
    if (graph.store.monitoringEnabled) graph.engine.start()
    // Pencere dışındaki bir arayüz hatası (tepsi, uygulama düzeyi) arayüzü durdurur: arayüz aynı süreçte yeniden kurulur,
    // motor bu sırada çalışmaya devam eder (bkz. runUi). Normal çıkışta (exit()) motor zaten kapatılmıştır.
    if (runUi(UiRestarts()) { application(exitProcessOnExit = false) { SwitchGuardApp(graph, instance) } }) exitProcess(0)
    // Arayüz kısa sürede tekrar tekrar çöktü: yarı ölü bir süreç bırakma; çıkıştaki gibi kapat, kodla çık. Bir sonraki
    // açılış izlemeyi ve kapatılmamış alarmı geri getirir.
    instance.markExiting()
    runCatching { graph.shutdown() }.onFailure { Log.w("Main", "shutdown after the UI errors failed", it) }
    exitProcess(1)
}

/**
 * Windows ile başlatma ayarını kayıt defteriyle eşitler: açıksa değer yoksa yazılır, yanlış yolu varsa
 * (yeniden kurulum) düzeltilir; kapalıysa silinir. Kurulu uygulama değilsek (`./gradlew run`) hiç dokunulmaz.
 */
internal fun syncAutostart(autostart: Autostart, enabled: Boolean) {
    if (!autostart.available) return
    autostart.setEnabled(enabled)
}

/**
 * İkinci örneğin "göster" isteği, [AppGraph] daha kurulurken gelebilir: hazır olana kadar bekletilir, [bind] ile
 * hedef verilince uygulanır. İstek dinleyici iş parçacığından gelir; [AppState.showMainWindow] yalnızca
 * [kotlinx.coroutines.flow.StateFlow]'lara yazar, iş parçacığı geçişi gerekmez.
 */
private class ShowRelay {
    private var target: (() -> Unit)? = null
    private var pending = false

    @Synchronized
    fun request() {
        val t = target
        if (t == null) pending = true else t()
    }

    @Synchronized
    fun bind(t: () -> Unit) {
        target = t
        if (pending) {
            pending = false
            t()
        }
    }
}

private fun failStartup(dataDir: Path, e: Throwable): Nothing {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ApplicationScope.SwitchGuardApp(graph: AppGraph, instance: SingleInstance) {
    val appState = graph.appState
    val version by graph.store.version.collectAsState()
    val strings = remember(version) { graph.strings() }
    val monitoring = remember(version) { graph.store.monitoringEnabled }
    val canMonitor = remember(version) { graph.store.hasAnyAccount }
    val status by appState.status.collectAsState()
    val alarmLines by appState.alarmLines.collectAsState()
    var confirmExit by remember { mutableStateOf(false) }

    fun exit() {
        // İlk iş: bu andan sonra açılan yeni örnek "göster" ile kapanan bu örneğe güvenmesin, kilidi bekleyip sahip olsun.
        instance.markExiting()
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
                for (action in trayActions(alarmPending = alarmLines.isNotEmpty())) when (action) {
                    TrayAction.DISMISS_ALARM -> Item(strings["tray_dismiss_alarm"], onClick = graph::dismissAlarm)
                    TrayAction.OPEN -> Item(strings["tray_open"], onClick = appState::showMainWindow)
                    TrayAction.MONITORING -> Item(
                        strings[if (monitoring) "tray_stop" else "tray_start"],
                        enabled = monitoring || canMonitor,
                        onClick = { if (graph.store.monitoringEnabled) graph.engine.stop() else graph.engine.start() },
                    )
                    TrayAction.TEST_ALARM -> Item(strings["test_alarm"], onClick = graph.engine::testAlarm)
                    TrayAction.EXIT -> {
                        Separator()
                        Item(strings["tray_exit"], onClick = ::requestExit)
                    }
                }
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

    // Pencerelerdeki hatalar (Compose varsayılanı: hata penceresi + pencereyi kapat + yeniden fırlat) günlüğe yazılır,
    // asla yeniden fırlatılmaz; ana pencere ve alarm penceresi ayrıca kendilerini yeniden kurar (bkz. WindowErrors).
    val windowErrors = remember { windowErrorFactory("app") {} }
    val dialogErrors = remember { windowErrorFactory("exit-confirm") { confirmExit = false } }
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides windowErrors) {
        // Tepsi yoksa pencereyi gizlemenin geri dönüşü olmaz: kapatma çıkış sayılır.
        MainWindow(graph, onCloseRequest = { if (isTraySupported) graph.hideMainWindow() else requestExit() })
        AlarmWindow(graph)
        if (confirmExit) {
            CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides dialogErrors) {
                ExitConfirmDialog(graph, onConfirm = { confirmExit = false; exit() }, onCancel = { confirmExit = false })
            }
        }
    }
}
