package com.macerce.switchguard.desktop

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.DpapiCodec
import com.macerce.switchguard.desktop.engine.MonitorEngine
import com.macerce.switchguard.desktop.engine.Sources
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.DesktopHost
import com.macerce.switchguard.desktop.platform.SoundPlayer
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Uygulamanın tüm bağımlılıkları, [dataDir] (`%APPDATA%\SwitchGuard`) altında. Kurucu hata verebilir: ayarlar
 * okunamıyorsa ([Store] IOException atar) ya da veritabanları açılamıyorsa fırlatır; çağıran bir hata penceresi
 * gösterip çıkmalıdır, ayarlar olmadan hiçbir zaman başlanmaz.
 */
class AppGraph(val dataDir: Path) {
    init {
        Log.init(dataDir)
    }

    val store = Store(dataDir.resolve("settings.json"), DpapiCodec())
    val eventLog = EventLog(dataDir.resolve("events.db"))
    val metrics: MetricsLog = try {
        MetricsLog(dataDir.resolve("metrics.db"))
    } catch (e: Exception) {
        eventLog.close()
        throw e
    }

    val sounds = SoundPlayer()
    val appState = AppState()
    val tray = TrayState()

    private val stringsLock = Any()
    private var loadedLanguage = store.language
    private var current = Strings.load(loadedLanguage)

    val host = DesktopHost(store, sounds, appState, tray, ::strings)
    val engine = MonitorEngine(store, eventLog, metrics, ::strings, host, Sources.real(store))

    init {
        appState.setStatus(ConnState.STOPPED, strings()["state_stopped"])
    }

    /** Seçili dilin metinleri; tek örnek tutulur, yalnızca dil ayarı değişince yeniden yüklenir. */
    fun strings(): Strings = synchronized(stringsLock) {
        val language = store.language
        if (language != loadedLanguage) {
            current = Strings.load(language)
            loadedLanguage = language
        }
        current
    }

    /** Ana pencere kapatıldı: gizlenir, izleme sürer; ilk seferde (kurulum başına bir kez) tepsi ipucu gösterilir. */
    fun hideMainWindow() {
        appState.windowVisible.value = false
        if (!claimTrayHint()) return
        val s = strings()
        tray.sendNotification(Notification(s["app_name"], s["tray_minimized_hint"], Notification.Type.Info))
    }

    private var trayHintClaimed = false

    /**
     * İpucu gösterilecekse true: bu çalışmada ilk istek ve işaret dosyası yok. İşaret dosyası yazılamazsa bile
     * aynı çalışmada ikinci kez gösterilmez.
     */
    @Synchronized
    internal fun claimTrayHint(): Boolean {
        if (trayHintClaimed) return false
        trayHintClaimed = true
        val marker = dataDir.resolve(TRAY_HINT_MARKER)
        if (Files.exists(marker)) return false
        try {
            Files.createFile(marker)
        } catch (e: IOException) {
            Log.w(TAG, "could not write $marker", e)
        }
        return true
    }

    /**
     * Uygulamadan çıkış: motor kapanır (`monitoringEnabled` değişmez, bir sonraki açılışta izleme ve alarm geri
     * gelir), ses susar, günlük veritabanları kapanır. Motor kapandıktan sonra çağrılır; veritabanları yalnızca
     * burada kapatılır (kapandıktan sonraki çağrılar fırlatır).
     */
    fun shutdown() {
        engine.shutdown()
        sounds.stop()
        runCatching { eventLog.close() }
        runCatching { metrics.close() }
    }

    private companion object {
        const val TAG = "AppGraph"
        const val TRAY_HINT_MARKER = "tray-hint-shown"
    }
}
