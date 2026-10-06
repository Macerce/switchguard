package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.data.ConnState

/**
 * [MonitorEngine]'in platform kancaları: telefondaki `Notifier` ve `AlarmActivity`'nin yerine geçer.
 * Masaüstünde tepsi, alarm penceresi ve ses tarafından uygulanır. Tüm yöntemler motorun iş parçacığından
 * çağrılır; arayüze dokunan uygulama kendi iş parçacığına aktarmalıdır.
 */
interface MonitorHost {
    /** Alarmı [lines] (kapatılana kadar biriken satırlar) ile gösterir; [sound] cihaza özel ses, null = genel alarm sesi. */
    fun showAlarm(lines: List<String>, sound: String?)

    /** Kapatılmamış alarm var: pencere arkada kaldıysa ya da kapandıysa yeniden öne getir (her tick'te çağrılır). */
    fun ensureAlarmVisible()

    fun stopAlarm()

    /** Alarm yerine bildirim; [sound] cihaza özel ses, null = genel bildirim sesi. */
    fun notify(text: String, sound: String?)

    /** Oturum düştü: kullanıcı yeniden giriş yapana kadar kalan uyarı. */
    fun sessionLost(text: String, detail: String)

    /** [sessionLost] uyarısını kaldırır. */
    fun clearWarning()

    /** Kaynakların en kötü durumu ve ekranda gösterilecek metin (tepsi simgesi ve ipucu). */
    fun statusChanged(state: ConnState, text: String)

    /** Bilgisayarı uyanık tut / bırak. Motor iş parçacığında çağrılır (Windows durumu iş parçacığına bağlı). */
    fun keepAwake(on: Boolean)
}
