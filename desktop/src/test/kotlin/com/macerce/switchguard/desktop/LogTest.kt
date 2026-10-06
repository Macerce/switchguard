package com.macerce.switchguard.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

class LogTest {
    @get:Rule val tmp = TemporaryFolder()

    private val logs get() = tmp.root.toPath().resolve("logs")

    @Test
    fun writesUtf8LinesWithLevelTagAndStackTrace() {
        Log.init(tmp.root.toPath())
        Log.i("Tag", "bağlandı ✓")
        Log.w("Tag", "koptu", IllegalStateException("boom"))

        val text = String(Files.readAllBytes(logs.resolve("switchguard.log")), UTF_8)
        assertTrue(text, text.contains("I/Tag: bağlandı ✓"))
        assertTrue(text, text.contains("W/Tag: koptu"))
        assertTrue(text, text.contains("java.lang.IllegalStateException: boom"))
    }

    @Test
    fun rotatesAtFiveMegabytesKeepingThreeFiles() {
        Log.init(tmp.root.toPath())
        val chunk = "x".repeat(1024 * 1024)
        repeat(16) { Log.i("Big", chunk) }

        val names = Files.list(logs).use { st -> st.map { it.fileName.toString() }.toList() }.sorted()
        assertEquals(listOf("switchguard.log", "switchguard.log.1", "switchguard.log.2"), names)
        for (n in names) assertTrue(n, Files.size(logs.resolve(n)) <= 5L * 1024 * 1024)
        assertFalse(Files.exists(logs.resolve("switchguard.log.3")))
    }
}
