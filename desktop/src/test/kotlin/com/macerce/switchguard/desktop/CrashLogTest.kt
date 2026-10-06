package com.macerce.switchguard.desktop

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import kotlin.concurrent.thread

class CrashLogTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun uncaughtExceptionsOfAnyThreadAreLogged() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        try {
            Log.init(tmp.root.toPath())
            CrashLog.install()

            thread(name = "worker-x") { throw IllegalStateException("kaboom") }.join()

            val text = String(Files.readAllBytes(tmp.root.toPath().resolve("logs").resolve("switchguard.log")), UTF_8)
            assertTrue(text, text.contains("W/Crash: uncaught exception in thread 'worker-x'"))
            assertTrue(text, text.contains("java.lang.IllegalStateException: kaboom"))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
    }
}
