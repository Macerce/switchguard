package com.macerce.switchguard.desktop.platform

import androidx.compose.ui.window.Notification
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.engine.MonitorHost
import com.macerce.switchguard.desktop.i18n.Strings

/**
 * [MonitorHost]'un masaüstü uygulaması: alarm penceresi ([AppState.alarmLines]) + ses, Windows bildirimi (tepsi) ve
 * durum. Yöntemler motor iş parçacığından çağrılır; dokunulan her şey iş parçacığı güvenlidir
 * ([AppState]'in akışları, [NotificationQueue] sınırsız bir kanala yazar, [SoundPlayer] kendi kilidini kullanır).
 * Bildirim metni hemen sıraya alınır (tepsiye aralıklı iletilir), ses ise hemen çalar.
 */
class DesktopHost(
    private val store: Store,
    private val sounds: SoundPlayer,
    private val state: AppState,
    private val notifications: NotificationQueue,
    /** Her çağrıda o anki dil (bkz. AppGraph.strings). */
    private val strings: () -> Strings,
) : MonitorHost {

    /** Çalmakta olan alarm sesi; aynı ses yeni bir alarm satırıyla baştan başlatılıp tıkırdamasın diye. */
    private data class Ringing(val sound: String?, val volume: Float)

    private var ringing: Ringing? = null

    @Synchronized
    override fun showAlarm(lines: List<String>, sound: String?) {
        state.setAlarmLines(lines)
        val wanted = Ringing(sound ?: store.alarmSoundUri, store.alarmVolume)
        if (ringing != wanted) {
            ringing = wanted
            sounds.loop(wanted.sound, wanted.volume)
        }
        state.raiseAlarmWindow()
    }

    @Synchronized
    override fun ensureAlarmVisible() {
        // Pencere hiç yoksa (satırlar boş) ama kapatılmamış alarm duruyorsa yeniden kur; yoksa yalnızca öne getir.
        val pending = store.pendingAlarm
        if (state.alarmLines.value.isEmpty() && pending.isNotEmpty()) showAlarm(pending, null) else state.raiseAlarmWindow()
    }

    @Synchronized
    override fun stopAlarm() {
        ringing = null
        sounds.stop()
        state.clearAlarm()
    }

    override fun notify(text: String, sound: String?) {
        notifications.enqueue(Notification(strings()["app_name"], text, Notification.Type.Info))
        sounds.once(sound, store.alarmVolume)
    }

    override fun sessionLost(text: String, detail: String) {
        state.setWarning(listOf(text, detail).filter { it.isNotEmpty() }.joinToString("\n"))
        notifications.enqueue(Notification(strings()["app_name"], text, Notification.Type.Warning))
    }

    override fun clearWarning() {
        state.setWarning(null)
    }

    override fun statusChanged(state: ConnState, text: String) {
        this.state.setStatus(state, text)
    }

    override fun keepAwake(on: Boolean) {
        // Görev 10'a kadar boş: SetThreadExecutionState orada bağlanır.
    }
}
