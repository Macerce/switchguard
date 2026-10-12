package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {
    private val meta = CrashReport.Meta(appVersion = "2.10.0", versionCode = 22, android = "16", sdk = 36, device = "Xiaomi 21081111RG")

    @Test
    fun `kayit surum, cihaz ve yigini icerir`() {
        val text = CrashReport.format(meta, time = "2026-10-12T09:00:00Z", thread = "monitor", stack = "java.lang.IllegalStateException: x\n\tat a.b(C.kt:1)")
        assertTrue(text.contains("SwitchGuard 2.10.0 (22)"))
        assertTrue(text.contains("Android 16 (API 36)"))
        assertTrue(text.contains("Xiaomi 21081111RG"))
        assertTrue(text.contains("Thread: monitor"))
        assertTrue(text.endsWith("\tat a.b(C.kt:1)\n"))
    }

    @Test
    fun `en yeni bes kayit kalir, eskiler silinir`() {
        val names = (1..8).map { "crash-%013d.txt".format(it * 1000L) }.shuffled()
        assertEquals((1..3).map { "crash-%013d.txt".format(it * 1000L) }, CrashReport.toDelete(names, keep = 5))
        assertTrue(CrashReport.toDelete(names.take(3), keep = 5).isEmpty())
    }

    @Test
    fun `e-posta metni en yeniden baslar ve sinirda kesilir`() {
        val body = CrashReport.body(meta, diagnostics = listOf("monitoring=true"), reports = listOf("ESKI", "YENI"), maxChars = 10_000)
        assertTrue(body.indexOf("YENI") < body.indexOf("ESKI"))
        assertTrue(body.contains("monitoring=true"))
        val long = CrashReport.body(meta, emptyList(), listOf("x".repeat(50_000)), maxChars = 2_000)
        assertTrue(long.length <= 2_000)
    }

    @Test
    fun `rapor yoksa da tanilama gonderilebilir`() {
        val body = CrashReport.body(meta, diagnostics = listOf("state=LIVE"), reports = emptyList(), maxChars = 5_000)
        assertTrue(body.contains("state=LIVE"))
        assertTrue(body.contains("2.10.0"))
    }
}
