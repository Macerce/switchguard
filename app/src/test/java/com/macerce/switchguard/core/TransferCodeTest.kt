package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

class TransferCodeTest {
    @Test
    fun codeHasTwelveUnambiguousCharacters() {
        repeat(200) {
            val code = Transfer.newCode()
            assertEquals(12, code.length)
            assertTrue(Transfer.isValidCode(code))
            assertTrue(code.none { it in "01OIL" })
        }
    }

    @Test
    fun codeIsStrongEnoughForOfflineGuessing() {
        val bits = Transfer.CODE_LENGTH * ln(Transfer.CODE_ALPHABET.length.toDouble()) / ln(2.0)
        assertTrue("≈$bits bit", bits >= 50)
    }

    @Test
    fun codeIsShownInGroupsOfFour() {
        assertEquals("ABCD-EFGH-JKMN", Transfer.formatCode("ABCDEFGHJKMN"))
    }

    @Test
    fun typedCodeIsNormalized() {
        assertEquals("ABCDEFGHJKMN", Transfer.normalizeCode("abcd-efgh jkmn"))
        assertTrue(Transfer.isValidCode(Transfer.normalizeCode("abcd efgh jkmn")))
        assertFalse(Transfer.isValidCode("ABCD-EFGH"))
        assertFalse(Transfer.isValidCode("ABCDEFGHJKM0"))
    }

    @Test
    fun oldSixDigitCodeIsRecognized() {
        assertTrue(Transfer.looksLikeOldCode("123 456"))
        assertTrue(Transfer.looksLikeOldCode("123456"))
        assertFalse(Transfer.looksLikeOldCode("ABCD"))
        assertFalse(Transfer.looksLikeOldCode("1234567"))
    }

    @Test
    fun versionIsTwo() {
        assertEquals(2, Transfer.VERSION)
    }
}
