package com.macerce.switchguard.data

import android.content.Context
import android.os.Build
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.core.CrashReport
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Çökmeleri uygulamanın özel klasörüne yazar (en yeni [CrashReport.KEEP] tanesi kalır). Önceki yakalayıcı
 * yine çağrılır: sistem uygulamayı kapatır ve Play Console'un çökme istatistikleri etkilenmez.
 */
object CrashLog {
    private fun dir(context: Context) = File(context.filesDir, "crashes").apply { mkdirs() }

    fun meta() = CrashReport.Meta(
        appVersion = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        android = Build.VERSION.RELEASE,
        sdk = Build.VERSION.SDK_INT,
        device = "${Build.MANUFACTURER} ${Build.MODEL}",
    )

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread.name, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun write(context: Context, thread: String, error: Throwable) {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val now = System.currentTimeMillis()
        File(dir(context), "crash-%013d.txt".format(now))
            .writeText(CrashReport.format(meta(), Instant.ofEpochMilli(now).toString(), thread, stack))
        val files = dir(context).listFiles().orEmpty().associateBy { it.name }
        CrashReport.toDelete(files.keys.toList()).forEach { files[it]?.delete() }
    }

    /** Kayıtlı çökmeler, eskiden yeniye. */
    fun reports(context: Context): List<String> =
        dir(context).listFiles().orEmpty().sortedBy { it.name }.mapNotNull { runCatching { it.readText() }.getOrNull() }

    fun count(context: Context): Int = dir(context).listFiles()?.size ?: 0

    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}
