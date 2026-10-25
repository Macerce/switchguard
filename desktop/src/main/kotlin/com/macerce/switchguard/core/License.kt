package com.macerce.switchguard.core

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Masaüstü Pro lisansı: süresiz, çevrimdışı doğrulanan imzalı anahtar.
 * Biçim `SG1.<payload>.<imza>` (base64url, dolgusuz); payload = "<lisans sahibi>|<YYYY-MM-DD>" (UTF-8),
 * imza = Ed25519(payload). Anahtarlar depo dışındaki keygen ile üretilir; uygulamada yalnızca açık anahtar bulunur.
 */
data class License(val owner: String, val issued: String) {
    companion object {
        private const val PREFIX = "SG1"
        private const val PUBLIC_KEY_RAW = "vSvs//weASCtjB1F9el06/bEk6C+WJZFH7JYj+swOdE="
        /** Ham 32 baytlık Ed25519 açık anahtarını X.509 biçimine çeviren sabit ön ek. */
        private val X509_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00)

        val publicKey: PublicKey by lazy { publicKeyOf(Base64.getDecoder().decode(PUBLIC_KEY_RAW)) }

        fun publicKeyOf(raw: ByteArray): PublicKey =
            KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(X509_PREFIX + raw))

        /** Anahtar geçerliyse lisansı, değilse null döndürür. Kopyalarken araya giren boşluklar yok sayılır. */
        fun parse(key: String?, trusted: PublicKey = publicKey): License? = runCatching {
            val parts = key.orEmpty().filterNot(Char::isWhitespace).split('.')
            if (parts.size != 3 || parts[0] != PREFIX) return null
            val decoder = Base64.getUrlDecoder()
            val payload = decoder.decode(parts[1])
            val ok = Signature.getInstance("Ed25519").run {
                initVerify(trusted)
                update(payload)
                verify(decoder.decode(parts[2]))
            }
            if (!ok) return null
            val fields = String(payload, Charsets.UTF_8).split('|')
            if (fields.size != 2 || fields[0].isBlank()) null else License(fields[0], fields[1])
        }.getOrNull()
    }
}
