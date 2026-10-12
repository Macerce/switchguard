package com.macerce.switchguard.core

import java.util.Base64

/** Şifreli ayar değerinin metin biçimi: "enc1:<iv>:<şifreli>" (Base64). Düz (eski) değerler bu öneki taşımaz. */
object SecretFormat {
    const val PREFIX = "enc1:"
    private val enc = Base64.getEncoder()
    private val dec = Base64.getDecoder()

    fun isSealed(value: String) = value.startsWith(PREFIX)

    fun seal(iv: ByteArray, ciphertext: ByteArray): String = PREFIX + enc.encodeToString(iv) + ":" + enc.encodeToString(ciphertext)

    fun parse(value: String): Pair<ByteArray, ByteArray>? {
        if (!isSealed(value)) return null
        val parts = value.removePrefix(PREFIX).split(':')
        if (parts.size != 2) return null
        return runCatching { dec.decode(parts[0]) to dec.decode(parts[1]) }.getOrNull()
    }
}

/**
 * Gizli ayarların (App Secret, token'lar) şifreli saklanması. Şifreleme dışarıdan verilir (Android Keystore);
 * böylece mantık telefon olmadan test edilir. Eski sürümlerden kalan düz değer okunduğunda şifrelenip geri
 * yazılır. Çözülen değerler bellekte tutulur: her okuma Keystore'a gitmez. Çözülemeyen değer boş sayılır
 * (ör. anahtar silinmiş): kullanıcı yeniden giriş yapar, uygulama çökmez.
 */
class SecretValues(
    private val encrypt: (String) -> String?,
    private val decrypt: (String) -> String?,
) {
    private val cache = HashMap<String, String>()

    /** Saklanacak metin; şifreleme yapılamazsa (Keystore yok) değer düz kalır. */
    @Synchronized
    fun write(plain: String): String {
        if (plain.isEmpty()) return ""
        val sealed = encrypt(plain) ?: return plain
        cache[sealed] = plain
        return sealed
    }

    /** [raw] saklanan metin. Düzse [rewrite] ile şifreli hali yazdırılır. */
    @Synchronized
    fun read(raw: String, rewrite: (String) -> Unit): String {
        if (raw.isEmpty()) return ""
        if (!SecretFormat.isSealed(raw)) {
            encrypt(raw)?.let { sealed ->
                cache[sealed] = raw
                rewrite(sealed)
            }
            return raw
        }
        cache[raw]?.let { return it }
        val plain = decrypt(raw) ?: return ""
        cache[raw] = plain
        return plain
    }
}
