package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import com.macerce.switchguard.desktop.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

@OptIn(ExperimentalComposeUiApi::class)
class WindowErrorsTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun logText() = String(Files.readAllBytes(tmp.root.toPath().resolve("logs").resolve("switchguard.log")), UTF_8)

    @Test
    fun windowErrorIsLoggedAndReportedNeverRethrown() {
        Log.init(tmp.root.toPath())
        val seen = mutableListOf<Throwable>()
        val handler = windowErrorHandler("main") { seen += it }
        val boom = IllegalStateException("bad screen")

        handler.onException(boom) // fırlatmamalı: fırlatırsa varsayılan gibi süreci/pencereyi düşürür

        assertEquals(1, seen.size)
        assertSame(boom, seen[0])
        val text = logText()
        assertTrue(text, text.contains("W/WindowErrors: main window failed"))
        assertTrue(text, text.contains("java.lang.IllegalStateException: bad screen"))
    }

    @Test
    fun failingRecoveryIsAlsoContained() {
        Log.init(tmp.root.toPath())
        val handler = windowErrorHandler("alarm") { throw IllegalArgumentException("recovery broke") }

        handler.onException(IllegalStateException("first"))

        assertTrue(logText().contains("recovery broke"))
    }

    @Test
    fun aWindowFailingEveryFrameDoesNotFloodTheLog() {
        Log.init(tmp.root.toPath())
        val handler = windowErrorHandler("main") {}

        repeat(500) { handler.onException(IllegalStateException("frame $it")) }

        val logged = Regex("main window failed").findAll(logText()).count()
        assertTrue("logged $logged times", logged in 1..10)
    }
}
