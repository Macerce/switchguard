package com.macerce.ewelinkalarm.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SignerTest {
    // Beklenen değerler openssl ile üretildi:
    // printf '%s' '<mesaj>' | openssl dgst -sha256 -hmac 'secret' -binary | base64
    @Test
    fun `govde imzasi openssl ile ayni`() {
        assertEquals("5fvAwftWdTAhhRBS5qkisjGgyQk+//t0R0hcC/MCxyU=", Signer.sign("""{"rt":"abc"}""", "secret"))
    }

    @Test
    fun `oauth url imzasi openssl ile ayni`() {
        assertEquals("dCYoytG6xwk+Vqj49118X+esTZOzJnjjEdyzV7jgBQU=", Signer.sign("myapp_1700000000000", "secret"))
    }
}
