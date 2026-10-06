package com.macerce.switchguard.desktop.platform

import androidx.compose.ui.window.Notification
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.engine.MonitorHost
import com.macerce.switchguard.desktop.i18n.Strings

/**
 * [MonitorHost]'un masaüstü uygulaması: alarm penceresi ([AppState.alarmLines]) + ses, Windows bildirimi (tepsi) ve
 * durum. Yöntemler motor iş parçacığından çağrılır; dokunulan her şey iş parçacığı güvenlidir
 * ([AppState]'in akışları, [NotificationQueue] sınırsız bir kanala yazar, [SoundPlayer] kendi kilidini kullanır).
 * Bildirim metni hemen sıraya alınır (tepsiye aralıklı iletilir), ses ise hemen çalar.
 *
 * Alarm sesi telefondaki `Notifier.startSound` gibi kendini toparlar: başlatılamazsa (o an ses cihazı yok) ya da
 * sonradan durursa (cihaz kayboldu) [ensureAlarmVisible] (alarm sürdükçe her tick) yeniden başlatır. Alarm ayrıca
 * ekranı uyandırır ([display]): kapalı ekranda tam ekran alarm görünmez, monitör hoparlörü de uykuda kalır.
 */
class DesktopHost(
    private val store: Store,
    private val sounds: AlarmSound,
    private val state: AppState,
    private val notifications: NotificationQueue,
    /** Her çağrıda o anki dil (bkz. AppGraph.strings). */
    private val strings: () -> Strings,
    private val display: DisplayWaker = DisplayWake,
) : MonitorHost {

    /** Alarm sesi ve ses düzeyi. */
    private data class Ringing(val sound: String?, val volume: Float)

    /** Alarm sürdükçe çalması gereken ses; alarm kapatılınca null. */
    private var wanted: Ringing? = null

    /** Kullanıcının kapattığı ama motorun henüz temizlemediği satırlar ([silenceNow]); yoksa null. */
    private var silenced: List<String>? = null

    /** Gerçekten çalmaya başlamış ses (yalnızca `loop` başarılıysa); aynı ses yeni satırla baştan başlayıp tıkırdamasın. */
    private var ringing: Ringing? = null

    @Synchronized
    override fun showAlarm(lines: List<String>, sound: String?) {
        silenced = null
        state.setAlarmLines(lines)
        ring(Ringing(sound ?: store.alarmSoundUri, store.alarmVolume))
        display.wake()
        state.raiseAlarmWindow()
    }

    @Synchronized
    override fun ensureAlarmVisible() {
        // Pencere hiç yoksa (satırlar boş) ama kapatılmamış alarm duruyorsa yeniden kur; yoksa öne getir.
        val pending = store.pendingAlarm
        // Kullanıcı az önce kapattı, motor temizliği yolda: geri getirme.
        if (pending == silenced) return
        if (state.alarmLines.value.isEmpty() && pending.isNotEmpty()) {
            showAlarm(pending, null)
            return
        }
        // Ses bekçisi: hiç başlayamadıysa ya da sonradan durduysa (ses cihazı değişti) yeniden başlat.
        ring(wanted ?: Ringing(store.alarmSoundUri, store.alarmVolume))
        display.keepOn()
        state.raiseAlarmWindow()
    }

    /** [w] çalmıyorsa başlatır; [ringing] yalnızca başarıda kaydedilir, başarısızsa sonraki tick yeniden dener. */
    private fun ring(w: Ringing) {
        val previous = wanted
        wanted = w
        if (ringing == w && sounds.isLooping()) return
        // Aynı alarm sürerken ses çalmıyorsa (önceki deneme başarısız ya da klip durdu): bekçi devrede.
        if (previous == w) Log.w(TAG, "alarm sound is not playing; restarting it")
        ringing = if (sounds.loop(w.sound, w.volume)) w else null
        if (ringing == null) Log.w(TAG, "alarm sound could not start; retrying on the next tick")
    }

    /**
     * Arayüzden "Kapat": [lines] (ekranda görünen satırlar) için ses ve pencere **hemen** kapanır; kalıcı temizliği
     * motor ([MonitorEngine.dismissAlarm]) sonra yapar. O arada gelen tick, aynı satırları yeniden açmasın diye
     * [silenced] tutulur; yeni bir satır ([showAlarm]) bu işareti kaldırır.
     */
    @Synchronized
    fun silenceNow(lines: List<String>) {
        silenced = lines
        wanted = null
        ringing = null
        sounds.stop()
        state.clearAlarm()
    }

    @Synchronized
    override fun stopAlarm() {
        silenced = null
        wanted = null
        ringing = null
        sounds.stop()
        state.clearAlarm()
    }

    override fun notify(text: String, sound: String?) {
        notifications.enqueue(Notification(strings()["app_name"], text, Notification.Type.Info))
        // Cihaza özel ses yoksa masaüstünün bildirim sesi (o da yoksa yerleşik varsayılan).
        sounds.once(sound ?: store.notifySoundUri, store.alarmVolume)
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

    /** Motor iş parçacığından çağrılır; Windows uyku engelini iş parçacığı başına tuttuğu için burada iş parçacığı değiştirilmez. */
    override fun keepAwake(on: Boolean) {
        KeepAwake.set(on)
    }

    private companion object {
        const val TAG = "DesktopHost"
    }
}
