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

    /** Ana penceredeki sekme (sol menü; telefon 2.13 ile aynı sıra). */
    enum class Tab { DEVICES, ALERTS, AUTOMATIONS, HISTORY, SETTINGS }

    /** Seçili sekme; pencere yeniden kurulsa da (hata sonrası) korunur. */
    val tab = MutableStateFlow(Tab.DEVICES)

    /** Açık cihaz sayfası (Cihazlar sekmesinin üstünde); null = liste. Telefondaki `openDeviceId`. */
    val openDevice = MutableStateFlow<String?>(null)

    /** Ayarlar sekmesine yeniden tıklandı: alt sayfadan kategori listesine dönülür (her tıklamada artar). */
    val settingsHome = MutableStateFlow(0)

    /** Uyarılar sekmesine yeniden tıklandı: alt sayfadan (kurallar, düzenleyici, alarm ayarları) sekmenin başına dönülür. */
    val alertsHome = MutableStateFlow(0)

    /** Başka bir ekrandan istenen Ayarlar alt sayfası (`SettingsPage` adı, ör. "ACCOUNTS"); açılınca null'a döner. */
    val settingsOpen = MutableStateFlow<String?>(null)

    /** Ayarlar sekmesine geçip [page] alt sayfasını açar (ör. Cihazlar'daki "Hesap ekle", "Yeniden giriş yap"). */
    fun openSettings(page: String) {
        settingsOpen.value = page
        tab.value = Tab.SETTINGS
    }

    /** Sol menüden sekme seçimi: seçili sekmeye yeniden tıklamak o sekmenin başına döner. */
    fun select(t: Tab) {
        if (t == tab.value && t == Tab.SETTINGS) settingsHome.update { it + 1 }
        if (t == tab.value && t == Tab.ALERTS) alertsHome.update { it + 1 }
        if (t == Tab.DEVICES) openDevice.value = null
        tab.value = t
    }

    /** Ana pencere görünür mü; kapatma düğmesi gizler, tepsi "Aç" yeniden gösterir. */
    val windowVisible = MutableStateFlow(true)

    private val _warning = MutableStateFlow<String?>(null)

    /** Kullanıcı yeniden giriş yapana kadar kalan oturum uyarısı; yoksa null. */
    val warning: StateFlow<String?> = _warning.asStateFlow()

    private val _blind = MutableStateFlow<String?>(null)

    /** İzleme uzun süredir cihazları göremiyor: düzelene kadar kalan uyarı metni; yoksa null. */
    val blind: StateFlow<String?> = _blind.asStateFlow()

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

    fun setBlind(text: String?) {
        _blind.value = text
    }

    /** Alarm penceresini öne getirmesini ister (her tick'te çağrılabilir). */
    fun raiseAlarmWindow() = _alarmRaise.update { it + 1 }

    /** Ana pencereyi gösterir ve öne getirir (tepsi "Aç", tepsi simgesine çift tık; ileride ikinci örnek isteği). */
    fun showMainWindow() {
        windowVisible.value = true
        _mainRaise.update { it + 1 }
    }
}
