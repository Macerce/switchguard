package com.macerce.ewelinkalarm.data

import android.content.Context
import android.content.SharedPreferences
import com.macerce.ewelinkalarm.core.DeviceParser
import com.macerce.ewelinkalarm.core.DeviceSnapshot

/** Uygulamanın tüm kalıcı verisi. Uygulamaya özel alanda saklanır, yedeklenmez (allowBackup=false). */
class Store(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ewelink", Context.MODE_PRIVATE)

    // --- Ayarlar ---
    var appId: String
        get() = prefs.getString("appId", "")!!
        set(v) = prefs.edit().putString("appId", v.trim()).apply()
    var appSecret: String
        get() = prefs.getString("appSecret", "")!!
        set(v) = prefs.edit().putString("appSecret", v.trim()).apply()
    var redirectUrl: String
        get() = prefs.getString("redirectUrl", DEFAULT_REDIRECT)!!
        set(v) = prefs.edit().putString("redirectUrl", v.trim().ifEmpty { DEFAULT_REDIRECT }).apply()
    var intervalSec: Int
        get() = prefs.getInt("intervalSec", 30)
        set(v) = prefs.edit().putInt("intervalSec", v.coerceAtLeast(MIN_INTERVAL)).apply()

    // --- Oturum ---
    var region: String
        get() = prefs.getString("region", "eu")!!
        set(v) = prefs.edit().putString("region", v).apply()
    var accessToken: String
        get() = prefs.getString("at", "")!!
        set(v) = prefs.edit().putString("at", v).apply()
    var refreshToken: String
        get() = prefs.getString("rt", "")!!
        set(v) = prefs.edit().putString("rt", v).apply()

    val hasCredentials get() = appId.isNotEmpty() && appSecret.isNotEmpty()
    val isLoggedIn get() = accessToken.isNotEmpty()

    // --- İzleme durumu ---
    var monitoringEnabled: Boolean
        get() = prefs.getBoolean("monitoring", false)
        set(v) = prefs.edit().putBoolean("monitoring", v).apply()

    /** Son bilinen cihaz durumları; boşsa henüz temel alınmış durum yok demektir. */
    var snapshots: List<DeviceSnapshot>
        get() = prefs.getString("snapshots", null)?.let {
            runCatching { DeviceParser.snapshotsFromJson(it) }.getOrNull()
        } ?: emptyList()
        set(v) = prefs.edit().putString("snapshots", DeviceParser.snapshotsToJson(v)).apply()

    /** null = kullanıcı henüz seçim yapmadı → tüm cihazlar izlenir. */
    var monitoredIds: Set<String>?
        get() = prefs.getStringSet("monitored", null)?.toSet()
        set(v) = prefs.edit().putStringSet("monitored", v).apply()

    fun effectiveMonitored(): Set<String> = monitoredIds ?: snapshots.map { it.id }.toSet()

    /** Kullanıcı kapatana kadar alarmda gösterilen satırlar. */
    var pendingAlarm: List<String>
        get() = prefs.getString("pendingAlarm", "")!!.split('\n').filter { it.isNotEmpty() }
        set(v) = prefs.edit().putString("pendingAlarm", v.takeLast(MAX_ALARM_LINES).joinToString("\n")).apply()

    var statusText: String
        get() = prefs.getString("status", "İzleme kapalı")!!
        set(v) = prefs.edit().putString("status", v).apply()

    fun logout() {
        prefs.edit().remove("at").remove("rt").apply()
    }

    companion object {
        const val DEFAULT_REDIRECT = "https://127.0.0.1"
        const val MIN_INTERVAL = 15
        const val MAX_ALARM_LINES = 20
    }
}
