package com.macerce.switchguard.data

import android.content.Context
import android.content.SharedPreferences
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.QuietHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Uygulamanın tüm kalıcı ayar ve durumu (tek örnek). Uygulamaya özel alanda saklanır.
 * Arayüz, [version] değiştikçe yeniden çizilir.
 */
class Store private constructor(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("switchguard", Context.MODE_PRIVATE)

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    // Güçlü referans: SharedPreferences dinleyicileri zayıf referansla tutar.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _version.value++ }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun str(key: String, def: String = "") = prefs.getString(key, def)!!
    private fun put(block: SharedPreferences.Editor.() -> Unit) = prefs.edit().apply(block).apply()

    // --- Hesap ---
    var appId: String
        get() = str("appId")
        set(v) = put { putString("appId", v.trim()) }
    var appSecret: String
        get() = str("appSecret")
        set(v) = put { putString("appSecret", v.trim()) }
    var redirectUrl: String
        get() = str("redirectUrl", DEFAULT_REDIRECT)
        set(v) = put { putString("redirectUrl", v.trim().ifEmpty { DEFAULT_REDIRECT }) }
    var region: String
        get() = str("region", "eu")
        set(v) = put { putString("region", v) }
    var accessToken: String
        get() = str("at")
        set(v) = put { putString("at", v) }
    var refreshToken: String
        get() = str("rt")
        set(v) = put { putString("rt", v) }
    /** WebSocket el sıkışması için kullanıcının apikey'i. */
    var userApiKey: String
        get() = str("apikey")
        set(v) = put { putString("apikey", v) }
    var accountEmail: String
        get() = str("email")
        set(v) = put { putString("email", v) }

    val hasCredentials get() = appId.isNotEmpty() && appSecret.isNotEmpty()
    val isLoggedIn get() = accessToken.isNotEmpty()

    fun logout() = put { remove("at"); remove("rt"); remove("apikey"); remove("email") }

    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboardingDone", false)
        set(v) = put { putBoolean("onboardingDone", v) }

    // --- İzleme ---
    var monitoringEnabled: Boolean
        get() = prefs.getBoolean("monitoring", false)
        set(v) = put { putBoolean("monitoring", v) }
    /** Açıkken işlemci uyanık tutulur: en güvenilir, en çok pil harcayan mod. */
    var alwaysAwake: Boolean
        get() = prefs.getBoolean("alwaysAwake", false)
        set(v) = put { putBoolean("alwaysAwake", v) }
    /** WebSocket kullanılamazsa yedek sorgulama aralığı. */
    var pollIntervalSec: Int
        get() = prefs.getInt("pollIntervalSec", 30)
        set(v) = put { putInt("pollIntervalSec", v.coerceAtLeast(MIN_POLL)) }
    /** Offline olayını bu kadar bekletip kısa kopmaları yok say (0 = hemen). */
    var offlineGraceSec: Int
        get() = prefs.getInt("offlineGraceSec", 0)
        set(v) = put { putInt("offlineGraceSec", v.coerceAtLeast(0)) }

    // --- Alarm ---
    /** null = sistemin varsayılan alarm sesi. */
    var alarmSoundUri: String?
        get() = prefs.getString("alarmSound", null)
        set(v) = put { putString("alarmSound", v) }
    var alarmVolume: Float
        get() = prefs.getFloat("alarmVolume", 1f)
        set(v) = put { putFloat("alarmVolume", v.coerceIn(0.05f, 1f)) }
    var vibrate: Boolean
        get() = prefs.getBoolean("vibrate", true)
        set(v) = put { putBoolean("vibrate", v) }
    var quietHours: QuietHours
        get() = QuietHours(
            prefs.getBoolean("quietEnabled", false),
            prefs.getInt("quietStart", 23 * 60),
            prefs.getInt("quietEnd", 7 * 60),
        )
        set(v) = put {
            putBoolean("quietEnabled", v.enabled); putInt("quietStart", v.startMinute); putInt("quietEnd", v.endMinute)
        }

    // --- Cihazlar ---
    /** Son bilinen cihaz durumları; boşsa henüz temel alınmış durum yok demektir. */
    var snapshots: List<DeviceSnapshot>
        get() = prefs.getString("snapshots", null)?.let {
            runCatching { DeviceParser.snapshotsFromJson(it) }.getOrNull()
        } ?: emptyList()
        set(v) = put { putString("snapshots", DeviceParser.snapshotsToJson(v)) }

    var rules: Map<String, DeviceRules>
        get() = prefs.getString("rules", null)?.let {
            runCatching { DeviceRules.mapFromJson(it) }.getOrNull()
        } ?: emptyMap()
        set(v) = put { putString("rules", DeviceRules.mapToJson(v)) }

    fun rulesFor(deviceId: String) = rules[deviceId] ?: DeviceRules()
    fun setRules(deviceId: String, r: DeviceRules) { rules = rules + (deviceId to r) }
    fun monitoredIds(): Set<String> = snapshots.map { it.id }.filter { rulesFor(it).monitored }.toSet()

    /** Kullanıcı kapatana kadar alarmda gösterilen satırlar. */
    var pendingAlarm: List<String>
        get() = str("pendingAlarm").split('\n').filter { it.isNotEmpty() }
        set(v) = put { putString("pendingAlarm", v.takeLast(MAX_ALARM_LINES).joinToString("\n")) }

    companion object {
        const val DEFAULT_REDIRECT = "https://127.0.0.1"
        const val MIN_POLL = 15
        const val MAX_ALARM_LINES = 20

        @Volatile private var instance: Store? = null
        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context).also { instance = it } }
    }
}
