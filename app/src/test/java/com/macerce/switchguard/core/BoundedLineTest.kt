package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class BoundedLineTest {
    private fun read(text: String, max: Int = 100) = BoundedLine.read(ByteArrayInputStream(text.toByteArray()), max)

    @Test
    fun `ilk satiri okur`() {
        assertEquals("""{"ok":true}""", read("{\"ok\":true}\nsonraki"))
        assertEquals("abc", read("abc\r\n"))
        assertEquals("çğü", read("çğü\n"))
    }

    @Test
    fun `satir sonu olmadan biten veri de okunur`() = assertEquals("abc", read("abc"))

    @Test
    fun `bos baglanti null`() = assertNull(read(""))

    @Test
    fun `sinir tam dolabilir ama asilamaz`() {
        assertEquals("x".repeat(10), read("x".repeat(10) + "\n", max = 10))
        assertNull(read("x".repeat(11) + "\n", max = 10))
    }

    @Test
    fun `bitmeyen satir sinirda kesilir`() {
        var served = 0L
        val endless = object : InputStream() {
            override fun read(): Int { served++; return 'x'.code }
        }
        assertNull(BoundedLine.read(endless, 1024))
        assertEquals(1025L, served)
    }
}
