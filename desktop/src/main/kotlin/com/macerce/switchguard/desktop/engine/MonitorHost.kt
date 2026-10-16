package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.data.ConnState

/**
 * [MonitorEngine]'in platform kancaları: telefondaki `Notifier` ve `AlarmActivity`'nin yerine geçer.
 * Masaüstünde tepsi, alarm penceresi ve ses tarafından uygulanır. Tüm yöntemler motorun iş parçacığından
 * çağrılır; arayüze dokunan uygulama kendi iş parçacığına aktarmalıdır.
 */
interface MonitorHost {
    /**
     * Alarmı [lines] (kapatılana kadar biriken satırlar) ile gösterir; [sound] cihaza özel ses, null = genel alarm sesi.
     * [silent]: yalnızca sessiz test alarmı var, pencere açılır ama ses çalmaz.
     */
    fun showAlarm(lines: List<String>, sound: String?, silent: Boolean = false)

    /** Kapatılmamış alarm var: pencere arkada kaldıysa ya da kapandıysa yeniden öne getir (her tick'te çağrılır). */
    fun ensureAlarmVisible()

    fun stopAlarm()

    /** Alarm yerine bildirim; [sound] cihaza özel ses, null = genel bildirim sesi; [title] null ise uygulama adı. */
    fun notify(text: String, sound: String?, title: String? = null)

    /** İzleme uzun süredir cihazları göremiyor (internet/bulut): düzelene kadar kalan uyarı. */
    fun monitorBlind(text: String)

    /** [monitorBlind] uyarısını kaldırır. */
    fun clearBlind()

    /** Oturum düştü: kullanıcı yeniden giriş yapana kadar kalan uyarı. */
    fun sessionLost(text: String, detail: String)

    /** [sessionLost] uyarısını kaldırır. */
    fun clearWarning()

    /** Kaynakların en kötü durumu ve ekranda gösterilecek metin (tepsi simgesi ve ipucu). */
    fun statusChanged(state: ConnState, text: String)

    /** Bilgisayarı uyanık tut / bırak. Motor iş parçacığında çağrılır (Windows durumu iş parçacığına bağlı). */
    fun keepAwake(on: Boolean)
}
