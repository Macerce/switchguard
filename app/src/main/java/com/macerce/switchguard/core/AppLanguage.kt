package com.macerce.switchguard.core

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Uygulama içi dil seçimi. "" = telefonun dili.
 *
 * Android 13+ : sistemin uygulama başına dil desteği (LocaleManager). Seçim sistemde saklanır,
 * telefon ayarlarındaki "Uygulama dilleri" ile aynı şeydir ve açık ekranları sistem yeniler.
 * Android 8–12: seçim kendi tercihimizde saklanır; Application, Activity'ler, servis ve alıcılar
 * [wrap] ile bu dilde açılır.
 */
object AppLanguage {
    val options = listOf("", "en", "tr")

    private const val PREFS = "app_language"
    private const val KEY = "tag"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            val list = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (list.isEmpty) "" else list[0].language
        } else {
            saved(context)
        }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
            activity.recreate()
        }
    }

    /** Android 12 ve altında seçili dili uygulayan bağlam; 13+ ve "sistem dili" için [base]'in kendisi. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = saved(base).ifEmpty { return base }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun saved(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
}
