package com.macerce.switchguard

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.macerce.switchguard.core.SecretFormat
import com.macerce.switchguard.data.SecretBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Gerçek Android Keystore ile şifreleme (JVM birim testlerinde Keystore yok). */
@RunWith(AndroidJUnit4::class)
class SecretBoxDeviceTest {
    @Test
    fun encryptsAndDecryptsWithTheKeystoreKey() {
        val sealed = SecretBox.encrypt("gizli-anahtar-ğüşİ")!!
        assertTrue(SecretFormat.isSealed(sealed))
        assertEquals("gizli-anahtar-ğüşİ", SecretBox.decrypt(sealed))
    }

    @Test
    fun everyEncryptionUsesAFreshIv() {
        assertNotEquals(SecretBox.encrypt("aynı"), SecretBox.encrypt("aynı"))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val (iv, ct) = SecretFormat.parse(SecretBox.encrypt("x")!!)!!
        ct[0] = (ct[0] + 1).toByte()
        assertNull(SecretBox.decrypt(SecretFormat.seal(iv, ct)))
    }
}
