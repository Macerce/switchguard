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
            .put("app", "android")
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
        val settings = TypedPrefs.decode(content.optJSONObject("settings") ?: JSONObject(), KEYS)
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

    /**
     * Ayarları uygular. Yedekteki eWeLink App ID'si şu anki oturumunkinden farklıysa oturum kapatılır
     * (eski token o uygulamaya aittir); aynıysa oturum korunur. Tuya erişim belirteci her zaman yenilenir.
     */
    fun apply(store: Store, settings: Map<String, Any>) {
        val keepSession = store.isLoggedIn && settings["appId"] == store.appId
        val editor = store.prefs.edit()
        settings.forEach { (k, v) -> editor.putTyped(k, v) }
        editor.remove("tuyaToken").remove("tuyaTokenExp")
        if (!keepSession) editor.remove("at").remove("rt").remove("apikey").remove("email")
        editor.putBoolean("onboardingDone", true)
        editor.apply()
        store.sealSecrets()
    }
}
