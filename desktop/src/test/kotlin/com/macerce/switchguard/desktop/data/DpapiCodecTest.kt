package com.macerce.switchguard.desktop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test

class DpapiCodecTest {
    @Before
    fun windowsOnly() {
        Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"))
    }

    @Test
    fun roundTrip() {
        val codec = DpapiCodec()
        val encoded = codec.encode("gizli-Şifre-123")
        assertTrue(encoded.startsWith("dpapi:"))
        assertFalse(encoded.contains("gizli"))
        assertEquals("gizli-Şifre-123", codec.decode(encoded))
    }

    @Test
    fun foreignBlobDecodesToNull() {
        assertNull(DpapiCodec().decode("dpapi:AAAA"))
    }

    @Test
    fun invalidBase64DecodesToNull() {
        assertNull(DpapiCodec().decode("dpapi:***not-base64***"))
    }

    @Test
    fun plainValueWithoutPrefixIsReturnedAsIs() {
        assertEquals("duz-deger", DpapiCodec().decode("duz-deger"))
    }
}
