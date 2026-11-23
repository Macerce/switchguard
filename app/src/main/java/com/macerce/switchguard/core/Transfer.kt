package com.macerce.switchguard.core

import android.os.Build
import android.util.Base64
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.data.putTyped
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Başka bir cihaza (TV, tablet) kurulum aktarma: Tuya bilgileri, kurallar ve ayarlar.
 * Veri yalnızca ev ağında gider ve alıcı ekranındaki 6 haneli kodla şifrelenir (PBKDF2 + AES-GCM).
 *
 * Bilerek AKTARILMAYANLAR:
 * - eWeLink API anahtarları (App ID/Secret/Redirect) ve oturumu: anahtarlar, o cihazda giriş yapılan eWeLink
 *   hesabına ait olmalı. Her cihaz ayrı bir hesap kullanır (eWeLink hesap başına tek oturum tutar); o hesapta
 *   geliştirici özelliği açılır (eWeLink onayı) ve o hesabın kendi anahtarları girilir.
 * - Pro (her cihaz Google Play'den kendisi sorgular), cihaza özgü durum (izleme, bekleyen alarm, geçmiş),
 *   alarm sesi (zil sesi URI'si alıcıda olmayabilir).
 */
object Transfer {
    const val SERVICE_TYPE = "_switchguard._tcp."
    /** 2: 12 karakterlik kod (≈59 bit) ve zarfta açık sürüm alanı. 1: 6 haneli kod. */
    const val VERSION = 2

    /** Gönderen eWeLink'e bağlı ama hesabın e-postası bilinmiyor. */
    const val UNKNOWN_ACCOUNT = "*"

    /** Aktarılan SharedPreferences anahtarları (izin listesi). */
    val KEYS = setOf(
        "tuyaId", "tuyaSecret", "tuyaRegion", "tuyaScales",
        "rules", "automations", "layout", "freeDevice",
        "alarmVolume", "vibrate", "quietEnabled", "quietStart", "quietEnd",
        "offlineGraceSec", "pollIntervalSec", "summaryEnabled", "summaryMinute",
        "blindAlertMin", "alarmBoost", "reminderMin",
        "outageDetect", "energyAnomaly", "budget", "weeklyReport", "devicePrices", "lat", "lon", "threeZone", "priceDay", "pricePeak", "priceNight", "scenes", "groups", "lanRoutes", "localDevices", "cycles", "zones", "softenAtHome", "quietBypass",
        // Sohbet şablonları (saatli/tetikleyicili olanlar fiilen otomasyondur) ve varsayılan kWh fiyatı.
        "chatTemplates", "chatDefaultsSeen", "chatTemplateUses", "energyPrice",
    )

    private const val PBKDF2_ITER = 100_000
    private val random = SecureRandom()

    /**
     * Aktarım kodunun harfleri: karışanlar (0/O, 1/I/L) yok. 31 karakter × 12 ≈ 59 bit: yakalanan şifreli paket
     * sıradan bir bilgisayarda çevrimdışı denenerek açılamaz (6 haneli kodda 10⁶ olasılık dakikalar sürüyordu).
     */
    const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 12

    fun newCode(): String = (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    /** Ekranda 4-4-4 gruplu gösterim: "ABCD-EFGH-JKMN". */
    fun formatCode(code: String): String = code.chunked(4).joinToString("-")

    /** Kullanıcının yazdığı kod: boşluk/tire atılır, küçük harf büyür; geçersiz karakterler düşer. */
    fun normalizeCode(input: String): String = input.uppercase().filter { it in CODE_ALPHABET }.take(CODE_LENGTH)

    fun isValidCode(code: String): Boolean = code.length == CODE_LENGTH && code.all { it in CODE_ALPHABET }

    /** Eski sürümün 6 haneli kodu girildi mi (diğer cihaz güncellenmemiş). */
    fun looksLikeOldCode(input: String): Boolean = input.filter { !it.isWhitespace() }.let { it.length == 6 && it.all(Char::isDigit) }

    /** Gönderilecek düz veri. [source] alıcıda "bu hesapla giriş yapmayın" uyarısı için. */
    fun export(store: Store): JSONObject {
        val prefs = TypedPrefs.encode(store.plainPrefs(), KEYS)
        return JSONObject()
            .put("v", VERSION)
            .put("prefs", prefs)
            .put("source", JSONObject().put("email", store.accountEmail).put("ewelink", store.isLoggedIn).put("device", Build.MODEL))
    }

    /** Alınan veriyi uygular. Döner: gönderen cihazın eWeLink e-postası (boş olabilir). */
    fun import(store: Store, data: JSONObject): String {
        if (data.optInt("v") != VERSION) throw VersionMismatch()
        val editor = store.prefs.edit()
        TypedPrefs.decode(data.getJSONObject("prefs"), KEYS).forEach { (k, v) -> editor.putTyped(k, v) }
        // Tuya anahtarı değişmiş olabilir: eski erişim belirteci geçersiz.
        editor.remove("tuyaToken").remove("tuyaTokenExp")
        editor.apply()
        store.sealSecrets()
        val source = data.optJSONObject("source")
        val email = source?.optString("email").orEmpty()
        store.transferReceived = true
        // E-posta her zaman bilinmez (bazı App ID'lerde eWeLink profil sorgusu reddedilir): gönderen eWeLink'e
        // bağlıysa yine de uyarı gösterilsin diye işaretçi saklanır.
        store.transferSource = email.ifEmpty { if (source?.optBoolean("ewelink") == true) UNKNOWN_ACCOUNT else "" }
        return email
    }

    /** Gönderen ve alan farklı aktarım sürümünde: iki cihazda da uygulama güncellenmeli. */
    class VersionMismatch : Exception("transfer version")

    fun encrypt(code: String, plain: ByteArray): JSONObject {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(code, salt), GCMParameterSpec(128, iv))
        // Sürüm zarfta açıkta: alıcı, şifreyi çözmeye uğraşmadan uyuşmazlığı anlaşılır biçimde bildirir.
        return JSONObject().put("v", VERSION).put("salt", b64(salt)).put("iv", b64(iv)).put("data", b64(cipher.doFinal(plain)))
    }

    /** Zarfın sürümü (sürüm alanı olmayan eski zarf: 1). */
    fun envelopeVersion(msg: JSONObject): Int = msg.optInt("v", 1)

    /** Yanlış kodda [javax.crypto.AEADBadTagException] fırlatır. */
    fun decrypt(code: String, msg: JSONObject): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(code, unb64(msg.getString("salt"))), GCMParameterSpec(128, unb64(msg.getString("iv"))))
        return cipher.doFinal(unb64(msg.getString("data")))
    }

    private fun key(code: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(code.toCharArray(), salt, PBKDF2_ITER, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    /** "ahmet.yilmaz@gmail.com" -> "a***z@gmail.com" (uyarıda tam adres göstermeden tanınır). */
    fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) return email
        val name = email.substring(0, at)
        return name.first() + "***" + name.last() + email.substring(at)
    }
}
