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
import com.macerce.switchguard.core.EwelinkRegion
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
    // Okuma, eski düz değerin şifreli haliyle değiştirilmesi ve yazma aynı kilitte: başka thread'in bu arada
    // yazdığı yeni token, eski değerin şifreli haliyle ezilmesin.
    private fun secret(key: String): String = synchronized(secrets) {
        secrets.read(str(key)) { sealed -> prefs.edit().putString(key, sealed).apply() }
    }
    private fun putSecret(key: String, value: String) = synchronized(secrets) {
        put { if (value.isEmpty()) remove(key) else putString(key, secrets.write(value)) }
    }

    /** Kayıtlı bir gizli değer (App Secret, token, Tuya Secret) bu cihazda çözülemedi: yeniden girilmeli. */
    val secretsUnreadable get() = secrets.unreadable

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
        // Sunucu adresine yazılır: geri yüklenen yedekten bilinmeyen bir değer gelse de varsayılana düşer.
        get() = EwelinkRegion.parse(str("region")) ?: EwelinkRegion.DEFAULT
        set(v) = put { putString("region", EwelinkRegion.parse(v) ?: EwelinkRegion.DEFAULT) }
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

    /** Şifre çözmeden bakılır: anahtar okunamazsa Tuya "bağlı değil" görünmez, kimlik hatası verir. */
    val hasTuya get() = tuyaAccessId.isNotEmpty() && str("tuyaSecret").isNotEmpty()
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
    /** Enerji raporunda maliyet için kWh birim fiyatı (yerel para birimi); 0 = maliyet gösterilmez. */
    var energyPrice: Double
        get() = str("energyPrice").toDoubleOrNull() ?: 0.0
        set(v) = put { putString("energyPrice", v.coerceAtLeast(0.0).toString()) }
    /** Cihaza özel kWh fiyatları (cihaz → fiyat); olmayan cihaz [energyPrice]'ı kullanır. */
    var devicePrices: Map<String, Double>
        get() = parsed.get("devicePrices", prefs.getString("devicePrices", null)) { json ->
            runCatching { org.json.JSONObject(json ?: "{}").let { o -> o.keys().asSequence().associateWith { o.getDouble(it) } } }.getOrDefault(emptyMap())
        }
        set(v) = put { putString("devicePrices", org.json.JSONObject(v.filterValues { it > 0 }).toString()) }
    /** Cihazın kWh fiyatı: kendi fiyatı, yoksa varsayılan; 0 = maliyet hesaplanmaz. */
    fun priceFor(deviceId: String): Double = devicePrices[deviceId] ?: energyPrice
    /** Varsayılan tarife üç zamanlı mı (gündüz/puant/gece); değilse [energyPrice] tek fiyat. */
    var threeZone: Boolean
        get() = prefs.getBoolean("threeZone", false)
        set(v) = put { putBoolean("threeZone", v) }
    var priceDay: Double
        get() = str("priceDay").toDoubleOrNull() ?: 0.0
        set(v) = put { putString("priceDay", v.coerceAtLeast(0.0).toString()) }
    var pricePeak: Double
        get() = str("pricePeak").toDoubleOrNull() ?: 0.0
        set(v) = put { putString("pricePeak", v.coerceAtLeast(0.0).toString()) }
    var priceNight: Double
        get() = str("priceNight").toDoubleOrNull() ?: 0.0
        set(v) = put { putString("priceNight", v.coerceAtLeast(0.0).toString()) }
    /** Cihazın tarifesi: kendi fiyatı varsa tek fiyat, yoksa varsayılan (tek ya da üç zamanlı). */
    fun tariffFor(deviceId: String): com.macerce.switchguard.core.Tariff = devicePrices[deviceId]?.let { com.macerce.switchguard.core.Tariff.Flat(it) }
        ?: if (threeZone) com.macerce.switchguard.core.Tariff.ThreeZone(priceDay, pricePeak, priceNight)
        else com.macerce.switchguard.core.Tariff.Flat(energyPrice)
    /** Hiçbir cihaz için fiyat yok: maliyet hesaplanamaz. */
    val hasAnyPrice get() = energyPrice > 0 || devicePrices.isNotEmpty() || (threeZone && (priceDay > 0 || pricePeak > 0 || priceNight > 0))
    /** [price] null ya da 0 ise cihazın kendi fiyatı kalkar (varsayılan kullanılır). */
    fun setDevicePrice(deviceId: String, price: Double?) {
        devicePrices = if (price == null || price <= 0) devicePrices - deviceId else devicePrices + (deviceId to price)
    }
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
    // --- İkinci kişiye haber ver (Telegram) ---
    /** Kullanıcının kendi Telegram botunun anahtarı (BotFather). */
    var telegramToken: String
        get() = secret("tgToken")
        set(v) = putSecret("tgToken", v.trim())
    var telegramRecipients: List<com.macerce.switchguard.core.Recipient>
        get() = parsed.get("tgChats", prefs.getString("tgChats", null)) { com.macerce.switchguard.core.Recipient.listFromJson(it) }
        set(v) = put { putString("tgChats", com.macerce.switchguard.core.Recipient.listToJson(v)) }
    /** Botun kullanıcı adı ("t.me/<ad>" bağlantısı için); anahtar denenince kaydedilir. */
    var telegramBot: String
        get() = str("tgBot")
        set(v) = put { putString("tgBot", v) }
    /** Alarm bu kadar dakika kapatılmazsa haber ver; 0 = kapalı. */
    var escalateMin: Int
        get() = prefs.getInt("escalateMin", 0)
        set(v) = put { putInt("escalateMin", v.coerceAtLeast(0)) }
    /** Haber verme açık ve gönderilebilir durumda (anahtar ve en az bir kişi var). Şifre çözmeden bakılır. */
    val escalationReady get() = escalateMin > 0 && str("tgToken").isNotEmpty() && telegramRecipients.isNotEmpty()

    fun disableEscalation() = put { remove("tgToken"); remove("tgBot"); remove("tgChats"); remove("escalateMin"); remove("wdMode") }

    /** Telegram'dan /durum, /ac, /kapat komutları ve "Gördüm" düğmesi. */
    var telegramCommands: Boolean
        get() = prefs.getBoolean("tgCommands", true)
        set(v) = put { putBoolean("tgCommands", v) }
    /** Telegram'dan cihaz açıp kapatmaya izin (varsayılan kapalı: yalnızca durum sorulabilir). */
    var telegramControl: Boolean
        get() = prefs.getBoolean("tgControl", false)
        set(v) = put { putBoolean("tgControl", v) }
    /** Elektrik kesintisi, tüketim, bekçi ve haftalık rapor Telegram'a da yazılsın. */
    var telegramForward: Boolean
        get() = prefs.getBoolean("tgForward", true)
        set(v) = put { putBoolean("tgForward", v) }
    /** Bekçi rolü: [WATCHDOG_OFF], [WATCHDOG_SEND] (bu cihaz sinyal verir) ya da [WATCHDOG_WATCH] (diğerini denetler). */
    var watchdogMode: Int
        get() = prefs.getInt("wdMode", WATCHDOG_OFF)
        set(v) = put { putInt("wdMode", v) }
    /** Denetleyen: diğer cihaz bu kadar dakika sinyal vermezse alarm. */
    var watchdogMin: Int
        get() = prefs.getInt("wdMin", 60)
        set(v) = put { putInt("wdMin", v.coerceAtLeast(15)) }

    // --- Akıllı uyarılar ---
    /** İzlenen cihazların hepsi birlikte koparsa tek "elektrik kesildi" olayı. */
    var outageDetect: Boolean
        get() = prefs.getBoolean("outageDetect", true)
        set(v) = put { putBoolean("outageDetect", v) }
    /** Bir cihaz dün ortalamasından belirgin fazla/az enerji harcadıysa bildir. */
    var energyAnomaly: Boolean
        get() = prefs.getBoolean("energyAnomaly", true)
        set(v) = put { putBoolean("energyAnomaly", v) }
    /** Aylık tahmini maliyet bunu geçince bildir; 0 = kapalı ([energyPrice] gerekir). */
    var monthlyBudget: Double
        get() = str("budget").toDoubleOrNull() ?: 0.0
        set(v) = put { putString("budget", v.coerceAtLeast(0.0).toString()) }
    /** Bu cihaz (prize takılı telefon/tablet) şarjdan çıkınca elektrik kesintisi say. Yalnızca bu cihaza ait, aktarıma girmez. */
    var chargerOutage: Boolean
        get() = prefs.getBoolean("chargerOutage", false)
        set(v) = put { putBoolean("chargerOutage", v) }

    /** Evde / Dışarıda modu (true = dışarıda). Tatilde her zaman dışarıda sayılır ([isAway]). */
    var awayMode: Boolean
        get() = prefs.getBoolean("away", false)
        set(v) = put { putBoolean("away", v) }
    /** Evdeyken kritik olmayan cihazların alarmı bildirime düşer (sessiz saat gibi). */
    var softenAtHome: Boolean
        get() = prefs.getBoolean("softenAtHome", false)
        set(v) = put { putBoolean("softenAtHome", v) }
    /** Tatil modunun bittiği an (ms); 0 = kapalı. */
    var vacationUntil: Long
        get() = prefs.getLong("vacationUntil", 0)
        set(v) = put { putLong("vacationUntil", v) }
    fun isAway(now: Long = System.currentTimeMillis()) = com.macerce.switchguard.core.Presence.away(awayMode, vacationUntil, now)
    fun onVacation(now: Long = System.currentTimeMillis()) = com.macerce.switchguard.core.Presence.vacation(vacationUntil, now)

    /** Kademeli haber: kişi/grup → kaç dakika sonra (yoksa [escalateMin]). */
    var telegramStages: Map<Long, Int>
        get() = parsed.get("tgStages", prefs.getString("tgStages", null)) { json ->
            runCatching { org.json.JSONObject(json ?: "{}").let { o -> o.keys().asSequence().associate { it.toLong() to o.getInt(it) } } }.getOrDefault(emptyMap())
        }
        set(v) = put { putString("tgStages", org.json.JSONObject(v.mapKeys { it.key.toString() }).toString()) }
    fun stageFor(chatId: Long): Int = telegramStages[chatId] ?: escalateMin

    var scenes: List<com.macerce.switchguard.core.Scene>
        get() = parsed.get("scenes", prefs.getString("scenes", null)) { com.macerce.switchguard.core.Scene.listFromJson(it) }
        set(v) = put { putString("scenes", com.macerce.switchguard.core.Scene.listToJson(v)) }
    fun saveScene(s: com.macerce.switchguard.core.Scene) {
        val list = scenes
        scenes = if (list.any { it.id == s.id }) list.map { if (it.id == s.id) s else it } else list + s
    }
    fun deleteScene(id: String) { scenes = scenes.filterNot { it.id == id } }

    /** Gruplar: tek düğmeyle açılıp kapanan cihaz kümeleri (bkz. [com.macerce.switchguard.core.DeviceGroup]). */
    var groups: List<com.macerce.switchguard.core.DeviceGroup>
        get() = parsed.get("groups", prefs.getString("groups", null)) { com.macerce.switchguard.core.DeviceGroup.listFromJson(it) }
        set(v) = put { putString("groups", com.macerce.switchguard.core.DeviceGroup.listToJson(v)) }
    fun saveGroup(g: com.macerce.switchguard.core.DeviceGroup) {
        val list = groups
        groups = if (list.any { it.id == g.id }) list.map { if (it.id == g.id) g else it } else list + g
    }
    fun deleteGroup(id: String) { groups = groups.filterNot { it.id == id } }

    /** Gerçek cihazlar + grupların sanal cihazları. */
    val snapshotsWithGroups: List<DeviceSnapshot>
        get() = com.macerce.switchguard.core.DeviceGroup.withGroups(snapshots, groups)

    /** Gün doğumu/batımı ve hava sıcaklığı kuralları için konum; null = girilmedi. */
    var latitude: Double?
        get() = str("lat").toDoubleOrNull()
        set(v) = put { putString("lat", v?.toString() ?: "") }
    var longitude: Double?
        get() = str("lon").toDoubleOrNull()
        set(v) = put { putString("lon", v?.toString() ?: "") }

    /** Pazartesi sabahı geçen haftanın raporu. */
    var weeklyReport: Boolean
        get() = prefs.getBoolean("weeklyReport", false)
        set(v) = put { putBoolean("weeklyReport", v) }

    /** Üretici "otomatik başlatma" ayarı okunamaz: kullanıcı yaptığını "Yaptım" ile onaylar. */
    var autostartConfirmed: Boolean
        get() = prefs.getBoolean("autostartOk", false)
        set(v) = put { putBoolean("autostartOk", v) }
    /** Kullanıcı uygulamanın iki kopyasını bilerek kullanıyor: ikiz uyarısı gösterilmez (bkz. [com.macerce.switchguard.core.TwinApp]). */
    var twinAcknowledged: Boolean
        get() = prefs.getBoolean("twinOk", false)
        set(v) = put { putBoolean("twinOk", v) }
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

    /** Cihazın uyarı kategorisi (seçilen ya da kurallardan çıkan). */
    fun categoryOf(deviceId: String): com.macerce.switchguard.core.DeviceCategory = rulesFor(deviceId).effectiveCategory

    /** Sessiz saatlerde de alarm çalan uyarı kategorileri (Uyarılar → Uyarı kategorileri); seçilmemişse eski davranışa uyan varsayılan. */
    var quietBypass: Set<com.macerce.switchguard.core.DeviceCategory>
        get() = prefs.getString("quietBypass", null)?.let { com.macerce.switchguard.core.DeviceCategory.setFromString(it) }
            ?: com.macerce.switchguard.core.DeviceCategory.quietDefault(
                quietHours.enabled, rules.values.any { it.category == com.macerce.switchguard.core.DeviceCategory.CRITICAL },
            )
        set(v) = put { putString("quietBypass", com.macerce.switchguard.core.DeviceCategory.setToString(v)) }

    /** Cihazın alarmı sessiz saatlerde de çalar mı (kategorisine göre). */
    fun bypassesQuiet(deviceId: String): Boolean = categoryOf(deviceId) in quietBypass

    /** Servisin değerlendirdiği otomasyonlar: kategori tetikleyicileri cihazlara açılmış. */
    fun engineAutomations(devices: List<DeviceSnapshot> = snapshots): List<com.macerce.switchguard.core.Automation> {
        val all = rules
        return com.macerce.switchguard.core.DeviceCategory.expand(automations, devices) { id -> (all[id] ?: DeviceRules()).effectiveCategory }
    }
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
        val SECRET_KEYS = setOf("appSecret", "at", "rt", "apikey", "tuyaSecret", "tuyaToken", "tgToken")
        const val MIN_POLL = 15
        const val MAX_ALARM_LINES = 20
        const val WATCHDOG_OFF = 0
        const val WATCHDOG_SEND = 1
        const val WATCHDOG_WATCH = 2

        @Volatile private var instance: Store? = null
        fun get(context: Context): Store =
            instance ?: synchronized(this) { instance ?: Store(context).also { instance = it } }
    }
}
