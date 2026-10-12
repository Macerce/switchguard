package com.macerce.switchguard.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.macerce.switchguard.core.SecretFormat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore'da tutulan AES-256 anahtarıyla şifreleme. Anahtar cihazdan çıkarılamaz (çoğu telefonda
 * donanımda saklanır); uygulama silinince o da silinir. Hata olursa null döner, çağıran düz değere düşer.
 */
object SecretBox {
    private const val ALIAS = "switchguard_secrets"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val TAG = "SwitchGuard"

    @Synchronized
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        SecretFormat.seal(cipher.iv, cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }.onFailure { Log.w(TAG, "secret encrypt failed", it) }.getOrNull()

    fun decrypt(sealed: String): String? = runCatching {
        val (iv, ciphertext) = SecretFormat.parse(sealed) ?: error("not sealed")
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.onFailure { Log.w(TAG, "secret decrypt failed", it) }.getOrNull()
}
