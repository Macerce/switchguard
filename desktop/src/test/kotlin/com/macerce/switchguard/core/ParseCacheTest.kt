package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ParseCacheTest {
    @Test
    fun `ayni ham metin bir kez cozulur, degisince yeniden`() {
        val cache = ParseCache()
        var parses = 0
        val parse = { raw: String? -> parses++; raw.orEmpty().split(',') }
        val a = cache.get("k", "1,2", parse)
        assertSame(a, cache.get("k", "1,2", parse))
        assertEquals(1, parses)
        assertEquals(listOf("3"), cache.get("k", "3", parse))
        assertEquals(2, parses)
    }

    @Test
    fun `anahtarlar birbirini etkilemez, bos deger de onbelleklenir`() {
        val cache = ParseCache()
        var parses = 0
        val parse = { raw: String? -> parses++; raw ?: "yok" }
        assertEquals("yok", cache.get("a", null, parse))
        assertEquals("yok", cache.get("a", null, parse))
        assertEquals("x", cache.get("b", "x", parse))
        assertEquals(2, parses)
    }
}
