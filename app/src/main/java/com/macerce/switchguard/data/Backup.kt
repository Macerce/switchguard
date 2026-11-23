package com.macerce.switchguard.data

import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.BackupCrypto
import com.macerce.switchguard.core.BackupException
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.Transfer
import com.macerce.switchguard.core.TypedPrefs
import org.json.JSONObject
import java.time.Instant

/**
 * Şifreli yedek dosyası (yeni telefona geçiş, sıfırlama). Zarf [BackupCrypto]'dur (Windows sürümüyle aynı biçim);
 * içerik `{ app, appVersion, created, settings }`. Cihazlar arası aktarımdan farkı: aynı kişinin yeni cihazı için
 * olduğundan eWeLink geliştirici anahtarları da girer. Oturum token'ları, bekleyen alarm ve geçmiş hiçbir zaman girmez.
 */
object Backup {
    /**
     * Anahtar listesinin sürümü. 2: sohbet şablonları, sesli okuma ve bekleme ekranı düzeni eklendi ([KEYS_V2]).
     * Geri yüklemede "yedekte olmayanı temizle" kuralı, yedeğin alındığı sürümde var olmayan anahtarlara uygulanmaz.
     */
    const val KEYS_VERSION = 3

    /** Sürüm 2'de eklenen anahtarlar: bunlardan önce alınmış bir yedek yüklenince mevcut değerleri silinmez. */
    val KEYS_V2: Set<String> = setOf("chatTemplates", "chatDefaultsSeen", "chatTemplateUses", "chatSpeak", "standbyGrid", "standbyDim")

    /** Sürüm 3'te eklenenler: ntfy ve webhook haber kanalları. */
    val KEYS_V3: Set<String> = setOf("ntfyUrl", "webhookUrl")

    /** Telegram ile haber verme de girer: aynı kişinin yeni telefonu aynı kişilere haber versin. */
    val KEYS: Set<String> = Transfer.KEYS + setOf("appId", "appSecret", "redirectUrl", "region", "alwaysAwake", "tgToken", "tgBot", "tgChats", "escalateMin", "energyPrice", "devicePrices", "tgCommands", "tgControl", "tgForward", "tgStages") + KEYS_V2 + KEYS_V3

    /**
     * Yedeğe bilerek girmeyen ayarlar ve nedenleri. Koruma testi, Store'daki her anahtarın ya [KEYS]'te ya burada
     * olmasını ister: yeni bir ayar eklenip yedeğe koyması unutulursa test kırmızı olur.
     */
    val EXCLUDED: Map<String, String> = mapOf(
        "at" to "eWeLink oturum token'ı (gizli; yeni cihazda yeniden giriş yapılır)",
        "rt" to "eWeLink yenileme token'ı (gizli)",
        "apikey" to "eWeLink oturumunun hesap kimliği",
        "email" to "eWeLink oturumunun e-postası (oturumla birlikte)",
        "sessionLost" to "oturum düştü uyarısı (bu cihazın durumu)",
        "tuyaToken" to "Tuya erişim belirteci (anahtarlardan yeniden alınır)",
        "tuyaTokenExp" to "Tuya erişim belirtecinin süresi",
        "lanControl" to "yerel ağ kontrolü cihaza özel (bu cihazın bağlı olduğu ağ)",
        "lanKeys" to "cihaz yerel ağ anahtarları (gizli; buluttan yeniden okunur)",
        "tuyaLive" to "Tuya anlık güncellemeleri cihaza özel (aynı anahtarlarla tek cihaz anlık mesaj alır)",
        "monitoring" to "izleme açık/kapalı: bu cihazın çalışma durumu",
        "pendingAlarm" to "çalmakta olan alarm",
        "snapshots" to "cihazların canlı durumu (buluttan yeniden okunur)",
        "onboardingDone" to "geri yüklemede ayrıca yazılır",
        "wdMode" to "bekçi kipi bu cihaza özel",
        "wdMin" to "bekçi aralığı bu cihaza özel",
        "chargerOutage" to "şarj kesintisinden elektrik kesintisi çıkarımı bu cihazın donanımına bağlı",
        "alarmSound" to "seçilen zil sesi bu cihazdaki bir dosyayı gösterir; yeni cihazda olmayabilir",
        "autostartOk" to "bu telefonun otomatik başlatma onayı",
        "twinOk" to "bu telefonun ikiz uygulama onayı",
        "away" to "şu anki evde/dışarıda durumu (yeni cihazda yeniden belirlenir)",
        "vacationUntil" to "geçici tatil kipi; yeni cihazda kendiliğinden başlamasın",
        "cycleDone" to "döngülerin çalışma durumu",
        "cycleRuns" to "döngülerin çalışma durumu",
        "demo" to "deneme (demo) kipi",
        "historySeen" to "geçmişte okunmamış sayacı",
        "transferReceived" to "Wi-Fi aktarımı işareti (bu cihaza özel)",
        "transferSource" to "aktarımın geldiği cihaz (bu cihaza özel)",
        "hbUrl" to "canlılık adresi cihaza özel: aynı adresi iki cihaz kullanırsa biri durunca fark edilmez",
        "hbMin" to "canlılık adresinin aralığı (adresle birlikte cihaza özel)",
        "tgSenders" to "Telegram gruplarında son yazanlar: yalnızca seçim listesi, yeni cihazda yeniden toplanır",
    )

    /** İçerik biçiminin etiketi; Windows sürümünün yedeği aynı zarfı kullanır ama içeriği farklıdır. */
    const val APP = "android"

    private val EWELINK_KEYS = setOf("appId", "appSecret", "redirectUrl", "region")
    private val TUYA_KEYS = setOf("tuyaId", "tuyaSecret", "tuyaRegion", "tuyaScales")
    private val SESSION_KEYS = setOf("at", "rt", "apikey", "email")

    /** Dosya bundan büyükse yedek değildir (gerçek yedekler birkaç KB). */
    const val MAX_BYTES = 2 * 1024 * 1024

    data class Summary(
        val created: String,
        val appVersion: String,
        val ewelinkKeys: Boolean,
        val tuya: Boolean,
        val rules: Int,
        val automations: Int,
        /** Yedeğin alındığı anahtar listesi sürümü; bu alan eklenmeden önceki yedeklerde 1. */
        val keysVersion: Int = KEYS_VERSION,
    )

    fun create(store: Store, appVersion: String, password: CharArray): ByteArray {
        val created = Instant.now()
        val content = JSONObject()
            .put("app", APP)
            .put("appVersion", appVersion)
            .put("created", created.toString())
            .put("keysVersion", KEYS_VERSION)
            .put("settings", TypedPrefs.encode(store.plainPrefs(), KEYS))
        return BackupCrypto.seal(content.toString().toByteArray(Charsets.UTF_8), password, created)
    }

    /** Açar ve özetler, henüz uygulamaz. Açılamazsa [BackupException]. */
    fun read(bytes: ByteArray, password: CharArray): Pair<Summary, Map<String, Any>> {
        if (bytes.size > MAX_BYTES) throw BackupException(BackupException.Kind.TOO_BIG)
        val plain = BackupCrypto.open(bytes, password)
        val content = runCatching { JSONObject(String(plain, Charsets.UTF_8)) }
            .getOrElse { throw BackupException(BackupException.Kind.NOT_BACKUP, it) }
        if (content.optString("app") != APP) throw BackupException(BackupException.Kind.OTHER_PLATFORM)
        val settings = TypedPrefs.decode(content.optJSONObject("settings") ?: JSONObject(), KEYS)
        if (settings.isEmpty()) throw BackupException(BackupException.Kind.NOT_BACKUP)
        val summary = Summary(
            created = content.optString("created"),
            appVersion = content.optString("appVersion"),
            ewelinkKeys = (settings["appId"] as? String).orEmpty().isNotEmpty(),
            tuya = (settings["tuyaId"] as? String).orEmpty().isNotEmpty(),
            rules = (settings["rules"] as? String)?.let { runCatching { DeviceRules.mapFromJson(it).size }.getOrNull() } ?: 0,
            automations = (settings["automations"] as? String)?.let { runCatching { Automation.listFromJson(it).size }.getOrNull() } ?: 0,
            keysVersion = content.optInt("keysVersion", 1),
        )
        return summary to settings
    }

    /** Geri yüklemede silinecek ve yazılacak ayarlar, eWeLink oturumunun korunup korunmayacağı. */
    data class RestorePlan(val remove: Set<String>, val put: Map<String, Any>, val keepSession: Boolean)

    /**
     * Yedekteki ayarlar mevcutların yerine geçer (yedekte olmayan ayar temizlenir). Yedekte olmayan hesap
     * bağlantısı (eWeLink ya da Tuya) olduğu gibi kalır. eWeLink oturumu yalnızca App ID ve bölge aynıysa
     * korunur: token o uygulamaya ve o bölgenin sunucusuna aittir.
     */
    fun plan(backup: Map<String, Any>, loggedIn: Boolean, currentAppId: String, currentRegion: String, keysVersion: Int = KEYS_VERSION): RestorePlan {
        val hasEwelink = (backup["appId"] as? String).orEmpty().isNotEmpty()
        val hasTuya = (backup["tuyaId"] as? String).orEmpty().isNotEmpty()
        val keep = (if (hasEwelink) emptySet() else EWELINK_KEYS) + (if (hasTuya) emptySet() else TUYA_KEYS)
        // Eski yedekte henüz var olmayan anahtarlar "yedekte yok" sayılıp silinmez (ör. sohbet şablonları).
        val unknownThen = (if (keysVersion < 2) KEYS_V2 else emptySet()) + (if (keysVersion < 3) KEYS_V3 else emptySet())
        val remove = (KEYS - keep - unknownThen) + (if (hasTuya) setOf("tuyaToken", "tuyaTokenExp") else emptySet())
        val sameAccount = backup["appId"] == currentAppId && ((backup["region"] as? String) ?: currentRegion) == currentRegion
        return RestorePlan(
            remove = remove,
            put = backup.filterKeys { it !in keep },
            keepSession = loggedIn && (!hasEwelink || sameAccount),
        )
    }

    fun apply(store: Store, settings: Map<String, Any>, keysVersion: Int = KEYS_VERSION) {
        val plan = plan(settings, store.isLoggedIn, store.appId, store.region, keysVersion)
        val editor = store.prefs.edit()
        plan.remove.forEach { editor.remove(it) }
        plan.put.forEach { (k, v) -> editor.putTyped(k, v) }
        if (!plan.keepSession) SESSION_KEYS.forEach { editor.remove(it) }
        editor.putBoolean("onboardingDone", true)
        editor.apply()
        store.sealSecrets()
    }
}
