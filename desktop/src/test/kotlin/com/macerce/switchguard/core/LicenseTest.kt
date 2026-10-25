package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

/** Keygen'in (Python, cryptography) gerçek açık anahtara karşı ürettiği anahtar; Pro gereken testler de bunu kullanır. */
const val TEST_LICENSE_KEY = "SG1.VGVzdCBMaXNhbnPEsXwyMDI2LTEwLTI1." +
    "6UQ1APeFNiurC625Vy_8ueW4wEoVvE2AfsRLFQShCfXsKnRU3TxIDWvpCK8LEN3B7nJR5Y8iZs0xSrXOhAt7Bg"

class LicenseTest {

    /** Python ve Kotlin tarafı aynı biçimi konuşuyor. */
    private val keygenKey = TEST_LICENSE_KEY

    @Test
    fun keygenKeyVerifiesWithEmbeddedPublicKey() {
        assertEquals(License("Test Lisansı", "2026-10-25"), License.parse(keygenKey))
    }

    @Test
    fun whitespaceFromCopyPasteIsIgnored() {
        assertEquals("Test Lisansı", License.parse("  " + keygenKey.chunked(20).joinToString("\n") + " ")?.owner)
    }

    @Test
    fun tamperedPayloadOrSignatureIsRejected() {
        val (prefix, payload, sig) = keygenKey.split('.')
        val otherOwner = Base64.getUrlEncoder().withoutPadding().encodeToString("Baskası|2026-10-25".toByteArray())
        assertNull(License.parse("$prefix.$otherOwner.$sig"))
        assertNull(License.parse("$prefix.$payload.${sig.reversed()}"))
        assertNull(License.parse("SG2.$payload.$sig"))
        assertNull(License.parse(""))
        assertNull(License.parse(null))
        assertNull(License.parse("rastgele metin"))
    }

    @Test
    fun keySignedByAnotherPrivateKeyIsRejected() {
        val pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val payload = "Sahte|2026-10-25".toByteArray()
        val sig = Signature.getInstance("Ed25519").run { initSign(pair.private); update(payload); sign() }
        val enc = Base64.getUrlEncoder().withoutPadding()
        val key = "SG1.${enc.encodeToString(payload)}.${enc.encodeToString(sig)}"
        assertNull(License.parse(key))
        assertEquals(License("Sahte", "2026-10-25"), License.parse(key, pair.public))
    }
}
