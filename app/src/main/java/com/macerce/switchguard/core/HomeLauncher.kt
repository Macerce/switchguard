package com.macerce.switchguard.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.macerce.switchguard.data.Store

/**
 * Duvar panosu kullanımı: SwitchGuard isteğe bağlı olarak ana ekran (launcher) olabilir ve cihaz yeniden
 * açılınca bekleme ekranını kendiliğinden gösterebilir.
 *
 * Ana ekran adaylığı varsayılan olarak kapalı bir activity-alias'tadır; açılmadıkça telefon ana ekran
 * seçicisinde SwitchGuard görünmez.
 */
object HomeLauncher {
    private fun alias(context: Context) = ComponentName(context.packageName, "com.macerce.switchguard.ui.HomeAlias")

    /** Ana ekran adaylığı açık mı (seçicide görünür mü). */
    fun isOffered(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(alias(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setOffered(context: Context, on: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            alias(context),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** Ana ekran tuşu şu an SwitchGuard'ı mı açıyor. */
    fun isDefault(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val r = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return r?.activityInfo?.packageName == context.packageName
    }

    /** Sistemin "Varsayılan ana ekran uygulaması" sayfası; yoksa genel ayarlar. */
    fun openChooser(context: Context) {
        val tries = listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS)
        for (a in tries) {
            if (runCatching { context.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    /**
     * Bu açılıştan sonra ilk kez mi soruluyor. Ana ekran yolu ile açılış yayını yarışır; hangisi önce
     * gelirse bekleme ekranını o açar, diğeri açmaz.
     */
    @Synchronized
    fun firstSinceBoot(context: Context): Boolean {
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        val store = Store.get(context)
        // Sayaç okunamazsa (çok eski Android): açılıştan sonraki ilk 10 dakikada bir kez.
        if (boot < 0) {
            val fresh = android.os.SystemClock.elapsedRealtime() < 10 * 60_000L
            val bootAt = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
            val key = (bootAt / 60_000L).toInt()
            if (!fresh || store.standbyBootSeen == key) return false
            store.standbyBootSeen = key
            return true
        }
        if (store.standbyBootSeen == boot) return false
        store.standbyBootSeen = boot
        return true
    }
}
