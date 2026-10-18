package com.macerce.switchguard.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretValuesTest {
    /** Sahte şifreleyici: ters çevirip biçime sarar; "bozuk" içeriği çözemez (Keystore anahtarı kaybolmuş gibi). */
    private var decrypts = 0
    private val encrypt = { plain: String -> SecretFormat.seal(byteArrayOf(1, 2, 3), plain.reversed().toByteArray()) }
    private val decrypt = { sealed: String ->
        decrypts++
        SecretFormat.parse(sealed)?.let { (_, ct) -> String(ct).takeIf { it != "kuzob" }?.reversed() }
    }
    private val values = SecretValues(encrypt, decrypt)

    @Test
    fun `bicim gidip gelir, bozuk bicim taninmaz`() {
        val s = SecretFormat.seal(byteArrayOf(9, 8), byteArrayOf(7, 6, 5))
        assertTrue(SecretFormat.isSealed(s))
        val (iv, ct) = SecretFormat.parse(s)!!
        assertArrayEquals(byteArrayOf(9, 8), iv)
        assertArrayEquals(byteArrayOf(7, 6, 5), ct)
        assertFalse(SecretFormat.isSealed("düz-değer"))
        assertNull(SecretFormat.parse("enc1:yok"))
    }

    @Test
    fun `yazilan deger sifreli saklanir ve okunur`() {
        val sealed = values.write("gizli")
        assertTrue(SecretFormat.isSealed(sealed))
        assertFalse(sealed.contains("gizli"))
        assertEquals("gizli", values.read(sealed) {})
    }

    @Test
    fun `eski duz deger okunur ve sifrelenip geri yazilir`() {
        var rewritten: String? = null
        assertEquals("eski-token", values.read("eski-token") { rewritten = it })
        assertTrue(SecretFormat.isSealed(rewritten!!))
        assertEquals("eski-token", values.read(rewritten) {})
    }

    @Test
    fun `cozulemeyen deger bos sayilir, uygulama cokmez`() {
        assertEquals("", values.read(SecretFormat.seal(byteArrayOf(1), "kuzob".toByteArray())) {})
        assertEquals("", values.read("") {})
    }

    @Test
    fun `ayni sifreli deger bir kez cozulur`() {
        val sealed = values.write("token")
        // Yeni süreç: önbellek boş, saklanan değer şifreli.
        val fresh = SecretValues(encrypt, decrypt)
        decrypts = 0
        repeat(5) { assertEquals("token", fresh.read(sealed) {}) }
        assertEquals(1, decrypts)
    }

    @Test
    fun `sifreleme yapilamazsa deger duz kalir`() {
        val failing = SecretValues(encrypt = { null }, decrypt = { null })
        assertEquals("x", failing.write("x"))
        var rewritten = false
        assertEquals("x", failing.read("x") { rewritten = true })
        assertFalse(rewritten)
    }

    @Test
    fun `sifreli hali cozulemiyorsa duz deger yerine yazilmaz`() {
        val noDecrypt = SecretValues(encrypt, decrypt = { null })
        var rewritten = false
        assertEquals("tek-kopya", noDecrypt.read("tek-kopya") { rewritten = true })
        assertFalse(rewritten)
    }

    @Test
    fun `cozulemeyen deger bir kez denenir ve okunamaz isaretlenir`() {
        var tries = 0
        val broken = SecretValues(encrypt, decrypt = { tries++; null })
        val sealed = SecretFormat.seal(byteArrayOf(1), byteArrayOf(2))
        assertEquals("", broken.read(sealed) {})
        assertEquals("", broken.read(sealed) {})
        assertEquals(1, tries)
        assertTrue(broken.unreadable)
        assertFalse(values.unreadable)
    }

    @Test
    fun `sifreleme hic calismiyorsa tekrar denenmez`() {
        var tries = 0
        val noKeystore = SecretValues(encrypt = { tries++; null }, decrypt = { null })
        repeat(3) { assertEquals("x", noKeystore.read("x") {}) }
        assertEquals("y", noKeystore.write("y"))
        assertEquals(1, tries)
    }
}
