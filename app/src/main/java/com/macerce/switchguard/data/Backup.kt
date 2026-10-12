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
    val KEYS: Set<String> = Transfer.KEYS + setOf("appId", "appSecret", "redirectUrl", "region", "alwaysAwake")

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
    )

    fun create(store: Store, appVersion: String, password: CharArray): ByteArray {
        val created = Instant.now()
        val content = JSONObject()
            .put("app", APP)
            .put("appVersion", appVersion)
            .put("created", created.toString())
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
    fun plan(backup: Map<String, Any>, loggedIn: Boolean, currentAppId: String, currentRegion: String): RestorePlan {
        val hasEwelink = (backup["appId"] as? String).orEmpty().isNotEmpty()
        val hasTuya = (backup["tuyaId"] as? String).orEmpty().isNotEmpty()
        val keep = (if (hasEwelink) emptySet() else EWELINK_KEYS) + (if (hasTuya) emptySet() else TUYA_KEYS)
        val remove = (KEYS - keep) + (if (hasTuya) setOf("tuyaToken", "tuyaTokenExp") else emptySet())
        val sameAccount = backup["appId"] == currentAppId && ((backup["region"] as? String) ?: currentRegion) == currentRegion
        return RestorePlan(
            remove = remove,
            put = backup.filterKeys { it !in keep },
            keepSession = loggedIn && (!hasEwelink || sameAccount),
        )
    }

    fun apply(store: Store, settings: Map<String, Any>) {
        val plan = plan(settings, store.isLoggedIn, store.appId, store.region)
        val editor = store.prefs.edit()
        plan.remove.forEach { editor.remove(it) }
        plan.put.forEach { (k, v) -> editor.putTyped(k, v) }
        if (!plan.keepSession) SESSION_KEYS.forEach { editor.remove(it) }
        editor.putBoolean("onboardingDone", true)
        editor.apply()
        store.sealSecrets()
    }
}
