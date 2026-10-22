package com.macerce.switchguard.desktop.backup

import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.platform.Sounds
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

/** Geri yüklemeden önce gösterilen özet. */
data class Summary(
    val created: String,
    val appVersion: String,
    val tuya: Boolean,
    val ewelinkKeys: Boolean,
    val rules: Int,
    val automations: Int,
    val sounds: Int,
)

/**
 * Yedeğin (şifrelenmeden önceki) içeriği: `{ app, created, settings, sounds }` (`spec-2-ayarlar.md` §6.2).
 * [KEYS] yedeğe giren ayar anahtarlarıdır (gizliler düz metin: dosyanın kendisi şifreli); [CLEARED] yedeğe girmeyen
 * ama geri yüklemede temizlenen anahtarlardır (oturum, token, anlık görüntüler, bekleyen alarm…).
 */
object BackupContent {
    val KEYS: Set<String> = setOf(
        // eWeLink anahtarları (oturum değil)
        "appId", "appSecret", "redirectUrl", "region",
        // Tuya
        "tuyaId", "tuyaSecret", "tuyaRegion", "tuyaScales", "tuyaLive",
        // kurallar, otomasyonlar, düzen
        "rules", "automations", "layout", "freeDevice",
        // sesler
        "alarmSound", "notifySound", "alarmVolume",
        // sessiz saatler, hatırlatma
        "quietEnabled", "quietStart", "quietEnd", "reminderMin",
        // ikinci kişiye haber ver (Telegram; telefonda da yedeğe girer)
        "tgToken", "tgBot", "tgChats", "escalateMin",
        // izleme ve genel
        "monitoring", "pollIntervalSec", "offlineGraceSec", "blindAlertMin", "alwaysAwake",
        "autostart", "language", "theme", "demo", "onboardingDone", "summaryEnabled", "summaryMinute",
        // telefon 2.14–2.15: enerji fiyatları, akıllı uyarılar, sahneler, konum, Telegram ek işleri
        "energyPrice", "devicePrices", "threeZone", "priceDay", "pricePeak", "priceNight", "budget",
        "outageDetect", "energyAnomaly", "weeklyReport", "softenAtHome", "scenes", "lat", "lon",
        "tgCommands", "tgControl", "tgForward", "tgStages", "wdMode", "wdMin", "chargerOutage",
    )

    val CLEARED: Set<String> = setOf(
        "at", "rt", "apikey", "email", "sessionLost", "tuyaToken", "tuyaTokenExp",
        "snapshots", "pendingAlarm", "historySeen", "transferReceived", "transferSource",
    )

    fun build(store: Store, userDir: Path, appVersion: String, created: String): JSONObject {
        val settings = store.exportSettings(KEYS)
        val sounds = JSONObject()
        val b64 = Base64.getEncoder()
        for (id in soundIds(settings)) {
            val name = id.removePrefix(Sounds.USER_PREFIX)
            if (!isPlainName(name)) continue
            val file = userDir.resolve(name)
            if (Files.isRegularFile(file)) {
                sounds.put(id, b64.encodeToString(Files.readAllBytes(file)))
            } else {
                Log.w(TAG, "sound $name is referenced but missing; not in the backup")
            }
        }
        return JSONObject().put("app", appVersion).put("created", created).put("settings", settings).put("sounds", sounds)
    }

    fun summary(content: JSONObject): Summary {
        val settings = content.optJSONObject("settings") ?: JSONObject()
        return Summary(
            created = content.optString("created"),
            appVersion = content.optString("app"),
            tuya = settings.optString("tuyaId").isNotEmpty(),
            ewelinkKeys = settings.optString("appId").isNotEmpty(),
            rules = settings.optString("rules").takeIf { it.isNotEmpty() }?.let { DeviceRules.mapFromJson(it).size } ?: 0,
            automations = settings.optString("automations").takeIf { it.isNotEmpty() }?.let { Automation.listFromJson(it).size } ?: 0,
            sounds = content.optJSONObject("sounds")?.length() ?: 0,
        )
    }

    /** Ayarlarda başvurulan yüklenmiş seslerin (`user:`) kimlikleri: genel alarm, bildirim ve kurallardaki sesler. */
    fun soundIds(settings: JSONObject): Set<String> {
        val ids = mutableSetOf<String>()
        listOf("alarmSound", "notifySound").forEach { k -> settings.optString(k).takeIf { it.startsWith(Sounds.USER_PREFIX) }?.let(ids::add) }
        settings.optString("rules").takeIf { it.isNotEmpty() }?.let { json ->
            DeviceRules.mapFromJson(json).values.mapNotNullTo(ids) { r -> r.soundUri?.takeIf { it.startsWith(Sounds.USER_PREFIX) } }
        }
        return ids
    }

    /** Yol ayırıcı ya da `..` içermeyen düz dosya adı (yedekten gelen adla klasör dışına yazılmasın). */
    internal fun isPlainName(name: String): Boolean =
        name.isNotEmpty() && !name.contains('/') && !name.contains('\\') && !name.contains("..")

    private const val TAG = "Backup"
}
