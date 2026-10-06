package com.macerce.switchguard.data

import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.data.SecretCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption

/**
 * Uygulamanın tüm kalıcı ayar ve durumu. Telefondaki `Store` ile aynı özellik adları, türleri, varsayılanları ve
 * JSON anahtarları; saklama `SharedPreferences` yerine tek bir JSON dosyası ([file]).
 *
 * Bellekte bir [JSONObject] tutulur; okuma ve yazmaların hepsi tek kilit altındadır (arayüz ve izleme iş
 * parçacıkları birlikte kullanır). Her yazma, bütün içeriği geçici dosyaya yazıp atomik taşıma ile asıl dosyanın
 * yerine koyar, sonra [version]'ı artırır; arayüz [version] değiştikçe yeniden çizilir.
 *
 * Gizli alanlar ([SECRET_KEYS]) diske yalnızca [secrets] ile kodlanmış olarak yazılır.
 */
class Store internal constructor(
    private val file: Path,
    private val secrets: SecretCodec,
    /** Bozuk dosyanın yedek adındaki zaman damgası; testler sabitleyebilsin diye ayrı verilir. */
    private val clock: () -> Long,
) {
    constructor(file: Path, secrets: SecretCodec) : this(file, secrets, System::currentTimeMillis)

    private val lock = Any()

    /** Anahtar → çözülemeyen saklı gizli değer ([secret]); değer değişince (yeni giriş) yeniden denenir. */
    private val undecodable = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val root: JSONObject = load()

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    // --- Depolama ---

    /**
     * Dosyayı okur. Yalnızca gerçek bir ayrıştırma hatası (JSON değil, üst düzey nesne değil) dosyayı kenara
     * alıp boş başlatır. Okuma hatası (kilitli ya da geçici okunamayan dosya) iyi bir dosyayı yok etmesin diye
     * birkaç kez denenir, sonra IOException olarak dışarı atılır; hiçbir durumda boş başlanıp asıl dosyanın
     * üzerine yazılmaz.
     */
    private fun load(): JSONObject {
        val bytes = try {
            readWithRetry()
        } catch (_: NoSuchFileException) {
            return JSONObject() // ilk açılış
        }
        return try {
            JSONObject(String(bytes, UTF_8).let { if (it.startsWith(BOM)) it.substring(1) else it })
        } catch (e: JSONException) {
            quarantine(e)
            JSONObject()
        }
    }

    private fun readWithRetry(): ByteArray {
        var attempt = 1
        while (true) {
            try {
                return Files.readAllBytes(file)
            } catch (e: NoSuchFileException) {
                throw e
            } catch (e: IOException) {
                if (attempt >= READ_ATTEMPTS) throw e
                Log.w(TAG, "reading $file failed ($e), retrying")
                try {
                    Thread.sleep(READ_RETRY_DELAY_MS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
                attempt++
            }
        }
    }

    /** Ayrıştırılamayan dosyayı incelenebilsin diye kenara alır. Taşınamazsa fırlatır: asıl dosya korunur. */
    private fun quarantine(cause: JSONException) {
        val backup = file.resolveSibling("${file.fileName}.corrupt-${clock()}")
        try {
            Files.move(file, backup, REPLACE_EXISTING)
        } catch (e: IOException) {
            throw IOException("$file is not valid JSON ($cause) and could not be moved to $backup; leaving it untouched", e)
        }
        Log.w(TAG, "$file is not valid JSON ($cause); moved to $backup, starting empty")
    }

    /** [lock] altında çağrılır. Yazılamazsa bellekteki durum geçerli kalır; sonraki yazma her şeyi yeniden yazar. */
    private fun persist() {
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        try {
            Files.createDirectories(file.toAbsolutePath().parent)
            val buf = ByteBuffer.wrap(root.toString(2).toByteArray(UTF_8))
            FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { ch ->
                while (buf.hasRemaining()) ch.write(buf)
                ch.force(true) // elektrik kesilirse taşıma sonrası boş dosya kalmasın
            }
            try {
                Files.move(tmp, file, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp, file, REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            Log.w(TAG, "could not write $file: $e")
            runCatching { Files.deleteIfExists(tmp) }
        }
    }

    /** Alanları değiştirir, diske yazar, sürümü artırır; hepsi tek kilit altında. */
    private fun edit(block: JSONObject.() -> Unit): Unit = synchronized(lock) {
        root.block()
        persist()
        _version.value++
    }

    private fun str(key: String, def: String = "") = synchronized(lock) { root.optString(key, def) }
    private fun strOrNull(key: String): String? = synchronized(lock) { if (root.isNull(key)) null else root.optString(key) }
    private fun bool(key: String, def: Boolean) = synchronized(lock) { root.optBoolean(key, def) }
    private fun int(key: String, def: Int) = synchronized(lock) { root.optInt(key, def) }
    private fun long(key: String, def: Long) = synchronized(lock) { root.optLong(key, def) }

    /**
     * Gizli alanı açar. DPAPI bazen bir anlık çözemeyebilir (ör. oturum açılışında profil henüz hazır değil): "çıkış
     * yapılmış" sayılmadan önce kısa bir bekleyişten sonra bir kez daha denenir; yine olmazsa boş döner ve günlüğe
     * yalnızca anahtar adı yazılır. Kalıcı olarak çözülemeyen değer (ör. başka bilgisayardan kopyalanmış) hatırlanır:
     * her okumada yeniden beklenmez, günlüğe bir kez yazılır.
     */
    private fun secret(key: String): String {
        check(key in SECRET_KEYS) { "$key is not a secret key" }
        val stored = str(key)
        if (stored.isEmpty()) return ""
        secrets.decode(stored)?.let { return it }
        if (undecodable[key] == stored) return ""

        try {
            Thread.sleep(SECRET_RETRY_DELAY_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        secrets.decode(stored)?.let { return it }
        undecodable[key] = stored
        Log.w(TAG, "could not decode secret '$key'")
        return ""
    }

    private fun setSecret(key: String, plain: String) {
        check(key in SECRET_KEYS) { "$key is not a secret key" }
        edit { if (plain.isEmpty()) remove(key) else put(key, secrets.encode(plain)) }
    }

    // --- Yedek (masaüstü) ---

    /**
     * [keys] içindeki ayarların taşınabilir kopyası (yedek dosyası için): değerler saklandığı türle; gizli alanlar
     * çözülmüş düz metin (yedek dosyasının kendisi şifrelidir, DPAPI başka bilgisayarda açılmaz). Olmayan anahtar yazılmaz.
     */
    fun exportSettings(keys: Set<String>): JSONObject = synchronized(lock) {
        val out = JSONObject()
        for (k in keys) {
            if (!root.has(k)) continue
            if (k in SECRET_KEYS) secret(k).takeIf { it.isNotEmpty() }?.let { out.put(k, it) } else out.put(k, root.get(k))
        }
        out
    }

    /**
     * Geri yükleme: [keys] ∪ [clear] anahtarları silinir, sonra [values] içindeki [keys] anahtarları yazılır (gizliler
     * bu bilgisayarın [secrets]'ı ile yeniden kodlanır). Tek kilit, tek dosya yazımı: yarım kalmış bir karışım olmaz.
     */
    fun replaceSettings(values: JSONObject, keys: Set<String>, clear: Set<String>): Unit = edit {
        (keys + clear).forEach { remove(it) }
        for (k in keys) {
            if (!values.has(k) || values.isNull(k)) continue
            val v = values.get(k)
            if (k in SECRET_KEYS) v.toString().takeIf { it.isNotEmpty() }?.let { put(k, secrets.encode(it)) } else put(k, v)
        }
    }

    // --- Hesap ---
    var appId: String
        get() = str("appId")
        set(v) = edit { put("appId", v.trim()) }
    var appSecret: String
        get() = secret("appSecret")
        set(v) = setSecret("appSecret", v.trim())
    var redirectUrl: String
        get() = str("redirectUrl", DEFAULT_REDIRECT)
        set(v) = edit { put("redirectUrl", v.trim().ifEmpty { DEFAULT_REDIRECT }) }
    var region: String
        get() = str("region", "eu")
        set(v) = edit { put("region", v) }
    var accessToken: String
        get() = secret("at")
        set(v) = setSecret("at", v)
    var refreshToken: String
        get() = secret("rt")
        set(v) = setSecret("rt", v)
    /** WebSocket el sıkışması için kullanıcının apikey'i. */
    var userApiKey: String
        get() = secret("apikey")
        set(v) = setSecret("apikey", v)
    var accountEmail: String
        get() = str("email")
        set(v) = edit { put("email", v) }

    /** Bu cihaz başka bir cihazdan kurulum aldı mı (kurulum sihirbazında "İleri" için). */
    var transferReceived: Boolean
        get() = bool("transferReceived", false)
        set(v) = edit { put("transferReceived", v) }

    /** Kurulumu başka cihazdan aktarıldıysa, gönderen cihazın eWeLink hesabı (aynı hesapla giriş uyarısı için). */
    var transferSource: String
        get() = str("transferSource")
        set(v) = edit { put("transferSource", v) }

    val hasCredentials get() = appId.isNotEmpty() && appSecret.isNotEmpty()
    val isLoggedIn get() = accessToken.isNotEmpty()

    fun logout(): Unit = edit { remove("at"); remove("rt"); remove("apikey"); remove("email") }

    // --- Tuya (Smart Life) ---
    var tuyaAccessId: String
        get() = str("tuyaId")
        set(v) = edit { put("tuyaId", v.trim()) }
    var tuyaSecret: String
        get() = secret("tuyaSecret")
        set(v) = setSecret("tuyaSecret", v.trim())
    /** Veri merkezi: eu, us, cn, in. */
    var tuyaRegion: String
        get() = str("tuyaRegion", "eu")
        set(v) = edit { put("tuyaRegion", v) }
    var tuyaToken: String
        get() = secret("tuyaToken")
        set(v) = setSecret("tuyaToken", v)
    var tuyaTokenExpiry: Long
        get() = long("tuyaTokenExp", 0)
        set(v) = edit { put("tuyaTokenExp", v) }
    /** Ürün kimliği → (DP kodu → scale). Ürün tanımı her ürün için bir kez çekilir. */
    var tuyaScales: Map<String, Map<String, Int>>
        get() = strOrNull("tuyaScales")?.let { json ->
            runCatching {
                val o = JSONObject(json)
                o.keys().asSequence().associateWith { pid ->
                    val m = o.getJSONObject(pid)
                    m.keys().asSequence().associateWith { m.getInt(it) }
                }
            }.getOrNull()
        } ?: emptyMap()
        set(v) = edit { put("tuyaScales", JSONObject(v.mapValues { JSONObject(it.value) }).toString()) }

    val hasTuya get() = tuyaAccessId.isNotEmpty() && tuyaSecret.isNotEmpty()
    /** eWeLink'e giriş yapılmış, Tuya bağlanmış ya da demo açık: izlenecek en az bir kaynak var. */
    val hasAnyAccount get() = isLoggedIn || hasTuya || demoMode

    // --- Demo ---
    /** Hesapsız deneme için sanal cihazlar açık mı (bkz. [Demo]). */
    var demoMode: Boolean
        get() = bool("demo", false)
        set(v) = edit { put("demo", v) }

    /** Demo cihazlarını ve örnek otomasyonlarını ekler; zaten varsa dokunmaz. */
    fun enableDemo(names: Demo.Names): Unit = synchronized(lock) {
        demoMode = true
        if (snapshots.none { it.cloud == Cloud.DEMO }) snapshots = snapshots + Demo.devices(names)
        val existing = automations.map { it.id }.toSet()
        automations = automations + Demo.automations(names).filter { it.id !in existing }
    }

    /** Demo cihazlarını, kurallarını ve örnek otomasyonlarını kaldırır. */
    fun disableDemo(): Unit = synchronized(lock) {
        demoMode = false
        snapshots = snapshots.filter { it.cloud != Cloud.DEMO }
        rules = rules.filterKeys { !Demo.isDemo(it) }
        automations = automations.filterNot { Demo.isDemoAutomation(it) }
        if (freeDeviceId?.let(Demo::isDemo) == true) freeDeviceId = null
    }

    fun disconnectTuya(): Unit = synchronized(lock) {
        edit { remove("tuyaId"); remove("tuyaSecret"); remove("tuyaToken"); remove("tuyaTokenExp"); remove("tuyaScales") }
        snapshots = snapshots.filter { it.cloud != Cloud.TUYA }
    }

    /** Oturum kullanıcı istemeden düştü (ör. aynı hesapla başka cihazda giriş). Giriş yapınca silinir. */
    var sessionLost: Boolean
        get() = bool("sessionLost", false)
        set(v) = edit { put("sessionLost", v) }

    var onboardingDone: Boolean
        get() = bool("onboardingDone", false)
        set(v) = edit { put("onboardingDone", v) }

    // --- İzleme ---
    var monitoringEnabled: Boolean
        get() = bool("monitoring", false)
        set(v) = edit { put("monitoring", v) }
    /** Açıkken bilgisayarın uykuya geçmesi engellenir: en güvenilir, en çok enerji harcayan mod. */
    var alwaysAwake: Boolean
        get() = bool("alwaysAwake", true)
        set(v) = edit { put("alwaysAwake", v) }
    /** WebSocket kullanılamazsa yedek sorgulama aralığı. */
    var pollIntervalSec: Int
        get() = int("pollIntervalSec", 30)
        set(v) = edit { put("pollIntervalSec", v.coerceAtLeast(MIN_POLL)) }
    /** Offline olayını bu kadar bekletip kısa kopmaları yok say (0 = hemen). */
    var offlineGraceSec: Int
        get() = int("offlineGraceSec", 0)
        set(v) = edit { put("offlineGraceSec", v.coerceAtLeast(0)) }

    // --- Uygulama ---
    /** Windows'la birlikte başlat. */
    var autostart: Boolean
        get() = bool("autostart", true)
        set(v) = edit { put("autostart", v) }
    /** Tuya cihazlarını canlı (WebSocket/MQTT) izle; kapalıysa yalnızca sorgulama. */
    var tuyaLive: Boolean
        get() = bool("tuyaLive", false)
        set(v) = edit { put("tuyaLive", v) }
    /** Arayüz dili; boş = sistem dili. */
    var language: String
        get() = str("language")
        set(v) = edit { put("language", v) }
    /** "system", "light" ya da "dark". */
    /** Masaüstü: Cihazlar ekranındaki kart boyutu ("large" / "medium" / "small"). */
    var cardSize: String
        get() = str("cardSize", "large")
        set(v) = edit { put("cardSize", v) }

    var theme: String
        get() = str("theme", "system")
        set(v) = edit { put("theme", v) }

    // --- Alarm ---
    /** null = varsayılan alarm sesi. */
    var alarmSoundUri: String?
        get() = strOrNull("alarmSound")
        set(v) = edit { if (v == null) remove("alarmSound") else put("alarmSound", v) }
    /** Masaüstü: bildirim sesi (telefonda yok); null ise yerleşik varsayılan. */
    var notifySoundUri: String?
        get() = strOrNull("notifySound")
        set(v) = edit { if (v == null) remove("notifySound") else put("notifySound", v) }
    var alarmVolume: Float
        get() = synchronized(lock) { root.optDouble("alarmVolume", 1.0).toFloat() }
        // Float'ı en kısa ondalık gösterimiyle yaz (0.05f → 0.05, 0.05000000074505806 değil).
        set(v) = edit { put("alarmVolume", v.coerceIn(0.05f, 1f).toString().toDouble()) }
    var quietHours: QuietHours
        get() = QuietHours(
            bool("quietEnabled", false),
            int("quietStart", 23 * 60),
            int("quietEnd", 7 * 60),
        )
        set(v) = edit {
            put("quietEnabled", v.enabled); put("quietStart", v.startMinute); put("quietEnd", v.endMinute)
        }

    // --- Cihazlar ---
    /** Son bilinen cihaz durumları; boşsa henüz temel alınmış durum yok demektir. */
    var snapshots: List<DeviceSnapshot>
        get() = strOrNull("snapshots")?.let {
            runCatching { DeviceParser.snapshotsFromJson(it) }.getOrNull()
        } ?: emptyList()
        set(v) = edit { put("snapshots", DeviceParser.snapshotsToJson(v)) }

    /** Cihazlar ekranının düzeni: sıra, öncelikliler, küçük kartlar, kategoriler. */
    var deviceLayout: DeviceLayout
        get() = DeviceLayout.fromJson(strOrNull("layout"))
        set(v) = edit { put("layout", v.toJson()) }

    var rules: Map<String, DeviceRules>
        get() = strOrNull("rules")?.let {
            runCatching { DeviceRules.mapFromJson(it) }.getOrNull()
        } ?: emptyMap()
        set(v) = edit { put("rules", DeviceRules.mapToJson(v)) }

    fun rulesFor(deviceId: String) = rules[deviceId] ?: DeviceRules()
    fun setRules(deviceId: String, r: DeviceRules): Unit = synchronized(lock) { rules = rules + (deviceId to r) }
    /**
     * Gerçekten izlenen cihazlar: kuralı açık olanlar. Masaüstü sürümü her zaman Pro olduğundan ücretsiz
     * sürümün tek cihaz sınırı uygulanmaz (bkz. [Entitlement]); demo cihazları da sınıra sayılmaz.
     */
    fun monitoredIds(): Set<String> {
        val all = rules
        val wants = { id: String -> (all[id] ?: DeviceRules()).monitored }
        val (demo, real) = snapshots.map { it.id }.partition(Demo::isDemo)
        return demo.filter(wants).toSet() + Entitlement.monitored(real, wants, isPro, freeDeviceId)
    }

    // --- Pro ---
    /** Masaüstü sürümünde her zaman açık. */
    val isPro: Boolean get() = true

    /** Geçmiş sekmesinde görülen son olayın kimliği; sonrakiler "görülmemiş" sayılır. Hiç ayarlanmadıysa null. */
    var historySeenId: Long?
        get() = synchronized(lock) { if (root.isNull("historySeen")) null else root.optLong("historySeen") }
        set(v) = edit { if (v == null) remove("historySeen") else put("historySeen", v) }

    /** Ücretsiz sürümde kullanıcının izlemek için seçtiği cihaz. */
    var freeDeviceId: String?
        get() = strOrNull("freeDevice")
        set(v) = edit { if (v == null) remove("freeDevice") else put("freeDevice", v) }

    // --- Otomasyon ---
    var automations: List<Automation>
        get() = strOrNull("automations")?.let {
            runCatching { Automation.listFromJson(it) }.getOrNull()
        } ?: emptyList()
        set(v) = edit { put("automations", Automation.listToJson(v)) }

    fun saveAutomation(a: Automation): Unit = synchronized(lock) {
        val list = automations
        automations = if (list.any { it.id == a.id }) list.map { if (it.id == a.id) a else it } else list + a
    }

    fun deleteAutomation(id: String): Unit = synchronized(lock) { automations = automations.filterNot { it.id == id } }

    // --- Günlük özet ---
    var dailySummaryEnabled: Boolean
        get() = bool("summaryEnabled", false)
        set(v) = edit { put("summaryEnabled", v) }
    /** Özetin gönderileceği saat (günün dakikası). */
    var dailySummaryMinute: Int
        get() = int("summaryMinute", 21 * 60)
        set(v) = edit { put("summaryMinute", v) }

    /** Kullanıcı kapatana kadar alarmda gösterilen satırlar. */
    var pendingAlarm: List<String>
        get() = str("pendingAlarm").split('\n').filter { it.isNotEmpty() }
        set(v) = edit { put("pendingAlarm", v.takeLast(MAX_ALARM_LINES).joinToString("\n")) }

    companion object {
        const val DEFAULT_REDIRECT = "https://127.0.0.1"
        const val MIN_POLL = 15
        const val MAX_ALARM_LINES = 20
        private const val TAG = "Store"

        /** Not Defteri gibi araçların eklediği UTF-8 işareti. */
        private val BOM = Char(0xFEFF)
        private const val READ_ATTEMPTS = 3
        private const val READ_RETRY_DELAY_MS = 200L
        private const val SECRET_RETRY_DELAY_MS = 50L

        /** Diske yalnızca [SecretCodec] ile kodlanarak yazılan alanlar. */
        val SECRET_KEYS: Set<String> = setOf("appSecret", "at", "rt", "apikey", "tuyaSecret", "tuyaToken")
    }
}
