package com.macerce.switchguard.data

import android.content.Context
import android.content.SharedPreferences
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.ParseCache
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.core.SecretValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Uygulamanın tüm kalıcı ayar ve durumu (tek örnek). Uygulamaya özel alanda saklanır.
 * Arayüz, [version] değiştikçe yeniden çizilir.
 */
/** [com.macerce.switchguard.core.TypedPrefs.decode] çıktısındaki bir değeri türüne göre yazar. */
fun SharedPreferences.Editor.putTyped(key: String, value: Any): SharedPreferences.Editor = when (value) {
    is String -> putString(key, value)
    is Boolean -> putBoolean(key, value)
    is Int -> putInt(key, value)
    is Long -> putLong(key, value)
    is Float -> putFloat(key, value)
    else -> this
}

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

    /** JSON ayarların çözülmüş halleri: her okumada (her canlı olayda defalarca) yeniden çözülmesin. */
    private val parsed = ParseCache()

    /** Gizli değerler Android Keystore anahtarıyla şifreli saklanır (bkz. [SecretValues]). */
    private val secrets = SecretValues(SecretBox::encrypt, SecretBox::decrypt)
    private fun secret(key: String): String = secrets.read(str(key)) { sealed -> prefs.edit().putString(key, sealed).apply() }
    private fun putSecret(key: String, value: String) = put { if (value.isEmpty()) remove(key) else putString(key, secrets.write(value)) }

    /**
     * Tüm ayarlar, gizliler çözülmüş olarak (yedek ve cihazlar arası aktarım için): şifreli hal bu cihazın
     * anahtarına bağlıdır, başka cihazda açılamaz. Dosya/aktarım kendi şifrelemesini kullanır.
     */
    fun plainPrefs(): Map<String, Any?> = prefs.all.mapValues { (k, v) -> if (k in SECRET_KEYS && v is String) secret(k) else v }

    /** Dışarıdan düz yazılmış gizlileri (geri yükleme, aktarım) şifreler. */
    fun sealSecrets() = SECRET_KEYS.forEach { secret(it) }

    // --- Hesap ---
    var appId: String
        get() = str("appId")
        set(v) = put { putString("appId", v.trim()) }
    var appSecret: String
        get() = secret("appSecret")
        set(v) = putSecret("appSecret", v.trim())
    var redirectUrl: String
        get() = str("redirectUrl", DEFAULT_REDIRECT)
        set(v) = put { putString("redirectUrl", v.trim().ifEmpty { DEFAULT_REDIRECT }) }
    var region: String
        get() = str("region", "eu")
        set(v) = put { putString("region", v) }
    var accessToken: String
        get() = secret("at")
        set(v) = putSecret("at", v)
    var refreshToken: String
        get() = secret("rt")
        set(v) = putSecret("rt", v)
    /** WebSocket el sıkışması için kullanıcının apikey'i. */
    var userApiKey: String
        get() = secret("apikey")
        set(v) = putSecret("apikey", v)
    var accountEmail: String
        get() = str("email")
        set(v) = put { putString("email", v) }

    /** Bu cihaz başka bir cihazdan kurulum aldı mı (kurulum sihirbazında "İleri" için). */
    var transferReceived: Boolean
        get() = prefs.getBoolean("transferReceived", false)
        set(v) = put { putBoolean("transferReceived", v) }

    /** Kurulumu başka cihazdan aktarıldıysa, gönderen cihazın eWeLink hesabı (aynı hesapla giriş uyarısı için). */
    var transferSource: String
        get() = str("transferSource")
        set(v) = put { putString("transferSource", v) }

    val hasCredentials get() = appId.isNotEmpty() && appSecret.isNotEmpty()
    /** Şifre çözmeden bakılır (sık çağrılır); çözülemeyen token ilk istekte oturum hatasına döner. */
    val isLoggedIn get() = str("at").isNotEmpty()

    fun logout() = put { remove("at"); remove("rt"); remove("apikey"); remove("email") }

    // --- Tuya (Smart Life) ---
    var tuyaAccessId: String
        get() = str("tuyaId")
        set(v) = put { putString("tuyaId", v.trim()) }
    var tuyaSecret: String
        get() = secret("tuyaSecret")
        set(v) = putSecret("tuyaSecret", v.trim())
    /** Veri merkezi: eu, us, cn, in. */
    var tuyaRegion: String
        get() = str("tuyaRegion", "eu")
        set(v) = put { putString("tuyaRegion", v) }
    var tuyaToken: String
        get() = secret("tuyaToken")
        set(v) = putSecret("tuyaToken", v)
    var tuyaTokenExpiry: Long
        get() = prefs.getLong("tuyaTokenExp", 0)
        set(v) = put { putLong("tuyaTokenExp", v) }
    /** Ürün kimliği → (DP kodu → scale). Ürün tanımı her ürün için bir kez çekilir. */
    var tuyaScales: Map<String, Map<String, Int>>
        get() = parsed.get("tuyaScales", prefs.getString("tuyaScales", null)) { raw -> raw?.let { json ->
            runCatching {
                val o = org.json.JSONObject(json)
                o.keys().asSequence().associateWith { pid ->
                    val m = o.getJSONObject(pid)
                    m.keys().asSequence().associateWith { m.getInt(it) }
                }
            }.getOrNull()
        } ?: emptyMap() }
        set(v) = put { putString("tuyaScales", org.json.JSONObject(v.mapValues { org.json.JSONObject(it.value) }).toString()) }

    val hasTuya get() = tuyaAccessId.isNotEmpty() && tuyaSecret.isNotEmpty()
    /** eWeLink'e giriş yapılmış, Tuya bağlanmış ya da demo açık: izlenecek en az bir kaynak var. */
    val hasAnyAccount get() = isLoggedIn || hasTuya || demoMode

    // --- Demo ---
    /** Hesapsız deneme için sanal cihazlar açık mı (bkz. [Demo]). */
    var demoMode: Boolean
        get() = prefs.getBoolean("demo", false)
        set(v) = put { putBoolean("demo", v) }

    /** Demo cihazlarını ve örnek otomasyonlarını ekler; zaten varsa dokunmaz. */
    fun enableDemo(names: Demo.Names) {
        demoMode = true
        if (snapshots.none { it.cloud == Cloud.DEMO }) snapshots = snapshots + Demo.devices(names)
        val existing = automations.map { it.id }.toSet()
        automations = automations + Demo.automations(names).filter { it.id !in existing }
    }

    /** Demo cihazlarını, kurallarını ve örnek otomasyonlarını kaldırır. */
    fun disableDemo() {
        demoMode = false
        snapshots = snapshots.filter { it.cloud != Cloud.DEMO }
        rules = rules.filterKeys { !Demo.isDemo(it) }
        automations = automations.filterNot { Demo.isDemoAutomation(it) }
        if (freeDeviceId?.let(Demo::isDemo) == true) freeDeviceId = null
    }

    fun disconnectTuya() {
        put { remove("tuyaId"); remove("tuyaSecret"); remove("tuyaToken"); remove("tuyaTokenExp"); remove("tuyaScales") }
        snapshots = snapshots.filter { it.cloud != Cloud.TUYA }
    }

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
    /** İzleme bu kadar dakika kör kalırsa (internet/bulut yok) uyar; 0 = kapalı. */
    var blindAlertMin: Int
        get() = prefs.getInt("blindAlertMin", 10)
        set(v) = put { putInt("blindAlertMin", v.coerceAtLeast(0)) }
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
    /** Alarm kapatıldı ama durum sürüyorsa bu kadar dakika sonra yeniden çal; 0 = kapalı. */
    var reminderMin: Int
        get() = prefs.getInt("reminderMin", 0)
        set(v) = put { putInt("reminderMin", v.coerceAtLeast(0)) }
    /** Üretici "otomatik başlatma" ayarı okunamaz: kullanıcı yaptığını "Yaptım" ile onaylar. */
    var autostartConfirmed: Boolean
        get() = prefs.getBoolean("autostartOk", false)
        set(v) = put { putBoolean("autostartOk", v) }
    /** Alarm çalarken telefonun alarm ses seviyesini geçici olarak en yükseğe çıkar. */
    var alarmBoost: Boolean
        get() = prefs.getBoolean("alarmBoost", true)
        set(v) = put { putBoolean("alarmBoost", v) }
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
        get() = parsed.get("snapshots", prefs.getString("snapshots", null)) { raw ->
            raw?.let { runCatching { DeviceParser.snapshotsFromJson(it) }.getOrNull() } ?: emptyList()
        }
        set(v) = put { putString("snapshots", DeviceParser.snapshotsToJson(v)) }

    /** Cihazlar ekranının düzeni: sıra, öncelikliler, küçük kartlar, kategoriler. */
    var deviceLayout: com.macerce.switchguard.core.DeviceLayout
        get() = parsed.get("layout", prefs.getString("layout", null)) { com.macerce.switchguard.core.DeviceLayout.fromJson(it) }
        set(v) = put { putString("layout", v.toJson()) }

    var rules: Map<String, DeviceRules>
        get() = parsed.get("rules", prefs.getString("rules", null)) { raw ->
            raw?.let { runCatching { DeviceRules.mapFromJson(it) }.getOrNull() } ?: emptyMap()
        }
        set(v) = put { putString("rules", DeviceRules.mapToJson(v)) }

    fun rulesFor(deviceId: String) = rules[deviceId] ?: DeviceRules()
    fun setRules(deviceId: String, r: DeviceRules) { rules = rules + (deviceId to r) }
    /**
     * Gerçekten izlenen cihazlar: kuralı açık olanlar, ücretsiz sürümde yalnızca biri (bkz. [Entitlement]).
     * Demo cihazları sınıra sayılmaz; denemede her özellik görülebilsin.
     */
    fun monitoredIds(): Set<String> {
        val all = rules
        val wants = { id: String -> (all[id] ?: DeviceRules()).monitored }
        val (demo, real) = snapshots.map { it.id }.partition(Demo::isDemo)
        return demo.filter(wants).toSet() + Entitlement.monitored(real, wants, isPro, freeDeviceId)
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
        get() = parsed.get("automations", prefs.getString("automations", null)) { raw ->
            raw?.let { runCatching { Automation.listFromJson(it) }.getOrNull() } ?: emptyList()
        }
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
        /** Keystore ile şifreli saklanan anahtarlar. */
        val SECRET_KEYS = setOf("appSecret", "at", "rt", "apikey", "tuyaSecret", "tuyaToken")
        const val MIN_POLL = 15
        const val MAX_ALARM_LINES = 20

        @Volatile private var instance: Store? = null
        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context).also { instance = it } }
    }
}
