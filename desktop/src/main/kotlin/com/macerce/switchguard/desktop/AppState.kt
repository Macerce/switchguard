package com.macerce.switchguard.desktop

import com.macerce.switchguard.data.ConnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Motor iş parçacığı ile arayüz arasındaki canlı durum. Hepsi [StateFlow]'dur: motor ([DesktopHost]) herhangi bir
 * iş parçacığından yazar, Compose arayüzü okur. Alarm ya da pencereyi "öne getir" istekleri artan birer sayaçtır;
 * arayüz sayaç her değiştiğinde pencereyi öne alır (iş parçacığı geçişi gerekmez).
 */
class AppState {
    private val _alarmLines = MutableStateFlow<List<String>>(emptyList())

    /** Kapatılmamış alarmın satırları; boşsa alarm penceresi yok. */
    val alarmLines: StateFlow<List<String>> = _alarmLines.asStateFlow()

    private val _status = MutableStateFlow(ConnState.STOPPED to "")

    /** Kaynakların en kötü durumu ve ekranda gösterilecek metin (tepsi simgesi, ipucu ve Cihazlar başlığı). */
    val status: StateFlow<Pair<ConnState, String>> = _status.asStateFlow()

    /** Ana penceredeki sekme (sol menü). */
    enum class Tab { DEVICES, SETTINGS }

    /** Seçili sekme; pencere yeniden kurulsa da (hata sonrası) korunur. */
    val tab = MutableStateFlow(Tab.DEVICES)

    /** Ana pencere görünür mü; kapatma düğmesi gizler, tepsi "Aç" yeniden gösterir. */
    val windowVisible = MutableStateFlow(true)

    private val _warning = MutableStateFlow<String?>(null)

    /** Kullanıcı yeniden giriş yapana kadar kalan oturum uyarısı; yoksa null. */
    val warning: StateFlow<String?> = _warning.asStateFlow()

    private val _alarmRaise = MutableStateFlow(0)
    val alarmRaise: StateFlow<Int> = _alarmRaise.asStateFlow()

    private val _mainRaise = MutableStateFlow(0)
    val mainRaise: StateFlow<Int> = _mainRaise.asStateFlow()

    fun setAlarmLines(lines: List<String>) {
        _alarmLines.value = lines
    }

    fun clearAlarm() {
        _alarmLines.value = emptyList()
    }

    fun setStatus(state: ConnState, text: String) {
        _status.value = state to text
    }

    fun setWarning(text: String?) {
        _warning.value = text
    }

    /** Alarm penceresini öne getirmesini ister (her tick'te çağrılabilir). */
    fun raiseAlarmWindow() = _alarmRaise.update { it + 1 }

    /** Ana pencereyi gösterir ve öne getirir (tepsi "Aç", tepsi simgesine çift tık; ileride ikinci örnek isteği). */
    fun showMainWindow() {
        windowVisible.value = true
        _mainRaise.update { it + 1 }
    }
}
