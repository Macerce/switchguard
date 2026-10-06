package com.macerce.switchguard.desktop.backup

import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets.UTF_8
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupException(val kind: Kind, cause: Throwable? = null) : Exception(kind.name, cause) {
    enum class Kind { NOT_BACKUP, TOO_NEW, WRONG_PASSWORD_OR_DAMAGED, TOO_BIG, IO }
}

/**
 * Yedek dosyasının zarfı (`spec-2-ayarlar.md` §6.1): okunabilir JSON, içerik AES-256-GCM ile şifreli; anahtar
 * kullanıcının şifresinden PBKDF2-HMAC-SHA256 ile türetilir. Zarfın `format/version/created/kdf/cipher` alanları
 * GCM ek doğrulama verisine (AAD) bağlanır: biri değiştirilirse açma başarısız olur. Şifre hiçbir yere yazılmaz;
 * aynı şifreyle her bilgisayarda açılır (DPAPI kullanılmaz).
 */
object BackupCrypto {
    const val FORMAT = "switchguard-backup"
    const val VERSION = 1
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val CIPHER = "AES/GCM/NoPadding"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val MAX_ITERATIONS = 10_000_000

    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()

    fun seal(plain: ByteArray, password: CharArray, created: Instant, iterations: Int = 600_000): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val createdText = created.toString()
        val cipher = Cipher.getInstance(CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(FORMAT, VERSION, createdText, KDF, iterations, b64.encodeToString(salt), CIPHER, b64.encodeToString(iv)))
        val data = cipher.doFinal(plain)
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("created", createdText)
            .put("kdf", JSONObject().put("name", KDF).put("iterations", iterations).put("salt", b64.encodeToString(salt)))
            .put("cipher", JSONObject().put("name", CIPHER).put("iv", b64.encodeToString(iv)))
            .put("data", b64.encodeToString(data))
            .toString(2)
            .toByteArray(UTF_8)
    }

    /** Zarfı açar; [BackupException.kind] neden açılamadığını söyler. */
    fun open(sealed: ByteArray, password: CharArray): ByteArray {
        val json = try {
            JSONObject(String(sealed, UTF_8))
        } catch (e: JSONException) {
            throw BackupException(BackupException.Kind.NOT_BACKUP, e)
        }
        if (json.optString("format") != FORMAT) throw BackupException(BackupException.Kind.NOT_BACKUP)
        val version = json.optInt("version", 0)
        if (version > VERSION) throw BackupException(BackupException.Kind.TOO_NEW)
        if (version < 1) throw BackupException(BackupException.Kind.NOT_BACKUP)

        val parts = try {
            val kdf = json.getJSONObject("kdf")
            val cipher = json.getJSONObject("cipher")
            Parts(
                created = json.getString("created"),
                kdfName = kdf.getString("name"),
                iterations = kdf.getInt("iterations"),
                salt = kdf.getString("salt"),
                cipherName = cipher.getString("name"),
                iv = cipher.getString("iv"),
                data = json.getString("data"),
            )
        } catch (e: JSONException) {
            throw BackupException(BackupException.Kind.NOT_BACKUP, e)
        }
        if (parts.kdfName != KDF || parts.cipherName != CIPHER || parts.iterations !in 1..MAX_ITERATIONS) {
            throw BackupException(BackupException.Kind.NOT_BACKUP)
        }

        return try {
            val dec = Base64.getDecoder()
            val cipher = Cipher.getInstance(CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, key(password, dec.decode(parts.salt), parts.iterations), GCMParameterSpec(TAG_BITS, dec.decode(parts.iv)))
            cipher.updateAAD(aad(FORMAT, version, parts.created, parts.kdfName, parts.iterations, parts.salt, parts.cipherName, parts.iv))
            cipher.doFinal(dec.decode(parts.data))
        } catch (e: AEADBadTagException) {
            throw BackupException(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, e)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, e) // bozuk Base64
        } catch (e: GeneralSecurityException) {
            throw BackupException(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, e)
        }
    }

    private class Parts(
        val created: String,
        val kdfName: String,
        val iterations: Int,
        val salt: String,
        val cipherName: String,
        val iv: String,
        val data: String,
    )

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(format: String, version: Int, created: String, kdf: String, iterations: Int, salt: String, cipher: String, iv: String) =
        "$format|$version|$created|$kdf|$iterations|$salt|$cipher|$iv".toByteArray(UTF_8)
}
