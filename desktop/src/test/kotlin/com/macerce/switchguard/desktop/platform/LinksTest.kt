package com.macerce.switchguard.desktop.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.URI

class LinksTest {
    @Test
    fun guideUrlFollowsTheLanguage() {
        assertEquals("https://github.com/Macerce/switchguard/blob/master/docs/guide/tuya.tr.md", Links.guide("tuya", "tr"))
        assertEquals("https://github.com/Macerce/switchguard/blob/master/docs/guide/ewelink.en.md", Links.guide("ewelink", "en"))
        assertEquals("https://github.com/Macerce/switchguard/blob/master/docs/guide/tuya.en.md", Links.guide("tuya", "de"))
    }

    @Test
    fun openedLinkIsNotCopied() {
        val opened = mutableListOf<URI>()
        val copied = mutableListOf<String>()

        assertTrue(Links.open("https://dev.ewelink.cc", browse = { opened += it }, copy = { copied += it }))

        assertEquals(listOf(URI("https://dev.ewelink.cc")), opened)
        assertTrue(copied.isEmpty())
    }

    @Test
    fun failedBrowseCopiesTheAddress() {
        val copied = mutableListOf<String>()

        val ok = Links.open("https://platform.tuya.com/cloud", browse = { throw IOException("no browser") }, copy = { copied += it })

        assertFalse(ok)
        assertEquals(listOf("https://platform.tuya.com/cloud"), copied)
    }
}
