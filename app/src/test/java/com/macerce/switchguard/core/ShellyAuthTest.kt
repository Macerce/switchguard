package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellyAuthTest {
    /** RFC 7616 bölüm 3.9.1'deki SHA-256 örneği. */
    @Test
    fun rfc7616Sha256Vector() {
        val ch = ShellyAuth.parseChallenge(
            """Digest realm="http-auth@example.org", qop="auth, auth-int", algorithm=SHA-256, nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v", opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS""""
        )!!
        assertEquals("auth", ch.qop)
        assertEquals("SHA-256", ch.algorithm)
        val h = ShellyAuth.digestHeader(
            ShellyAuth.Credentials("Mufasa", "Circle of Life"), ch, "GET", "/dir/index.html", 1,
            "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ",
        )
        assertTrue(h, h.contains("response=\"753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1\""))
        assertTrue(h.contains("nc=00000001"))
        assertTrue(h.contains("opaque=\"FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS\""))
    }

    /** RFC 7616 aynı örneğin MD5 hâli. */
    @Test
    fun rfc7616Md5Vector() {
        val ch = ShellyAuth.parseChallenge(
            """Digest realm="http-auth@example.org", qop="auth, auth-int", algorithm=MD5, nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v", opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS""""
        )!!
        val h = ShellyAuth.digestHeader(
            ShellyAuth.Credentials("Mufasa", "Circle of Life"), ch, "GET", "/dir/index.html", 1,
            "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ",
        )
        assertTrue(h, h.contains("response=\"8ca523f5e9506fed4657c9700eebdbec\""))
    }

    /** Shelly Gen2'nin gönderdiği biçim (tırnaksız algorithm, realm = cihaz kimliği). */
    @Test
    fun shellyChallenge() {
        val ch = ShellyAuth.parseChallenge("""Digest qop="auth", realm="shellyplus1pm-a8032ab12345", nonce="1700000000", algorithm=SHA-256""")!!
        assertEquals("shellyplus1pm-a8032ab12345", ch.realm)
        assertEquals("1700000000", ch.nonce)
        assertNull(ch.opaque)
        assertNull(ShellyAuth.parseChallenge("Basic realm=\"x\""))
        assertNull(ShellyAuth.parseChallenge(null))
    }

    @Test
    fun basic() {
        // RFC 7617 örneği
        assertEquals("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==", ShellyAuth.basicHeader(ShellyAuth.Credentials("Aladdin", "open sesame")))
    }

    @Test
    fun jsonRoundTrip() {
        val m = mapOf("shelly-a" to ShellyAuth.Credentials("admin", "p\"ş1"), "shelly-b" to ShellyAuth.Credentials("ben", ""))
        assertEquals(m, ShellyAuth.mapFromJson(ShellyAuth.mapToJson(m)))
        assertEquals(emptyMap<String, ShellyAuth.Credentials>(), ShellyAuth.mapFromJson("bozuk"))
    }
}
