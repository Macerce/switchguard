package com.macerce.switchguard.data

import android.content.Context
import android.content.SharedPreferences
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.QuietHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Uygulamanın tüm kalıcı ayar ve durumu (tek örnek). Uygulamaya özel alanda saklanır.
 * Arayüz, [version] değiştikçe yeniden çizilir.
 */
class Store private constructor(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("switchguard", Context.MODE_PRIVATE)

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    init {
        // Dinleyici Store'un kendisi: SharedPreferences dinleyicileri zayıf referansla tutar ve
        // yalnızca yazılan bir alandaki dinleyiciyi R8 silip çöp toplayıcıya bırakabilir.
        // Store tekil ve güçlü referanslı olduğundan hiç toplanmaz.
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        _version.value++
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

    /** Oturum kullanıcı istemeden düştü (ör. aynı hesapla başka cihazda giriş). Giriş yapınca silinir. */
    var sessionLost: Boolean
        get() = prefs.getBoolean("sessionLost", false)
        set(v) = put { putBoolean("sessionLost", v) }

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
    /** Gerçekten izlenen cihazlar: kuralı açık olanlar, ücretsiz sürümde yalnızca biri (bkz. [Entitlement]). */
    fun monitoredIds(): Set<String> {
        val all = rules
        return Entitlement.monitored(snapshots.map { it.id }, { (all[it] ?: DeviceRules()).monitored }, isPro, freeDeviceId)
    }

    // --- Pro ---
    /** Son bilinen satın alma durumu; Play'e ulaşılamadığında da (çevrimdışı) geçerli kalır. */
    var isPro: Boolean
        get() = BuildConfig.OWNER_UNLOCK || prefs.getBoolean("pro", false)
        set(v) = put { putBoolean("pro", v) }
    /** Ücretsiz sürümde kullanıcının izlemek için seçtiği cihaz. */
    /** Geçmiş sekmesinde görülen son olayın kimliği; sonrakiler "görülmemiş" sayılır. Hiç ayarlanmadıysa null. */
    var historySeenId: Long?
        get() = if (prefs.contains("historySeen")) prefs.getLong("historySeen", 0) else null
        set(v) = put { if (v == null) remove("historySeen") else putLong("historySeen", v) }

    var freeDeviceId: String?
        get() = prefs.getString("freeDevice", null)
        set(v) = put { if (v == null) remove("freeDevice") else putString("freeDevice", v) }

    // --- Otomasyon ---
    var automations: List<Automation>
        get() = prefs.getString("automations", null)?.let {
            runCatching { Automation.listFromJson(it) }.getOrNull()
        } ?: emptyList()
        set(v) = put { putString("automations", Automation.listToJson(v)) }

    fun saveAutomation(a: Automation) {
        val list = automations
        automations = if (list.any { it.id == a.id }) list.map { if (it.id == a.id) a else it } else list + a
    }

    fun deleteAutomation(id: String) { automations = automations.filterNot { it.id == id } }

    // --- Günlük özet ---
    var dailySummaryEnabled: Boolean
        get() = prefs.getBoolean("summaryEnabled", false)
        set(v) = put { putBoolean("summaryEnabled", v) }
    /** Özetin gönderileceği saat (günün dakikası). */
    var dailySummaryMinute: Int
        get() = prefs.getInt("summaryMinute", 21 * 60)
        set(v) = put { putInt("summaryMinute", v) }

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
